package com.vaibhawmishra.voela.ui.components
import com.vaibhawmishra.voela.ui.theme.LocalAccent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.ui.theme.SurfaceElevated
import com.vaibhawmishra.voela.ui.theme.TextSecondary

// Full-width purple CTA shared across screens — centered label with a trailing arrow
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LocalAccent.current.base,
            contentColor = LocalAccent.current.onAccent,
            disabledContainerColor = SurfaceElevated,
            disabledContentColor = TextSecondary,
        ),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Center))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.align(Alignment.CenterEnd).size(18.dp))
        }
    }
}
