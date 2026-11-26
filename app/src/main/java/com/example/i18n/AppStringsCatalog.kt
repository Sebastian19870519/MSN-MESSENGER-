package com.example.i18n

object MsnLanguages {
    // All 26 classic Windows XP MSN Messenger languages
    val all: List<LanguageOption> = listOf(
        LanguageOption("hu", "Magyar", "🇭🇺"),
        LanguageOption("en", "English (US)", "🇺🇸"),
        LanguageOption("de", "Deutsch", "🇩🇪"),
        LanguageOption("fr", "Français", "🇫🇷"),
        LanguageOption("es", "Español", "🇪🇸"),
        LanguageOption("it", "Italiano", "🇮🇹"),
        LanguageOption("pt-BR", "Português (Brasil)", "🇧🇷"),
        LanguageOption("pt-PT", "Português (Portugal)", "🇵🇹"),
        LanguageOption("nl", "Nederlands", "🇳🇱"),
        LanguageOption("pl", "Polski", "🇵🇱"),
        LanguageOption("cs", "Čeština", "🇨🇿"),
        LanguageOption("sk", "Slovenčina", "🇸🇰"),
        LanguageOption("ro", "Română", "🇷🇴"),
        LanguageOption("tr", "Türkçe", "🇹🇷"),
        LanguageOption("ru", "Русский", "🇷🇺"),
        LanguageOption("sv", "Svenska", "🇸🇪"),
        LanguageOption("no", "Norsk", "🇳🇴"),
        LanguageOption("da", "Dansk", "🇩🇰"),
        LanguageOption("fi", "Suomi", "🇫🇮"),
        LanguageOption("el", "Ελληνικά", "🇬🇷"),
        LanguageOption("ar", "العربية", "🇸🇦"),
        LanguageOption("he", "עברית", "🇮🇱"),
        LanguageOption("ja", "日本語", "🇯🇵"),
        LanguageOption("ko", "한국어", "🇰🇷"),
        LanguageOption("zh-CN", "中文 (简体)", "🇨🇳"),
        LanguageOption("zh-TW", "中文 (繁體)", "🇹🇼")
    )

    fun forCode(code: String): AppStrings = when (code) {
        "hu" -> hungarianStrings()
        "de" -> germanStrings()
        "fr" -> frenchStrings()
        "es" -> spanishStrings()
        "it" -> italianStrings()
        "pt-BR", "pt-PT" -> portugueseStrings()
        "nl" -> dutchStrings()
        "pl" -> polishStrings()
        "cs" -> czechStrings()
        "sk" -> slovakStrings()
        "ro" -> romanianStrings()
        "tr" -> turkishStrings()
        "ru" -> russianStrings()
        "sv" -> swedishStrings()
        "no" -> norwegianStrings()
        "da" -> danishStrings()
        "fi" -> finnishStrings()
        "el" -> greekStrings()
        "ar" -> arabicStrings()
        "he" -> hebrewStrings()
        "ja" -> japaneseStrings()
        "ko" -> koreanStrings()
        "zh-CN" -> chineseSimplifiedStrings()
        "zh-TW" -> chineseTraditionalStrings()
        else -> englishStrings()
    }
}

