package com.topaloglu.topalfx.data

/**
 * إعادة تنزيل مبلغ — turning cash into digital balance in the fund wallet.
 *
 * The operator takes a percentage of the cash handed over, so the wallet is credited
 * with less than what went in:
 *
 * ```
 * الرصيد = الكاش × (1 − النسبة)
 * ```
 *
 * The office works backwards from the balance it needs, so the engine solves for the
 * cash instead:
 *
 * ```
 * الكاش   = الرصيد ÷ (1 − النسبة)
 * التكلفة = الكاش − الرصيد        (‏= الكاش × النسبة)
 * ```
 *
 * The percentage is charged on the CASH, matching how [MarginEngine] charges إعادة
 * التنزيل on the cash taken in from the customer. Charging it on the balance instead
 * would give a slightly smaller figure — on 5,000 at 2.5% the cash needed would read
 * 5,125 rather than 5,128.21.
 *
 * Single currency by design: no rate is involved, only the percentage.
 */
object CashDownloadEngine {

    fun calculate(input: CashDownloadInput): CashDownloadResult {
        validate(input)?.let { return CashDownloadResult(error = it) }

        val rate = input.pctRate / 100.0
        val cashRequired = input.desiredDigital / (1.0 - rate)
        return CashDownloadResult(
            desiredDigital = input.desiredDigital,
            cashRequired = cashRequired,
            cost = cashRequired - input.desiredDigital,
        )
    }

    private fun validate(input: CashDownloadInput): CalcError? {
        val relevant = listOf(input.desiredDigital, input.pctRate)
        if (relevant.any { !it.isFinite() }) return CalcError.INVALID_NUMBER
        if (relevant.any { it < 0.0 }) return CalcError.NEGATIVE_VALUE
        // At 100% the whole float is eaten and no amount of cash ever arrives.
        if (input.pctRate >= 100.0) return CalcError.FEE_OVERFLOW
        return null
    }
}

data class CashDownloadInput(
    val currency: Currency = Currency.EUR,
    /** The balance that has to land in the wallet. */
    val desiredDigital: Double = 0.0,
    /** إعادة التنزيل percentage, charged on the cash. */
    val pctRate: Double = 0.0,
)

data class CashDownloadResult(
    val desiredDigital: Double = 0.0,
    /** The cash to hand over so [desiredDigital] arrives. */
    val cashRequired: Double = 0.0,
    /** What the operation costs — the gap between the two. */
    val cost: Double = 0.0,
    val error: CalcError? = null,
)
