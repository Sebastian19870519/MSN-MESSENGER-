package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.LocalAppStrings
import com.example.model.MsnCatalog
import com.example.ui.theme.MsnFontCatalog
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.toHexRgb

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FontColorStylePickerSheet(
    title: String,
    sampleText: String,
    initialFontId: String,
    initialColorHex: String,
    initialBold: Boolean,
    initialItalic: Boolean,
    initialUnderline: Boolean,
    initialStrike: Boolean = false,
    showStrike: Boolean = true,
    onDismiss: () -> Unit,
    onApply: (fontId: String, colorHex: String, bold: Boolean, italic: Boolean, underline: Boolean, strike: Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    var fontId by remember { mutableStateOf(initialFontId) }
    val initialColor = remember(initialColorHex) { parseHexColor(initialColorHex, Color(0xFF102033)) }
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }
    var bold by remember { mutableStateOf(initialBold) }
    var italic by remember { mutableStateOf(initialItalic) }
    var underline by remember { mutableStateOf(initialUnderline) }
    var strike by remember { mutableStateOf(initialStrike) }

    val selectedColor = Color(red, green, blue)
    val selectedHex = selectedColor.toHexRgb()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)

            // Live Preview Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val deco = when {
                    underline && strike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    underline -> TextDecoration.Underline
                    strike -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                }
                Text(
                    text = sampleText,
                    color = selectedColor,
                    fontFamily = MsnFontCatalog.resolve(fontId),
                    fontSize = 18.sp,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = deco,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Style toggles
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = bold,
                    onClick = { bold = !bold },
                    label = { Text(strings.boldLabel, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("style_bold_chip")
                )
                FilterChip(
                    selected = italic,
                    onClick = { italic = !italic },
                    label = { Text(strings.italicLabel, fontStyle = FontStyle.Italic) },
                    modifier = Modifier.testTag("style_italic_chip")
                )
                FilterChip(
                    selected = underline,
                    onClick = { underline = !underline },
                    label = { Text(strings.underlineLabel, textDecoration = TextDecoration.Underline) },
                    modifier = Modifier.testTag("style_underline_chip")
                )
                if (showStrike) {
                    FilterChip(
                        selected = strike,
                        onClick = { strike = !strike },
                        label = { Text(strings.strikeLabel, textDecoration = TextDecoration.LineThrough) },
                        modifier = Modifier.testTag("style_strike_chip")
                    )
                }
            }

            // Color swatches + RGB sliders
            Text(
                text = "${strings.colorLabel} ($selectedHex)",
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MsnCatalog.classicColors.forEach { hex ->
                    val swatch = parseHexColor(hex)
                    val isSelected = selectedHex.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                red = swatch.red
                                green = swatch.green
                                blue = swatch.blue
                            }
                    )
                }
            }

            // Fine-grained RGB sliders for any custom color
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("R", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                Slider(
                    value = red,
                    onValueChange = { red = it },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
                )
                Text("G", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2EB840))
                Slider(
                    value = green,
                    onValueChange = { green = it },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF2EB840), activeTrackColor = Color(0xFF2EB840))
                )
                Text("B", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0F62C9))
                Slider(
                    value = blue,
                    onValueChange = { blue = it },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF0F62C9), activeTrackColor = Color(0xFF0F62C9))
                )
            }

            // Font family list
            Text(text = strings.fontLabel, style = MaterialTheme.typography.labelMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(MsnFontCatalog.options, key = { it.id }) { opt ->
                    val selected = opt.id == fontId
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fontId = opt.id }
                            .testTag("font_option_${opt.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt.displayName,
                                fontFamily = opt.fontFamily,
                                fontSize = 15.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = opt.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        onApply(fontId, selectedHex, bold, italic, underline, strike)
                    },
                    modifier = Modifier.testTag("apply_font_style_button")
                ) {
                    Text(strings.saveBtn)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
