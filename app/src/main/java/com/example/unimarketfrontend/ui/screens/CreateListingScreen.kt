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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.unimarketfrontend.network.model.CreateListingRequest
import com.example.unimarketfrontend.network.model.ListingCategory
import com.example.unimarketfrontend.network.model.ListingCondition
import com.example.unimarketfrontend.ui.components.AccentButton
import com.example.unimarketfrontend.ui.components.NeumorphicTextField
import com.example.unimarketfrontend.ui.theme.BackgroundLight
import com.example.unimarketfrontend.ui.theme.PrimaryIndigo
import com.example.unimarketfrontend.ui.theme.SecondaryEmerald
import com.example.unimarketfrontend.ui.theme.TextPrimary
import com.example.unimarketfrontend.ui.theme.TextSecondary
import com.example.unimarketfrontend.viewmodel.CreateListingState
import com.example.unimarketfrontend.viewmodel.CreateListingViewModel
import java.io.File

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
    val selectedCondition = remember { mutableStateOf(ListingCondition.NEW) }
    var selectedCategory by remember { mutableStateOf("Books") }

    val categoryMap = mapOf(
        "Books" to ListingCategory.TEXTBOOK,
        "Electronics" to ListingCategory.ELECTRONICS,
        "Notes" to ListingCategory.NOTES,
        "Furniture" to ListingCategory.OTHER
    )

    fun addUris(newUris: List<Uri>) {
        if (newUris.isEmpty()) return
        selectedImageUris = (selectedImageUris + newUris)
            .distinct()
            .take(MAX_IMAGES_PER_LISTING)
    }

    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES_PER_LISTING)
    ) { uris ->
        addUris(uris)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            addUris(listOf(pendingCameraUri!!))
        }
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
            localError = "Se requiere permiso de camara para tomar fotos"
        }
    }

    LaunchedEffect(state) {
        if (state is CreateListingState.Success) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.clickable { navController.popBackStack() },
                tint = TextPrimary
            )

            Text(
                text = "Create listing",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Box(modifier = Modifier.size(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f)
                )
                .background(Color(0xFFEFF1FC), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeumorphicTextField(
                value = title,
                onValueChange = {
                    title = it
                    localError = null

                    if (it.isBlank()) {
                        isAutoDescription = true
                        isAutoPrice = true
                        isAutoCategory = true
                    } else if (it.length >= 3) {

                        val suggestion = viewModel.suggestFromText(it)

                        if (isAutoDescription) {
                            description = suggestion.description
                        }

                        if (isAutoPrice) {
                            price = suggestion.price.toString()
                        }

                        if (isAutoCategory) {
                            selectedCategory = suggestion.category
                        }
                    }
                },
                label = "Title",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Image, contentDescription = null, tint = TextSecondary)
                }
            )

            NeumorphicTextField(
                value = description,
                onValueChange = {
                    description = it
                    isAutoDescription = false
                },
                label = "Description",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                }
            )

            NeumorphicTextField(
                value = price,
                onValueChange = {
                    price = it
                    localError = null
                    isAutoPrice = false
                },
                label = "Price",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Star, contentDescription = null, tint = TextSecondary)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Books", "Notes", "Electronics", "Furniture", "Other").forEach { category ->
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = Color.Black.copy(alpha = 0.08f)
                            )
                            .background(
                                if (selectedCategory == category) PrimaryIndigo else Color(0xFFDDF3F0),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedCategory == category) Color.White else TextPrimary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .clickable {
                        pickImagesLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedImageUris.isEmpty()) "Image upload placeholder" else "${selectedImageUris.size} image(s) selected",
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            }

            if (selectedImageUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedImageUris) { uri ->
                        Box(modifier = Modifier.size(84.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUris = selectedImageUris.filterNot { it == uri } },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = SecondaryEmerald)
                            }
                        }
                    }
                }
            }

            AccentButton(
                text = "Publish",
                onClick = {
                    val parsedPrice = price.toDoubleOrNull()
                    when {
                        title.isBlank() -> localError = "El titulo es obligatorio"
                        parsedPrice == null || parsedPrice <= 0.0 -> localError = "Ingresa un precio valido"
                        selectedImageUris.isEmpty() -> localError = "Debes seleccionar al menos 1 imagen"
                        else -> {
                            localError = null
                            val request = CreateListingRequest(
                                title = title.trim(),
                                product = description.ifBlank { null },
                                category = categoryMap[selectedCategory]?.value,
                                condition = selectedCondition.value.value,
                                original_price = null,
                                selling_price = parsedPrice,
                                course_id = null
                            )
                            viewModel.createListing(request, selectedImageUris)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clickable {
                            pickImagesLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Gallery", color = TextPrimary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clickable {
                            val permissionState = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            )
                            if (permissionState == PackageManager.PERMISSION_GRANTED) {
                                val uri = createTempImageUri(context)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Camera", color = TextPrimary)
                }
            }

            if (localError != null) {
                Text(text = localError!!, color = MaterialTheme.colorScheme.error)
            }

            when (val currentState = state) {
                is CreateListingState.CreatingListing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(10.dp))
                        Text("Creando listing...")
                    }
                }
                is CreateListingState.UploadingImages -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(10.dp))
                        Text("Subiendo fotos ${currentState.uploaded}/${currentState.total}")
                    }
                }
                is CreateListingState.Error -> {
                    Text(text = currentState.message, color = MaterialTheme.colorScheme.error)
                }
                else -> Unit
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
