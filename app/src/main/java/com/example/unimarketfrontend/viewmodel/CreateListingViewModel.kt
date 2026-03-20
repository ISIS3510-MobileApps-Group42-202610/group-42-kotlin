package com.example.unimarketfrontend.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.network.CloudinaryUploadService
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.CloudinarySignatureRequest
import com.example.unimarketfrontend.network.model.CreateListingRequest
import com.example.unimarketfrontend.network.model.Listing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed class CreateListingState {
    object Idle : CreateListingState()
    object CreatingListing : CreateListingState()
    data class UploadingImages(val uploaded: Int, val total: Int) : CreateListingState()
    object Success : CreateListingState()
    data class Error(val message: String) : CreateListingState()
}

data class SmartSuggestion(
    val category: String,
    val price: Double,
    val description: String
)

class CreateListingViewModel(application: Application) : AndroidViewModel(application) {

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
            append("$context fallida")
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

                val createResponse: Response<Listing> = RetrofitInstance.api.createListing(requestWithImages)

                if (!createResponse.isSuccessful) {
                    _state.value = CreateListingState.Error(
                        buildHttpError("Creacion de listing", createResponse)
                    )
                    return@launch
                }

                val listing = createResponse.body()
                if (listing == null) {
                    _state.value = CreateListingState.Error("Respuesta invalida al crear el listing")
                    return@launch
                }


                _state.value = CreateListingState.Success

            } catch (e: Exception) {
                _state.value = CreateListingState.Error(e.message ?: "Network error")
            }
        }
    }

    private suspend fun uploadImagesToCloudinary(imageUris: List<Uri>): List<String> {
        val signatureResponse = withContext(Dispatchers.IO) {
            RetrofitInstance.api.getCloudinarySignature(
                CloudinarySignatureRequest(listing_id = 0)  // dummy value para obtener firma
            )
        }

        if (!signatureResponse.isSuccessful || signatureResponse.body() == null) {
            throw IllegalStateException(
                buildHttpError("Firma de subida segura", signatureResponse)
            )
        }

        val signature = signatureResponse.body()!!
        val contentResolver = getApplication<Application>().contentResolver
        val uploadedUrls = mutableListOf<String>()

        imageUris.forEachIndexed { index, uri ->
            _state.value = CreateListingState.UploadingImages(index, imageUris.size)

            val imageUrl: String = withContext(Dispatchers.IO) {
                CloudinaryUploadService.uploadImage(
                    contentResolver = contentResolver,
                    imageUri = uri,
                    signature = signature
                )
            }

            uploadedUrls.add(imageUrl)
            _state.value = CreateListingState.UploadingImages(index + 1, imageUris.size)
        }

        return uploadedUrls
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
                    price = 20000.0,
                    description = "Good condition item for university use."
                )
            }
        }
    }
}