package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.audio.VoiceClipPlayer
import com.example.audio.VoiceClipRecorder
import com.example.i18n.LocalAppStrings
import com.example.media.MediaUtils
import com.example.model.BuddyDirectoryEntry
import com.example.model.ChatMessage
import com.example.model.ChatThread
import com.example.model.GameType
import com.example.model.MessageType
import com.example.model.MsnCatalog
import com.example.model.MsnStatus
import com.example.model.MsnWink
import com.example.model.MultiplayerGame
import com.example.ui.components.FontColorStylePickerSheet
import com.example.ui.components.InChatGamePanel
import com.example.ui.components.MsnAvatarFrame
import com.example.ui.components.StyledBuddyName
import com.example.ui.components.statusLabel
import com.example.ui.theme.MsnAmberAccent
import com.example.ui.theme.MsnAzurePrimary
import com.example.ui.theme.MsnFontCatalog
import com.example.ui.theme.MsnHeaderBottom
import com.example.ui.theme.MsnHeaderTop
import com.example.ui.theme.MsnIceBlue
import com.example.ui.theme.MsnRedBusy
import com.example.ui.theme.parseHexColor
import com.example.viewmodel.MessageStyleDraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatAndGamesPane(
    currentUserId: String,
    myDirectory: BuddyDirectoryEntry,
    partnerDirectory: BuddyDirectoryEntry,
    chatThread: ChatThread?,
    messages: List<ChatMessage>,
    games: List<MultiplayerGame>,
    messageStyle: MessageStyleDraft,
    nudgeTrigger: Long,
    onBack: (() -> Unit)?,
    onDraftChanged: (String) -> Unit,
    onStyleChanged: (MessageStyleDraft) -> Unit,
    onSendText: (String) -> Unit,
    onSendNudge: (String) -> Unit,
    onSendWink: (MsnWink) -> Unit,
    onSendRandomWink: () -> Unit,
    onReplayWink: (MsnWink) -> Unit,
    onSendImage: (String) -> Unit,
    onSendVideo: (String, String, Long) -> Unit,
    onSendVoice: (String, Long) -> Unit,
    onErrorNotice: (String) -> Unit,
    onStartNewGame: (GameType) -> Unit,
    onPlayGomoku: (MultiplayerGame, Int, Boolean) -> Unit,
    onPlayMemory: (MultiplayerGame, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    if (onBack != null) {
        BackHandler { onBack() }
    }

    var draftText by remember { mutableStateOf("") }
    var showEmoticons by remember { mutableStateOf(false) }
    var showWinks by remember { mutableStateOf(false) }
    var showFontPicker by remember { mutableStateOf(false) }
    var showGames by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    var fullscreenImageBase64 by remember { mutableStateOf<String?>(null) }
    var playingVideoMsg by remember { mutableStateOf<ChatMessage?>(null) }

    val voiceRecorder = remember(context) { VoiceClipRecorder(context) }
    val voicePlayer = remember(context) { VoiceClipPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.stopAndDiscard()
            voicePlayer.stop()
        }
    }

    // Classic MSN Nudge screen shake animation
    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }
    LaunchedEffect(nudgeTrigger) {
        if (nudgeTrigger > 0L) {
            val offsets = listOf(-20f, 20f, -16f, 16f, -12f, 12f, -6f, 6f, 0f)
            for (o in offsets) {
                shakeX.animateTo(o, tween(35))
                shakeY.animateTo(o * 0.45f, tween(35))
            }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (MediaUtils.isVideoUri(context, uri)) {
                MediaUtils.encodeVideoUriToPayload(context, uri)
                    .onSuccess { payload ->
                        onSendVideo(payload.videoBase64, payload.thumbBase64, payload.durationMs)
                    }
                    .onFailure { err ->
                        onErrorNotice(err.message ?: "Video too large")
                    }
            } else {
                val b64 = MediaUtils.encodeImageUriToBase64(context, uri, maxDimPx = 720, maxChars = 480_000)
                if (b64 != null) {
                    onSendImage(b64)
                } else {
                    onErrorNotice("Could not encode selected image.")
                }
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (voiceRecorder.startRecording()) {
                isRecordingVoice = true
            }
        } else {
            onErrorNotice("Microphone permission is needed to record voice clips.")
        }
    }

    if (showFontPicker) {
        FontColorStylePickerSheet(
            title = strings.fontAction,
            sampleText = draftText.ifBlank { "MSN Messenger 😊" },
            initialFontId = messageStyle.fontId,
            initialColorHex = messageStyle.colorHex,
            initialBold = messageStyle.bold,
            initialItalic = messageStyle.italic,
            initialUnderline = messageStyle.underline,
            initialStrike = messageStyle.strikethrough,
            showStrike = true,
            onDismiss = { showFontPicker = false },
            onApply = { fId, col, b, i, u, s ->
                onStyleChanged(MessageStyleDraft(fId, col, b, i, u, s))
                showFontPicker = false
            }
        )
    }

    if (fullscreenImageBase64 != null) {
        val bmp = remember(fullscreenImageBase64) { MediaUtils.decodeBase64ToBitmap(fullscreenImageBase64 ?: "") }
        Dialog(onDismissRequest = { fullscreenImageBase64 = null }) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.Black) {
                Box(modifier = Modifier.padding(8.dp)) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Full Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        )
                    }
                    IconButton(
                        onClick = { fullscreenImageBase64 = null },
                        modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }

    if (playingVideoMsg != null) {
        val vidMsg = playingVideoMsg!!
        val vidFile = remember(vidMsg.id) {
            MediaUtils.writeBase64VideoToCache(context, vidMsg.id, vidMsg.mediaBase64)
        }
        Dialog(onDismissRequest = { playingVideoMsg = null }) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.Black) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.End) {
                    IconButton(onClick = { playingVideoMsg = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    if (vidFile != null) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoPath(vidFile.absolutePath)
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    start()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(300.dp)
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(shakeX.value.roundToInt(), shakeY.value.roundToInt()) }
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .navigationBarsPadding()
    ) {
        // Glossy MSN Conversation Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(MsnHeaderTop, MsnHeaderBottom)))
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
            MsnAvatarFrame(
                avatarPreset = partnerDirectory.avatarPresetEnum(),
                avatarBase64 = partnerDirectory.avatarBase64,
                status = partnerDirectory.statusEnum(),
                size = 44.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Surface(color = Color.White.copy(0.9f), shape = RoundedCornerShape(6.dp)) {
                    StyledBuddyName(
                        entry = partnerDirectory,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = buildString {
                        append(statusLabel(partnerDirectory.statusEnum(), strings))
                        if (partnerDirectory.personalMessage.isNotBlank()) {
                            append(" — ${partnerDirectory.personalMessage}")
                        }
                    },
                    color = Color(0xFFDCEBFC),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = { showGames = !showGames },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (showGames) Color.White else Color.White.copy(0.18f),
                    contentColor = if (showGames) MsnAzurePrimary else Color.White
                ),
                modifier = Modifier.testTag("toggle_games_button")
            ) {
                Icon(Icons.Default.SportsEsports, contentDescription = strings.gamesAction, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.gamesAction, maxLines = 1)
            }
        }

        // Collapsible In-Chat Multiplayer Games (XO 5-in-a-row & Memory)
        AnimatedVisibility(visible = showGames) {
            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                InChatGamePanel(
                    currentUserId = currentUserId,
                    partnerUid = partnerDirectory.userId,
                    games = games,
                    onStartNewGame = onStartNewGame,
                    onPlayGomoku = onPlayGomoku,
                    onPlayMemory = onPlayMemory,
                    onClose = { showGames = false }
                )
            }
        }

        // Message History
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id.ifBlank { "${it.senderId}_${it.createdAt}" } }) { msg ->
                val sender = if (msg.senderId == currentUserId) myDirectory else partnerDirectory
                MsnMessageItem(
                    message = msg,
                    sender = sender,
                    isMine = msg.senderId == currentUserId,
                    isPlayingVoice = playingVoiceId == msg.id,
                    onOpenImage = { fullscreenImageBase64 = msg.mediaBase64 },
                    onPlayVideo = { playingVideoMsg = msg },
                    onToggleVoice = {
                        if (playingVoiceId == msg.id) {
                            voicePlayer.stop()
                            playingVoiceId = null
                        } else {
                            playingVoiceId = msg.id
                            voicePlayer.play(msg.id, msg.mediaBase64) {
                                playingVoiceId = null
                            }
                        }
                    },
                    onReplayWink = {
                        val w = MsnCatalog.winks.firstOrNull { it.id == msg.winkId } ?: MsnCatalog.winks.first()
                        onReplayWink(w)
                    }
                )
            }
        }

        // Typing Indicator
        val partnerTyping = chatThread?.typingUids?.any { it == partnerDirectory.userId && it != currentUserId } == true
        if (partnerTyping) {
            Text(
                text = "✍️ ${partnerDirectory.displayName} ${strings.typingIndicator}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )
        }

        // Emoticons Picker Tray
        AnimatedVisibility(visible = showEmoticons) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MsnCatalog.emoticons.forEach { emote ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    draftText += emote.glyph
                                    onDraftChanged(draftText)
                                }
                                .testTag("emote_${emote.shortcut}")
                        ) {
                            Text(
                                text = emote.glyph,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Winks & Random Animation Tray
        AnimatedVisibility(visible = showWinks) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.winkAction, style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = {
                                showWinks = false
                                onSendRandomWink()
                            },
                            modifier = Modifier.testTag("random_wink_button")
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.randomWinkAction)
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MsnCatalog.winks.forEach { wink ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MsnIceBlue,
                                modifier = Modifier
                                    .clickable {
                                        showWinks = false
                                        onSendWink(wink)
                                    }
                                    .testTag("wink_${wink.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(wink.glyph, fontSize = 28.sp)
                                    Text(wink.title, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Classic MSN Formatting & Action Toolbar + Composer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showFontPicker = true },
                        modifier = Modifier.testTag("toolbar_font_button")
                    ) {
                        Icon(
                            Icons.Default.FormatColorText,
                            contentDescription = strings.fontAction,
                            tint = parseHexColor(messageStyle.colorHex, MsnAzurePrimary)
                        )
                    }
                    IconButton(
                        onClick = {
                            showEmoticons = !showEmoticons
                            showWinks = false
                        },
                        modifier = Modifier.testTag("toolbar_emoticon_button")
                    ) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = strings.emoticonsAction, tint = MsnAmberAccent)
                    }
                    IconButton(
                        onClick = { onSendNudge(strings.nudgeSentText) },
                        modifier = Modifier.testTag("toolbar_nudge_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = strings.nudgeAction, tint = MsnRedBusy)
                    }
                    IconButton(
                        onClick = {
                            showWinks = !showWinks
                            showEmoticons = false
                        },
                        modifier = Modifier.testTag("toolbar_wink_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = strings.winkAction, tint = Color(0xFF8E24AA))
                    }
                    IconButton(
                        onClick = {
                            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        modifier = Modifier.testTag("toolbar_media_button")
                    ) {
                        Icon(Icons.Default.PermMedia, contentDescription = strings.mediaAction, tint = MsnAzurePrimary)
                    }
                    IconButton(
                        onClick = {
                            if (isRecordingVoice) {
                                isRecordingVoice = false
                                val res = voiceRecorder.stopAndEncode()
                                if (res != null) {
                                    onSendVoice(res.first, res.second)
                                } else {
                                    onErrorNotice("Voice recording failed or was too long.")
                                }
                            } else {
                                val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                if (perm == PackageManager.PERMISSION_GRANTED) {
                                    if (voiceRecorder.startRecording()) isRecordingVoice = true
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier.testTag("toolbar_voice_button")
                    ) {
                        Icon(
                            imageVector = if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = strings.voiceAction,
                            tint = if (isRecordingVoice) MsnRedBusy else Color(0xFF00897B)
                        )
                    }
                    if (isRecordingVoice) {
                        Text(
                            text = "● ${strings.recordingLabel}",
                            color = MsnRedBusy,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val draftColor = parseHexColor(messageStyle.colorHex, MaterialTheme.colorScheme.onSurface)
                    val draftDeco = when {
                        messageStyle.underline && messageStyle.strikethrough ->
                            TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                        messageStyle.underline -> TextDecoration.Underline
                        messageStyle.strikethrough -> TextDecoration.LineThrough
                        else -> TextDecoration.None
                    }
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = {
                            draftText = it
                            onDraftChanged(it)
                        },
                        placeholder = { Text(strings.typeMessageHint) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = draftColor,
                            fontFamily = MsnFontCatalog.resolve(messageStyle.fontId),
                            fontWeight = if (messageStyle.bold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (messageStyle.italic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = draftDeco
                        ),
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("chat_message_input")
                    )

                    Button(
                        onClick = {
                            val text = draftText.trim()
                            if (text.isNotEmpty()) {
                                onSendText(text)
                                draftText = ""
                                onDraftChanged("")
                                showEmoticons = false
                            }
                        },
                        enabled = draftText.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp).testTag("send_message_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = strings.sendBtn)
                    }
                }
            }
        }
    }
}

@Composable
private fun MsnMessageItem(
    message: ChatMessage,
    sender: BuddyDirectoryEntry,
    isMine: Boolean,
    isPlayingVoice: Boolean,
    onOpenImage: () -> Unit,
    onPlayVideo: () -> Unit,
    onToggleVoice: () -> Unit,
    onReplayWink: () -> Unit
) {
    val timeText = remember(message.createdAt) {
        val date = message.createdAt?.toDate() ?: Date()
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        // Classic MSN attribution line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            StyledBuddyName(entry = sender, fontSize = 12.sp)
            Text(
                text = "($timeText):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (message.messageTypeEnum()) {
            MessageType.NUDGE -> {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MsnRedBusy.copy(alpha = 0.12f),
                    modifier = Modifier.border(1.dp, MsnRedBusy.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = "⚡ ${sender.displayName} ${message.text}",
                        color = MsnRedBusy,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            MessageType.WINK -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MsnIceBlue,
                    modifier = Modifier
                        .border(1.dp, MsnAzurePrimary.copy(0.5f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onReplayWink)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MsnAzurePrimary)
                        Text(
                            text = message.text,
                            fontWeight = FontWeight.Bold,
                            color = MsnAzurePrimary
                        )
                    }
                }
            }

            MessageType.IMAGE -> {
                val bmp = remember(message.mediaBase64) { MediaUtils.decodeBase64ToBitmap(message.mediaBase64) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.clickable(onClick = onOpenImage)
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Shared Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 210.dp, height = 160.dp)
                        )
                    } else {
                        Text("📷 Photo", modifier = Modifier.padding(12.dp))
                    }
                }
            }

            MessageType.VIDEO -> {
                val thumb = remember(message.mediaThumbBase64) { MediaUtils.decodeBase64ToBitmap(message.mediaThumbBase64) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black,
                    modifier = Modifier
                        .size(width = 220.dp, height = 145.dp)
                        .clickable(onClick = onPlayVideo)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = "Video Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        val secs = (message.mediaDurationMs / 1000L).coerceAtLeast(1L)
                        Text(
                            text = "${secs}s",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            MessageType.VOICE -> {
                val secs = (message.mediaDurationMs / 1000L).coerceAtLeast(1L)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMine) MsnIceBlue else MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(0.5f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggleVoice)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingVoice) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Play Voice Clip",
                            tint = MsnAzurePrimary
                        )
                        Text(
                            text = "🎤 ${secs}s",
                            fontWeight = FontWeight.SemiBold,
                            color = MsnAzurePrimary
                        )
                    }
                }
            }

            MessageType.TEXT -> {
                val msgColor = remember(message.colorHex) {
                    parseHexColor(message.colorHex, Color(0xFF102033))
                }
                val deco = when {
                    message.underline && message.strikethrough ->
                        TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    message.underline -> TextDecoration.Underline
                    message.strikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMine) Color(0xFFE7F2FE) else MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.border(
                        1.dp,
                        if (isMine) Color(0xFFB5D3F8) else MaterialTheme.colorScheme.outline.copy(0.4f),
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Text(
                        text = message.text,
                        color = msgColor,
                        fontFamily = MsnFontCatalog.resolve(message.fontId),
                        fontSize = 15.5.sp,
                        fontWeight = if (message.bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (message.italic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = deco,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
