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
    sellerId: Int,
    sellerName: String,
    viewModel: ChatViewModel = viewModel()
) {
    // Suscripción al flujo de mensajes de Room (MessageEntity)
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isOnCampus by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Gestión del sensor GPS
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                val building = LocationHelper.getNearbyBuilding(context)
                isOnCampus = building != null
            }
        }
    }

    // Efecto de carga inicial y detección de contexto (Smart Feature BQ10)
    LaunchedEffect(sellerId) {
        viewModel.loadThread(sellerId)
        if (LocationHelper.hasPermission(context)) {
            val building = LocationHelper.getNearbyBuilding(context)
            isOnCampus = building != null
            if (isOnCampus) {
                // Registramos el evento para el dashboard de analíticas
                com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider.tracker.trackCampusBannerShown(
                    listingId = -1,
                    sellerId = sellerId,
                    metadata = mapOf("building" to building!!)
                )
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sellerName) },
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
                .padding(innerPadding)
        ) {
            // SPRINT 3: Banner informativo de conexión (Evita NIM)
            if (isOffline) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "Modo Offline - Viendo mensajes guardados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // SMART FEATURE: Sugerencia de punto de encuentro según edificio detectado
            if (isOnCampus) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("You are on campus", style = MaterialTheme.typography.labelSmall)
                        Text("Suggested meeting point: Mario Laserna lobby", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Lista de mensajes reactiva a Room
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                // Usamos message.isMine que ya viene calculado del repositorio bilateral
                items(messages.reversed()) { message ->
                    ChatBubble(
                        content = message.content,
                        isMine = message.isMine
                    )
                }
            }

            // Input con soporte para Conectividad Eventual
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
                            viewModel.sendMessage(sellerId, inputText)
                            inputText = ""
                        }
                    },
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
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
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(12.dp),
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
