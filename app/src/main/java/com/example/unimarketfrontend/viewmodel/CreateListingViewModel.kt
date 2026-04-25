package com.example.unimarketfrontend.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.network.external.CloudinaryUploadService
import com.example.unimarketfrontend.model.uploads.CloudinarySignatureRequest
import com.example.unimarketfrontend.model.listing.CreateListingRequest
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.repository.ListingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger

sealed class CreateListingState {
    object Idle : CreateListingState()
    object CreatingListing : CreateListingState()
    data class UploadingImages(val uploaded: Int, val total: Int, val retryMessage: String? = null) : CreateListingState()
    object Success : CreateListingState()
    data class Error(val message: String) : CreateListingState()
}

data class SmartSuggestion(
    val category: String,
    val price: Double,
    val description: String
)

class CreateListingViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository = ListingRepository(
        listingDao = AppDatabase.getInstance(application).listingDao()
    )

    private val _state =
        MutableStateFlow<CreateListingState>(CreateListingState.Idle)

    val state: StateFlow<CreateListingState> = _state

    private fun buildHttpError(context: String, response: Response<*>): String {
        val errorBody = try {
            response.errorBody()?.string()?.take(300)
        } catch (_: Exception) {
            null
        }

        val details = buildString {
            append("$context failed")
            append(" (HTTP ${response.code()})")
            if (!response.message().isNullOrBlank()) {
                append(": ${response.message()}")
            }
            if (!errorBody.isNullOrBlank()) {
                append(" - $errorBody")
            }
        }

        return details
    }

    fun createListing(request: CreateListingRequest, imageUris: List<Uri>) {
        viewModelScope.launch {
            _state.value = CreateListingState.CreatingListing

            try {

                val imageUrls = if (imageUris.isNotEmpty()) {
                    uploadImagesToCloudinary(imageUris)
                } else {
                    emptyList()
                }


                val requestWithImages = request.copy(
                    images = imageUrls.mapIndexed { index, url ->
                        mapOf(
                            "url" to url,
                            "is_primary" to (index == 0)
                        )
                    }
                )

                val createResponse: Response<Listing> = listingRepository.createListing(requestWithImages)

                if (!createResponse.isSuccessful) {
                    _state.value = CreateListingState.Error(
                        buildHttpError("Listing creation", createResponse)
                    )
                    return@launch
                }

                val listing = createResponse.body()
                if (listing == null) {
                    _state.value = CreateListingState.Error("Invalid response when creating listing")
                    return@launch
                }


                _state.value = CreateListingState.Success

            } catch (e: Exception) {
                val userErrorMessage = when (e) {
                    is java.net.UnknownHostException -> "No internet connection. Please check your network."
                    is java.util.concurrent.TimeoutException -> "Upload took too long. Please try again."
                    else -> e.message ?: "An unexpected error occurred"
                }
                _state.value = CreateListingState.Error(userErrorMessage)
            }
        }
    }

    private suspend fun uploadImagesToCloudinary(imageUris: List<Uri>): List<String> = coroutineScope {
        val signatureResponse = withContext(Dispatchers.IO) {
            listingRepository.getCloudinarySignature(
                CloudinarySignatureRequest(listing_id = 0)
            )
        }

        if (!signatureResponse.isSuccessful || signatureResponse.body() == null) {
            throw IllegalStateException(
                buildHttpError("Secure upload signature", signatureResponse)
            )
        }

        val signature = signatureResponse.body()!!
        val contentResolver = getApplication<Application>().contentResolver

        val completedUploads = AtomicInteger(0)
        _state.value = CreateListingState.UploadingImages(0, imageUris.size)

        val deferredUploads = imageUris.mapIndexed { index, uri ->
            async(Dispatchers.IO) {
                val imageUrl: String = retryIO(
                    times = 3,
                    onRetry = { attempt ->
                        _state.value = CreateListingState.UploadingImages(
                            completedUploads.get(),
                            imageUris.size,
                            "Photo ${index + 1}: Attempt $attempt/3..."
                        )
                    }
                ) {
                    CloudinaryUploadService.uploadImage(
                        contentResolver = contentResolver,
                        imageUri = uri,
                        signature = signature
                    )
                }
                val currentCompleted = completedUploads.incrementAndGet()
                _state.value = CreateListingState.UploadingImages(currentCompleted, imageUris.size, null)
                imageUrl
            }
        }

        return@coroutineScope deferredUploads.awaitAll()
    }

    private suspend fun <T> retryIO(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 5000,
        factor: Double = 2.0,
        onRetry: (Int) -> Unit = {},
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                onRetry(attempt + 1)
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block()
    }

    fun suggestFromText(title: String): SmartSuggestion {
        val lower = title.lowercase()

        return when {
            lower.contains("calculator") || lower.contains("ti-84") -> {
                SmartSuggestion(
                    category = "Electronics",
                    price = 50000.0,
                    description = "Scientific calculator in good condition"
                )
            }

            lower.contains("book") || lower.contains("calculus") -> {
                SmartSuggestion(
                    category = "Books",
                    price = 30000.0,
                    description = "Academic textbook used for courses"
                )
            }

            lower.contains("notes") -> {
                SmartSuggestion(
                    category = "Notes",
                    price = 10000.0,
                    description = "Well-organized study notes"
                )
            }

            lower.contains("furniture") -> {
                SmartSuggestion(
                    category = "Furniture",
                    price = 100000.0,
                    description = "Comfortable and spacious furniture"
                )
            }

            else -> {
                SmartSuggestion(
                    category = "Other",
                    price = 0.0,
                    description = "Good condition item for university use."
                )
            }
        }
    }
}