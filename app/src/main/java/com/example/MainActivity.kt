package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PreferencesManager
import com.example.i18n.LocalAppStrings
import com.example.i18n.MsnLanguages
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.AuthenticatedMsnApp
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MsnRootContent()
            }
        }
    }
}

@Composable
fun MsnRootContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember(context) { PreferencesManager(context.applicationContext) }
    val languageCode by prefsManager.languageCode.collectAsStateWithLifecycle(initialValue = "hu")
    val strings = remember(languageCode) { MsnLanguages.forCode(languageCode) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                selectedLanguageCode = languageCode,
                onSelectLanguage = { newCode ->
                    scope.launch { prefsManager.setLanguageCode(newCode) }
                },
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        } else {
            val uid = user.uid
            key(uid) {
                AuthenticatedMsnApp(
                    currentUserId = uid,
                    prefsManager = prefsManager,
                    languageCode = languageCode
                )
            }
        }
    }
}
