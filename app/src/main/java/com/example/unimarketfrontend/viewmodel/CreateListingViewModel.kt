package com.example.unimarketfrontend.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.network.external.CloudinaryUploadService
import com.example.unimarketfrontend.model.uploads.CloudinarySignatureRequest
import com.example.unimarketfrontend.model.listing.CreateListingRequest
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.repository.ListingRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

sealed class CreateListingState {
    object Idle : CreateListingState()
    object CreatingListing : CreateListingState()
    data class UploadingImages(val uploaded: Int, val total: Int, val retryMessage: String? = null) : CreateListingState()
    object Success : CreateListingState()
    object PendingRetry : CreateListingState()
    data class Error(val message: String) : CreateListingState()
}

data class SmartSuggestion(
    val category: String,
    val price: Double,
    val description: String
)

data class DraftListing(
    val title: String,
    val description: String,
    val price: String,
    val category: String,
    val imageUris: List<String>
)

class CreateListingViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository = ListingRepository(
        listingDao = AppDatabase.getInstance(application).listingDao()
    )

    private val prefs = application.getSharedPreferences("listing_draft", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<CreateListingState>(CreateListingState.Idle)
    val state: StateFlow<CreateListingState> = _state

    fun saveDraft(title: String, description: String, price: String, category: String, uris: List<Uri>) {
        prefs.edit().apply {
            putString("title", title)
            putString("description", description)
            putString("price", price)
            putString("category", category)
            putString("uris", uris.joinToString(",") { it.toString() })
            apply()
        }
    }

    fun loadDraft(): DraftListing? {
        val title = prefs.getString("title", "") ?: ""
        val description = prefs.getString("description", "") ?: ""
        val price = prefs.getString("price", "") ?: ""
        val category = prefs.getString("category", "Books") ?: "Books"
        val urisRaw = prefs.getString("uris", "") ?: ""

        if (title.isBlank() && description.isBlank() && price.isBlank() && urisRaw.isBlank()) {
            return null
        }

        val uris = if (urisRaw.isNotEmpty()) urisRaw.split(",") else emptyList()
        return DraftListing(title, description, price, category, uris)
    }

    fun clearDraft() {
        prefs.edit().clear().apply()
    }

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

    private suspend fun compressImage(context: Context, uri: Uri): Uri = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            var scale = 1
            while (options.outWidth / scale / 2 >= 1024 && options.outHeight / scale / 2 >= 1024) {
                scale *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@withContext uri

            val file = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            bitmap.recycle()

            Uri.fromFile(file)
        } catch (e: Exception) {
            uri
        }
    }

    private fun deleteTempFile(uri: Uri) {
        if (uri.scheme == "file") {
            uri.path?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
    }

    fun createListing(request: CreateListingRequest, imageUris: List<Uri>) {
        if (_state.value is CreateListingState.CreatingListing || _state.value is CreateListingState.UploadingImages) return

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
                    handleFailureAsPending()
                    return@launch
                }

                clearDraft()
                _state.value = CreateListingState.Success

            } catch (e: Exception) {
                handleFailureAsPending()
            }
        }
    }

    private fun handleFailureAsPending() {
        _state.value = CreateListingState.PendingRetry
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
        val applicationContext = getApplication<Application>()
        val contentResolver = applicationContext.contentResolver

        val completedUploads = AtomicInteger(0)
        _state.value = CreateListingState.UploadingImages(0, imageUris.size)

        val deferredUploads = imageUris.mapIndexed { index, uri ->
            async(Dispatchers.IO) {
                val compressedUri = compressImage(applicationContext, uri)

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
                        imageUri = compressedUri,
                        signature = signature
                    )
                }

                deleteTempFile(compressedUri)

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