package com.topaloglu.topalfx

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.topaloglu.topalfx.ui.AppScaffold
import com.topaloglu.topalfx.ui.theme.TopalFXTheme
import com.topaloglu.topalfx.util.LocaleHelper
import com.topaloglu.topalfx.util.Prefs

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TopalFXTheme {
                AppScaffold(
                    onToggleLanguage = {
                        Prefs.setLanguage(this, LocaleHelper.nextLanguage(this))
                        recreate()
                    },
                )
            }
        }
    }
}