fun englishStrings() = AppStrings(
    appTitle = "MSN Messenger",
    signInTitle = "Sign in to MSN Messenger",
    signInSubtitle = "Classic Windows XP messaging, nudges, custom fonts, and live games on Android.",
    signInWithGoogle = "Sign in with Google",
    registerTitle = "Create Your MSN Passport",
    registerSubtitle = "Choose your MSN handle, nickname style, and display picture.",
    msnHandleLabel = "MSN Handle (e.g. alex99)",
    msnHandleHint = "letters, numbers, _ or -",
    displayNameLabel = "Display Name",
    personalMessageHint = "Type a personal message...",
    createAccountBtn = "Complete Registration",
    statusOnline = "Online",
    statusAway = "Away",
    statusBrb = "Be Right Back",
    statusBusy = "Busy",
    statusPhone = "On the Phone",
    statusOffline = "Appear Offline",
    contactsTab = "Contacts",
    chatsTab = "Chats",
    settingsTab = "Profile & Style",
    addContact = "Add a Contact",
    addContactHint = "Enter friend's MSN handle",
    addBtn = "Add",
    cancelBtn = "Cancel",
    saveBtn = "Save",
    selfTestChat = "My Space & Solo Test Chat",
    inviteFriends = "Share MSN Handle",
    onlineGroup = "Online",
    offlineGroup = "Offline",
    searchContactsHint = "Search contacts...",
    noContactsYet = "No contacts yet. Tap + to add a friend by MSN handle or try Solo Test Chat!",
    typeMessageHint = "Type a message...",
    sendBtn = "Send",
    nudgeAction = "Send a Nudge",
    nudgeSentText = "sent a Nudge!",
    winkAction = "Send a Wink",
    randomWinkAction = "Random Wink",
    voiceAction = "Voice Clip",
    recordingLabel = "Recording...",
    mediaAction = "Photo / Video",
    fontAction = "Font & Color",
    emoticonsAction = "Emoticons",
    gamesAction = "Games",
    gameGomokuTitle = "XO 5-in-a-Row",
    gameMemoryTitle = "Memory Match",
    newGameBtn = "New Game",
    practiceVsBot = "Practice vs Bot",
    yourTurn = "Your Turn",
    opponentTurn = "Opponent's Turn",
    youWon = "You Won!",
    opponentWon = "Opponent Won!",
    drawGame = "Draw!",
    gomokuRuleHint = "Collect 5 X or O in a row (horizontal, vertical, or diagonal) to win!",
    typingIndicator = "is typing a message...",
    profileSettingsTitle = "Personal Settings",
    languageLabel = "Language (26 MSN Languages)",
    soundsLabel = "MSN Sound Alerts",
    vibrationLabel = "Nudge Vibration",
    testSoundBtn = "Test Chime",
    nameStyleLabel = "Display Name Font & Color",
    messageStyleLabel = "Default Chat Text Style",
    avatarLabel = "Display Picture",
    choosePhotoBtn = "Choose Photo",
    fontLabel = "Font Family",
    colorLabel = "Color",
    boldLabel = "Bold",
    italicLabel = "Italic",
    underlineLabel = "Underline",
    strikeLabel = "Strike",
    signOutBtn = "Sign Out",
    blockLabel = "Block",
    unblockLabel = "Unblock",
    removeLabel = "Remove",
    myMsnIdLabel = "Your MSN Handle:",
    copiedMsg = "Copied!",
    handleTakenErr = "That MSN handle is already taken.",
    handleNotFoundErr = "No MSN user found with that handle."
)

