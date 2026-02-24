package com.example.unimarketfrontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HeaderSection(userName: String) {

    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFF4A57E9), Color(0xFF1EBAB8))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .padding(20.dp)
    ) {

        Text(
            text = "Good afternoon, $userName 👋",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )

        Text(
            text = "Universidad de los Andes",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search for books, calculators, notes...") }
        )
    }
}