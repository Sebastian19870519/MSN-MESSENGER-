package com.example.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.MsnRepository
import com.example.data.PreferencesManager
import com.example.i18n.LocalAppStrings
import com.example.model.BuddyDirectoryEntry
import com.example.model.UserAccount
import com.example.ui.components.WinkOverlay
import com.example.viewmodel.MsnViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthenticatedMsnApp(
    currentUserId: String,
    prefsManager: PreferencesManager,
    languageCode: String
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MsnViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MsnViewModel(
                    repository = MsnRepository(db),
                    prefs = prefsManager,
                    currentUserId = currentUserId
                )
            }
        }
    )

    LaunchedEffect(currentUserId) {
        viewModel.observeIncomingChatEffects(context)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val myAccountState by viewModel.myAccountState.collectAsStateWithLifecycle()
    val myDirState by viewModel.myDirectoryState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val buddyMap by viewModel.buddyDirectoryMap.collectAsStateWithLifecycle()
    val activePartnerUid by viewModel.activePartnerUid.collectAsStateWithLifecycle()
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val gamesState by viewModel.activeGamesState.collectAsStateWithLifecycle()
    val messageStyle by viewModel.messageStyle.collectAsStateWithLifecycle()
    val nudgeTrigger by viewModel.nudgeTrigger.collectAsStateWithLifecycle()
    val activeWink by viewModel.activeWink.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var showingSettings by remember { mutableStateOf(false) }

    fun performSignOut() {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { outerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = outerPadding.calculateBottomPadding())) {
            when (val dirState = myDirState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(dirState.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            Button(onClick = { performSignOut() }) {
                                Text(strings.signOutBtn)
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val myDir = dirState.data
                    val myAcc = (myAccountState as? UiState.Success)?.data
                        ?: UserAccount(userId = currentUserId, languageCode = languageCode)

                    if (myDir == null) {
                        val user = Firebase.auth.currentUser
                        RegistrationScreen(
                            defaultEmail = user?.email ?: "",
                            defaultDisplayName = user?.displayName ?: "",
                            selectedLanguageCode = languageCode,
                            isBusy = isBusy,
                            onSelectLanguage = { newCode ->
                                scope.launch { prefsManager.setLanguageCode(newCode) }
                            },
                            onRegister = { handle, dispName, pMsg, col, font, bold, ital, under, preset, avatarB64 ->
                                viewModel.registerOrSaveProfile(
                                    msnHandle = handle,
                                    displayName = dispName,
                                    personalMessage = pMsg,
                                    languageCode = languageCode,
                                    nameColorHex = col,
                                    nameFontId = font,
                                    nameBold = bold,
                                    nameItalic = ital,
                                    nameUnderline = under,
                                    avatarPreset = preset,
                                    avatarBase64 = avatarB64
                                )
                            }
                        )
                    } else if (showingSettings) {
                        BackHandler { showingSettings = false }
                        ProfileAndSettingsScreen(
                            myAccount = myAcc,
                            myDirectory = myDir,
                            isBusy = isBusy,
                            onBack = { showingSettings = false },
                            onSaveProfile = { handle, dispName, pMsg, lang, col, font, bold, ital, under, preset, avatarB64 ->
                                viewModel.registerOrSaveProfile(
                                    msnHandle = handle,
                                    displayName = dispName,
                                    personalMessage = pMsg,
                                    languageCode = lang,
                                    nameColorHex = col,
                                    nameFontId = font,
                                    nameBold = bold,
                                    nameItalic = ital,
                                    nameUnderline = under,
                                    avatarPreset = preset,
                                    avatarBase64 = avatarB64,
                                    onSuccess = { showingSettings = false }
                                )
                            },
                            onSavePrefs = { lang, soundOn, vibOn, defStyle ->
                                viewModel.updateAccountPreferences(lang, soundOn, vibOn, defStyle)
                            },
                            onSignOut = { performSignOut() }
                        )
                    } else {
                        val contacts = (contactsState as? UiState.Success)?.data ?: emptyList()
                        val chats = (chatsState as? UiState.Success)?.data ?: emptyList()
                        val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
                        val games = (gamesState as? UiState.Success)?.data ?: emptyList()
                        val activeThread = chats.firstOrNull { it.id == activeChatId }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isTabletLayout = maxWidth >= 840.dp

                            if (isTabletLayout) {
                                // Tablet / Wide Screen Canonical Two-Pane MSN Desktop Experience
                                Row(modifier = Modifier.fillMaxSize()) {
                                    BuddyListPane(
                                        currentUserId = currentUserId,
                                        myDirectory = myDir,
                                        contacts = contacts,
                                        chats = chats,
                                        buddyDirectoryMap = buddyMap,
                                        selectedPartnerUid = activePartnerUid,
                                        onSelectPartner = { viewModel.openChatWith(it) },
                                        onStatusChange = { viewModel.updateStatus(it) },
                                        onPersonalMessageSave = { viewModel.updatePersonalMessage(it) },
                                        onAddContact = { handle ->
                                            viewModel.addContactByHandle(handle, strings.handleNotFoundErr) { uid ->
                                                viewModel.openChatWith(uid)
                                            }
                                        },
                                        onToggleBlock = { uid, blk -> viewModel.toggleBlockContact(uid, blk) },
                                        onRemoveContact = { uid -> viewModel.removeContact(uid) },
                                        onOpenSettings = { showingSettings = true },
                                        onCopiedNotice = { viewModel.emitNotice(it) },
                                        modifier = Modifier.width(360.dp).fillMaxHeight()
                                    )
                                    VerticalDivider()
                                    val partnerUid = activePartnerUid
                                    if (partnerUid != null) {
                                        val partnerDir = buddyMap[partnerUid] ?: BuddyDirectoryEntry(
                                            userId = partnerUid,
                                            msnHandle = partnerUid,
                                            displayName = partnerUid
                                        )
                                        ChatAndGamesPane(
                                            currentUserId = currentUserId,
                                            myDirectory = myDir,
                                            partnerDirectory = partnerDir,
                                            chatThread = activeThread,
                                            messages = messages,
                                            games = games,
                                            messageStyle = messageStyle,
                                            nudgeTrigger = nudgeTrigger,
                                            onBack = null,
                                            onDraftChanged = { viewModel.onDraftChanged(it) },
                                            onStyleChanged = { viewModel.updateMessageStyle(it) },
                                            onSendText = { viewModel.sendTextMessage(it) },
                                            onSendNudge = { txt -> viewModel.sendNudge(context, txt) },
                                            onSendWink = { viewModel.sendWink(it) },
                                            onSendRandomWink = { viewModel.sendRandomWink() },
                                            onReplayWink = { viewModel.replayWink(it) },
                                            onSendImage = { b64 -> viewModel.sendImageMessage(b64) },
                                            onSendVideo = { vb64, tb64, dur -> viewModel.sendVideoMessage(vb64, tb64, dur) },
                                            onSendVoice = { ab64, dur -> viewModel.sendVoiceMessage(ab64, dur) },
                                            onErrorNotice = { viewModel.emitNotice(it) },
                                            onStartNewGame = { viewModel.startNewGame(it) },
                                            onPlayGomoku = { g, idx, bot -> viewModel.playGomokuMove(g, idx, bot) },
                                            onPlayMemory = { g, idx -> viewModel.playMemoryFlip(g, idx) },
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    } else {
                                        EmptyChatPlaceholder(
                                            onStartSelfChat = { viewModel.openChatWith(currentUserId) },
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                }
                            } else {
                                // Phone Layout: Buddy List or Active Chat
                                val partnerUid = activePartnerUid
                                if (partnerUid != null) {
                                    val partnerDir = buddyMap[partnerUid] ?: BuddyDirectoryEntry(
                                        userId = partnerUid,
                                        msnHandle = partnerUid,
                                        displayName = partnerUid
                                    )
                                    ChatAndGamesPane(
                                        currentUserId = currentUserId,
                                        myDirectory = myDir,
                                        partnerDirectory = partnerDir,
                                        chatThread = activeThread,
                                        messages = messages,
                                        games = games,
                                        messageStyle = messageStyle,
                                        nudgeTrigger = nudgeTrigger,
                                        onBack = { viewModel.closeActiveChat() },
                                        onDraftChanged = { viewModel.onDraftChanged(it) },
                                        onStyleChanged = { viewModel.updateMessageStyle(it) },
                                        onSendText = { viewModel.sendTextMessage(it) },
                                        onSendNudge = { txt -> viewModel.sendNudge(context, txt) },
                                        onSendWink = { viewModel.sendWink(it) },
                                        onSendRandomWink = { viewModel.sendRandomWink() },
                                        onReplayWink = { viewModel.replayWink(it) },
                                        onSendImage = { b64 -> viewModel.sendImageMessage(b64) },
                                        onSendVideo = { vb64, tb64, dur -> viewModel.sendVideoMessage(vb64, tb64, dur) },
                                        onSendVoice = { ab64, dur -> viewModel.sendVoiceMessage(ab64, dur) },
                                        onErrorNotice = { viewModel.emitNotice(it) },
                                        onStartNewGame = { viewModel.startNewGame(it) },
                                        onPlayGomoku = { g, idx, bot -> viewModel.playGomokuMove(g, idx, bot) },
                                        onPlayMemory = { g, idx -> viewModel.playMemoryFlip(g, idx) }
                                    )
                                } else {
                                    BuddyListPane(
                                        currentUserId = currentUserId,
                                        myDirectory = myDir,
                                        contacts = contacts,
                                        chats = chats,
                                        buddyDirectoryMap = buddyMap,
                                        selectedPartnerUid = null,
                                        onSelectPartner = { viewModel.openChatWith(it) },
                                        onStatusChange = { viewModel.updateStatus(it) },
                                        onPersonalMessageSave = { viewModel.updatePersonalMessage(it) },
                                        onAddContact = { handle ->
                                            viewModel.addContactByHandle(handle, strings.handleNotFoundErr) { uid ->
                                                viewModel.openChatWith(uid)
                                            }
                                        },
                                        onToggleBlock = { uid, blk -> viewModel.toggleBlockContact(uid, blk) },
                                        onRemoveContact = { uid -> viewModel.removeContact(uid) },
                                        onOpenSettings = { showingSettings = true },
                                        onCopiedNotice = { viewModel.emitNotice(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            WinkOverlay(
                wink = activeWink,
                onDismiss = { viewModel.dismissWink() }
            )
        }
    }
}

@Composable
private fun EmptyChatPlaceholder(
    onStartSelfChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.msn_launcher_icon),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                )
                Text(strings.appTitle, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = strings.noContactsYet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onStartSelfChat) {
                    Text(strings.selfTestChat)
                }
            }
        }
    }
}