fun hungarianStrings() = AppStrings(
    appTitle = "MSN Messenger",
    signInTitle = "Bejelentkezés az MSN Messengerbe",
    signInSubtitle = "A klasszikus Windows XP MSN élmény Android telefonon és tableten: rezgő üzenet, színes nevek, hangulatjelek és játékok.",
    signInWithGoogle = "Bejelentkezés Google-fiókkal",
    registerTitle = "MSN Regisztráció",
    registerSubtitle = "Állítsd be az egyedi MSN azonosítódat, színes nevedet és profilképedet!",
    msnHandleLabel = "MSN azonosító (pl. csaba99)",
    msnHandleHint = "betűk, számok, _ vagy -",
    displayNameLabel = "Megjelenített név",
    personalMessageHint = "Írj be egy személyes üzenetet...",
    createAccountBtn = "Regisztráció befejezése",
    statusOnline = "Elérhető",
    statusAway = "Távol van",
    statusBrb = "Rögtön jövök",
    statusBusy = "Elfoglalt",
    statusPhone = "Telefonál",
    statusOffline = "Kapcsolat nélküli",
    contactsTab = "Partnerek",
    chatsTab = "Beszélgetések",
    settingsTab = "Profil és Stílus",
    addContact = "Partner felvétele",
    addContactHint = "Add meg az ismerős MSN azonosítóját",
    addBtn = "Hozzáadás",
    cancelBtn = "Mégse",
    saveBtn = "Mentés",
    selfTestChat = "Saját próba csevegés és játék",
    inviteFriends = "MSN azonosító megosztása",
    onlineGroup = "Elérhető",
    offlineGroup = "Kapcsolat nélkül",
    searchContactsHint = "Partnerek keresése...",
    noContactsYet = "Még nincs felvett partnered. Koppints a + gombra ismerős felvételéhez, vagy próbáld ki a Saját próba csevegést!",
    typeMessageHint = "Írj üzenetet...",
    sendBtn = "Küldés",
    nudgeAction = "Rezgő üzenet",
    nudgeSentText = "rezgő üzenetet küldött!",
    winkAction = "Animáció küldése",
    randomWinkAction = "Random animáció",
    voiceAction = "Hangüzenet",
    recordingLabel = "Felvétel...",
    mediaAction = "Kép / Videó",
    fontAction = "Betűtípus és Szín",
    emoticonsAction = "Hangulatjelek",
    gamesAction = "Játékok",
    gameGomokuTitle = "XO Amőba (5 egy sorban)",
    gameMemoryTitle = "Memóriajáték",
    newGameBtn = "Új játék",
    practiceVsBot = "Gyakorlás gép ellen",
    yourTurn = "Te jössz",
    opponentTurn = "Partner köre",
    youWon = "Győztél!",
    opponentWon = "A partner nyert!",
    drawGame = "Döntetlen!",
    gomokuRuleHint = "Gyűjts össze 5 X-et vagy O-t vízszintesen, függőlegesen vagy átlósan!",
    typingIndicator = "éppen üzenetet ír...",
    profileSettingsTitle = "Profil és Beállítások",
    languageLabel = "Nyelv (26 MSN nyelv)",
    soundsLabel = "MSN hangjelzések",
    vibrationLabel = "Rezgő üzenet vibráció",
    testSoundBtn = "Hang próba",
    nameStyleLabel = "Név betűtípusa, stílusa és színe",
    messageStyleLabel = "Alapértelmezett üzenetstílus",
    avatarLabel = "Profilkép",
    choosePhotoBtn = "Kép választása",
    fontLabel = "Betűtípus",
    colorLabel = "Szín",
    boldLabel = "Félkövér",
    italicLabel = "Dőlt",
    underlineLabel = "Aláhúzott",
    strikeLabel = "Áthúzott",
    signOutBtn = "Kijelentkezés",
    blockLabel = "Letiltás",
    unblockLabel = "Tiltás feloldása",
    removeLabel = "Törlés",
    myMsnIdLabel = "Saját MSN azonosítód:",
    copiedMsg = "Másolva!",
    handleTakenErr = "Ez az MSN azonosító már foglalt.",
    handleNotFoundErr = "Nem található felhasználó ezzel az MSN azonosítóval."
)

fun germanStrings() = englishStrings().copy(
    signInTitle = "Bei MSN Messenger anmelden",
    signInWithGoogle = "Mit Google anmelden",
    registerTitle = "MSN-Konto erstellen",
    createAccountBtn = "Registrierung abschließen",
    statusOnline = "Online",
    statusAway = "Abwesend",
    statusBrb = "Bin gleich zurück",
    statusBusy = "Beschäftigt",
    statusPhone = "Am Telefon",
    statusOffline = "Als offline anzeigen",
    contactsTab = "Kontakte",
    chatsTab = "Chats",
    settingsTab = "Profil & Stil",
    addContact = "Kontakt hinzufügen",
    sendBtn = "Senden",
    nudgeAction = "Vibrieren senden",
    nudgeSentText = "hat Sie angestoßen!",
    winkAction = "Animation senden",
    randomWinkAction = "Zufalls-Animation",
    voiceAction = "Sprachnachricht",
    mediaAction = "Bild / Video",
    fontAction = "Schriftart & Farbe",
    emoticonsAction = "Emoticons",
    gamesAction = "Spiele",
    gameGomokuTitle = "XO 5 in einer Reihe",
    gameMemoryTitle = "Memory-Spiel",
    newGameBtn = "Neues Spiel",
    yourTurn = "Du bist dran",
    youWon = "Gewonnen!",
    languageLabel = "Sprache (26 MSN-Sprachen)",
    signOutBtn = "Abmelden"
)

