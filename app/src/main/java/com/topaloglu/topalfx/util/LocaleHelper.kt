package com.topaloglu.topalfx.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Manual per-app locale switching without AppCompat: the activity wraps its base
 * context in [wrap] and calls recreate() after the language changes.
 */
object LocaleHelper {

    fun wrap(base: Context): Context {
        val locale = Locale.forLanguageTag(Prefs.getLanguage(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /** Returns the language code to switch to (simple ar/en toggle). */
    fun nextLanguage(context: Context): String =
        if (Prefs.getLanguage(context) == Prefs.LANG_ARABIC) Prefs.LANG_ENGLISH
        else Prefs.LANG_ARABIC
}
