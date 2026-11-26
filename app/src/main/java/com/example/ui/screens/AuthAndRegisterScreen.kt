package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.i18n.LocalAppStrings
import com.example.i18n.MsnLanguages
import com.example.media.MediaUtils
import com.example.model.AvatarPreset
import com.example.model.MsnStatus
import com.example.ui.components.FontColorStylePickerSheet
import com.example.ui.components.MsnAvatarFrame
import com.example.ui.theme.MsnAzurePrimary
import com.example.ui.theme.MsnFontCatalog
import com.example.ui.theme.MsnHeaderBottom
import com.example.ui.theme.MsnHeaderTop
import com.example.ui.theme.parseHexColor
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for Google Sign-In")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

@Composable
fun AuthScreen(
    selectedLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isAutoChecking by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isAutoChecking = false
                onAuthSuccess()
            },
            onUnauthenticated = { isAutoChecking = false },
            scope = scope
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "msn_spin")
    val sway by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "sway"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A4A9E), Color(0xFF4CA1F5), Color(0xFFEAF3FD))
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Classic MSN Aero Hero Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_msn_hero),
                            contentDescription = "MSN Messenger Hero",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xAA0A3E85))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.msn_launcher_icon),
                                contentDescription = "MSN Buddy Icon",
                                modifier = Modifier
                                    .size(56.dp)
                                    .graphicsLayer { rotationZ = if (isLoading) sway * 3f else sway }
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                            )
                            Column {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Windows XP Classic Edition",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFD6EAFF)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = strings.signInTitle,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = strings.signInSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isLoading = false }
                                )
                            },
                            enabled = !isLoading && !isAutoChecking,
                            colors = ButtonDefaults.buttonColors(containerColor = MsnAzurePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading || isAutoChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = strings.signInWithGoogle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 26-Language Picker right on the Sign-In screen
            LanguageSelectorCard(
                selectedCode = selectedLanguageCode,
                label = strings.languageLabel,
                onSelectLanguage = onSelectLanguage
            )
        }
    }
}

@Composable
fun LanguageSelectorCard(
    selectedCode: String,
    label: String,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val current = MsnLanguages.all.firstOrNull { it.code == selectedCode } ?: MsnLanguages.all.first()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("language_selector_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${current.flagEmoji} ${current.nativeName}", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.height(340.dp)
            ) {
                MsnLanguages.all.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text("${lang.flagEmoji}  ${lang.nativeName}") },
                        onClick = {
                            expanded = false
                            onSelectLanguage(lang.code)
                        },
                        modifier = Modifier.testTag("lang_option_${lang.code}")
                    )
                }
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    defaultEmail: String,
    defaultDisplayName: String,
    selectedLanguageCode: String,
    isBusy: Boolean,
    onSelectLanguage: (String) -> Unit,
    onRegister: (
        handle: String,
        displayName: String,
        personalMessage: String,
        nameColorHex: String,
        nameFontId: String,
        nameBold: Boolean,
        nameItalic: Boolean,
        nameUnderline: Boolean,
        avatarPreset: AvatarPreset,
        avatarBase64: String
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var handle by remember {
        mutableStateOf(defaultEmail.substringBefore("@").ifBlank { "msnuser" })
    }
    var displayName by remember {
        mutableStateOf(defaultDisplayName.ifBlank { handle })
    }
    var personalMessage by remember { mutableStateOf("") }
    var nameColorHex by remember { mutableStateOf("#0F62C9") }
    var nameFontId by remember { mutableStateOf("open_sans") }
    var nameBold by remember { mutableStateOf(true) }
    var nameItalic by remember { mutableStateOf(false) }
    var nameUnderline by remember { mutableStateOf(false) }
    var avatarPreset by remember { mutableStateOf(AvatarPreset.DUCK) }
    var avatarBase64 by remember { mutableStateOf("") }
    var showStylePicker by remember { mutableStateOf(false) }

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

    if (showStylePicker) {
        FontColorStylePickerSheet(
            title = strings.nameStyleLabel,
            sampleText = displayName.ifBlank { handle },
            initialFontId = nameFontId,
            initialColorHex = nameColorHex,
            initialBold = nameBold,
            initialItalic = nameItalic,
            initialUnderline = nameUnderline,
            showStrike = false,
            onDismiss = { showStylePicker = false },
            onApply = { fId, col, b, i, u, _ ->
                nameFontId = fId
                nameColorHex = col
                nameBold = b
                nameItalic = i
                nameUnderline = u
                showStylePicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(MsnHeaderTop, MsnHeaderBottom)))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = strings.registerTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = strings.registerSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFDCEBFC)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar Selection
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MsnAvatarFrame(
                            avatarPreset = avatarPreset,
                            avatarBase64 = avatarBase64,
                            status = MsnStatus.ONLINE,
                            size = 72.dp
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
                                    val selected = avatarPreset == preset && avatarBase64.isBlank()
                                    MsnAvatarFrame(
                                        avatarPreset = preset,
                                        avatarBase64 = "",
                                        status = if (selected) MsnStatus.ONLINE else MsnStatus.OFFLINE,
                                        size = 42.dp,
                                        modifier = Modifier.testTag("preset_${preset.code.lowercase()}"),
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
                                modifier = Modifier.testTag("register_choose_photo")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(strings.choosePhotoBtn)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = handle,
                        onValueChange = { handle = it },
                        label = { Text(strings.msnHandleLabel) },
                        supportingText = { Text(strings.msnHandleHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("register_handle_input")
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(strings.displayNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("register_name_input")
                    )

                    // Live Styled Name Preview + Font/Color button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName.ifBlank { handle },
                                color = parseHexColor(nameColorHex, MsnAzurePrimary),
                                fontFamily = MsnFontCatalog.resolve(nameFontId),
                                fontSize = 18.sp,
                                fontWeight = if (nameBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (nameItalic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = if (nameUnderline) TextDecoration.Underline else TextDecoration.None,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { showStylePicker = true },
                                modifier = Modifier.testTag("register_name_style_button")
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(strings.fontAction)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = personalMessage,
                        onValueChange = { personalMessage = it },
                        label = { Text(strings.personalMessageHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("register_personal_msg_input")
                    )

                    LanguageSelectorCard(
                        selectedCode = selectedLanguageCode,
                        label = strings.languageLabel,
                        onSelectLanguage = onSelectLanguage
                    )

                    Button(
                        onClick = {
                            onRegister(
                                handle,
                                displayName,
                                personalMessage,
                                nameColorHex,
                                nameFontId,
                                nameBold,
                                nameItalic,
                                nameUnderline,
                                avatarPreset,
                                avatarBase64
                            )
                        },
                        enabled = !isBusy && handle.trim().length >= 2 && displayName.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("complete_registration_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(strings.createAccountBtn, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