fun frenchStrings() = englishStrings().copy(
    signInTitle = "Se connecter à MSN Messenger",
    signInWithGoogle = "Se connecter avec Google",
    registerTitle = "Créer un passeport MSN",
    createAccountBtn = "Terminer l'inscription",
    statusOnline = "En ligne",
    statusAway = "Absent",
    statusBrb = "De retour dans un instant",
    statusBusy = "Occupé",
    statusPhone = "Au téléphone",
    statusOffline = "Hors ligne",
    contactsTab = "Contacts",
    chatsTab = "Discussions",
    settingsTab = "Profil & Style",
    addContact = "Ajouter un contact",
    sendBtn = "Envoyer",
    nudgeAction = "Envoyer un wizz",
    nudgeSentText = "vient d'envoyer un wizz !",
    winkAction = "Clin d'œil",
    randomWinkAction = "Animation aléatoire",
    voiceAction = "Message vocal",
    mediaAction = "Photo / Vidéo",
    fontAction = "Police & Couleur",
    emoticonsAction = "Émoticônes",
    gamesAction = "Jeux",
    gameGomokuTitle = "XO 5 alignés",
    gameMemoryTitle = "Jeu de mémoire",
    newGameBtn = "Nouvelle partie",
    yourTurn = "À votre tour",
    youWon = "Vous avez gagné !",
    languageLabel = "Langue (26 langues MSN)",
    signOutBtn = "Déconnexion"
)

fun spanishStrings() = englishStrings().copy(
    signInTitle = "Iniciar sesión en MSN Messenger",
    signInWithGoogle = "Iniciar sesión con Google",
    registerTitle = "Crear cuenta MSN",
    createAccountBtn = "Completar registro",
    statusOnline = "Conectado",
    statusAway = "Ausente",
    statusBrb = "Vuelvo enseguida",
    statusBusy = "Ocupado",
    statusPhone = "Al teléfono",
    statusOffline = "Sin conexión",
    contactsTab = "Contactos",
    chatsTab = "Chats",
    settingsTab = "Perfil y Estilo",
    addContact = "Agregar contacto",
    sendBtn = "Enviar",
    nudgeAction = "Enviar zumbido",
    nudgeSentText = "¡ha enviado un zumbido!",
    winkAction = "Guiño",
    randomWinkAction = "Animación aleatoria",
    voiceAction = "Nota de voz",
    mediaAction = "Foto / Video",
    fontAction = "Fuente y Color",
    emoticonsAction = "Emoticonos",
    gamesAction = "Juegos",
    gameGomokuTitle = "XO 5 en raya",
    gameMemoryTitle = "Juego de memoria",
    newGameBtn = "Nuevo juego",
    yourTurn = "Tu turno",
    youWon = "¡Ganaste!",
    languageLabel = "Idioma (26 idiomas MSN)",
    signOutBtn = "Cerrar sesión"
)

