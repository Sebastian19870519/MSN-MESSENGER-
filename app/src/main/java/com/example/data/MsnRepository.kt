package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AvatarPreset
import com.example.model.BuddyDirectoryEntry
import com.example.model.ChatMessage
import com.example.model.ChatThread
import com.example.model.GameLogic
import com.example.model.GameType
import com.example.model.MessageType
import com.example.model.MsnCatalog
import com.example.model.MsnStatus
import com.example.model.MultiplayerGame
import com.example.model.UserAccount
import com.example.model.UserContact
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MsnRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun normalizeHandleId(rawHandle: String): String {
        val cleaned = rawHandle.trim().lowercase()
            .replace("@", "_at_")
            .replace(".", "_")
            .replace(Regex("[^a-z0-9_\\-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return cleaned.take(64).ifBlank { "user" }
    }

    fun deterministicChatId(uidA: String, uidB: String): String {
        return if (uidA <= uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
    }

    fun observeUserAccount(userId: String): Flow<UserAccount?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user account read." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeDirectoryEntry(targetUserId: String): Flow<BuddyDirectoryEntry?> = flow {
        val uid = requireUserId()
        require(targetUserId.isNotBlank()) { "targetUserId cannot be blank" }
        val path = "directory/$targetUserId"
        emitAll(
            db.collection("directory").document(targetUserId)
                .snapshots()
                .map { it.toObject(BuddyDirectoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeContacts(userId: String): Flow<List<UserContact>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user contacts read." }
        val path = "users/$uid/contacts"
        emitAll(
            db.collection("users").document(uid).collection("contacts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.msnHandle.lowercase() }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeChats(userId: String): Flow<List<ChatThread>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user chats read." }
        val path = "chats"
        emitAll(
            db.collection("chats")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        require(chatId.isNotBlank()) { "chatId cannot be blank" }
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<ChatMessage>(
                                { it.createdAt?.seconds ?: Long.MAX_VALUE },
                                { it.createdAt?.nanoseconds ?: Int.MAX_VALUE }
                            )
                        )
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeGames(chatId: String): Flow<List<MultiplayerGame>> = flow {
        val uid = requireUserId()
        require(chatId.isNotBlank()) { "chatId cannot be blank" }
        val path = "chats/$chatId/games"
        emitAll(
            db.collection("chats").document(chatId).collection("games")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MultiplayerGame::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun registerOrUpdateProfile(
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
        previousHandle: String? = null
    ): Result<Unit> {
        val cleanHandle = msnHandle.trim().take(64)
        val cleanName = displayName.trim().take(60)
        val cleanMsg = personalMessage.trim().take(140)
        if (cleanHandle.length < 2) {
            return Result.failure(IllegalArgumentException("Handle must be at least 2 characters."))
        }
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Display name cannot be empty."))
        }
        return try {
            val uid = requireUserId()
            val email = auth.currentUser?.email ?: ""
            val handleId = normalizeHandleId(cleanHandle)
            val handleDoc = db.collection("handles").document(handleId).get().await()
            if (handleDoc.exists() && handleDoc.getString("userId") != uid) {
                return Result.failure(IllegalStateException("HANDLE_TAKEN"))
            }

            // Create or update UserAccount
            val userRef = db.collection("users").document(uid)
            val existingUser = userRef.get().await()
            if (!existingUser.exists()) {
                userRef.set(
                    mapOf(
                        "userId" to uid,
                        "passportEmail" to email.take(200),
                        "languageCode" to languageCode.take(12),
                        "soundEnabled" to true,
                        "vibrationEnabled" to true,
                        "chatFontId" to "open_sans",
                        "chatColorHex" to "#102033",
                        "chatBold" to false,
                        "chatItalic" to false,
                        "chatUnderline" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                userRef.update(
                    mapOf(
                        "languageCode" to languageCode.take(12),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Create or update public BuddyDirectoryEntry
            val dirRef = db.collection("directory").document(uid)
            val existingDir = dirRef.get().await()
            val safeAvatarB64 = avatarBase64.take(299_000)
            if (!existingDir.exists()) {
                dirRef.set(
                    mapOf(
                        "userId" to uid,
                        "msnHandle" to cleanHandle,
                        "displayName" to cleanName,
                        "personalMessage" to cleanMsg,
                        "status" to MsnStatus.ONLINE.code,
                        "nameColorHex" to nameColorHex.take(16),
                        "nameFontId" to nameFontId.take(40),
                        "nameBold" to nameBold,
                        "nameItalic" to nameItalic,
                        "nameUnderline" to nameUnderline,
                        "avatarPreset" to avatarPreset.code,
                        "avatarBase64" to safeAvatarB64,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                dirRef.update(
                    mapOf(
                        "msnHandle" to cleanHandle,
                        "displayName" to cleanName,
                        "personalMessage" to cleanMsg,
                        "nameColorHex" to nameColorHex.take(16),
                        "nameFontId" to nameFontId.take(40),
                        "nameBold" to nameBold,
                        "nameItalic" to nameItalic,
                        "nameUnderline" to nameUnderline,
                        "avatarPreset" to avatarPreset.code,
                        "avatarBase64" to safeAvatarB64,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Claim new handle index
            if (!handleDoc.exists()) {
                db.collection("handles").document(handleId).set(
                    mapOf(
                        "userId" to uid,
                        "msnHandle" to cleanHandle,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("handles").document(handleId).update(
                    mapOf("msnHandle" to cleanHandle)
                ).await()
            }

            // Release old handle if changed
            if (!previousHandle.isNullOrBlank()) {
                val oldHandleId = normalizeHandleId(previousHandle)
                if (oldHandleId != handleId) {
                    runCatching { db.collection("handles").document(oldHandleId).delete().await() }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "directory/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    suspend fun updateStatus(status: MsnStatus): Result<Unit> = try {
        val uid = requireUserId()
        val path = "directory/$uid"
        db.collection("directory").document(uid).update(
            mapOf(
                "status" to status.code,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "directory/${auth.currentUser?.uid}")
        Result.failure(e)
    }

    suspend fun updatePersonalMessage(message: String): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("directory").document(uid).update(
            mapOf(
                "personalMessage" to message.trim().take(140),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "directory/${auth.currentUser?.uid}")
        Result.failure(e)
    }

    suspend fun updateAccountPreferences(
        languageCode: String,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        chatFontId: String,
        chatColorHex: String,
        chatBold: Boolean,
        chatItalic: Boolean,
        chatUnderline: Boolean
    ): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).update(
            mapOf(
                "languageCode" to languageCode.take(12),
                "soundEnabled" to soundEnabled,
                "vibrationEnabled" to vibrationEnabled,
                "chatFontId" to chatFontId.take(40),
                "chatColorHex" to chatColorHex.take(16),
                "chatBold" to chatBold,
                "chatItalic" to chatItalic,
                "chatUnderline" to chatUnderline,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
        Result.failure(e)
    }

    suspend fun addContactByHandle(rawHandle: String, groupName: String = "Friends"): Result<String> {
        val trimmed = rawHandle.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Handle is empty"))
        return try {
            val uid = requireUserId()
            val handleId = normalizeHandleId(trimmed)
            val handleSnap = db.collection("handles").document(handleId).get().await()
            if (!handleSnap.exists()) {
                return Result.failure(NoSuchElementException("HANDLE_NOT_FOUND"))
            }
            val contactUid = handleSnap.getString("userId")
                ?: return Result.failure(NoSuchElementException("HANDLE_NOT_FOUND"))
            val canonicalHandle = handleSnap.getString("msnHandle") ?: trimmed

            val contactRef = db.collection("users").document(uid).collection("contacts").document(contactUid)
            val existing = contactRef.get().await()
            if (!existing.exists()) {
                contactRef.set(
                    mapOf(
                        "contactUid" to contactUid,
                        "msnHandle" to canonicalHandle.take(64),
                        "groupName" to groupName.take(40),
                        "blocked" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                contactRef.update(
                    mapOf(
                        "msnHandle" to canonicalHandle.take(64),
                        "blocked" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(contactUid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "handles/${normalizeHandleId(rawHandle)}")
            Result.failure(e)
        }
    }

    suspend fun toggleBlockContact(contactUid: String, blocked: Boolean): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("contacts").document(contactUid)
            .update(
                mapOf(
                    "blocked" to blocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/contacts/$contactUid")
        Result.failure(e)
    }

    suspend fun removeContact(contactUid: String): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("contacts").document(contactUid).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/contacts/$contactUid")
        Result.failure(e)
    }

    suspend fun ensureChatExists(partnerUid: String): Result<Pair<String, List<String>>> = try {
        val uid = requireUserId()
        val chatId = deterministicChatId(uid, partnerUid)
        val members = if (uid == partnerUid) listOf(uid) else listOf(uid, partnerUid).sorted()
        val chatRef = db.collection("chats").document(chatId)
        val snap = chatRef.get().await()
        if (!snap.exists()) {
            chatRef.set(
                mapOf(
                    "memberUids" to members,
                    "lastMessageText" to "",
                    "lastMessageType" to MessageType.TEXT.code,
                    "lastSenderId" to uid,
                    "typingUids" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(chatId to members)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "chats/${deterministicChatId(auth.currentUser?.uid ?: "", partnerUid)}")
        Result.failure(e)
    }

    suspend fun sendMessage(
        partnerUid: String,
        type: MessageType,
        text: String,
        fontId: String,
        colorHex: String,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        strikethrough: Boolean,
        winkId: String? = null,
        mediaBase64: String? = null,
        mediaThumbBase64: String? = null,
        mediaDurationMs: Long? = null
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val ensured = ensureChatExists(partnerUid).getOrElse { return Result.failure(it) }
            val (chatId, members) = ensured
            val expandedText = if (type == MessageType.TEXT) MsnCatalog.expandEmoticons(text) else text
            val safeText = expandedText.trim().take(2000)
            if (type == MessageType.TEXT && safeText.isEmpty()) {
                return Result.failure(IllegalArgumentException("Message cannot be empty."))
            }

            val msgRef = db.collection("chats").document(chatId).collection("messages").document()
            val payload = mapOf(
                "chatId" to chatId,
                "senderId" to uid,
                "memberUids" to members,
                "type" to type.code,
                "text" to safeText,
                "fontId" to fontId.take(40),
                "colorHex" to colorHex.take(16),
                "bold" to bold,
                "italic" to italic,
                "underline" to underline,
                "strikethrough" to strikethrough,
                "winkId" to winkId?.take(40),
                "mediaBase64" to mediaBase64?.take(899_000),
                "mediaThumbBase64" to mediaThumbBase64?.take(149_000),
                "mediaDurationMs" to mediaDurationMs?.coerceIn(0L, 300_000L),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            msgRef.set(payload).await()

            val preview = when (type) {
                MessageType.NUDGE -> "⚡ Nudge!"
                MessageType.WINK -> "✨ ${safeText.ifBlank { "Wink" }}"
                MessageType.IMAGE -> "📷 Photo"
                MessageType.VIDEO -> "🎬 Video"
                MessageType.VOICE -> "🎤 Voice clip"
                MessageType.TEXT -> safeText
            }.take(500)

            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessageText" to preview,
                    "lastMessageType" to type.code,
                    "lastSenderId" to uid,
                    "typingUids" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "chats/messages")
            Result.failure(e)
        }
    }

    suspend fun setTyping(chatId: String, isTyping: Boolean): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("chats").document(chatId)
        val snap = ref.get().await()
        if (snap.exists()) {
            val current = (snap.get("typingUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val updated = if (isTyping) (current + uid).distinct().take(2) else current.filter { it != uid }
            if (updated != current) {
                ref.update(
                    mapOf(
                        "typingUids" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun startNewGame(partnerUid: String, gameType: GameType): Result<String> = try {
        val uid = requireUserId()
        val ensured = ensureChatExists(partnerUid).getOrElse { return Result.failure(it) }
        val (chatId, members) = ensured
        val gameRef = db.collection("chats").document(chatId).collection("games").document()

        val initialCells = when (gameType) {
            GameType.GOMOKU_XO -> GameLogic.createEmptyGomokuBoard()
            GameType.MEMORY_MATCH -> GameLogic.createShuffledMemoryBoard()
        }
        val initialMask = if (gameType == GameType.MEMORY_MATCH) List(16) { 0L } else emptyList()

        gameRef.set(
            mapOf(
                "chatId" to chatId,
                "gameType" to gameType.code,
                "memberUids" to members,
                "playerXUid" to uid,
                "playerOUid" to partnerUid,
                "currentTurnUid" to uid,
                "status" to "ACTIVE",
                "winnerUid" to "",
                "boardCells" to initialCells,
                "matchedMask" to initialMask,
                "revealedIndices" to emptyList<Long>(),
                "winningLine" to emptyList<Long>(),
                "scoreX" to 0L,
                "scoreO" to 0L,
                "moveCount" to 0L,
                "createdByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(gameRef.id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "chats/games")
        Result.failure(e)
    }

    suspend fun playGomokuMove(game: MultiplayerGame, cellIndex: Int, isSoloBot: Boolean): Result<Unit> = try {
        val uid = requireUserId()
        if (game.status != "ACTIVE") return Result.success(Unit)
        if (!isSoloBot && game.currentTurnUid != uid) {
            return Result.failure(IllegalStateException("Not your turn."))
        }
        if (cellIndex !in game.boardCells.indices || game.boardCells[cellIndex].isNotEmpty()) {
            return Result.failure(IllegalArgumentException("Cell already taken."))
        }

        val mutableBoard = game.boardCells.toMutableList()
        val isSingleUserChat = game.playerXUid == game.playerOUid
        val symbol = if (isSoloBot) "X" else if (isSingleUserChat) {
            if (game.moveCount % 2L == 0L) "X" else "O"
        } else {
            if (uid == game.playerXUid) "X" else "O"
        }
        mutableBoard[cellIndex] = symbol

        var winLine = GameLogic.checkGomokuWin(mutableBoard, cellIndex)
        var status = if (winLine != null || mutableBoard.none { it.isEmpty() }) "FINISHED" else "ACTIVE"
        var winnerUid = when {
            winLine != null -> if (symbol == "X") game.playerXUid else game.playerOUid
            status == "FINISHED" -> "DRAW"
            else -> ""
        }
        var moves = game.moveCount + 1L
        var nextTurn = if (game.playerXUid == game.playerOUid) uid else {
            if (uid == game.playerXUid) game.playerOUid else game.playerXUid
        }

        // If practicing solo against Bot inside self-chat, bot responds immediately!
        if (isSoloBot && status == "ACTIVE") {
            val botIdx = GameLogic.chooseGomokuBotMove(mutableBoard, botSymbol = "O", humanSymbol = "X")
            if (botIdx != null) {
                mutableBoard[botIdx] = "O"
                moves += 1L
                val botWin = GameLogic.checkGomokuWin(mutableBoard, botIdx)
                if (botWin != null) {
                    winLine = botWin
                    status = "FINISHED"
                    winnerUid = "BOT"
                } else if (mutableBoard.none { it.isEmpty() }) {
                    status = "FINISHED"
                    winnerUid = "DRAW"
                }
            }
            nextTurn = uid
        }

        val scoreX = if (winnerUid == game.playerXUid) 1L else game.scoreX
        val scoreO = if (winnerUid == game.playerOUid || winnerUid == "BOT") 1L else game.scoreO

        db.collection("chats").document(game.chatId).collection("games").document(game.id)
            .update(
                mapOf(
                    "boardCells" to mutableBoard,
                    "currentTurnUid" to nextTurn,
                    "status" to status,
                    "winnerUid" to winnerUid,
                    "winningLine" to (winLine ?: emptyList()),
                    "scoreX" to scoreX,
                    "scoreO" to scoreO,
                    "moveCount" to moves,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "chats/${game.chatId}/games/${game.id}")
        Result.failure(e)
    }

    suspend fun playMemoryFlip(game: MultiplayerGame, cardIndex: Int): Result<Unit> = try {
        val uid = requireUserId()
        if (game.status != "ACTIVE") return Result.success(Unit)
        val isSingleUser = game.playerXUid == game.playerOUid
        if (!isSingleUser && game.currentTurnUid != uid) {
            return Result.failure(IllegalStateException("Not your turn."))
        }
        if (cardIndex !in 0 until 16) return Result.failure(IllegalArgumentException("Invalid card"))
        val mask = game.matchedMask.toMutableList()
        while (mask.size < 16) mask.add(0L)
        if (mask[cardIndex] == 1L) return Result.success(Unit)

        val currentRevealed = game.revealedIndices.toMutableList()
        // If 2 cards were already showing (mismatch), clear them and start new flip
        if (currentRevealed.size >= 2) {
            currentRevealed.clear()
        }
        if (currentRevealed.contains(cardIndex.toLong())) return Result.success(Unit)
        currentRevealed.add(cardIndex.toLong())

        var scoreX = game.scoreX
        var scoreO = game.scoreO
        var nextTurn = game.currentTurnUid
        var status = game.status
        var winnerUid = game.winnerUid

        if (currentRevealed.size == 2) {
            val first = currentRevealed[0].toInt()
            val second = currentRevealed[1].toInt()
            if (game.boardCells.getOrNull(first) == game.boardCells.getOrNull(second)) {
                mask[first] = 1L
                mask[second] = 1L
                currentRevealed.clear()
                if (uid == game.playerXUid) scoreX += 1L else scoreO += 1L
                if (mask.all { it == 1L }) {
                    status = "FINISHED"
                    winnerUid = when {
                        scoreX > scoreO -> game.playerXUid
                        scoreO > scoreX -> game.playerOUid
                        else -> "DRAW"
                    }
                }
            } else {
                // Mismatch: switch turn on next move (we keep revealedIndices=2 briefly so both see them)
                nextTurn = if (isSingleUser) uid else {
                    if (uid == game.playerXUid) game.playerOUid else game.playerXUid
                }
            }
        }

        db.collection("chats").document(game.chatId).collection("games").document(game.id)
            .update(
                mapOf(
                    "matchedMask" to mask,
                    "revealedIndices" to currentRevealed,
                    "currentTurnUid" to nextTurn,
                    "status" to status,
                    "winnerUid" to winnerUid,
                    "scoreX" to scoreX,
                    "scoreO" to scoreO,
                    "moveCount" to (game.moveCount + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "chats/${game.chatId}/games/${game.id}")
        Result.failure(e)
    }

    suspend fun clearMemoryRevealed(game: MultiplayerGame): Result<Unit> = try {
        requireUserId()
        if (game.status != "ACTIVE" || game.revealedIndices.size < 2) return Result.success(Unit)
        db.collection("chats").document(game.chatId).collection("games").document(game.id)
            .update(
                mapOf(
                    "revealedIndices" to emptyList<Long>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }
}
