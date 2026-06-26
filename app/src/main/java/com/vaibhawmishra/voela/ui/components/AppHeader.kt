package com.vaibhawmishra.voela.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.theme.TextPrimary

// Shared top bar: app logo on the left, a back (or home) button on the right.
// Pass home = true to show a Home icon instead of the back arrow.
@Composable
fun AppHeader(onBack: () -> Unit, modifier: Modifier = Modifier, home: Boolean = false) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_voela),
            contentDescription = stringResource(R.string.cd_logo),
            modifier = Modifier.height(42.dp),
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onBack) {
            if (home) {
                Icon(Icons.Outlined.Home, stringResource(R.string.cd_home), tint = TextPrimary)
            } else {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cd_back), tint = TextPrimary)
            }
        }
    }
}
