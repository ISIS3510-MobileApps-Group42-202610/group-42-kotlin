
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimarketfrontend.location.LocationHelper
import com.example.unimarketfrontend.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    sellerId: Int,
    sellerName: String,
    viewModel: ChatViewModel = viewModel()
) {
    // Observa la lista de mensajes del ViewModel
    val messages by viewModel.messages.collectAsState()
    // Observa si hay un mensaje siendo enviado y determina el estado actual
    val isSending by viewModel.isSending.collectAsState()
    // Estado local del campo de texto — no necesita persistir entre recomposiciones
    var inputText by remember { mutableStateOf("") }
    // Estado local de si el usuario está en el campus
    var isOnCampus by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Launcher para pedir el permiso de ubicación al usuario
    // Se ejecuta cuando el usuario acepta o rechaza el permiso
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                isOnCampus = LocationHelper.isOnCampus(context)
            }
        }
    }

    // LaunchedEffect se ejecuta UNA SOLA VEZ cuando la pantalla aparece
    // sellerId es la "key" — si cambia, se ejecuta de nuevo
    LaunchedEffect(sellerId) {
        // Carga los mensajes del hilo con este vendedor
        viewModel.loadThread(sellerId)
        // Verifica si el usuario está en el campus
        if (LocationHelper.hasPermission(context)) {
            isOnCampus = LocationHelper.isOnCampus(context)
        } else {
            // Pide el permiso si no lo tiene
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Muestra el nombre del vendedor en la barra superior
                title = { Text(sellerName) },
                navigationIcon = {
                    // Botón de volver atrás
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

            // reverseLayout = true hace que los mensajes nuevos aparezcan abajo
            LazyColumn(
                modifier = Modifier
                    .weight(1f)  // ocupa todo el espacio disponible entre el banner y el input
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                // .reversed() porque reverseLayout invierte el orden visual
                items(messages.reversed()) { message ->
                    ChatBubble(
                        content = message.content,
                        // Si sent_by es "buyer", el mensaje es mío (va a la derecha)
                        isMine = message.sent_by == "buyer"
                    )
                }
            }

            // Fila de input para escribir y enviar mensajes
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
                        // isNotBlank verifica que no sea vacío ni solo espacios
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(sellerId, inputText)
                            inputText = ""  // limpia el campo después de enviar
                        }
                    },
                    enabled = !isSending  // desactiva el botón mientras se está enviando
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

// Burbuja de mensaje individual
// isMine = true → burbuja a la derecha con color primario (mis mensajes)
// isMine = false → burbuja a la izquierda con color secundario (mensajes del otro)
@Composable
private fun ChatBubble(content: String, isMine: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        // Alinea a la derecha si es mío, a la izquierda si es del otro
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isMine) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            // Limita el ancho de la burbuja para que no ocupe toda la pantalla
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
