package com.vaibhawmishra.voela.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.ui.theme.LocalAccent
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary

// Renders the small slice of Markdown our release notes use: "#"/"##"/"###" headings,
// "-"/"*" bullets, "**bold**" emphasis, "[label](url)" and bare URLs as tappable links,
// and blank-line spacing. Deliberately tiny and dependency-free; anything fancier just
// renders as a plain paragraph. Bare URLs are shown as a short "link" so notes stay clean.
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    bodyColor: Color = TextSecondary,
    headingColor: Color = TextPrimary,
) {
    val linkColor = LocalAccent.current.base
    Column(modifier) {
        var first = true
        text.trim().lines().forEach { raw ->
            val line = raw.trim()
            when {
                line.isBlank() -> Spacer(Modifier.height(6.dp))
                line.startsWith("### ") -> { Heading(line.removePrefix("### "), headingColor, linkColor, first); first = false }
                line.startsWith("## ") -> { Heading(line.removePrefix("## "), headingColor, linkColor, first); first = false }
                line.startsWith("# ") -> { Heading(line.removePrefix("# "), headingColor, linkColor, first); first = false }
                line.startsWith("- ") || line.startsWith("* ") -> { Bullet(line.drop(2), bodyColor, linkColor); first = false }
                else -> { Text(inline(line, linkColor), style = MaterialTheme.typography.bodySmall, color = bodyColor); first = false }
            }
        }
    }
}

@Composable
private fun Heading(text: String, color: Color, linkColor: Color, first: Boolean) {
    if (!first) Spacer(Modifier.height(10.dp))
    Text(inline(text, linkColor), style = MaterialTheme.typography.labelLarge, color = color)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Bullet(text: String, color: Color, linkColor: Color) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("•", style = MaterialTheme.typography.bodySmall, color = LocalAccent.current.base)
        Spacer(Modifier.width(8.dp))
        Text(inline(text, linkColor), style = MaterialTheme.typography.bodySmall, color = color)
    }
}

// Tokenises "**bold**", "[label](url)" and bare http(s) URLs. Links open via the platform
// UriHandler; bare URLs collapse to a short "link ↗" so a wall of URL never shows.
private fun inline(s: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    val linkStyle = TextLinkStyles(SpanStyle(color = linkColor, fontWeight = FontWeight.Medium))
    var i = 0
    var bold = false
    fun emit(t: String) {
        if (bold) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(t) } else append(t)
    }
    while (i < s.length) {
        if (s.startsWith("**", i)) { bold = !bold; i += 2; continue }
        if (s[i] == '[') {
            val close = s.indexOf(']', i)
            if (close > i && close + 1 < s.length && s[close + 1] == '(') {
                val end = s.indexOf(')', close + 2)
                if (end > close) {
                    withLink(LinkAnnotation.Url(s.substring(close + 2, end), linkStyle)) { append(s.substring(i + 1, close)) }
                    i = end + 1
                    continue
                }
            }
        }
        if (s.startsWith("http://", i) || s.startsWith("https://", i)) {
            var j = i
            while (j < s.length && !s[j].isWhitespace()) j++
            withLink(LinkAnnotation.Url(s.substring(i, j), linkStyle)) { append("link ↗") }
            i = j
            continue
        }
        emit(s[i].toString())
        i++
    }
}
