package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.i18n.AppStrings
import com.example.i18n.LocalAppStrings
import com.example.media.MediaUtils
import com.example.model.AvatarPreset
import com.example.model.BuddyDirectoryEntry
import com.example.model.MsnStatus
import com.example.ui.theme.MsnFontCatalog
import com.example.ui.theme.MsnGrayOffline
import com.example.ui.theme.MsnGreenOnline
import com.example.ui.theme.MsnOrangeAway
import com.example.ui.theme.MsnRedBusy
import com.example.ui.theme.parseHexColor

fun statusColor(status: MsnStatus): Color = when (status) {
    MsnStatus.ONLINE -> MsnGreenOnline
    MsnStatus.AWAY, MsnStatus.BRB -> MsnOrangeAway
    MsnStatus.BUSY, MsnStatus.PHONE -> MsnRedBusy
    MsnStatus.OFFLINE -> MsnGrayOffline
}

fun statusLabel(status: MsnStatus, strings: AppStrings): String = when (status) {
    MsnStatus.ONLINE -> strings.statusOnline
    MsnStatus.AWAY -> strings.statusAway
    MsnStatus.BRB -> strings.statusBrb
    MsnStatus.BUSY -> strings.statusBusy
    MsnStatus.PHONE -> strings.statusPhone
    MsnStatus.OFFLINE -> strings.statusOffline
}

@Composable
fun MsnAvatarFrame(
    avatarPreset: AvatarPreset,
    avatarBase64: String,
    status: MsnStatus,
    size: Dp = 54.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val borderColor = statusColor(status)
    val customBitmap = remember(avatarBase64) {
        if (avatarBase64.isNotBlank()) MediaUtils.decodeBase64ToBitmap(avatarBase64) else null
    }
    Box(
        modifier = modifier
            .size(size)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFD5E6FA))
                    )
                )
                .border(2.5.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (customBitmap != null) {
                Image(
                    bitmap = customBitmap.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val resId = when (avatarPreset) {
                    AvatarPreset.DUCK -> R.drawable.img_avatar_duck
                    AvatarPreset.ASTRONAUT -> R.drawable.img_avatar_astronaut
                    AvatarPreset.CAT -> R.drawable.img_avatar_cat
                    AvatarPreset.BUTTERFLY -> R.drawable.msn_launcher_icon
                    AvatarPreset.CUSTOM -> R.drawable.img_avatar_duck
                }
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "Avatar Preset",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        // Classic MSN status orb at bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 3.dp, y = 3.dp)
                .size((size * 0.28f).coerceAtLeast(12.dp))
                .clip(CircleShape)
                .background(borderColor)
                .border(1.5.dp, Color.White, CircleShape)
        )
    }
}

@Composable
fun StyledBuddyName(
    entry: BuddyDirectoryEntry,
    fontSize: TextUnit = 16.sp,
    fallbackColor: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    val parsedColor = remember(entry.nameColorHex) {
        parseHexColor(entry.nameColorHex, fallbackColor)
    }
    val fontFamily = remember(entry.nameFontId) {
        MsnFontCatalog.resolve(entry.nameFontId)
    }
    Text(
        text = entry.displayName.ifBlank { entry.msnHandle },
        color = parsedColor,
        fontFamily = fontFamily,
        fontSize = fontSize,
        fontWeight = if (entry.nameBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (entry.nameItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (entry.nameUnderline) TextDecoration.Underline else TextDecoration.None,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun StatusDropdownChip(
    currentStatus: MsnStatus,
    onStatusSelected: (MsnStatus) -> Unit,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("status_dropdown_button")
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor(currentStatus))
                    .border(1.dp, Color.White, CircleShape)
            )
            Text(
                text = "(${statusLabel(currentStatus, strings)})",
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Change Status",
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MsnStatus.entries.forEach { st ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = statusColor(st),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(statusLabel(st, strings))
                        }
                    },
                    onClick = {
                        expanded = false
                        onStatusSelected(st)
                    },
                    modifier = Modifier.testTag("status_option_${st.code.lowercase()}")
                )
            }
        }
    }
}
