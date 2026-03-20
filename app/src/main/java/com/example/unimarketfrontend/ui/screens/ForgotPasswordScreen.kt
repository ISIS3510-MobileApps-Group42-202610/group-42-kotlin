package com.example.unimarketfrontend.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarketfrontend.viewmodel.ForgotPasswordViewModel

private enum class ForgotStep { EMAIL, RESET }

@Composable
fun ForgotPasswordScreen(
    onNavigateToLogin: () -> Unit = {}
) {
    val viewModel: ForgotPasswordViewModel = viewModel()

    var step        by remember { mutableStateOf(ForgotStep.EMAIL) }
    var email       by remember { mutableStateOf("") }
    var token       by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Unimarket",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5C6BC0)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (step == ForgotStep.EMAIL) "Recuperar contraseña" else "Nueva contraseña",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(28.dp))

        if (step == ForgotStep.EMAIL) {

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMsg = null },
                label = { Text("Email") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color(0xFF5C6BC0),
                    unfocusedLabelColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg != null) {
                Spacer(Modifier.height(6.dp))
                Text(errorMsg!!, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (email.isBlank()) { errorMsg = "Ingresa tu email"; return@Button }
                    isLoading = true
                    viewModel.forgotPassword(email,
                        onSuccess = { isLoading = false; step = ForgotStep.RESET },
                        onError   = { msg -> isLoading = false; errorMsg = msg }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAAA0F0)),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Enviar token", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

        } else {

            OutlinedTextField(
                value = token,
                onValueChange = { token = it; errorMsg = null },
                label = { Text("Token recibido por email") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color(0xFF5C6BC0),
                    unfocusedLabelColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; errorMsg = null },
                label = { Text("Nueva contraseña (mín. 6 caracteres)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = Color(0xFF5C6BC0),
                    unfocusedLabelColor = Color.Gray
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg != null) {
                Spacer(Modifier.height(6.dp))
                Text(errorMsg!!, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    when {
                        token.isBlank()       -> errorMsg = "Ingresa el token"
                        newPassword.length < 6 -> errorMsg = "La contraseña debe tener al menos 6 caracteres"
                        else -> {
                            isLoading = true
                            viewModel.resetPassword(token, newPassword,
                                onSuccess = { isLoading = false; onNavigateToLogin() },
                                onError   = { msg -> isLoading = false; errorMsg = msg }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAAA0F0)),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Restablecer contraseña", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Volver al inicio de sesión", color = Color(0xFF5C6BC0))
        }
    }
}
