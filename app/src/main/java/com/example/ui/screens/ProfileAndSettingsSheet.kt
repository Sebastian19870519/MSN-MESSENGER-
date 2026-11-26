package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.MsnSoundEngine
import com.example.i18n.LocalAppStrings
import com.example.media.MediaUtils
import com.example.model.AvatarPreset
import com.example.model.BuddyDirectoryEntry
import com.example.model.UserAccount
import com.example.ui.components.FontColorStylePickerSheet
import com.example.ui.components.MsnAvatarFrame
import com.example.ui.theme.MsnAzurePrimary
import com.example.ui.theme.MsnFontCatalog
import com.example.ui.theme.MsnRedBusy
import com.example.ui.theme.parseHexColor
import com.example.viewmodel.MessageStyleDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndSettingsScreen(
    myAccount: UserAccount,
    myDirectory: BuddyDirectoryEntry,
    isBusy: Boolean,
    onBack: (() -> Unit)?,
    onSaveProfile: (
        handle: String,
        displayName: String,
        personalMessage: String,
        languageCode: String,
        nameColorHex: String,
        nameFontId: String,
        nameBold: Boolean,
        nameItalic: Boolean,
        nameUnderline: Boolean,
        avatarPreset: AvatarPreset,
        avatarBase64: String
    ) -> Unit,
    onSavePrefs: (
        languageCode: String,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        defaultStyle: MessageStyleDraft
    ) -> Unit,
    onSignOut: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var msnHandle by remember(myDirectory.msnHandle) { mutableStateOf(myDirectory.msnHandle) }
    var displayName by remember(myDirectory.displayName) { mutableStateOf(myDirectory.displayName) }
    var personalMsg by remember(myDirectory.personalMessage) { mutableStateOf(myDirectory.personalMessage) }
    var nameColorHex by remember(myDirectory.nameColorHex) { mutableStateOf(myDirectory.nameColorHex) }
    var nameFontId by remember(myDirectory.nameFontId) { mutableStateOf(myDirectory.nameFontId) }
    var nameBold by remember(myDirectory.nameBold) { mutableStateOf(myDirectory.nameBold) }
    var nameItalic by remember(myDirectory.nameItalic) { mutableStateOf(myDirectory.nameItalic) }
    var nameUnderline by remember(myDirectory.nameUnderline) { mutableStateOf(myDirectory.nameUnderline) }
    var avatarPreset by remember(myDirectory.avatarPreset) { mutableStateOf(myDirectory.avatarPresetEnum()) }
    var avatarBase64 by remember(myDirectory.avatarBase64) { mutableStateOf(myDirectory.avatarBase64) }

    var languageCode by remember(myAccount.languageCode) { mutableStateOf(myAccount.languageCode) }
    var soundEnabled by remember(myAccount.soundEnabled) { mutableStateOf(myAccount.soundEnabled) }
    var vibrationEnabled by remember(myAccount.vibrationEnabled) { mutableStateOf(myAccount.vibrationEnabled) }

    var chatStyle by remember(myAccount) {
        mutableStateOf(
            MessageStyleDraft(
                fontId = myAccount.chatFontId,
                colorHex = myAccount.chatColorHex,
                bold = myAccount.chatBold,
                italic = myAccount.chatItalic,
                underline = myAccount.chatUnderline
            )
        )
    }

    var editingNameStyle by remember { mutableStateOf(false) }
    var editingChatStyle by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = MediaUtils.encodeImageUriToBase64(context, uri, maxDimPx = 280, maxChars = 160_000)
            if (encoded != null) {
                avatarBase64 = encoded
                avatarPreset = AvatarPreset.CUSTOM
            }
        }
    }

    if (editingNameStyle) {
        FontColorStylePickerSheet(
            title = strings.nameStyleLabel,
            sampleText = displayName.ifBlank { msnHandle },
            initialFontId = nameFontId,
            initialColorHex = nameColorHex,
            initialBold = nameBold,
            initialItalic = nameItalic,
            initialUnderline = nameUnderline,
            showStrike = false,
            onDismiss = { editingNameStyle = false },
            onApply = { fId, col, b, i, u, _ ->
                nameFontId = fId
                nameColorHex = col
                nameBold = b
                nameItalic = i
                nameUnderline = u
                editingNameStyle = false
            }
        )
    }

    if (editingChatStyle) {
        FontColorStylePickerSheet(
            title = strings.messageStyleLabel,
            sampleText = "MSN Messenger ${strings.typeMessageHint}",
            initialFontId = chatStyle.fontId,
            initialColorHex = chatStyle.colorHex,
            initialBold = chatStyle.bold,
            initialItalic = chatStyle.italic,
            initialUnderline = chatStyle.underline,
            showStrike = false,
            onDismiss = { editingChatStyle = false },
            onApply = { fId, col, b, i, u, _ ->
                chatStyle = chatStyle.copy(fontId = fId, colorHex = col, bold = b, italic = i, underline = u)
                editingChatStyle = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.profileSettingsTitle) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MsnAzurePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar Section
                Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MsnAvatarFrame(
                            avatarPreset = avatarPreset,
                            avatarBase64 = avatarBase64,
                            status = myDirectory.statusEnum(),
                            size = 68.dp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings.avatarLabel, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    AvatarPreset.DUCK,
                                    AvatarPreset.ASTRONAUT,
                                    AvatarPreset.CAT,
                                    AvatarPreset.BUTTERFLY
                                ).forEach { preset ->
                                    MsnAvatarFrame(
                                        avatarPreset = preset,
                                        avatarBase64 = "",
                                        status = if (avatarPreset == preset && avatarBase64.isBlank()) myDirectory.statusEnum() else com.example.model.MsnStatus.OFFLINE,
                                        size = 40.dp,
                                        onClick = {
                                            avatarPreset = preset
                                            avatarBase64 = ""
                                        }
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.testTag("settings_choose_photo")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(strings.choosePhotoBtn)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = msnHandle,
                    onValueChange = { msnHandle = it },
                    label = { Text(strings.msnHandleLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(strings.displayNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = personalMsg,
                    onValueChange = { personalMsg = it },
                    label = { Text(strings.personalMessageHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Name Font & Color
                Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.nameStyleLabel, style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = displayName.ifBlank { msnHandle },
                                color = parseHexColor(nameColorHex, MsnAzurePrimary),
                                fontFamily = MsnFontCatalog.resolve(nameFontId),
                                fontSize = 18.sp,
                                fontWeight = if (nameBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (nameItalic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = if (nameUnderline) TextDecoration.Underline else TextDecoration.None
                            )
                        }
                        OutlinedButton(
                            onClick = { editingNameStyle = true },
                            modifier = Modifier.testTag("settings_name_style_button")
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(strings.fontAction)
                        }
                    }
                }

                // Default Chat Text Font & Color
                Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.messageStyleLabel, style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "AaBbCc 😊",
                                color = parseHexColor(chatStyle.colorHex, MaterialTheme.colorScheme.onSurface),
                                fontFamily = MsnFontCatalog.resolve(chatStyle.fontId),
                                fontSize = 17.sp,
                                fontWeight = if (chatStyle.bold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (chatStyle.italic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = if (chatStyle.underline) TextDecoration.Underline else TextDecoration.None
                            )
                        }
                        OutlinedButton(
                            onClick = { editingChatStyle = true },
                            modifier = Modifier.testTag("settings_chat_style_button")
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(strings.fontAction)
                        }
                    }
                }

                // 26 Languages Picker
                LanguageSelectorCard(
                    selectedCode = languageCode,
                    label = strings.languageLabel,
                    onSelectLanguage = { newCode ->
                        languageCode = newCode
                        onSavePrefs(newCode, soundEnabled, vibrationEnabled, chatStyle)
                    }
                )

                // Sounds & Vibration
                Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.soundsLabel, style = MaterialTheme.typography.titleMedium)
                            Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.vibrationLabel, style = MaterialTheme.typography.titleMedium)
                            Switch(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it })
                        }
                        OutlinedButton(
                            onClick = { MsnSoundEngine.playSignIn(true) },
                            modifier = Modifier.testTag("test_sound_button")
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(strings.testSoundBtn)
                        }
                    }
                }

                Button(
                    onClick = {
                        onSaveProfile(
                            msnHandle,
                            displayName,
                            personalMsg,
                            languageCode,
                            nameColorHex,
                            nameFontId,
                            nameBold,
                            nameItalic,
                            nameUnderline,
                            avatarPreset,
                            avatarBase64
                        )
                        onSavePrefs(languageCode, soundEnabled, vibrationEnabled, chatStyle)
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button")
                ) {
                    Text(strings.saveBtn, style = MaterialTheme.typography.titleMedium)
                }

                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MsnRedBusy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(strings.signOutBtn)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
