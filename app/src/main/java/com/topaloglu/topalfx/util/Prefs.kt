package com.topaloglu.topalfx.util

import android.content.Context
import android.content.SharedPreferences
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.data.RatePair
import com.topaloglu.topalfx.data.TransferDirection

object Prefs {
    private const val FILE_NAME = "topalfx_prefs"
    private const val KEY_LANGUAGE = "app_lang"
    private const val KEY_TICKER_PAIRS = "ticker_pairs"
    private const val KEY_DEFAULT_PCT_AGENT_COST = "default_pct_agent_cost"
    private const val KEY_DEFAULT_DEDUCTION_BASE = "default_deduction_base"
    private const val KEY_DEFAULT_CUSTOMER_DISCOUNT = "default_customer_discount"
    private const val KEY_DEFAULT_PCT_FEE = "default_pct_fee"
    private const val KEY_DEFAULT_FEE_INCLUSIVE = "default_fee_inclusive"
    private const val KEY_DEFAULT_DIRECTION = "default_direction"
    private const val KEY_DEFAULT_MODE = "default_mode"

    /** Retired in 1.2.0 — purged on the next settings save. */
    private const val KEY_RETIRED_FLAT_AGENT_COST = "default_flat_agent_cost"

    /** Customer rate sits this far below the market rate by default (1.15 → 1.14). */
    const val DEFAULT_CUSTOMER_DISCOUNT = 0.01

    /** The typed amount is normally the cash the customer hands over. */
    const val DEFAULT_FEE_INCLUSIVE = true

    val DEFAULT_DIRECTION = TransferDirection.EUR_TO_USD
    val DEFAULT_MODE = CalcMode.SEND_EXACT
    val DEFAULT_DEDUCTION_BASE = DeductionBase.ON_RECEIVED

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

    /** Default الأتعاب percentage, kept as raw text for the input field. */
    fun getDefaultPctFee(context: Context): String =
        prefs(context).getString(KEY_DEFAULT_PCT_FEE, "") ?: ""

    /** Default إعادة التنزيل percentage, kept as raw text for the input field. */
    fun getDefaultPctAgentCost(context: Context): String =
        prefs(context).getString(KEY_DEFAULT_PCT_AGENT_COST, "") ?: ""

    fun getDefaultFeeInclusive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DEFAULT_FEE_INCLUSIVE, DEFAULT_FEE_INCLUSIVE)

    fun getDefaultDeductionBase(context: Context): DeductionBase =
        readEnum(context, KEY_DEFAULT_DEDUCTION_BASE, DeductionBase.entries, DEFAULT_DEDUCTION_BASE)

    fun getDefaultDirection(context: Context): TransferDirection =
        readEnum(context, KEY_DEFAULT_DIRECTION, TransferDirection.entries, DEFAULT_DIRECTION)

    fun getDefaultMode(context: Context): CalcMode =
        readEnum(context, KEY_DEFAULT_MODE, CalcMode.entries, DEFAULT_MODE)

    fun getDefaultCustomerDiscount(context: Context): Double =
        prefs(context).getFloat(KEY_DEFAULT_CUSTOMER_DISCOUNT, DEFAULT_CUSTOMER_DISCOUNT.toFloat())
            .toDouble()

    fun setDefaults(
        context: Context,
        direction: TransferDirection,
        mode: CalcMode,
        pctFee: String,
        feeInclusive: Boolean,
        pctAgentCost: String,
        deductionBase: DeductionBase,
        customerDiscount: Double,
    ) {
        prefs(context).edit()
            .putString(KEY_DEFAULT_DIRECTION, direction.name)
            .putString(KEY_DEFAULT_MODE, mode.name)
            .putString(KEY_DEFAULT_PCT_FEE, pctFee.trim())
            .putBoolean(KEY_DEFAULT_FEE_INCLUSIVE, feeInclusive)
            .putString(KEY_DEFAULT_PCT_AGENT_COST, pctAgentCost.trim())
            .putString(KEY_DEFAULT_DEDUCTION_BASE, deductionBase.name)
            .putFloat(KEY_DEFAULT_CUSTOMER_DISCOUNT, customerDiscount.toFloat())
            .remove(KEY_RETIRED_FLAT_AGENT_COST)
            .apply()
    }

    /** Enum lookup by name so reordering the enum can never repoint a stored value. */
    private fun <T : Enum<T>> readEnum(
        context: Context,
        key: String,
        entries: List<T>,
        fallback: T,
    ): T {
        val stored = prefs(context).getString(key, null)
        return entries.firstOrNull { it.name == stored } ?: fallback
    }
}
