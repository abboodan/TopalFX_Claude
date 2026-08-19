package com.topaloglu.topalfx.util

import android.content.Context
import android.content.SharedPreferences
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.data.RatePair

object Prefs {
    private const val FILE_NAME = "topalfx_prefs"
    private const val KEY_LANGUAGE = "app_lang"
    private const val KEY_TICKER_PAIRS = "ticker_pairs"
    private const val KEY_DEFAULT_PCT_AGENT_COST = "default_pct_agent_cost"
    private const val KEY_DEFAULT_FLAT_AGENT_COST = "default_flat_agent_cost"
    private const val KEY_DEFAULT_DEDUCTION_BASE = "default_deduction_base"
    private const val KEY_DEFAULT_CUSTOMER_DISCOUNT = "default_customer_discount"

    /** Customer rate sits this far below the market rate by default (1.15 → 1.14). */
    const val DEFAULT_CUSTOMER_DISCOUNT = 0.01

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

    /** Default office (agent) percentage cost, kept as raw text for the input field. */
    fun getDefaultPctAgentCost(context: Context): String =
        prefs(context).getString(KEY_DEFAULT_PCT_AGENT_COST, "") ?: ""

    fun getDefaultFlatAgentCost(context: Context): String =
        prefs(context).getString(KEY_DEFAULT_FLAT_AGENT_COST, "") ?: ""

    fun getDefaultDeductionBase(context: Context): DeductionBase {
        val stored = prefs(context).getString(KEY_DEFAULT_DEDUCTION_BASE, null)
        return DeductionBase.entries.firstOrNull { it.name == stored } ?: DeductionBase.ON_RECEIVED
    }

    fun getDefaultCustomerDiscount(context: Context): Double =
        prefs(context).getFloat(KEY_DEFAULT_CUSTOMER_DISCOUNT, DEFAULT_CUSTOMER_DISCOUNT.toFloat())
            .toDouble()

    fun setDefaults(
        context: Context,
        pctAgentCost: String,
        flatAgentCost: String,
        deductionBase: DeductionBase,
        customerDiscount: Double,
    ) {
        prefs(context).edit()
            .putString(KEY_DEFAULT_PCT_AGENT_COST, pctAgentCost.trim())
            .putString(KEY_DEFAULT_FLAT_AGENT_COST, flatAgentCost.trim())
            .putString(KEY_DEFAULT_DEDUCTION_BASE, deductionBase.name)
            .putFloat(KEY_DEFAULT_CUSTOMER_DISCOUNT, customerDiscount.toFloat())
            .apply()
    }
}
