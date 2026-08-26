package com.topaloglu.topalfx.data

/**
 * إعادة تنزيل مبلغ — a standalone quick calculator for cash-to-wallet conversions.
 *
 * **This is deliberately independent of [MarginEngine].** It exists to price a download
 * on its own, not as part of a transfer, and it must not be tied to how the transfer
 * engine charges its own إعادة التنزيل line.
 *
 * The office quotes the percentage against the amount being downloaded — the number typed
 * in — and never against the grossed-up cash. So the fee is the same in both directions:
 *
 * ```
 * الأجرة = المبلغ المكتوب × النسبة
 *
 * داخلي (من نفس المبلغ): الكاش = المبلغ        ، الواصل = المبلغ − الأجرة
 * خارجي:                  الواصل = المبلغ       ، الكاش  = المبلغ + الأجرة
 * ```
 *
 * At 2.5% on 100 the fee is 2.50 either way: 97.50 arrives when it comes out of the
 * amount, 102.50 is handed over when it does not.
 *
 * Single currency by design: no rate is involved, only the percentage.
 */
object CashDownloadEngine {

    fun calculate(input: CashDownloadInput): CashDownloadResult {
        validate(input)?.let { return CashDownloadResult(error = it) }

        val fee = input.amount * input.pctRate / 100.0
        return if (input.feeFromAmount) {
            CashDownloadResult(
                cashRequired = input.amount,
                digitalArrived = input.amount - fee,
                cost = fee,
            )
        } else {
            CashDownloadResult(
                cashRequired = input.amount + fee,
                digitalArrived = input.amount,
                cost = fee,
            )
        }
    }

    private fun validate(input: CashDownloadInput): CalcError? {
        val relevant = listOf(input.amount, input.pctRate)
        if (relevant.any { !it.isFinite() }) return CalcError.INVALID_NUMBER
        if (relevant.any { it < 0.0 }) return CalcError.NEGATIVE_VALUE
        // At 100% the fee swallows the whole amount and nothing arrives.
        if (input.pctRate >= 100.0) return CalcError.FEE_OVERFLOW
        return null
    }
}

data class CashDownloadInput(
    val currency: Currency = Currency.EUR,
    /** The cash in hand when [feeFromAmount], otherwise the balance that must arrive. */
    val amount: Double = 0.0,
    /** إعادة التنزيل percentage, always charged on [amount]. */
    val pctRate: Double = 0.0,
    /** true → the fee comes out of the typed amount; false → it is added on top. */
    val feeFromAmount: Boolean = true,
)

data class CashDownloadResult(
    /** The cash handed over. */
    val cashRequired: Double = 0.0,
    /** What lands in the wallet. */
    val digitalArrived: Double = 0.0,
    /** The office's fee for the operation. */
    val cost: Double = 0.0,
    val error: CalcError? = null,
)
