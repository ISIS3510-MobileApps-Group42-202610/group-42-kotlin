package com.example.unimarketfrontend.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.BuildConfig
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.network.external.CloudinaryUploadService
import com.example.unimarketfrontend.model.uploads.CloudinarySignatureRequest
import com.example.unimarketfrontend.model.listing.CreateListingRequest
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.repository.ListingRepository
import com.example.unimarketfrontend.model.repository.CourseRepository
import com.example.unimarketfrontend.model.repository.CourseResult
import com.example.unimarketfrontend.model.utils.ErrorTranslator
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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

data class CourseOption(
    val id: Int,
    val code: String,
    val name: String,
    val faculty: String,
    val departmentCode: String
) {
    val displayLabel: String = "$code - $name"
}

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

    private val courseRepository = CourseRepository(
        courseDao = AppDatabase.getInstance(application).courseDao()
    )

    init {
        loadCourses()
    }

    private val prefs = application.getSharedPreferences("listing_draft", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<CreateListingState>(CreateListingState.Idle)
    val state: StateFlow<CreateListingState> = _state

    private val _listingCreated = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val listingCreated: SharedFlow<Int> = _listingCreated

    private val _courses = MutableStateFlow<List<CourseOption>>(emptyList())
    val courses: StateFlow<List<CourseOption>> = _courses

    private val _courseLoading = MutableStateFlow(false)
    val courseLoading: StateFlow<Boolean> = _courseLoading

    private val _courseLoadError = MutableStateFlow<String?>(null)
    val courseLoadError: StateFlow<String?> = _courseLoadError

    private val _selectedCourseId = MutableStateFlow<Int?>(null)
    val selectedCourseId: StateFlow<Int?> = _selectedCourseId


    private val httpClient = OkHttpClient()
    private val aiSuggestionsCache = mutableMapOf<String, SmartSuggestion>()

    private val _aiSuggestion = MutableStateFlow<SmartSuggestion?>(null)
    val aiSuggestion: StateFlow<SmartSuggestion?> = _aiSuggestion

    private val _isLoadingAI = MutableStateFlow(false)
    val isLoadingAI: StateFlow<Boolean> = _isLoadingAI

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage

    var usedSuggestedPrice: Boolean = false

    fun setSelectedCourse(courseId: Int?) {
        _selectedCourseId.value = courseId
        if (courseId != null) {
            val course = _courses.value.find { it.id == courseId }
            BusinessAnalyticsProvider.tracker.trackEvent(
                eventName = "create_listing_course_selected",
                params = mapOf(
                    "course_id" to courseId.toString(),
                    "course_code" to course?.code,
                    "course_name" to course?.name,
                    "faculty" to course?.faculty,
                    "department_code" to course?.departmentCode,
                    "source_screen" to "CreateListing"
                )
            )
        }
    }

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
        val category = prefs.getString("category", "Textbooks") ?: "Textbooks"
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

        return buildString {
            append("$context failed")
            append(" (HTTP ${response.code()})")
            if (!response.message().isNullOrBlank()) {
                append(": ${response.message()}")
            }
            if (!errorBody.isNullOrBlank()) {
                append(" - $errorBody")
            }
        }
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
        } catch (_: Exception) {
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
        if (_state.value is CreateListingState.CreatingListing || _state.value is CreateListingState.UploadingImages) {
            return
        }

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

                val createResponse: Response<Listing> = listingRepository.createListing(requestWithImages)

                if (!createResponse.isSuccessful) {
                    handleFailureAsPending()
                    return@launch
                }

                val listing = createResponse.body()
                if (listing == null) {
                    _state.value = CreateListingState.Error("Respuesta invalida al crear el listing")
                    return@launch
                }

                listingRepository.cacheRemoteListings(listOf(listing))

                val courseId = listing.course_id ?: _selectedCourseId.value
                val course = courseId?.let { id -> _courses.value.find { it.id == id } }
                val fallbackCourseCode = if (courseId == null) "OTHER" else course?.code
                val fallbackCourseName = if (courseId == null) "Other / Optional course" else course?.name
                val fallbackDepartmentCode = if (courseId == null) "" else course?.departmentCode
                val fallbackFaculty = if (courseId == null) null else course?.faculty
                BusinessAnalyticsProvider.tracker.trackEvent(
                    eventName = "listing_created",
                    params = mapOf(
                        "listing_id" to listing.id.toString(),
                        "course_id" to courseId?.toString(),
                        "course_code" to fallbackCourseCode,
                        "course_name" to fallbackCourseName,
                        "faculty" to fallbackFaculty,
                        "department_code" to fallbackDepartmentCode,
                        "category" to listing.category,
                        "condition" to listing.condition,
                        "source_screen" to "CreateListing"
                    )
                )

                clearDraft()

                logBQ7SuggestionEvent(listing.id)

                _listingCreated.tryEmit(listing.id)
                _state.value = CreateListingState.Success

            } catch (e: IOException) {
                handleFailureAsPending()
            } catch (e: Exception) {
                val userMessage = ErrorTranslator.getUserFriendlyMessage(e)
                _state.value = CreateListingState.Error(userMessage)
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
                _state.value = CreateListingState.UploadingImages(
                    currentCompleted,
                    imageUris.size,
                    null
                )

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
            } catch (_: Exception) {
                onRetry(attempt + 1)
            }

            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }

        return block()
    }


    fun requestAISuggestion(title: String) {

        val cacheKey = title.lowercase().trim()


        if (aiSuggestionsCache.containsKey(cacheKey)) {
            _aiSuggestion.value = aiSuggestionsCache[cacheKey]
            _aiErrorMessage.value = null
            return
        }

        viewModelScope.launch {
            _isLoadingAI.value = true
            _aiErrorMessage.value = null
            try {
                val suggestion = withContext(Dispatchers.IO) {
                    callGeminiAPI(title)
                }
                if (suggestion != null) {
                    aiSuggestionsCache[cacheKey] = suggestion
                    _aiSuggestion.value = suggestion
                } else {
                    _aiErrorMessage.value = "Could not connect to Gemini API. Default suggestion applied."
                }
            } catch (e: Exception) {
                Log.e("GeminiAPI", "Exception in requestAISuggestion: ${e.message}", e)

                _aiErrorMessage.value = "Error connecting to AI. Default suggestion applied."
            } finally {
                _isLoadingAI.value = false
            }
        }
    }

    private fun callGeminiAPI(title: String): SmartSuggestion? {
        val geminiApiKey = BuildConfig.GEMINI_API_KEY

        val prompt = """
            You are a pricing assistant for UniMarket, a university marketplace in Colombia where students buy and sell used academic items.
            Given this product listing title: "$title"
            Respond ONLY with a JSON object, no markdown, no explanation:
            {
              "category": "one of [Books, Electronics, Notes, Furniture, Other]",
              "price": suggested price in COP as a number,
              "description": "a short 1-sentence product description in Spanish"
            }
        """.trimIndent()

        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }.toString()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=$geminiApiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .build()

        Log.i("GeminiAPI", "Llamando el API");
        return try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return null

            if (!response.isSuccessful) {
                Log.e("GeminiAPI", "HTTP Error ${response.code}: $responseBody")
                return null
            }

            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.has("error")) {
                Log.e("GeminiAPI", "API Error: ${jsonResponse.getJSONObject("error").getString("message")}")
                return null
            }

            val text = jsonResponse.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "{}")?.trim() ?: "{}"

            val cleanText = text.replace("```json", "", ignoreCase = true).replace("```", "").trim()
            val result = JSONObject(cleanText)

            Log.i("GeminiAPI","Result is $result")
            SmartSuggestion(
                category = result.optString("category", "Other"),
                price = result.optDouble("price", 0.0),
                description = result.optString("description", "")
            )
        } catch (e: Exception) {
            Log.e("GeminiAPI", "Parsing error in callGeminiAPI: ${e.message}", e)
            null
        }
    }

    fun markSuggestionAsAccepted(accepted: Boolean) {
        this.usedSuggestedPrice = accepted
    }

    private fun logBQ7SuggestionEvent(listingId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                BusinessAnalyticsProvider.tracker.trackListingCreated(
                    listingId = listingId,
                    sellerId = null,
                    metadata = mapOf(
                        "bq_type" to "BQ7",
                        "price_suggestion_accepted" to usedSuggestedPrice,
                        "suggestion_status" to if (usedSuggestedPrice) "ACCEPTED" else "IGNORED"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                usedSuggestedPrice = false
            }
        }
    }

    fun loadCourses(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _courseLoading.value = true
            try {
                when (val result = courseRepository.getCourses(forceRefresh)) {
                    is CourseResult.Success -> {
                        val mapped = result.courses
                            .sortedBy { it.code }
                            .map { course ->
                                CourseOption(
                                    id = course.id,
                                    code = course.code,
                                    name = course.name,
                                    faculty = course.faculty,
                                    departmentCode = course.departmentCode
                                )
                            }
                        _courses.value = mapped
                        _courseLoadError.value = null
                    }
                    is CourseResult.Error -> {
                        _courseLoadError.value = result.message
                    }
                }
            } finally {
                _courseLoading.value = false
            }
        }
    }
}
