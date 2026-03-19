package com.example.unimarketfrontend.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.unimarketfrontend.ui.theme.AccentOrange
import com.example.unimarketfrontend.ui.theme.PrimaryIndigo
import com.example.unimarketfrontend.ui.theme.SurfaceGray
import com.example.unimarketfrontend.ui.theme.TextPrimary
import com.example.unimarketfrontend.ui.theme.TextSecondary

@Composable
fun NeumorphicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            ambientColor = Color.Black.copy(alpha = 0.1f),
            spotColor = Color.Black.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) PrimaryIndigo else AccentOrange,
            disabledContainerColor = SurfaceGray
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        enabled = enabled
    ) {
        Text(text = text, color = Color.White)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.shadow(
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp),
            ambientColor = Color.Black.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        enabled = enabled
    ) {
        Text(text = text, color = PrimaryIndigo)
    }
}

@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            ambientColor = Color.Black.copy(alpha = 0.1f),
            spotColor = Color.Black.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentOrange,
            disabledContainerColor = SurfaceGray
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        enabled = enabled
    ) {
        Text(text = text, color = Color.White)
    }
}

@Composable
fun NeumorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    maxLines: Int = 1
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        modifier = modifier.shadow(
            elevation = 6.dp,
            shape = RoundedCornerShape(12.dp),
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedLabelColor = TextSecondary,
            focusedLabelColor = PrimaryIndigo,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = PrimaryIndigo
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        leadingIcon = leadingIcon,
        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = TextPrimary),
        maxLines = maxLines
    )
}

