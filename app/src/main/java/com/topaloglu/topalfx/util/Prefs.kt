package com.topaloglu.topalfx.util

import android.content.Context
import android.content.SharedPreferences
import com.topaloglu.topalfx.data.RatePair

object Prefs {
    private const val FILE_NAME = "topalfx_prefs"
    private const val KEY_LANGUAGE = "app_lang"
    private const val KEY_TICKER_PAIRS = "ticker_pairs"

    const val LANG_ARABIC = "ar"
    const val LANG_ENGLISH = "en"

    val DEFAULT_PAIRS = listOf(
        RatePair("EUR", "USD"),
        RatePair("USD", "TRY"),
        RatePair("EUR", "TRY"),
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun getLanguage(context: Context): String =
        prefs(context).getString(KEY_LANGUAGE, LANG_ARABIC) ?: LANG_ARABIC

    fun setLanguage(context: Context, lang: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun getTickerPairs(context: Context): List<RatePair> {
        val csv = prefs(context).getString(KEY_TICKER_PAIRS, null)
            ?: return DEFAULT_PAIRS
        val pairs = csv.split(',').mapNotNull { RatePair.parse(it) }
        return pairs.ifEmpty { DEFAULT_PAIRS }
    }

    fun setTickerPairs(context: Context, pairs: List<RatePair>) {
        val csv = pairs.joinToString(",") { it.label }
        prefs(context).edit().putString(KEY_TICKER_PAIRS, csv).apply()
    }
}