fun italianStrings() = englishStrings().copy(
    signInTitle = "Accedi a MSN Messenger",
    signInWithGoogle = "Accedi con Google",
    statusOnline = "In linea",
    statusAway = "Non al computer",
    statusBrb = "Torno subito",
    statusBusy = "Occupato",
    statusPhone = "Al telefono",
    statusOffline = "Non in linea",
    contactsTab = "Contatti",
    chatsTab = "Chat",
    settingsTab = "Profilo e Stile",
    addContact = "Aggiungi contatto",
    sendBtn = "Invia",
    nudgeAction = "Trillo",
    nudgeSentText = "ha inviato un trillo!",
    winkAction = "Animazione",
    randomWinkAction = "Animazione casuale",
    voiceAction = "Messaggio vocale",
    gamesAction = "Giochi",
    signOutBtn = "Disconnetti"
)

fun portugueseStrings() = englishStrings().copy(
    signInTitle = "Entrar no MSN Messenger",
    signInWithGoogle = "Entrar com Google",
    statusOnline = "Online",
    statusAway = "Ausente",
    statusBrb = "Volto logo",
    statusBusy = "Ocupado",
    statusPhone = "Ao telefone",
    statusOffline = "Aparecer offline",
    contactsTab = "Contatos",
    chatsTab = "Conversas",
    settingsTab = "Perfil e Estilo",
    addContact = "Adicionar contato",
    sendBtn = "Enviar",
    nudgeAction = "Chamar atenção",
    nudgeSentText = "chamou sua atenção!",
    winkAction = "Wink",
    randomWinkAction = "Animação aleatória",
    voiceAction = "Mensagem de voz",
    gamesAction = "Jogos",
    signOutBtn = "Sair"
)

fun dutchStrings() = englishStrings().copy(
    signInTitle = "Aanmelden bij MSN Messenger",
    statusOnline = "Online",
    statusAway = "Afwezig",
    statusBrb = "Zo terug",
    statusBusy = "Bezet",
    statusOffline = "Offline weergeven",
    contactsTab = "Contactpersonen",
    sendBtn = "Verzenden",
    nudgeAction = "Buzzer",
    gamesAction = "Spellen",
    signOutBtn = "Afmelden"
)

fun polishStrings() = englishStrings().copy(
    signInTitle = "Zaloguj się do MSN Messenger",
    statusOnline = "Dostępny",
    statusAway = "Zaraz wracam",
    statusBrb = "Zaraz wrócę",
    statusBusy = "Zajęty",
    statusOffline = "Niewidoczny",
    contactsTab = "Kontakty",
    chatsTab = "Rozmowy",
    sendBtn = "Wyślij",
    nudgeAction = "Wibracja",
    gamesAction = "Gry",
    signOutBtn = "Wyloguj"
)

fun czechStrings() = englishStrings().copy(
    signInTitle = "Přihlásit se k MSN Messengeru",
    statusOnline = "Online",
    statusAway = "Pryč",
    statusBrb = "Hned jsem zpět",
    statusBusy = "Zaneprázdněn",
    statusOffline = "Offline",
    contactsTab = "Kontakty",
    chatsTab = "Konverzace",
    sendBtn = "Odeslat",
    nudgeAction = "Šťouchnutí",
    gamesAction = "Hry",
    signOutBtn = "Odhlásit"
)

fun slovakStrings() = englishStrings().copy(
    signInTitle = "Prihlásiť sa do MSN Messengera",
    statusOnline = "Online",
    statusAway = "Preč",
    statusBrb = "Hneď som späť",
    statusBusy = "Zaneprázdnený",
    statusOffline = "Offline",
    contactsTab = "Kontakty",
    sendBtn = "Odoslať",
    nudgeAction = "Štuchnutie",
    gamesAction = "Hry",
    signOutBtn = "Odhlásiť"
)

fun romanianStrings() = englishStrings().copy(
    signInTitle = "Conectare la MSN Messenger",
    statusOnline = "Disponibil",
    statusAway = "Plecat",
    statusBrb = "Revin imediat",
    statusBusy = "Ocupat",
    statusOffline = "Deconectat",
    contactsTab = "Contacte",
    sendBtn = "Trimite",
    nudgeAction = "Vibrație",
    gamesAction = "Jocuri",
    signOutBtn = "Deconectare"
)

