package com.example.unimarketfrontend.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.unimarketfrontend.model.listing.CreateListingRequest
import com.example.unimarketfrontend.model.listing.ListingCategory
import com.example.unimarketfrontend.model.listing.ListingCondition
import com.example.unimarketfrontend.ui.components.AccentButton
import com.example.unimarketfrontend.ui.components.NeumorphicTextField
import com.example.unimarketfrontend.ui.theme.*
import com.example.unimarketfrontend.viewmodel.CreateListingState
import com.example.unimarketfrontend.viewmodel.CreateListingViewModel
import kotlinx.coroutines.delay
import java.io.File
import kotlinx.coroutines.flow.collectLatest
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider

private const val MAX_IMAGES_PER_LISTING = 8

private fun createTempImageUri(context: android.content.Context): Uri {
    val imageDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imageDir, "listing_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}

@Composable
fun CreateListingScreen(
    navController: NavController,
    viewModel: CreateListingViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val selectedCourseId by viewModel.selectedCourseId.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var isAutoDescription by remember { mutableStateOf(true) }
    var isAutoPrice by remember { mutableStateOf(true) }
    var isAutoCategory by remember { mutableStateOf(true) }
    var localError by remember { mutableStateOf<String?>(null) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCondition by remember { mutableStateOf(ListingCondition.NEW) }
    var selectedCategory by remember { mutableStateOf("Textbooks") }
    var showCourseDialog by remember { mutableStateOf(false) }
    var courseQuery by remember { mutableStateOf("") }

    val filteredCourses by remember(courses, courseQuery) {
        derivedStateOf {
            val query = courseQuery.trim().lowercase()
            if (query.isBlank()) {
                courses
            } else {
                courses.filter { course ->
                    course.code.lowercase().contains(query) ||
                        course.name.lowercase().contains(query) ||
                        course.faculty.lowercase().contains(query) ||
                        course.departmentCode.lowercase().contains(query)
                }
            }
        }
    }

    var isDraftLoaded by remember { mutableStateOf(false) }

    val isLoading = state is CreateListingState.CreatingListing || state is CreateListingState.UploadingImages

    val MAX_TITLE_CHARS = 100
    val MAX_DESC_CHARS = 320
    val MAX_PRICE = 25000000.0

    val categoryMap = mapOf(
        "Textbooks" to ListingCategory.TEXTBOOK,
        "Notes" to ListingCategory.NOTES,
        "Supplies" to ListingCategory.SUPPLIES,
        "Electronics" to ListingCategory.ELECTRONICS,
        "Other" to ListingCategory.OTHER
    )

    val conditionOptions = listOf(
        "New" to ListingCondition.NEW,
        "Like New" to ListingCondition.LIKE_NEW,
        "Good" to ListingCondition.GOOD,
        "Fair" to ListingCondition.FAIR,
        "Poor" to ListingCondition.POOR
    )

    fun addUris(newUris: List<Uri>) {
        if (newUris.isEmpty()) return
        selectedImageUris = (selectedImageUris + newUris)
            .distinct()
            .take(MAX_IMAGES_PER_LISTING)
    }

    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES_PER_LISTING)
    ) { uris -> addUris(uris) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) addUris(listOf(pendingCameraUri!!))
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            localError = "Camera permission is required to take photos"
        }
    }

    val analyticsTracker = remember { BusinessAnalyticsProvider.tracker }

    LaunchedEffect(Unit) {
        try {
            analyticsTracker.trackCustomEvent("create_listing_screen_opened")
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(state) {
        if (state is CreateListingState.Success) {
            try {
                analyticsTracker.trackCustomEvent("listing_created")
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) {
        val draft = viewModel.loadDraft()
        if (draft != null) {
            title = draft.title
            description = draft.description
            price = draft.price
            selectedCategory = draft.category
            selectedImageUris = draft.imageUris.mapNotNull {
                try { Uri.parse(it) } catch (e: Exception) { null }
            }
            isAutoDescription = false
            isAutoPrice = false
            isAutoCategory = false
        }
        isDraftLoaded = true
    }

    LaunchedEffect(title, description, price, selectedCategory, selectedImageUris) {
        if (isDraftLoaded && !isLoading) {
            delay(500)
            viewModel.saveDraft(title, description, price, selectedCategory, selectedImageUris)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.listingCreated.collectLatest { createdListingId ->
            navController.previousBackStackEntry?.savedStateHandle?.set("listing_created", true)
            navController.previousBackStackEntry?.savedStateHandle?.set("listing_created_id", createdListingId)
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.clickable(enabled = !isLoading) { navController.popBackStack() },
                tint = TextPrimary
            )
            Text("Create listing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(
                onClick = {
                    if (!isLoading) {
                        viewModel.clearDraft()
                        title = ""
                        description = ""
                        price = ""
                        selectedCategory = "Textbooks"
                        selectedImageUris = emptyList()
                        isAutoDescription = true
                        isAutoPrice = true
                        isAutoCategory = true
                    }
                },
                modifier = Modifier.size(24.dp),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Discard Draft", tint = MaterialTheme.colorScheme.error)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.08f))
                .background(Color(0xFFEFF1FC), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeumorphicTextField(
                value = title,
                onValueChange = {
                    if (!isLoading && it.length <= MAX_TITLE_CHARS) {
                        title = it
                        localError = null
                        if (it.length >= 3) {
                            val suggestion = viewModel.suggestFromText(it)
                            if (isAutoDescription) description = suggestion.description
                            if (isAutoPrice) price = suggestion.price.toString()
                            if (isAutoCategory) selectedCategory = suggestion.category
                        }
                    }
                },
                label = "Title (${title.length}/$MAX_TITLE_CHARS)",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = TextSecondary) }
            )

            NeumorphicTextField(
                value = description,
                onValueChange = {
                    if (it.length <= MAX_DESC_CHARS) {
                        description = it
                        isAutoDescription = false
                    }
                },
                label = "Description (${description.length}/$MAX_DESC_CHARS)",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) }
            )

            NeumorphicTextField(
                value = price,
                onValueChange = {
                    if (!isLoading) {
                        val inputPrice = it.toDoubleOrNull()
                        if (it.isEmpty() || (inputPrice != null && inputPrice <= MAX_PRICE)) {
                            price = it
                            localError = null
                            isAutoPrice = false
                        }
                    }
                },
                label = "Price (Max 25M)",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = TextSecondary) }
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Textbooks", "Notes", "Supplies", "Electronics", "Other").forEach { cat ->
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.08f))
                            .background(if (selectedCategory == cat) PrimaryIndigo else Color(0xFFDDF3F0), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isLoading) {
                                selectedCategory = cat
                                isAutoCategory = false
                                try {
                                    analyticsTracker.trackCustomEvent(
                                        "create_listing_category_selected",
                                        metadata = mapOf("category" to cat)
                                    )
                                } catch (_: Exception) {
                                }
                            }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selectedCategory == cat) Color.White else TextPrimary)
                    }
                }
            }

            CreateListingConditionSelector(
                conditionOptions = conditionOptions,
                selectedCondition = selectedCondition,
                onConditionSelected = { condition ->
                    selectedCondition = condition
                    try {
                        analyticsTracker.trackCustomEvent(
                            "create_listing_condition_selected",
                            metadata = mapOf("condition" to condition.value)
                        )
                    } catch (_: Exception) {
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.08f))
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .clickable(enabled = !isLoading) { pickImagesLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedImageUris.isEmpty()) "Upload Images (Max $MAX_IMAGES_PER_LISTING)"
                    else "${selectedImageUris.size} / $MAX_IMAGES_PER_LISTING images selected",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }

            if (selectedImageUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedImageUris) { uri ->
                        Box(modifier = Modifier.size(84.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    if (!isLoading) {
                                        selectedImageUris = selectedImageUris.filterNot { it == uri }
                                    }
                                },
                                modifier = Modifier.align(Alignment.TopEnd),
                                enabled = !isLoading
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = SecondaryEmerald)
                            }
                        }
                    }
                }
            }

            CreateListingCourseSelector(
                selectedCourseLabel = courses.firstOrNull { it.id == selectedCourseId }?.displayLabel
                    ?: "Course (optional)",
                onClick = { showCourseDialog = true }
            )

            if (showCourseDialog) {
                AlertDialog(
                    onDismissRequest = { showCourseDialog = false },
                    title = { Text("Select course") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = courseQuery,
                                onValueChange = { courseQuery = it },
                                label = { Text("Search by code, name or faculty") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 260.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredCourses, key = { it.id }) { course ->
                                    Text(
                                        text = course.displayLabel,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setSelectedCourse(course.id)
                                                showCourseDialog = false
                                                try {
                                                    analyticsTracker.trackCustomEvent(
                                                        "create_listing_course_selected",
                                                        metadata = mapOf(
                                                            "course_id" to course.id,
                                                            "course_code" to course.code
                                                        )
                                                    )
                                                } catch (_: Exception) {
                                                }
                                            }
                                            .padding(vertical = 6.dp),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.setSelectedCourse(null)
                                showCourseDialog = false
                            }
                        ) { Text("Clear") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCourseDialog = false }) { Text("Close") }
                    }
                )
            }

            AccentButton(
                text = "Publish",
                onClick = {
                    if (isLoading) return@AccentButton
                    val p = price.toDoubleOrNull()
                    when {
                        title.isBlank() -> localError = "Title is required"
                        p == null || p <= 0.0 -> localError = "Enter a valid price"
                        selectedImageUris.isEmpty() -> localError = "Select at least 1 image"
                        else -> {
                            localError = null
                            val req = CreateListingRequest(
                                title = title.trim(),
                                product = description.ifBlank { null },
                                category = categoryMap[selectedCategory]?.value,
                                condition = selectedCondition.value,

                                original_price = null,
                                selling_price = p,
                                course_id = selectedCourseId
                            )
                            viewModel.createListing(req, selectedImageUris)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.weight(1f).height(42.dp).background(Color.White, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isLoading) { pickImagesLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) { Text("Gallery", color = TextPrimary) }

                Box(
                    modifier = Modifier.weight(1f).height(42.dp).background(Color.White, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isLoading) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val uri = createTempImageUri(context)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Text("Camera", color = TextPrimary) }
            }

            if (localError != null) {
                Text(text = localError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            when (val cur = state) {
                is CreateListingState.CreatingListing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(10.dp))
                        Text("Creating listing...")
                    }
                }
                is CreateListingState.UploadingImages -> {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(10.dp))
                            Text("Uploading photos ${cur.uploaded}/${cur.total}")
                        }

                        cur.retryMessage?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 30.dp)
                            )
                        }
                    }
                }
                is CreateListingState.PendingRetry -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Sin conexión a internet.",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            "Tu borrador está guardado. Intenta publicar de nuevo cuando tengas señal.",
                            fontSize = 12.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
                is CreateListingState.Error -> {
                    Text(cur.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                else -> Unit
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CreateListingConditionSelector(
    conditionOptions: List<Pair<String, com.example.unimarketfrontend.model.listing.ListingCondition>>,
    selectedCondition: com.example.unimarketfrontend.model.listing.ListingCondition,
    onConditionSelected: (com.example.unimarketfrontend.model.listing.ListingCondition) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Condition", fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            conditionOptions.forEach { (label, condition) ->
                FilterChip(
                    selected = selectedCondition == condition,
                    onClick = { onConditionSelected(condition) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun CreateListingCourseSelector(
    selectedCourseLabel: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(Color.White, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = selectedCourseLabel, color = TextPrimary)
    }
}
