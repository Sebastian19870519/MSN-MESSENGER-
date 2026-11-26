package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MsnStatus(val code: String) {
    ONLINE("ONLINE"),
    AWAY("AWAY"),
    BRB("BRB"),
    BUSY("BUSY"),
    PHONE("PHONE"),
    OFFLINE("OFFLINE");

    companion object {
        fun fromRaw(raw: String?): MsnStatus =
            entries.firstOrNull { it.code == raw } ?: ONLINE
    }
}

enum class MessageType(val code: String) {
    TEXT("TEXT"),
    NUDGE("NUDGE"),
    WINK("WINK"),
    IMAGE("IMAGE"),
    VIDEO("VIDEO"),
    VOICE("VOICE");

    companion object {
        fun fromRaw(raw: String?): MessageType =
            entries.firstOrNull { it.code == raw } ?: TEXT
    }
}

enum class GameType(val code: String) {
    GOMOKU_XO("GOMOKU_XO"),
    MEMORY_MATCH("MEMORY_MATCH");

    companion object {
        fun fromRaw(raw: String?): GameType =
            entries.firstOrNull { it.code == raw } ?: GOMOKU_XO
    }
}

enum class AvatarPreset(val code: String) {
    DUCK("DUCK"),
    ASTRONAUT("ASTRONAUT"),
    CAT("CAT"),
    BUTTERFLY("BUTTERFLY"),
    CUSTOM("CUSTOM");

    companion object {
        fun fromRaw(raw: String?): AvatarPreset =
            entries.firstOrNull { it.code == raw } ?: DUCK
    }
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val passportEmail: String = "",
    val languageCode: String = "hu",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val chatFontId: String = "open_sans",
    val chatColorHex: String = "#102033",
    val chatBold: Boolean = false,
    val chatItalic: Boolean = false,
    val chatUnderline: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BuddyDirectoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val msnHandle: String = "",
    val displayName: String = "",
    val personalMessage: String = "",
    val status: String = MsnStatus.ONLINE.code,
    val nameColorHex: String = "#0F62C9",
    val nameFontId: String = "open_sans",
    val nameBold: Boolean = true,
    val nameItalic: Boolean = false,
    val nameUnderline: Boolean = false,
    val avatarPreset: String = AvatarPreset.DUCK.code,
    val avatarBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): MsnStatus = MsnStatus.fromRaw(status)
    fun avatarPresetEnum(): AvatarPreset = AvatarPreset.fromRaw(avatarPreset)
}

@Keep
@IgnoreExtraProperties
data class UserContact(
    @DocumentId val id: String = "",
    val contactUid: String = "",
    val msnHandle: String = "",
    val groupName: String = "Friends",
    val blocked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val memberUids: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageType: String = MessageType.TEXT.code,
    val lastSenderId: String = "",
    val typingUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val memberUids: List<String> = emptyList(),
    val type: String = MessageType.TEXT.code,
    val text: String = "",
    val fontId: String = "open_sans",
    val colorHex: String = "#102033",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val winkId: String = "",
    val mediaBase64: String = "",
    val mediaThumbBase64: String = "",
    val mediaDurationMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun messageTypeEnum(): MessageType = MessageType.fromRaw(type)
}

@Keep
@IgnoreExtraProperties
data class MultiplayerGame(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val gameType: String = GameType.GOMOKU_XO.code,
    val memberUids: List<String> = emptyList(),
    val playerXUid: String = "",
    val playerOUid: String = "",
    val currentTurnUid: String = "",
    val status: String = "ACTIVE",
    val winnerUid: String = "",
    val boardCells: List<String> = emptyList(),
    val matchedMask: List<Long> = emptyList(),
    val revealedIndices: List<Long> = emptyList(),
    val winningLine: List<Long> = emptyList(),
    val scoreX: Long = 0L,
    val scoreO: Long = 0L,
    val moveCount: Long = 0L,
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun gameTypeEnum(): GameType = GameType.fromRaw(gameType)
}

data class MsnEmoticon(
    val shortcut: String,
    val glyph: String,
    val label: String
)

