package com.example.unimarketfrontend.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
import com.example.unimarketfrontend.model.utils.LocationHelper
import com.example.unimarketfrontend.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

/*
 * Pantalla de Chat Individual - SPRINT 3
 * Corregida para ser 100% reactiva con Room y soportar el modo Offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    otherId: Int,
    otherName: String,
    iAmBuyer: Boolean,          // NUEVO: recibimos el rol directamente
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var nearbyBuilding by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(otherId) {
        // Pasamos iAmBuyer para que el ViewModel sepa qué endpoint usar desde el inicio
        viewModel.loadThread(otherId, iAmBuyer)

        val building = LocationHelper.getNearbyBuilding(context)
        nearbyBuilding = building
        if (building != null) {
            BusinessAnalyticsProvider.tracker.trackCampusBannerShown(
                listingId = -1,
                sellerId = otherId,
                buildingName = building,
                metadata = mapOf("building" to building)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            if (isOffline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Offline - Showing saved messages",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (nearbyBuilding != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "You are near $nearbyBuilding",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Suggested meeting point: Mario Laserna building lobby",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    ChatBubble(content = message.content, isMine = message.isMine)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            // Pasamos iAmBuyer para que el ViewModel use el endpoint correcto
                            viewModel.sendMessage(otherId, inputText, iAmBuyer)
                            inputText = ""
                        }
                    },
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(content: String, isMine: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isMine) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (isMine) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}