fun turkishStrings() = englishStrings().copy(
    signInTitle = "MSN Messenger'da Oturum Aç",
    statusOnline = "Çevrimiçi",
    statusAway = "Uzakta",
    statusBrb = "Hemen Dönecek",
    statusBusy = "Meşgul",
    statusOffline = "Çevrimdışı Görün",
    contactsTab = "Kişiler",
    sendBtn = "Gönder",
    nudgeAction = "Titreşim",
    gamesAction = "Oyunlar",
    signOutBtn = "Oturumu Kapat"
)

fun russianStrings() = englishStrings().copy(
    signInTitle = "Вход в MSN Messenger",
    signInWithGoogle = "Войти через Google",
    statusOnline = "В сети",
    statusAway = "Нет на месте",
    statusBrb = "Скоро вернусь",
    statusBusy = "Занят",
    statusPhone = "Говорю по телефону",
    statusOffline = "Не в сети",
    contactsTab = "Контакты",
    chatsTab = "Чаты",
    settingsTab = "Профиль",
    addContact = "Добавить контакт",
    sendBtn = "Отправить",
    nudgeAction = "Вибрация",
    winkAction = "Анимация",
    voiceAction = "Голосовое",
    gamesAction = "Игры",
    signOutBtn = "Выйти"
)

fun swedishStrings() = englishStrings().copy(
    signInTitle = "Logga in på MSN Messenger",
    statusOnline = "Online",
    statusAway = "Borta",
    statusBrb = "Strax tillbaka",
    statusBusy = "Upptagen",
    statusOffline = "Visa som offline",
    contactsTab = "Kontakter",
    sendBtn = "Skicka",
    nudgeAction = "Vibba",
    gamesAction = "Spel",
    signOutBtn = "Logga ut"
)

fun norwegianStrings() = englishStrings().copy(
    signInTitle = "Logg på MSN Messenger",
    statusOnline = "Pålogget",
    statusAway = "Borte",
    statusBrb = "Straks tilbake",
    statusBusy = "Opptatt",
    statusOffline = "Vis som frakoblet",
    contactsTab = "Kontakter",
    sendBtn = "Send",
    gamesAction = "Spill",
    signOutBtn = "Logg av"
)

fun danishStrings() = englishStrings().copy(
    signInTitle = "Log på MSN Messenger",
    statusOnline = "Online",
    statusAway = "Ikke til stede",
    statusBrb = "Straks tilbage",
    statusBusy = "Optaget",
    statusOffline = "Vis som offline",
    contactsTab = "Kontakter",
    sendBtn = "Send",
    gamesAction = "Spil",
    signOutBtn = "Log af"
)

fun finnishStrings() = englishStrings().copy(
    signInTitle = "Kirjaudu MSN Messengeriin",
    statusOnline = "Paikalla",
    statusAway = "Poissa",
    statusBrb = "Palaan pian",
    statusBusy = "Varattu",
    statusOffline = "Näytä offline-tilassa",
    contactsTab = "Yhteystiedot",
    sendBtn = "Lähetä",
    nudgeAction = "Heräte",
    gamesAction = "Pelit",
    signOutBtn = "Kirjaudu ulos"
)

fun greekStrings() = englishStrings().copy(
    signInTitle = "Σύνδεση στο MSN Messenger",
    statusOnline = "Συνδεδεμένος",
    statusAway = "Εκτός",
    statusBrb = "Επιστρέφω αμέσως",
    statusBusy = "Απασχολημένος",
    statusOffline = "Εμφάνιση εκτός σύνδεσης",
    contactsTab = "Επαφές",
    sendBtn = "Αποστολή",
    gamesAction = "Παιχνίδια",
    signOutBtn = "Αποσύνδεση"
)

