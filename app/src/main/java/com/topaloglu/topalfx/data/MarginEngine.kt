package com.topaloglu.topalfx.data

/**
 * Pure calculation engine — no Android dependencies, fully unit-testable.
 *
 * ## The office's model
 *
 * `R` is the cash the customer physically hands over; `F` is the amount that actually
 * gets transferred, i.e. `R` minus الأتعاب. The beneficiary receives `F × customerRate`
 * and **nothing is ever deducted from that** — إعادة التنزيل and أجرة تسليم المكتب
 * both come out of the office's own profit.
 *
 * ```
 * الأتعاب  = R − F
 * السبريد  = F × (1 − customerRate / marketRate)
 * التنزيل  = R × a  (على المقبوض)  |  F × a  (على المسلّم)
 * التسليم  = deliveryFee / marketRate
 * الربح    = الأتعاب + السبريد − التنزيل − التسليم
 * ```
 *
 * Same-currency directions lock both rates to 1.0 and force the spread to 0.0.
 */
object MarginEngine {

    fun calculateProfit(rawInput: CalcInput): CalcResult {
        val input = normalize(rawInput)
        validate(input)?.let { return CalcResult(error = it) }
        val result = when (input.mode) {
            CalcMode.SEND_EXACT -> sendExact(input)
            CalcMode.RECEIVE_EXACT -> receiveExact(input)
            CalcMode.CUSTOM_DEAL -> customDeal(input)
        }
        return withEurProfit(result, input)
    }

    /** Same-currency directions lock rates at 1.0000; Custom Deal has no customer fees. */
    private fun normalize(input: CalcInput): CalcInput {
        var result = input
        if (input.direction.isSameCurrency) {
            result = result.copy(marketRate = 1.0, customerRate = 1.0)
        }
        if (input.mode == CalcMode.CUSTOM_DEAL) {
            result = result.copy(pctFee = 0.0, deliveryFee = 0.0, feeInclusive = false)
        }
        return result
    }

    private fun validate(input: CalcInput): CalcError? {
        val relevant = when (input.mode) {
            CalcMode.SEND_EXACT -> listOf(
                input.baseAmount, input.marketRate, input.customerRate,
                input.pctFee, input.deliveryFee, input.pctAgentCost,
            )
            CalcMode.RECEIVE_EXACT -> listOf(
                input.targetReceived, input.marketRate, input.customerRate,
                input.pctFee, input.deliveryFee, input.pctAgentCost,
            )
            CalcMode.CUSTOM_DEAL -> listOf(
                input.customBaseReceived, input.customTargetDelivered,
                input.marketRate, input.pctAgentCost,
            )
        }
        if (relevant.any { !it.isFinite() }) return CalcError.INVALID_NUMBER
        if (relevant.any { it < 0.0 }) return CalcError.NEGATIVE_VALUE
        if (input.marketRate <= 0.0) return CalcError.MISSING_RATE
        if (input.mode != CalcMode.CUSTOM_DEAL) {
            if (input.customerRate <= 0.0) return CalcError.MISSING_RATE
            if (input.pctFee >= 100.0) return CalcError.FEE_OVERFLOW
        }
        return null
    }

    private fun sendExact(input: CalcInput): CalcResult {
        val p = input.pctFee / 100.0
        return if (input.feeInclusive) {
            val cash = input.baseAmount
            assemble(input, cash, cash * (1.0 - p), cashAnchored = true, anchor = cash)
        } else {
            val transfer = input.baseAmount
            assemble(input, transfer * (1.0 + p), transfer, cashAnchored = false, anchor = transfer)
        }
    }

    private fun receiveExact(input: CalcInput): CalcResult {
        val p = input.pctFee / 100.0
        // The beneficiary gets exactly what was asked — the delivery fee is the office's cost.
        val transfer = input.targetReceived / input.customerRate
        val cash = if (input.feeInclusive) transfer / (1.0 - p) else transfer * (1.0 + p)
        return assemble(input, cash, transfer, cashAnchored = false, anchor = transfer)
    }

