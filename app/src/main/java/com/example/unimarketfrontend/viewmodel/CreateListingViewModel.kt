package com.example.unimarketfrontend.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.network.external.CloudinaryUploadService
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.uploads.CloudinarySignatureRequest
import com.example.unimarketfrontend.model.listing.CreateListingRequest
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.repository.ListingRepository
import com.example.unimarketfrontend.model.utils.ErrorTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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

data class CourseOption(
    val id: Int,
    val name: String
)

class CreateListingViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository = ListingRepository(
        listingDao = AppDatabase.getInstance(application).listingDao()
    )

    private val _state =
        MutableStateFlow<CreateListingState>(CreateListingState.Idle)

    val state: StateFlow<CreateListingState> = _state

    private val _listingCreated = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val listingCreated: SharedFlow<Int> = _listingCreated

    private val _courses = MutableStateFlow(
        listOf(
            CourseOption(101, "Matematicas I"),
            CourseOption(102, "Calculo"),
            CourseOption(103, "Fisica"),
            CourseOption(104, "Programacion"),
            CourseOption(105, "Electronica")
        )
    )
    val courses: StateFlow<List<CourseOption>> = _courses

    private val _selectedCourseId = MutableStateFlow<Int?>(null)
    val selectedCourseId: StateFlow<Int?> = _selectedCourseId

    fun setSelectedCourse(courseId: Int?) {
        _selectedCourseId.value = courseId
    }

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
                    course_id = request.course_id ?: _selectedCourseId.value,
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

                // Persistimos el listing en cache local para que aparezca en listados al volver.
                listingRepository.cacheRemoteListings(listOf(listing))
                _listingCreated.tryEmit(listing.id)
                _state.value = CreateListingState.Success

            } catch (e: Exception) {
                val userMessage = ErrorTranslator.getUserFriendlyMessage(e)
                _state.value = CreateListingState.Error(userMessage)
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