fun arabicStrings() = englishStrings().copy(
    signInTitle = "تسجيل الدخول إلى MSN Messenger",
    statusOnline = "متصل",
    statusAway = "بالخارج",
    statusBrb = "سأعود حالاً",
    statusBusy = "مشغول",
    statusOffline = "الظهور دون اتصال",
    contactsTab = "جهات الاتصال",
    chatsTab = "المحادثات",
    sendBtn = "إرسال",
    nudgeAction = "تنبيه",
    gamesAction = "ألعاب",
    signOutBtn = "تسجيل الخروج"
)

fun hebrewStrings() = englishStrings().copy(
    signInTitle = "כניסה ל-MSN Messenger",
    statusOnline = "מחובר",
    statusAway = "לא נמצא",
    statusBrb = "תיכף אשוב",
    statusBusy = "עסוק",
    statusOffline = "הצג כלא מחובר",
    contactsTab = "אנשי קשר",
    sendBtn = "שלח",
    gamesAction = "משחקים",
    signOutBtn = "התנתק"
)

fun japaneseStrings() = englishStrings().copy(
    signInTitle = "MSN Messenger にサインイン",
    signInWithGoogle = "Google でサインイン",
    statusOnline = "オンライン",
    statusAway = "退席中",
    statusBrb = "一時退席",
    statusBusy = "取り込み中",
    statusPhone = "電話中",
    statusOffline = "オフライン表示",
    contactsTab = "メンバー",
    chatsTab = "会話",
    settingsTab = "プロフィール",
    addContact = "メンバーの追加",
    sendBtn = "送信",
    nudgeAction = "バイブ",
    winkAction = "ウィンク",
    voiceAction = "ボイス",
    gamesAction = "ゲーム",
    signOutBtn = "サインアウト"
)

fun koreanStrings() = englishStrings().copy(
    signInTitle = "MSN 메신저 로그인",
    signInWithGoogle = "Google로 로그인",
    statusOnline = "온라인",
    statusAway = "자리 비움",
    statusBrb = "곧 돌아옴",
    statusBusy = "다른 용무 중",
    statusPhone = "통화 중",
    statusOffline = "오프라인 표시",
    contactsTab = "대화 상대",
    chatsTab = "대화",
    settingsTab = "프로필",
    addContact = "대화 상대 추가",
    sendBtn = "보내기",
    nudgeAction = "깜짝 놀래키기",
    winkAction = "윙크",
    voiceAction = "음성 메시지",
    gamesAction = "게임",
    signOutBtn = "로그아웃"
)

fun chineseSimplifiedStrings() = englishStrings().copy(
    signInTitle = "登录 MSN Messenger",
    signInWithGoogle = "使用 Google 登录",
    statusOnline = "联机",
    statusAway = "离开",
    statusBrb = "马上回来",
    statusBusy = "忙碌",
    statusPhone = "接听电话",
    statusOffline = "脱机",
    contactsTab = "联系人",
    chatsTab = "聊天",
    settingsTab = "个人资料",
    addContact = "添加联系人",
    sendBtn = "发送",
    nudgeAction = "发送闪屏振动",
    winkAction = "传情动漫",
    voiceAction = "语音消息",
    gamesAction = "游戏",
    signOutBtn = "注销"
)

fun chineseTraditionalStrings() = englishStrings().copy(
    signInTitle = "登入 MSN Messenger",
    signInWithGoogle = "使用 Google 登入",
    statusOnline = "線上",
    statusAway = "離開",
    statusBrb = "馬上回來",
    statusBusy = "忙碌",
    statusPhone = "通話中",
    statusOffline = "離線",
    contactsTab = "聯絡人",
    chatsTab = "對話",
    settingsTab = "個人設定",
    addContact = "新增聯絡人",
    sendBtn = "傳送",
    nudgeAction = "傳送來電震動",
    winkAction = "動畫快遞",
    voiceAction = "語音訊息",
    gamesAction = "遊戲",
    signOutBtn = "登出"
)