    private fun customDeal(input: CalcInput): CalcResult {
        val received = input.customBaseReceived
        val spread = received - input.customTargetDelivered / input.marketRate
        val agentCost = agentCostBase(input, cash = received, transfer = received)
        return CalcResult(
            cashReceived = received,
            transferAmount = received,
            targetDelivered = input.customTargetDelivered,
            hiddenSpread = spread,
            agentCostBase = agentCost,
            netProfitBase = spread - agentCost,
        )
    }

    /**
     * Everything downstream of (R, F). The profit line exists exactly once in this file
     * so it cannot drift between modes.
     */
    private fun assemble(
        input: CalcInput,
        cash: Double,
        transfer: Double,
        cashAnchored: Boolean,
        anchor: Double,
    ): CalcResult {
        val customerFee = cash - transfer
        val spread = transfer * spreadRate(input)
        val agentCost = agentCostBase(input, cash, transfer)
        val deliveryCost = input.deliveryFee / input.marketRate
        return CalcResult(
            cashReceived = cash,
            transferAmount = transfer,
            targetDelivered = transfer * input.customerRate,
            customerFeeBase = customerFee,
            hiddenSpread = spread,
            agentCostBase = agentCost,
            deliveryFeeBase = deliveryCost,
            netProfitBase = customerFee + spread - agentCost - deliveryCost,
            breakEvenPctFee = breakEvenPctFee(input, cashAnchored, anchor),
        )
    }

    private fun spreadRate(input: CalcInput): Double =
        if (input.direction.isSameCurrency) 0.0
        else 1.0 - input.customerRate / input.marketRate

    private fun agentCostBase(input: CalcInput, cash: Double, transfer: Double): Double {
        val a = input.pctAgentCost / 100.0
        return when (input.deductionBase) {
            DeductionBase.ON_RECEIVED -> cash * a
            DeductionBase.ON_DELIVERED -> transfer * a
        }
    }

    /**
     * Lowest الأتعاب % that keeps the profit at or above zero.
     *
     * The profit rearranges to `R·kR − F·kF − d`, so substituting whichever of R/F is not
     * anchored by the mode leaves a linear equation in `p` with a closed-form root.
     * Returns null when the answer is undefined or unreachable.
     */
    private fun breakEvenPctFee(
        input: CalcInput,
        cashAnchored: Boolean,
        anchor: Double,
    ): Double? {
        if (input.mode == CalcMode.CUSTOM_DEAL) return null
        if (!anchor.isFinite() || anchor <= 0.0) return null
        if (input.marketRate <= 0.0 || input.customerRate <= 0.0) return null

        val a = input.pctAgentCost / 100.0
        val s = spreadRate(input)
        val onReceived = input.deductionBase == DeductionBase.ON_RECEIVED
        val kR = if (onReceived) 1.0 - a else 1.0
        val kF = if (onReceived) 1.0 - s else 1.0 - s + a
        val d = input.deliveryFee / input.marketRate

        val p = when {
            cashAnchored -> {
                val denominator = anchor * kF
                if (denominator <= 0.0) return null
                1.0 - (anchor * kR - d) / denominator
            }
            input.feeInclusive -> {
                val denominator = anchor * kF + d
                if (denominator <= 0.0) return null
                1.0 - (anchor * kR) / denominator
            }
            else -> {
                val denominator = anchor * kR
                if (denominator <= 0.0) return null
                (anchor * kF + d) / denominator - 1.0
            }
        }
        if (!p.isFinite() || p >= 1.0) return null
        return (p * 100.0).coerceAtLeast(0.0)
    }

    /**
     * The office fund is accounted in EUR, so the profit is unified to EUR after
     * the transfer math is done in the direction's own base currency.
     */
    private fun withEurProfit(result: CalcResult, input: CalcInput): CalcResult {
        val rate = eurPerBaseUnit(input) ?: return result
        return result.copy(
            eurConversionRate = rate,
            netProfitEur = result.netProfitBase * rate,
        )
    }

    private fun eurPerBaseUnit(input: CalcInput): Double? = when (input.direction.base) {
        Currency.EUR -> 1.0
        Currency.USD -> when {
            input.usdToEurRate > 0.0 -> input.usdToEurRate
            // USD➔EUR already carries EUR per USD in its market rate.
            input.direction.target == Currency.EUR && input.marketRate > 0.0 -> input.marketRate
            else -> null
        }
    }
}
