package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MsnSoundEngine
import com.example.data.MsnRepository
import com.example.data.PreferencesManager
import com.example.model.AvatarPreset
import com.example.model.BuddyDirectoryEntry
import com.example.model.ChatMessage
import com.example.model.ChatThread
import com.example.model.GameType
import com.example.model.MessageType
import com.example.model.MsnCatalog
import com.example.model.MsnStatus
import com.example.model.MsnWink
import com.example.model.MultiplayerGame
import com.example.model.UserAccount
import com.example.model.UserContact
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MessageStyleDraft(
    val fontId: String = "open_sans",
    val colorHex: String = "#102033",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MsnViewModel(
    private val repository: MsnRepository,
    private val prefs: PreferencesManager,
    private val currentUserId: String
) : ViewModel() {

    private fun unwrapAndCheck(error: Throwable, tag: String): String {
        val fsErr = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsErr == null && error is CancellationException) throw error
        val root = fsErr ?: error
        Log.w(tag, "Stream error", root)
        return root.message ?: "Connection error"
    }

    val myAccountState: StateFlow<UiState<UserAccount?>> = repository.observeUserAccount(currentUserId)
        .map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapAndCheck(it, "MsnVM.Account"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myDirectoryState: StateFlow<UiState<BuddyDirectoryEntry?>> = repository.observeDirectoryEntry(currentUserId)
        .map<BuddyDirectoryEntry?, UiState<BuddyDirectoryEntry?>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapAndCheck(it, "MsnVM.Directory"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val contactsState: StateFlow<UiState<List<UserContact>>> = repository.observeContacts(currentUserId)
        .map<List<UserContact>, UiState<List<UserContact>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapAndCheck(it, "MsnVM.Contacts"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val chatsState: StateFlow<UiState<List<ChatThread>>> = repository.observeChats(currentUserId)
        .map<List<ChatThread>, UiState<List<ChatThread>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(unwrapAndCheck(it, "MsnVM.Chats"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _buddyDirectoryMap = MutableStateFlow<Map<String, BuddyDirectoryEntry>>(emptyMap())
    val buddyDirectoryMap: StateFlow<Map<String, BuddyDirectoryEntry>> = _buddyDirectoryMap.asStateFlow()

    private val directoryJobs = mutableMapOf<String, Job>()

    private val _activePartnerUid = MutableStateFlow<String?>(null)
    val activePartnerUid: StateFlow<String?> = _activePartnerUid.asStateFlow()

    val activeChatId: StateFlow<String?> = _activePartnerUid.map { partnerUid ->
        partnerUid?.let { repository.deterministicChatId(currentUserId, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> = activeChatId.flatMapLatest { chatId ->
        if (chatId == null) flowOf(UiState.Success(emptyList()))
        else repository.observeMessages(chatId)
            .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
            .catch { emit(UiState.Error(unwrapAndCheck(it, "MsnVM.Messages"))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    val activeGamesState: StateFlow<UiState<List<MultiplayerGame>>> = activeChatId.flatMapLatest { chatId ->
        if (chatId == null) flowOf(UiState.Success(emptyList()))
        else repository.observeGames(chatId)
            .map<List<MultiplayerGame>, UiState<List<MultiplayerGame>>> { UiState.Success(it) }
            .catch { emit(UiState.Error(unwrapAndCheck(it, "MsnVM.Games"))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    private val _messageStyle = MutableStateFlow(MessageStyleDraft())
    val messageStyle: StateFlow<MessageStyleDraft> = _messageStyle.asStateFlow()

    private val _nudgeTrigger = MutableStateFlow(0L)
    val nudgeTrigger: StateFlow<Long> = _nudgeTrigger.asStateFlow()

    private val _activeWink = MutableStateFlow<MsnWink?>(null)
    val activeWink: StateFlow<MsnWink?> = _activeWink.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private var seenMessageIds = mutableSetOf<String>()
    private var initialMessagesLoadedForChat: String? = null
    private var typingDebounceJob: Job? = null
    private var styleInitializedFromAccount = false
    private var signInSoundPlayed = false

    init {
        observeBuddyDirectoryEntries()
        syncPreferencesFromAccount()
    }

    private fun syncPreferencesFromAccount() {
        viewModelScope.launch {
            myAccountState.collect { state ->
                if (state is UiState.Success && state.data != null) {
                    val acc = state.data
                    prefs.setLanguageCode(acc.languageCode)
                    prefs.setSoundEnabled(acc.soundEnabled)
                    prefs.setVibrationEnabled(acc.vibrationEnabled)
                    if (!styleInitializedFromAccount) {
                        styleInitializedFromAccount = true
                        _messageStyle.value = MessageStyleDraft(
                            fontId = acc.chatFontId,
                            colorHex = acc.chatColorHex,
                            bold = acc.chatBold,
                            italic = acc.chatItalic,
                            underline = acc.chatUnderline,
                            strikethrough = false
                        )
                    }
                    if (!signInSoundPlayed) {
                        signInSoundPlayed = true
                        MsnSoundEngine.playSignIn(acc.soundEnabled)
                    }
                }
            }
        }
    }

    private fun observeBuddyDirectoryEntries() {
        viewModelScope.launch {
            combine(contactsState, chatsState, _activePartnerUid) { cState, chState, activePartner ->
                val uids = mutableSetOf(currentUserId)
                if (cState is UiState.Success) {
                    cState.data.forEach { uids.add(it.contactUid) }
                }
                if (chState is UiState.Success) {
                    chState.data.forEach { thread -> uids.addAll(thread.memberUids) }
                }
                if (activePartner != null) uids.add(activePartner)
                uids.toSet()
            }.collect { uids ->
                // Cancel removed listeners
                val toRemove = directoryJobs.keys - uids
                toRemove.forEach { uid ->
                    directoryJobs.remove(uid)?.cancel()
                }
                // Start new listeners
                uids.forEach { uid ->
                    if (!directoryJobs.containsKey(uid)) {
                        directoryJobs[uid] = viewModelScope.launch {
                            repository.observeDirectoryEntry(uid)
                                .catch { Log.w("MsnVM", "Buddy directory watch failed for $uid", it) }
                                .collect { entry ->
                                    if (entry != null) {
                                        val prev = _buddyDirectoryMap.value[uid]
                                        _buddyDirectoryMap.value = _buddyDirectoryMap.value + (uid to entry)
                                        if (uid != currentUserId &&
                                            prev != null &&
                                            prev.status == MsnStatus.OFFLINE.code &&
                                            entry.status == MsnStatus.ONLINE.code
                                        ) {
                                            val sound = (myAccountState.value as? UiState.Success)?.data?.soundEnabled ?: true
                                            MsnSoundEngine.playContactOnline(sound)
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    fun observeIncomingChatEffects(context: Context) {
        viewModelScope.launch {
            activeMessagesState.collect { state ->
                val chatId = activeChatId.value ?: return@collect
                if (state !is UiState.Success) return@collect
                val list = state.data
                if (initialMessagesLoadedForChat != chatId) {
                    initialMessagesLoadedForChat = chatId
                    seenMessageIds = list.map { it.id }.toMutableSet()
                    return@collect
                }
                val newMsgs = list.filter { it.id.isNotBlank() && seenMessageIds.add(it.id) }
                if (newMsgs.isEmpty()) return@collect

                val account = (myAccountState.value as? UiState.Success)?.data
                val soundOn = account?.soundEnabled ?: true
                val vibOn = account?.vibrationEnabled ?: true

                for (msg in newMsgs) {
                    when (msg.messageTypeEnum()) {
                        MessageType.NUDGE -> {
                            MsnSoundEngine.playNudge(context, soundOn, vibOn)
                            _nudgeTrigger.value = System.currentTimeMillis()
                        }
                        MessageType.WINK -> {
                            MsnSoundEngine.playWink(soundOn)
                            val wink = MsnCatalog.winks.firstOrNull { it.id == msg.winkId }
                                ?: MsnCatalog.winks.first()
                            _activeWink.value = wink
                        }
                        else -> {
                            if (msg.senderId != currentUserId || msg.chatId == "${currentUserId}_${currentUserId}") {
                                MsnSoundEngine.playNewMessage(soundOn)
                            }
                        }
                    }
                }
            }
        }
    }

    fun dismissWink() {
        _activeWink.value = null
    }

    fun replayWink(wink: MsnWink) {
        val soundOn = (myAccountState.value as? UiState.Success)?.data?.soundEnabled ?: true
        MsnSoundEngine.playWink(soundOn)
        _activeWink.value = wink
    }

    fun openChatWith(partnerUid: String) {
        if (_activePartnerUid.value != partnerUid) {
            initialMessagesLoadedForChat = null
            seenMessageIds.clear()
            _activePartnerUid.value = partnerUid
            viewModelScope.launch {
                repository.ensureChatExists(partnerUid)
            }
        }
    }

    fun closeActiveChat() {
        _activePartnerUid.value = null
    }

    fun updateMessageStyle(style: MessageStyleDraft) {
        _messageStyle.value = style
    }

    fun onDraftChanged(text: String) {
        val chatId = activeChatId.value ?: return
        typingDebounceJob?.cancel()
        if (text.isBlank()) {
            viewModelScope.launch { repository.setTyping(chatId, false) }
            return
        }
        typingDebounceJob = viewModelScope.launch {
            repository.setTyping(chatId, true)
            delay(2500L)
            repository.setTyping(chatId, false)
        }
    }

    fun registerOrSaveProfile(
        msnHandle: String,
        displayName: String,
        personalMessage: String,
        languageCode: String,
        nameColorHex: String,
        nameFontId: String,
        nameBold: Boolean,
        nameItalic: Boolean,
        nameUnderline: Boolean,
        avatarPreset: AvatarPreset,
        avatarBase64: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val prevHandle = (myDirectoryState.value as? UiState.Success)?.data?.msnHandle
            val res = repository.registerOrUpdateProfile(
                msnHandle = msnHandle,
                displayName = displayName,
                personalMessage = personalMessage,
                languageCode = languageCode,
                nameColorHex = nameColorHex,
                nameFontId = nameFontId,
                nameBold = nameBold,
                nameItalic = nameItalic,
                nameUnderline = nameUnderline,
                avatarPreset = avatarPreset,
                avatarBase64 = avatarBase64,
                previousHandle = prevHandle
            )
            _isBusy.value = false
            res.onSuccess {
                prefs.setLanguageCode(languageCode)
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Failed to save profile")
            }
        }
    }

    fun updateStatus(status: MsnStatus) {
        viewModelScope.launch {
            repository.updateStatus(status).onFailure {
                _snackbarEvents.tryEmit(it.message ?: "Failed to update status")
            }
        }
    }

    fun updatePersonalMessage(message: String) {
        viewModelScope.launch {
            repository.updatePersonalMessage(message).onFailure {
                _snackbarEvents.tryEmit(it.message ?: "Failed to update personal message")
            }
        }
    }

    fun updateAccountPreferences(
        languageCode: String,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        defaultStyle: MessageStyleDraft
    ) {
        viewModelScope.launch {
            prefs.setLanguageCode(languageCode)
            prefs.setSoundEnabled(soundEnabled)
            prefs.setVibrationEnabled(vibrationEnabled)
            _messageStyle.value = defaultStyle
            repository.updateAccountPreferences(
                languageCode = languageCode,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                chatFontId = defaultStyle.fontId,
                chatColorHex = defaultStyle.colorHex,
                chatBold = defaultStyle.bold,
                chatItalic = defaultStyle.italic,
                chatUnderline = defaultStyle.underline
            ).onFailure {
                _snackbarEvents.tryEmit(it.message ?: "Failed to save preferences")
            }
        }
    }

    fun addContactByHandle(rawHandle: String, notFoundMsg: String, onAdded: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addContactByHandle(rawHandle)
            _isBusy.value = false
            res.onSuccess { uid -> onAdded(uid) }
                .onFailure { err ->
                    val msg = if (err.message == "HANDLE_NOT_FOUND") notFoundMsg else (err.message ?: notFoundMsg)
                    _snackbarEvents.tryEmit(msg)
                }
        }
    }

    fun toggleBlockContact(contactUid: String, blocked: Boolean) {
        viewModelScope.launch {
            repository.toggleBlockContact(contactUid, blocked)
        }
    }

    fun removeContact(contactUid: String) {
        viewModelScope.launch {
            repository.removeContact(contactUid)
            if (_activePartnerUid.value == contactUid) _activePartnerUid.value = null
        }
    }

    fun sendTextMessage(text: String) {
        val partner = _activePartnerUid.value ?: return
        val style = _messageStyle.value
        viewModelScope.launch {
            repository.sendMessage(
                partnerUid = partner,
                type = MessageType.TEXT,
                text = text,
                fontId = style.fontId,
                colorHex = style.colorHex,
                bold = style.bold,
                italic = style.italic,
                underline = style.underline,
                strikethrough = style.strikethrough
            ).onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to send") }
        }
    }

    fun sendNudge(context: Context, nudgeText: String) {
        val partner = _activePartnerUid.value ?: return
        val account = (myAccountState.value as? UiState.Success)?.data
        MsnSoundEngine.playNudge(context, account?.soundEnabled ?: true, account?.vibrationEnabled ?: true)
        _nudgeTrigger.value = System.currentTimeMillis()
        viewModelScope.launch {
            repository.sendMessage(
                partnerUid = partner,
                type = MessageType.NUDGE,
                text = nudgeText,
                fontId = "open_sans",
                colorHex = "#E53935",
                bold = true,
                italic = false,
                underline = false,
                strikethrough = false
            )
        }
    }

    fun sendWink(wink: MsnWink) {
        val partner = _activePartnerUid.value ?: return
        val soundOn = (myAccountState.value as? UiState.Success)?.data?.soundEnabled ?: true
        MsnSoundEngine.playWink(soundOn)
        _activeWink.value = wink
        viewModelScope.launch {
            repository.sendMessage(
                partnerUid = partner,
                type = MessageType.WINK,
                text = "${wink.glyph} ${wink.title}",
                fontId = "fredoka",
                colorHex = wink.accentHex,
                bold = true,
                italic = false,
                underline = false,
                strikethrough = false,
                winkId = wink.id
            )
        }
    }

    fun sendRandomWink() {
        sendWink(MsnCatalog.winks.random())
    }

    fun sendImageMessage(imageBase64: String, caption: String = "") {
        val partner = _activePartnerUid.value ?: return
        val style = _messageStyle.value
        viewModelScope.launch {
            _isBusy.value = true
            repository.sendMessage(
                partnerUid = partner,
                type = MessageType.IMAGE,
                text = caption,
                fontId = style.fontId,
                colorHex = style.colorHex,
                bold = style.bold,
                italic = style.italic,
                underline = style.underline,
                strikethrough = style.strikethrough,
                mediaBase64 = imageBase64
            ).onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to send image") }
            _isBusy.value = false
        }
    }

    fun sendVideoMessage(videoBase64: String, thumbBase64: String, durationMs: Long) {
        val partner = _activePartnerUid.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.sendMessage(
                partnerUid = partner,
                type = MessageType.VIDEO,
                text = "Video",
                fontId = "open_sans",
                colorHex = "#0F62C9",
                bold = false,
                italic = false,
                underline = false,
                strikethrough = false,
                mediaBase64 = videoBase64,
                mediaThumbBase64 = thumbBase64,
                mediaDurationMs = durationMs
            ).onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to send video") }
            _isBusy.value = false
        }
    }

    fun sendVoiceMessage(audioBase64: String, durationMs: Long) {
        val partner = _activePartnerUid.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                partnerUid = partner,
                type = MessageType.VOICE,
                text = "Voice Clip",
                fontId = "open_sans",
                colorHex = "#0F62C9",
                bold = false,
                italic = false,
                underline = false,
                strikethrough = false,
                mediaBase64 = audioBase64,
                mediaDurationMs = durationMs
            ).onFailure { _snackbarEvents.tryEmit(it.message ?: "Failed to send voice clip") }
        }
    }

    fun startNewGame(gameType: GameType) {
        val partner = _activePartnerUid.value ?: return
        viewModelScope.launch {
            repository.startNewGame(partner, gameType).onFailure {
                _snackbarEvents.tryEmit(it.message ?: "Failed to start game")
            }
        }
    }

    fun playGomokuMove(game: MultiplayerGame, cellIndex: Int, isSoloBot: Boolean) {
        val soundOn = (myAccountState.value as? UiState.Success)?.data?.soundEnabled ?: true
        MsnSoundEngine.playGameMove(soundOn)
        viewModelScope.launch {
            repository.playGomokuMove(game, cellIndex, isSoloBot).onFailure {
                _snackbarEvents.tryEmit(it.message ?: "Move rejected")
            }
        }
    }

    fun playMemoryFlip(game: MultiplayerGame, cardIndex: Int) {
        val soundOn = (myAccountState.value as? UiState.Success)?.data?.soundEnabled ?: true
        MsnSoundEngine.playGameMove(soundOn)
        viewModelScope.launch {
            repository.playMemoryFlip(game, cardIndex).onSuccess {
                delay(900L)
                repository.clearMemoryRevealed(game)
            }.onFailure {
                _snackbarEvents.tryEmit(it.message ?: "Move rejected")
            }
        }
    }

    fun emitNotice(message: String) {
        _snackbarEvents.tryEmit(message)
    }
}
