package com.example.unimarketfrontend.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.unimarketfrontend.model.listing.Review
import com.example.unimarketfrontend.model.user.User
import com.example.unimarketfrontend.ui.navigation.ChatRoutesFactory
import com.example.unimarketfrontend.viewmodel.ListingDetailUiState
import com.example.unimarketfrontend.viewmodel.ListingDetailViewModel
import com.example.unimarketfrontend.viewmodel.ListingDetailViewModelFactory
import java.util.Locale
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    navController: NavController,
    listingId: Int
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: ListingDetailViewModel = viewModel(factory = ListingDetailViewModelFactory(app, listingId))
    val state by vm.uiState.collectAsState()

    var showMessageDialog by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var messageError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    var showRatingDialog by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableIntStateOf(5) }

    when (val current = state) {
        is ListingDetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is ListingDetailUiState.Error -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Detalle") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(current.message)
                }
            }
        }

        is ListingDetailUiState.Success -> {
            val listing = current.listing
            val seller = current.seller
            val contactTargetId = current.contactTargetId
            val contactTargetName = current.contactTargetName ?: seller?.let { "${it.name} ${it.last_name}".trim() } ?: "Vendedor"
            val contactActionLabel = current.contactActionLabel
            val reviews = current.reviews
            val ratingSummary = current.ratingSummary
            var selectedImageIndex by remember { mutableIntStateOf(0) }
            val images = listing.images.orEmpty()

            var errorText by remember { mutableStateOf<String?>(null) } // ✅ Nuevo estado para el error

            if (showMessageDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showMessageDialog = false
                        errorText = null
                    },
                    title = { Text("Enviar mensaje") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = {
                                    messageText = it
                                    errorText = null // Limpiamos error al escribir
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Mensaje") },
                                isError = errorText != null
                            )
                            if (errorText != null) {
                                Text(
                                    text = errorText!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    vm.sendFirstMessage(
                                        recipientUserId = listing.seller_id,
                                        content = messageText,
                                        onComplete = {
                                            messageText = ""
                                            showMessageDialog = false
                                            errorText = null
                                            // Navegamos al chat bilateral con el rol correcto
                                            val iAmBuyer = !current.isOwner
                                            navController.navigate(
                                                ChatRoutesFactory.chatPath(
                                                    otherId = listing.seller_id,
                                                    otherName = seller?.let { "${it.name} ${it.last_name}".trim() } ?: "Vendedor",
                                                    iAmBuyer = iAmBuyer
                                                )
                                            )
                                        },
                                        onError = { error ->
                                            errorText = error //  Mostramos el error real
                                        }
                                    )
                                }
                            }
                        ) { Text("Enviar") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showMessageDialog = false
                            messageError = null
                        }) { Text("Cancelar") }
                    }
                )
            }

            if (showRatingDialog) {
                AlertDialog(
                    onDismissRequest = { showRatingDialog = false },
                    title = { Text("Finalizar Compra") },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("¿Cómo calificarías la experiencia con este vendedor?")
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(5) { index ->
                                    val starIndex = index + 1
                                    Icon(
                                        imageVector = if (starIndex <= selectedRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { selectedRating = starIndex }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                vm.trackTransactionCompleted(rating = selectedRating)
                                showRatingDialog = false
                            }
                        ) { Text("Finalizar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRatingDialog = false }) { Text("Cancelar") }
                    }
                )
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Detalle") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (images.isNotEmpty()) {
                        AsyncImage(
                            model = images[selectedImageIndex].url,
                            contentDescription = listing.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )

                        if (images.size > 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(images) { index, image ->
                                    AsyncImage(
                                        model = image.url,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedImageIndex = index },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = listing.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$${listing.selling_price}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listing.condition?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                        listing.category?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                        if (!listing.active) {
                            AssistChip(onClick = {}, label = { Text("Vendida") })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Vendedor", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    SellerSection(seller = seller, fallbackName = current.sellerDisplayName)

                    Spacer(modifier = Modifier.height(12.dp))
                    if (current.isOwner) {
                        AssistChip(onClick = {}, label = { Text("Tu publicación") })
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Calificacion", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingStars(rating = ratingSummary.average)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", ratingSummary.average)} (${ratingSummary.count} reviews)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (current.hasExternalComments) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AssistChip(onClick = {}, label = { Text("${current.externalCommentsCount} comentarios de otros usuarios") })
                    }

                    val productDescription = listing.product.orEmpty().trim()
                    if (productDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Descripcion", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = productDescription, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Reviews", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (reviews.isEmpty()) {
                        Text(
                            text = "Reviews unavailable offline. Connect to see reviews.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        reviews.forEach { review ->
                            ReviewItem(review = review)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    actionError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (current.canMarkAsSold) {
                        Button(
                            onClick = {
                                vm.markAsSold(
                                    onComplete = {
                                        actionError = null
                                        navController.popBackStack()
                                    },
                                    onError = { error ->
                                        actionError = error
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Marcar como vendida")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Boton para iniciar contacto
                    if (contactTargetId != null) {
                        Button(
                            onClick = {
                                showMessageDialog = true
                                messageError = null
                                vm.trackChatStarted()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = contactTargetId != null
                        ) {
                            Text(contactActionLabel)
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun SellerSection(seller: User?, fallbackName: String) {
    if (seller == null) {
        Text(
            text = fallbackName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!seller.profile_pic.isNullOrBlank()) {
            AsyncImage(
                model = seller.profile_pic,
                contentDescription = seller.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = seller.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        Column {
            Text(
                text = "${seller.name} ${seller.last_name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = seller.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            seller.semester?.let {
                Text(
                    text = "Semestre $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RatingStars(rating: Double, maxStars: Int = 5) {
    val filledStars = floor(rating).toInt().coerceIn(0, maxStars)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(maxStars) { index ->
            Icon(
                imageVector = if (index < filledStars) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    val authorName = review.user?.let { "${it.name} ${it.last_name}".trim() }
        ?.takeIf { it.isNotBlank() }
        ?: review.reviewer_id?.let { "Usuario #$it" }
        ?: review.user_id?.let { "Usuario #$it" }
        ?: "Usuario"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = authorName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        review.rating?.let {
            Spacer(modifier = Modifier.height(4.dp))
            RatingStars(rating = it.toDouble())
        }

        if (!review.content.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = review.content, style = MaterialTheme.typography.bodyMedium)
        }

        review.created_at?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}