package com.topaloglu.topalfx.data

enum class Currency(val symbol: String) { EUR("€"), USD("$") }

enum class TransferDirection(val base: Currency, val target: Currency) {
    EUR_TO_USD(Currency.EUR, Currency.USD),
    USD_TO_EUR(Currency.USD, Currency.EUR),
    EUR_TO_EUR(Currency.EUR, Currency.EUR),
    USD_TO_USD(Currency.USD, Currency.USD);

    val isSameCurrency: Boolean get() = base == target
}

enum class CalcMode { SEND_EXACT, RECEIVE_EXACT, CUSTOM_DEAL }

/**
 * أساس التنزيل — which amount إعادة التنزيل is charged on.
 *
 * ON_RECEIVED charges it on the cash taken in from the customer, which is what
 * physically needs converting to digital balance. ON_DELIVERED charges it on the
 * transfer amount instead. Neither ever reduces what the beneficiary receives.
 */
enum class DeductionBase { ON_RECEIVED, ON_DELIVERED }

enum class CalcError { INVALID_NUMBER, NEGATIVE_VALUE, FEE_OVERFLOW, MISSING_RATE }

data class CalcInput(
    val direction: TransferDirection,
    val mode: CalcMode,
    val deductionBase: DeductionBase = DeductionBase.ON_RECEIVED,
    /** true → the typed amount is the cash R; false → it is the transfer amount F. */
    val feeInclusive: Boolean = true,
    /** SEND_EXACT: R when [feeInclusive], otherwise F. */
    val baseAmount: Double = 0.0,
    /** RECEIVE_EXACT: the exact amount the beneficiary must receive, in target currency. */
    val targetReceived: Double = 0.0,
    val customBaseReceived: Double = 0.0,
    val customTargetDelivered: Double = 0.0,
    val marketRate: Double = 0.0,
    val customerRate: Double = 0.0,
    /** الأتعاب — the office's fee, as a percentage. */
    val pctFee: Double = 0.0,
    /** أجرة تسليم المكتب — a flat amount in the TARGET currency, set by the paying office. */
    val deliveryFee: Double = 0.0,
    /** إعادة التنزيل — cash-to-digital conversion cost, as a percentage. */
    val pctAgentCost: Double = 0.0,
    /**
     * Live EUR per 1 USD, used to express the office profit in EUR — the fund's
     * accounting currency. 0.0 means unavailable (offline).
     */
    val usdToEurRate: Double = 0.0,
)

data class CalcResult(
    /** R — cash physically received from the customer, in base currency. */
    val cashReceived: Double = 0.0,
    /** F — the transferable amount after الأتعاب, in base currency. */
    val transferAmount: Double = 0.0,
    /** F × customerRate. Never reduced by any cost. */
    val targetDelivered: Double = 0.0,
    val customerFeeBase: Double = 0.0,
    val hiddenSpread: Double = 0.0,
    val agentCostBase: Double = 0.0,
    val deliveryFeeBase: Double = 0.0,
    val netProfitBase: Double = 0.0,
    /** Lowest الأتعاب % that keeps the profit at or above zero; null when undefined. */
    val breakEvenPctFee: Double? = null,
    /** EUR per 1 unit of the direction's base currency; null when unavailable. */
    val eurConversionRate: Double? = null,
    /** Office profit unified to EUR; null when the live EUR rate is unavailable. */
    val netProfitEur: Double? = null,
    val error: CalcError? = null,
)

data class RatePair(val base: String, val symbol: String) {
    val label: String get() = "$base/$symbol"

    companion object {
        fun parse(text: String): RatePair? {
            val parts = text.trim().uppercase().split('/')
            if (parts.size != 2) return null
            val (base, symbol) = parts
            if (base.length != 3 || symbol.length != 3) return null
            if (!base.all { it.isLetter() } || !symbol.all { it.isLetter() }) return null
            return RatePair(base, symbol)
        }
    }
}

data class TickerRate(
    val pair: RatePair,
    val rate: Double,
    val lastUpdated: Long = 0L,
)