data class MsnWink(
    val id: String,
    val glyph: String,
    val title: String,
    val accentHex: String
)

object MsnCatalog {
    val emoticons: List<MsnEmoticon> = listOf(
        MsnEmoticon(":)", "😊", "Mosoly"),
        MsnEmoticon(":D", "😃", "Nevetés"),
        MsnEmoticon(";)", "😉", "Kacsintás"),
        MsnEmoticon(":P", "😛", "Nyelvnyújtás"),
        MsnEmoticon(":O", "😮", "Meglepett"),
        MsnEmoticon(":(", "😞", "Szomorú"),
        MsnEmoticon(":'(", "😢", "Sírás"),
        MsnEmoticon(":@", "😡", "Dühös"),
        MsnEmoticon(":S", "😖", "Zavarodott"),
        MsnEmoticon(":$", "😳", "Elpirult"),
        MsnEmoticon(":|", "😐", "Közömbös"),
        MsnEmoticon("(H)", "😎", "Menő"),
        MsnEmoticon("(A)", "😇", "Angyalka"),
        MsnEmoticon("(6)", "😈", "Ördög"),
        MsnEmoticon("(L)", "❤️", "Szív"),
        MsnEmoticon("(U)", "💔", "Összetört szív"),
        MsnEmoticon("(K)", "💋", "Csók"),
        MsnEmoticon("(F)", "🌹", "Rózsa"),
        MsnEmoticon("(W)", "🥀", "Hervadt rózsa"),
        MsnEmoticon("(G)", "🎁", "Ajándék"),
        MsnEmoticon("(*)", "⭐", "Csillag"),
        MsnEmoticon("(8)", "🎵", "Zene"),
        MsnEmoticon("(C)", "☕", "Kávé"),
        MsnEmoticon("(B)", "🍺", "Sör"),
        MsnEmoticon("(D)", "🍸", "Koktél"),
        MsnEmoticon("(Z)", "👦", "Fiú"),
        MsnEmoticon("(X)", "👧", "Lány"),
        MsnEmoticon("(@)", "🐱", "Cica"),
        MsnEmoticon("(&)", "🐶", "Kutya"),
        MsnEmoticon("(Y)", "👍", "Tetszik"),
        MsnEmoticon("(N)", "👎", "Nem tetszik"),
        MsnEmoticon("(I)", "💡", "Ötlet"),
        MsnEmoticon("(S)", "🌙", "Alvó hold"),
        MsnEmoticon("(^)", "🎂", "Torta"),
        MsnEmoticon("(T)", "☎️", "Telefon"),
        MsnEmoticon("(E)", "✉️", "Levél")
    )

    val winks: List<MsnWink> = listOf(
        MsnWink("kiss", "💋", "Big Kiss", "#E91E63"),
        MsnWink("heart_beat", "💖", "Beating Heart", "#EC407A"),
        MsnWink("laugh_out", "🤣", "LOL Burst", "#FFB300"),
        MsnWink("guitar_rock", "🎸", "Guitar Riff", "#7E57C2"),
        MsnWink("lightning", "⚡", "Electric Zap", "#0F62C9"),
        MsnWink("butterfly", "🦋", "MSN Butterfly", "#2EB840"),
        MsnWink("party_pop", "🎉", "Confetti Party", "#FF5722"),
        MsnWink("sleeping", "😴", "Sleepy Moon", "#3F51B5"),
        MsnWink("cake_wish", "🎂", "Birthday Cake", "#D81B60"),
        MsnWink("cool_star", "🌟", "Superstar", "#00ACC1")
    )

    val classicColors: List<String> = listOf(
        "#102033", "#0F62C9", "#1E88E5", "#00897B",
        "#2EB840", "#7CB342", "#F59E0B", "#FB8C00",
        "#E53935", "#D81B60", "#8E24AA", "#5E35B1",
        "#4E342E", "#546E7A", "#FFFFFF"
    )

    fun expandEmoticons(input: String): String {
        var result = input
        for (emote in emoticons) {
            if (result.contains(emote.shortcut, ignoreCase = false)) {
                result = result.replace(emote.shortcut, emote.glyph)
            }
        }
        return result
    }
}
