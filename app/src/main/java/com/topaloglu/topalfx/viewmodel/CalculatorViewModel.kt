package com.topaloglu.topalfx.viewmodel

import androidx.lifecycle.ViewModel
import com.topaloglu.topalfx.data.CalcInput
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.CalcResult
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.data.MarginEngine
import com.topaloglu.topalfx.data.TransferDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class InputField {
    BASE_AMOUNT, TARGET_RECEIVED, CUSTOM_BASE_RECEIVED, CUSTOM_TARGET_DELIVERED,
    MARKET_RATE, CUSTOMER_RATE, FLAT_FEE, PCT_FEE, DELIVERY_FEE,
    FLAT_AGENT_COST, PCT_AGENT_COST,
}

data class CalculatorUiState(
    val direction: TransferDirection = TransferDirection.EUR_TO_USD,
    val mode: CalcMode = CalcMode.SEND_EXACT,
    val deductionBase: DeductionBase = DeductionBase.ON_RECEIVED,
    val feeInclusive: Boolean = false,
    val fields: Map<InputField, String> = emptyMap(),
    val result: CalcResult? = null,
) {
    val ratesLocked: Boolean get() = direction.isSameCurrency
    fun field(field: InputField): String = fields[field] ?: ""
}

/**
 * Reactive calculation engine host: every input change immediately re-runs
 * [MarginEngine.calculateProfit] — pure math, no coroutines needed.
 */
class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(recalculated(CalculatorUiState()))
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    fun onFieldChanged(field: InputField, raw: String) {
        update { it.copy(fields = it.fields + (field to raw)) }
    }

    fun onDirectionChanged(direction: TransferDirection) {
        update { state ->
            var fields = state.fields
            if (direction.isSameCurrency) {
                fields = fields + (InputField.MARKET_RATE to "1.0000") +
                    (InputField.CUSTOMER_RATE to "1.0000")
            }
            state.copy(direction = direction, fields = fields)
        }
    }

    fun onModeChanged(mode: CalcMode) = update { it.copy(mode = mode) }

    fun onDeductionBaseChanged(base: DeductionBase) = update { it.copy(deductionBase = base) }

    fun onFeeInclusiveChanged(enabled: Boolean) = update { it.copy(feeInclusive = enabled) }

    private fun update(transform: (CalculatorUiState) -> CalculatorUiState) {
        _uiState.value = recalculated(transform(_uiState.value))
    }

    private fun recalculated(state: CalculatorUiState): CalculatorUiState {
        val primary = when (state.mode) {
            CalcMode.SEND_EXACT -> state.field(InputField.BASE_AMOUNT)
            CalcMode.RECEIVE_EXACT -> state.field(InputField.TARGET_RECEIVED)
            CalcMode.CUSTOM_DEAL -> state.field(InputField.CUSTOM_BASE_RECEIVED)
        }
        if (primary.isBlank()) return state.copy(result = null)

        val input = CalcInput(
            direction = state.direction,
            mode = state.mode,
            deductionBase = state.deductionBase,
            feeInclusive = state.feeInclusive,
            baseAmount = parse(state.field(InputField.BASE_AMOUNT)),
            targetReceived = parse(state.field(InputField.TARGET_RECEIVED)),
            customBaseReceived = parse(state.field(InputField.CUSTOM_BASE_RECEIVED)),
            customTargetDelivered = parse(state.field(InputField.CUSTOM_TARGET_DELIVERED)),
            marketRate = parse(state.field(InputField.MARKET_RATE)),
            customerRate = parse(state.field(InputField.CUSTOMER_RATE)),
            flatFee = parse(state.field(InputField.FLAT_FEE)),
            pctFee = parse(state.field(InputField.PCT_FEE)),
            deliveryFee = parse(state.field(InputField.DELIVERY_FEE)),
            flatAgentCost = parse(state.field(InputField.FLAT_AGENT_COST)),
            pctAgentCost = parse(state.field(InputField.PCT_AGENT_COST)),
        )
        return state.copy(result = MarginEngine.calculateProfit(input))
    }

    companion object {
        /**
         * Locale-tolerant numeric parsing: Arabic-Indic digits and Arabic/European
         * decimal separators are normalized. Blank → 0.0, garbage → NaN so the
         * engine reports INVALID_NUMBER.
         */
        fun parse(raw: String): Double {
            if (raw.isBlank()) return 0.0
            val normalized = buildString(raw.length) {
                for (ch in raw.trim()) {
                    when (ch) {
                        in '٠'..'٩' -> append('0' + (ch - '٠'))
                        in '۰'..'۹' -> append('0' + (ch - '۰'))
                        '٫', ',' -> append('.')
                        else -> append(ch)
                    }
                }
            }
            return normalized.toDoubleOrNull() ?: Double.NaN
        }
    }
}
