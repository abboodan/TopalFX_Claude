package com.topaloglu.topalfx

import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarginEngineModeCTest {

    private val delta = 1e-6

    @Test
    fun `custom deal profit is spread minus agent costs`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.CUSTOM_DEAL,
                customBaseReceived = 1000.0,
                customTargetDelivered = 1040.0,
                flatAgentCost = 10.0,
                marketRate = 1.10,
            )
        )
        assertNull(result.error)
        val expectedSpread = 1000.0 - 1040.0 / 1.10
        assertEquals(expectedSpread, result.hiddenSpread, delta)
        assertEquals(expectedSpread - 10.0, result.netProfitBase, delta)
        assertEquals(1000.0, result.totalPaidByCustomer, delta)
        assertEquals(1040.0, result.totalReceivedByCustomer, delta)
    }

    @Test
    fun `customer fees and delivery fee are forced to zero`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.EUR_TO_USD,
                mode = CalcMode.CUSTOM_DEAL,
                customBaseReceived = 1000.0,
                customTargetDelivered = 1040.0,
                // These must all be ignored in Custom Deal mode.
                flatFee = 99.0,
                pctFee = 7.0,
                deliveryFee = 50.0,
                marketRate = 1.10,
            )
        )
        assertNull(result.error)
        assertEquals(0.0, result.flatFee, 0.0)
        assertEquals(0.0, result.pctFeeBase, 0.0)
        assertEquals(0.0, result.deliveryFeeBase, 0.0)
        assertEquals(1000.0 - 1040.0 / 1.10, result.netProfitBase, delta)
    }

    @Test
    fun `same-currency custom deal margin is the raw difference`() {
        val result = MarginEngine.calculateProfit(
            CalcInput(
                direction = TransferDirection.USD_TO_USD,
                mode = CalcMode.CUSTOM_DEAL,
                customBaseReceived = 1000.0,
                customTargetDelivered = 980.0,
                marketRate = 55.0,
            )
        )
        assertNull(result.error)
        assertEquals(20.0, result.hiddenSpread, delta)
        assertEquals(20.0, result.netProfitBase, delta)
    }
}
