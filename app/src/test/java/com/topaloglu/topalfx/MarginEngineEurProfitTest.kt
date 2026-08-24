package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The office fund is accounted in EUR, so every profit is restated in EUR. */
class MarginEngineEurProfitTest {

    private val delta = 1e-6

    private fun input(
        direction: TransferDirection,
        marketRate: Double,
        customerRate: Double,
        usdToEurRate: Double = 0.0,
    ) = CalcInput(
        direction = direction,
        mode = CalcMode.SEND_EXACT,
        feeInclusive = true,
        baseAmount = 1000.0,
        marketRate = marketRate,
        customerRate = customerRate,
        pctFee = 4.0,
        pctAgentCost = 2.0,
        usdToEurRate = usdToEurRate,
    )

    @Test
    fun `an EUR base needs no conversion`() {
        val result = MarginEngine.calculateProfit(
            input(TransferDirection.EUR_TO_USD, marketRate = 1.10, customerRate = 1.05)
        )
        assertEquals(1.0, result.eurConversionRate!!, 0.0)
        assertEquals(result.netProfitBase, result.netProfitEur!!, delta)
    }

    @Test
    fun `a USD base converts with the live EUR rate`() {
        val result = MarginEngine.calculateProfit(
            input(
                TransferDirection.USD_TO_USD,
                marketRate = 1.0,
                customerRate = 1.0,
                usdToEurRate = 0.92,
            )
        )
        assertEquals(0.92, result.eurConversionRate!!, 0.0)
        assertEquals(result.netProfitBase * 0.92, result.netProfitEur!!, delta)
    }

    @Test
    fun `USD to EUR falls back to its own market rate when the live rate is missing`() {
        val result = MarginEngine.calculateProfit(
            input(TransferDirection.USD_TO_EUR, marketRate = 0.92, customerRate = 0.90)
        )
        assertEquals(0.92, result.eurConversionRate!!, 0.0)
        assertNotNull(result.netProfitEur)
    }

    @Test
    fun `USD to USD without a live rate reports no EUR figure`() {
        val result = MarginEngine.calculateProfit(
            input(TransferDirection.USD_TO_USD, marketRate = 1.0, customerRate = 1.0)
        )
        assertNull(result.eurConversionRate)
        assertNull(result.netProfitEur)
    }
}
