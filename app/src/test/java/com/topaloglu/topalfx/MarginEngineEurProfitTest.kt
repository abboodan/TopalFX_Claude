package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The office fund is accounted in EUR, so profit is unified to EUR. */
class MarginEngineEurProfitTest {

    private val delta = 1e-6

    @Test
    fun `EUR base profit is already in EUR`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                baseAmount = 1000.0,
                marketRate = 1.15,
                customerRate = 1.14,
            )
        )
        assertNull(result.error)
        assertEquals(1.0, result.eurConversionRate!!, delta)
        assertEquals(result.netProfitBase, result.netProfitEur!!, delta)
    }

    @Test
    fun `USD to USD profit converts with the live EUR rate`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.USD_TO_USD,
                mode = CalcMode.SEND_EXACT,
                baseAmount = 1000.0,
                flatFee = 50.0,
                deliveryFee = 20.0,
                usdToEurRate = 0.92,
            )
        )
        assertNull(result.error)
        // Same-currency: no spread, profit = 50 flat fee - 20 delivery = 30 USD.
        assertEquals(30.0, result.netProfitBase, delta)
        assertEquals(0.92, result.eurConversionRate!!, delta)
        assertEquals(30.0 * 0.92, result.netProfitEur!!, delta)
    }

    @Test
    fun `USD to EUR falls back to the market rate when live EUR rate is missing`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.USD_TO_EUR,
                mode = CalcMode.SEND_EXACT,
                baseAmount = 1000.0,
                flatFee = 10.0,
                marketRate = 0.92,
                customerRate = 0.91,
                usdToEurRate = 0.0,
            )
        )
        assertNull(result.error)
        assertEquals(0.92, result.eurConversionRate!!, delta)
        assertEquals(result.netProfitBase * 0.92, result.netProfitEur!!, delta)
    }

    @Test
    fun `USD to USD without a live EUR rate reports no EUR figure`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.USD_TO_USD,
                mode = CalcMode.SEND_EXACT,
                baseAmount = 1000.0,
                flatFee = 50.0,
                usdToEurRate = 0.0,
            )
        )
        assertNull(result.error)
        assertNull(result.eurConversionRate)
        assertNull(result.netProfitEur)
        assertEquals(50.0, result.netProfitBase, delta)
    }

    @Test
    fun `customer rate one cent below market produces the expected spread`() {
        // 1.15 market, 1.14 customer — the office keeps the one-cent difference.
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.SEND_EXACT,
                baseAmount = 1000.0,
                marketRate = 1.15,
                customerRate = 1.14,
            )
        )
        assertNull(result.error)
        assertEquals(1140.0, result.targetDelivered, delta)
        assertEquals(1000.0 - 1140.0 / 1.15, result.hiddenSpread, delta)
        assertEquals(result.hiddenSpread, result.netProfitEur!!, delta)
    }
}
