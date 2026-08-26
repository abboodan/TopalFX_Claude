package com.topaloglu.topalfx

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.topaloglu.topalfx.ui.AppScaffold
import com.topaloglu.topalfx.ui.theme.TopalFXTheme
import com.topaloglu.topalfx.util.LocaleHelper
import com.topaloglu.topalfx.util.Prefs
import com.topaloglu.topalfx.util.ThemeMode

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(Prefs.getThemeMode(this)) }
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            TopalFXTheme(darkTheme = darkTheme) {
                AppScaffold(
                    onToggleLanguage = {
                        Prefs.setLanguage(this, LocaleHelper.nextLanguage(this))
                        recreate()
                    },
                    isDarkTheme = darkTheme,
                    // One tap always flips what is on screen, and the choice sticks —
                    // including the first tap away from following the system.
                    onToggleTheme = {
                        val next = if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK
                        Prefs.setThemeMode(this, next)
                        themeMode = next
                    },
                )
            }
        }
    }
}
