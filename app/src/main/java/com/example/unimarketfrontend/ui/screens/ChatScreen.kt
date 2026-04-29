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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
import com.example.unimarketfrontend.model.utils.LocationHelper
import com.example.unimarketfrontend.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    otherId: Int,
    otherName: String,
    iAmBuyer: Boolean,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var nearbyBuilding by remember { mutableStateOf<String?>(null) }
    var permissionRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Callback cuando el usuario responde al diálogo de permisos
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                val building = LocationHelper.getNearbyBuilding(context)
                android.util.Log.d("ChatScreen", "Permission granted, nearby building: $building")
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
        } else {
            android.util.Log.d("ChatScreen", "Location permission denied")
        }
    }

    // Cargar thread y ubicación
    LaunchedEffect(otherId) {
        viewModel.loadThread(otherId, iAmBuyer)

        if (LocationHelper.hasPermission(context)) {
            val building = LocationHelper.getNearbyBuilding(context)
            android.util.Log.d("ChatScreen", "Has permission, nearby building: $building")
            nearbyBuilding = building
            if (building != null) {
                BusinessAnalyticsProvider.tracker.trackCampusBannerShown(
                    listingId = -1,
                    sellerId = otherId,
                    buildingName = building,
                    metadata = mapOf("building" to building)
                )
            }
        } else {
            // Marcar que necesitamos pedir permiso (se hace fuera del LaunchedEffect)
            permissionRequested = true
        }
    }

    // Pedir permiso fuera del LaunchedEffect para que funcione correctamente con Compose
    LaunchedEffect(permissionRequested) {
        if (permissionRequested) {
            permissionRequested = false
            android.util.Log.d("ChatScreen", "Requesting location permission...")
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                            text = "\uD83D\uDCCD You are near $nearbyBuilding",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Suggested meeting point: $nearbyBuilding lobby",
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
