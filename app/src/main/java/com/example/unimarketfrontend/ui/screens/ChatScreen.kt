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
import com.example.unimarketfrontend.repository.BusinessAnalyticsProvider
import com.example.unimarketfrontend.utils.LocationHelper
import com.example.unimarketfrontend.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

/*
 * Esta pantalla maneja la conversacion individual entre un comprador y un vendedor.
 * Es la pieza clave para la BQ4 (tiempos de respuesta) y la BQ10 (efecto del campus).
 * Implementa una arquitectura reactiva donde la UI solo dibuja lo que el StateFlow emite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    sellerId: Int,      // ID del vendedor que viene de la ruta de navegacion
    sellerName: String, // Nombre del vendedor para mostrar en el TopBar
    viewModel: ChatViewModel = viewModel()
) {
    // Suscripcion reactiva a la lista de mensajes (Patron Observer)
    val messages by viewModel.messages.collectAsState()

    // Estado que bloquea el boton de enviar mientras la peticion viaja al servidor (UX)
    val isSending by viewModel.isSending.collectAsState()

    // Estado local para lo que el usuario escribe en el teclado
    var inputText by remember { mutableStateOf("") }

    // Estado de la Smart Feature: ¿Esta el usuario fisicamente en la U?
    var isOnCampus by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Gestion de permisos nativos para el sensor GPS (Google Play Services)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                // Si el usuario acepta, el helper calcula la posicion con Haversine
                isOnCampus = LocationHelper.isOnCampus(context)
            }
        }
    }

    // Efecto de carga inicial: se dispara cada vez que cambia el sellerId
    LaunchedEffect(sellerId) {
        // Traemos el historial de mensajes desde la DB local (Room) o el servidor
        viewModel.loadThread(sellerId)

        // Verificamos el contexto fisico para la feature Context-Aware
        if (LocationHelper.hasPermission(context)) {
            isOnCampus = LocationHelper.isOnCampus(context)

            // SPRINT 3 - BQ10: Si el sensor dice que estamos en la U, registramos el evento.
            // Esto sirve para ver si la sugerencia de sitio de encuentro aumenta las ventas.
            if (isOnCampus) {
                BusinessAnalyticsProvider.tracker.trackCampusBannerShown(
                    listingId = -1, // No tenemos el listing aca, mandamos -1 por ahora
                    sellerId = sellerId
                )
            }
        } else {
            // Si no hay permiso, lo pedimos (Estrategia de Conectividad Eventual)
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
            // --- SMART FEATURE: BANNER CONTEXTUAL ---
            // Solo aparece si el sensor GPS confirma que el usuario esta a < 600m del campus
            if (isOnCampus) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "You are on campus",
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

            // Lista de mensajes: usa reverseLayout para que los nuevos salgan abajo (tipo WhatsApp)
            // Es eficiente en memoria gracias a LazyColumn
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    ChatBubble(
                        content = message.content,
                        // Diferenciamos visualmente mis mensajes de los del vendedor
                        isMine = message.sent_by == "buyer"
                    )
                }
            }

            // Input de texto y boton de envio
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
                            // SPRINT 3: Esta funcion ahora maneja colas de pendientes si no hay red
                            viewModel.sendMessage(sellerId, inputText)
                            inputText = ""
                        }
                    },
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

/*
 * Componente visual para las burbujas de chat.
 * Implementa la logica de alineacion (derecha/izquierda) segun el autor.
 */
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
