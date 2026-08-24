package com.topaloglu.topalfx.ui.settings

import android.content.Context
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.data.TransferDirection
import com.topaloglu.topalfx.ui.calculator.formatRate
import com.topaloglu.topalfx.util.Prefs

/**
 * The settings form as one value.
 *
 * Passing seven settings as separate parameter/callback pairs would give
 * [SettingsScreenContent] a fifteen-parameter signature and an unreadable preview.
 */
data class SettingsFormState(
    val direction: TransferDirection = Prefs.DEFAULT_DIRECTION,
    val mode: CalcMode = Prefs.DEFAULT_MODE,
    val pctFee: String = "",
    val feeInclusive: Boolean = Prefs.DEFAULT_FEE_INCLUSIVE,
    val pctAgentCost: String = "",
    val deductionBase: DeductionBase = Prefs.DEFAULT_DEDUCTION_BASE,
    val customerDiscount: String = "0.0100",
) {
    companion object {
        fun from(context: Context) = SettingsFormState(
            direction = Prefs.getDefaultDirection(context),
            mode = Prefs.getDefaultMode(context),
            pctFee = Prefs.getDefaultPctFee(context),
            feeInclusive = Prefs.getDefaultFeeInclusive(context),
            pctAgentCost = Prefs.getDefaultPctAgentCost(context),
            deductionBase = Prefs.getDefaultDeductionBase(context),
            customerDiscount = formatRate(Prefs.getDefaultCustomerDiscount(context)),
        )
    }
}
