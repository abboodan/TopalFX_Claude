package com.topaloglu.topalfx.data

/**
 * Pure calculation engine — no Android dependencies, fully unit-testable.
 *
 * All monetary results are expressed in the base currency unless stated otherwise.
 * Same-currency directions (EUR→EUR, USD→USD) statically lock both rates to 1.0
 * and force the hidden spread to 0.0.
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

    /** Same-currency directions lock rates at 1.0000; Custom Deal forces customer fees to zero. */
    private fun normalize(input: CalcInput): CalcInput {
        var result = input
        if (input.direction.isSameCurrency) {
            result = result.copy(marketRate = 1.0, customerRate = 1.0)
        }
        if (input.mode == CalcMode.CUSTOM_DEAL) {
            result = result.copy(flatFee = 0.0, pctFee = 0.0, deliveryFee = 0.0, feeInclusive = false)
        }
        return result
    }

    private fun validate(input: CalcInput): CalcError? {
        val relevant = when (input.mode) {
            CalcMode.SEND_EXACT -> listOf(
                input.baseAmount, input.marketRate, input.customerRate,
                input.flatFee, input.pctFee, input.deliveryFee,
                input.flatAgentCost, input.pctAgentCost,
            )
            CalcMode.RECEIVE_EXACT -> listOf(
                input.targetReceived, input.marketRate, input.customerRate,
                input.flatFee, input.pctFee, input.deliveryFee,
                input.flatAgentCost, input.pctAgentCost,
            )
            CalcMode.CUSTOM_DEAL -> listOf(
                input.customBaseReceived, input.customTargetDelivered,
                input.marketRate, input.flatAgentCost, input.pctAgentCost,
            )
        }
        if (relevant.any { !it.isFinite() }) return CalcError.INVALID_NUMBER
        if (relevant.any { it < 0.0 }) return CalcError.NEGATIVE_VALUE
        if (input.marketRate <= 0.0) return CalcError.MISSING_RATE
        if (input.mode != CalcMode.CUSTOM_DEAL && input.customerRate <= 0.0) return CalcError.MISSING_RATE
        if (input.mode == CalcMode.SEND_EXACT && input.feeInclusive) {
            val principal = (input.baseAmount - input.flatFee) / (1.0 + input.pctFee / 100.0)
            if (input.flatFee >= input.baseAmount || principal <= 0.0) return CalcError.FEE_OVERFLOW
        }
        return null
    }

    private fun sendExact(input: CalcInput): CalcResult {
        val principal =
            if (input.feeInclusive) (input.baseAmount - input.flatFee) / (1.0 + input.pctFee / 100.0)
            else input.baseAmount
        val targetDelivered = principal * input.customerRate
        val totalReceived = targetDelivered - input.deliveryFee
        val pctFeeBase =
            if (input.direction.base == Currency.EUR) principal * input.pctFee / 100.0
            else (targetDelivered * input.pctFee) / (100.0 * input.customerRate)
        val totalPaid =
            if (input.feeInclusive) input.baseAmount
            else principal + input.flatFee + pctFeeBase
        val hiddenSpread =
            if (input.direction.isSameCurrency) 0.0
            else principal - (principal * input.customerRate) / input.marketRate
        val deliveryFeeBase = deliveryFeeBase(input)
        val agentCostPct = pctAgentCostBase(input, targetDelivered, effectiveBaseAmount = input.baseAmount)
        return CalcResult(
            principal = principal,
            targetDelivered = targetDelivered,
            totalReceivedByCustomer = totalReceived,
            totalPaidByCustomer = totalPaid,
            flatFee = input.flatFee,
            pctFeeBase = pctFeeBase,
            hiddenSpread = hiddenSpread,
            agentCostFlat = input.flatAgentCost,
            agentCostPctBase = agentCostPct,
            deliveryFeeBase = deliveryFeeBase,
            netProfitBase = (input.flatFee + pctFeeBase + hiddenSpread) -
                (input.flatAgentCost + agentCostPct) - deliveryFeeBase,
        )
    }

    private fun receiveExact(input: CalcInput): CalcResult {
        val targetNeeded = input.targetReceived + input.deliveryFee
        val principal = targetNeeded / input.customerRate
        val pctFeeBase = principal * input.pctFee / 100.0
        val totalPaid = principal + input.flatFee + pctFeeBase
        val hiddenSpread =
            if (input.direction.isSameCurrency) 0.0
            else principal - targetNeeded / input.marketRate
        val deliveryFeeBase = deliveryFeeBase(input)
        val agentCostPct = pctAgentCostBase(input, targetNeeded, effectiveBaseAmount = principal)
        return CalcResult(
            principal = principal,
            targetDelivered = targetNeeded,
            totalReceivedByCustomer = input.targetReceived,
            totalPaidByCustomer = totalPaid,
            flatFee = input.flatFee,
            pctFeeBase = pctFeeBase,
            hiddenSpread = hiddenSpread,
            agentCostFlat = input.flatAgentCost,
            agentCostPctBase = agentCostPct,
            deliveryFeeBase = deliveryFeeBase,
            netProfitBase = (input.flatFee + pctFeeBase + hiddenSpread) -
                (input.flatAgentCost + agentCostPct) - deliveryFeeBase,
        )
    }

    private fun customDeal(input: CalcInput): CalcResult {
        val baseMarketCost = input.customTargetDelivered / input.marketRate
        val hiddenSpread = input.customBaseReceived - baseMarketCost
        val agentCostPct = pctAgentCostBase(
            input,
            input.customTargetDelivered,
            effectiveBaseAmount = input.customBaseReceived,
        )
        return CalcResult(
            principal = input.customBaseReceived,
            targetDelivered = input.customTargetDelivered,
            totalReceivedByCustomer = input.customTargetDelivered,
            totalPaidByCustomer = input.customBaseReceived,
            hiddenSpread = hiddenSpread,
            agentCostFlat = input.flatAgentCost,
            agentCostPctBase = agentCostPct,
            netProfitBase = hiddenSpread - (input.flatAgentCost + agentCostPct),
        )
    }

    /**
     * Office (agent) percentage cost in base currency — ALWAYS rounded to the
     * nearest whole integer with Math.round(), per business rule.
     *
     * ON_RECEIVED with a USD base (Send Exact) follows the spec's derivation via the
     * delivered target; for Receive Exact / Custom Deal the base-side amount is known
     * directly, so it is used as-is.
     */
    private fun pctAgentCostBase(
        input: CalcInput,
        targetDelivered: Double,
        effectiveBaseAmount: Double,
    ): Long = when (input.deductionBase) {
        DeductionBase.ON_RECEIVED ->
            if (input.mode == CalcMode.SEND_EXACT && input.direction.base == Currency.USD)
                Math.round((targetDelivered * input.pctAgentCost / 100.0) / input.customerRate)
            else
                Math.round(effectiveBaseAmount * input.pctAgentCost / 100.0)
        DeductionBase.ON_DELIVERED ->
            Math.round((targetDelivered / input.marketRate) * input.pctAgentCost / 100.0)
    }

    private fun deliveryFeeBase(input: CalcInput): Double =
        if (input.direction.isSameCurrency) input.deliveryFee
        else input.deliveryFee / input.marketRate
}
