package com.topaloglu.topalfx.data

/**
 * إعادة تنزيل مبلغ — turning cash into digital balance in the fund wallet.
 *
 * The operator always takes its cut out of the CASH, matching how [MarginEngine] charges
 * إعادة التنزيل on the cash taken in from a customer:
 *
 * ```
 * التكلفة = الكاش × النسبة
 * الرصيد  = الكاش − التكلفة
 * ```
 *
 * What changes is which end of that the office knows first, which is what
 * [CashDownloadInput.feeFromAmount] selects:
 *
 * - **من نفس المبلغ** — the typed amount is the cash in hand, and the cut comes out of
 *   it. 100 at 2% leaves 98 in the wallet.
 * - **خارجي** — the typed amount is what has to land in the wallet, and the cut is paid
 *   on top. 100 at 2% needs 102.04 in cash.
 *
 * Single currency by design: no rate is involved, only the percentage.
 */
object CashDownloadEngine {

    fun calculate(input: CashDownloadInput): CashDownloadResult {
        validate(input)?.let { return CashDownloadResult(error = it) }

        val rate = input.pctRate / 100.0
        val cash: Double
        val digital: Double
        if (input.feeFromAmount) {
            cash = input.amount
            digital = cash * (1.0 - rate)
        } else {
            digital = input.amount
            cash = digital / (1.0 - rate)
        }
        return CashDownloadResult(
            cashRequired = cash,
            digitalArrived = digital,
            cost = cash - digital,
        )
    }

    private fun validate(input: CashDownloadInput): CalcError? {
        val relevant = listOf(input.amount, input.pctRate)
        if (relevant.any { !it.isFinite() }) return CalcError.INVALID_NUMBER
        if (relevant.any { it < 0.0 }) return CalcError.NEGATIVE_VALUE
        // At 100% the whole float is eaten and nothing ever arrives.
        if (input.pctRate >= 100.0) return CalcError.FEE_OVERFLOW
        return null
    }
}

data class CashDownloadInput(
    val currency: Currency = Currency.EUR,
    /** The cash in hand when [feeFromAmount], otherwise the balance that must arrive. */
    val amount: Double = 0.0,
    /** إعادة التنزيل percentage, always charged on the cash. */
    val pctRate: Double = 0.0,
    /** true → the cut comes out of the typed amount; false → it is paid on top. */
    val feeFromAmount: Boolean = true,
)

data class CashDownloadResult(
    /** The cash handed over. */
    val cashRequired: Double = 0.0,
    /** What lands in the wallet. */
    val digitalArrived: Double = 0.0,
    /** What the operation costs — the gap between the two. */
    val cost: Double = 0.0,
    val error: CalcError? = null,
)
