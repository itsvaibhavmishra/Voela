package com.vaibhawmishra.voela.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.theme.LocalAccent
import com.vaibhawmishra.voela.ui.theme.TextSecondary

// Quiet "Developed by <name> 👾" credit. The name is tappable and opens the
// developer site. Shared across the main screens.
@Composable
fun DeveloperFooter(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.developer_url)
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${stringResource(R.string.footer_developed_by)} ",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        Text(
            stringResource(R.string.footer_developer_name),
            style = MaterialTheme.typography.labelMedium,
            color = LocalAccent.current.base,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { uriHandler.openUri(url) },
        )
        Text(
            " 👾",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}
