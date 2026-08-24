package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcError
import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarginEngineValidationTest {

    private val base = CalcInput(
        direction = TransferDirection.EUR_TO_USD,
        mode = CalcMode.SEND_EXACT,
        feeInclusive = true,
        baseAmount = 1000.0,
        marketRate = 1.10,
        customerRate = 1.05,
        pctFee = 5.0,
        pctAgentCost = 2.0,
    )

    @Test
    fun `each invalid input reports its own error`() {
        assertEquals(
            CalcError.NEGATIVE_VALUE,
            MarginEngine.calculateProfit(base.copy(baseAmount = -1.0)).error
        )
        assertEquals(
            CalcError.INVALID_NUMBER,
            MarginEngine.calculateProfit(base.copy(marketRate = Double.NaN)).error
        )
        assertEquals(
            CalcError.MISSING_RATE,
            MarginEngine.calculateProfit(base.copy(marketRate = 0.0)).error
        )
        assertEquals(
            CalcError.MISSING_RATE,
            MarginEngine.calculateProfit(base.copy(customerRate = 0.0)).error
        )
    }

    @Test
    fun `a fee of 100 percent or more is rejected as a typo`() {
        assertEquals(
            CalcError.FEE_OVERFLOW,
            MarginEngine.calculateProfit(base.copy(pctFee = 100.0)).error
        )
        assertEquals(
            CalcError.FEE_OVERFLOW,
            MarginEngine.calculateProfit(base.copy(pctFee = 120.0)).error
        )
        // ...in both fee modes, so the rule stays easy to explain.
        assertEquals(
            CalcError.FEE_OVERFLOW,
            MarginEngine.calculateProfit(base.copy(pctFee = 120.0, feeInclusive = false)).error
        )
    }

    @Test
    fun `an absurd but valid fee just under 100 percent is allowed`() {
        assertNull(MarginEngine.calculateProfit(base.copy(pctFee = 99.9)).error)
    }

    @Test
    fun `custom deal has no fee to overflow`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.CUSTOM_DEAL,
                customBaseReceived = 1000.0,
                customTargetDelivered = 1040.0,
                marketRate = 1.10,
                pctFee = 250.0,
                customerRate = 0.0,
            )
        )
        assertNull(result.error)
    }
}
