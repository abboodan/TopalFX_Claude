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

/** أساس التنزيل — which amount the percentage agent cost is charged on. */
enum class DeductionBase { ON_RECEIVED, ON_DELIVERED }

enum class CalcError { INVALID_NUMBER, NEGATIVE_VALUE, FEE_OVERFLOW, MISSING_RATE }

data class CalcInput(
    val direction: TransferDirection,
    val mode: CalcMode,
    val deductionBase: DeductionBase = DeductionBase.ON_RECEIVED,
    val feeInclusive: Boolean = false,
    // Mode A
    val baseAmount: Double = 0.0,
    // Mode B
    val targetReceived: Double = 0.0,
    // Mode C
    val customBaseReceived: Double = 0.0,
    val customTargetDelivered: Double = 0.0,
    // shared
    val marketRate: Double = 0.0,
    val customerRate: Double = 0.0,
    val flatFee: Double = 0.0,
    val pctFee: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val flatAgentCost: Double = 0.0,
    val pctAgentCost: Double = 0.0,
    /**
     * Live EUR per 1 USD, used to express the office profit in EUR — the fund's
     * accounting currency. 0.0 means unavailable (offline).
     */
    val usdToEurRate: Double = 0.0,
)

data class CalcResult(
    val principal: Double = 0.0,
    val targetDelivered: Double = 0.0,
    val totalReceivedByCustomer: Double = 0.0,
    val totalPaidByCustomer: Double = 0.0,
    val flatFee: Double = 0.0,
    val pctFeeBase: Double = 0.0,
    val hiddenSpread: Double = 0.0,
    val agentCostFlat: Double = 0.0,
    val agentCostPctBase: Long = 0L,
    val deliveryFeeBase: Double = 0.0,
    val netProfitBase: Double = 0.0,
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
