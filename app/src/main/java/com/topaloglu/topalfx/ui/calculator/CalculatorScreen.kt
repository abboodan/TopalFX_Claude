package com.topaloglu.topalfx.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.data.CalcError
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.DeductionBase
import com.topaloglu.topalfx.data.TransferDirection
import com.topaloglu.topalfx.viewmodel.CalculatorUiState
import com.topaloglu.topalfx.viewmodel.CalculatorViewModel
import com.topaloglu.topalfx.viewmodel.InputField

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Direction selector
        Text(
            text = stringResource(R.string.direction_title),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransferDirection.entries.forEach { direction ->
                FilterChip(
                    selected = state.direction == direction,
                    onClick = { viewModel.onDirectionChanged(direction) },
                    label = { Text("${direction.base.name} ➔ ${direction.target.name}") },
                )
            }
        }

        // Mode tabs
        PrimaryTabRow(selectedTabIndex = state.mode.ordinal) {
            CalcMode.entries.forEach { mode ->
                Tab(
                    selected = state.mode == mode,
                    onClick = { viewModel.onModeChanged(mode) },
                    text = { Text(modeLabel(mode)) },
                )
            }
        }

        ModeInputs(state, viewModel)

        // Global engine errors that don't belong to a single field
        val globalError = state.result?.error
        if (globalError == CalcError.FEE_OVERFLOW || globalError == CalcError.MISSING_RATE) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = errorLabel(globalError),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        state.result?.takeIf { it.error == null }?.let { result ->
            ResultsCard(result = result, direction = state.direction, mode = state.mode)
        }
    }
}

@Composable
private fun ModeInputs(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (state.mode) {
            CalcMode.SEND_EXACT -> {
                NumberField(state, viewModel, InputField.BASE_AMOUNT, R.string.input_base_amount)
                FeeInclusiveSwitch(state, viewModel)
                RateFields(state, viewModel)
                CustomerFeeFields(state, viewModel)
                AgentCostFields(state, viewModel)
            }
            CalcMode.RECEIVE_EXACT -> {
                NumberField(state, viewModel, InputField.TARGET_RECEIVED, R.string.input_target_received)
                RateFields(state, viewModel)
                CustomerFeeFields(state, viewModel)
                AgentCostFields(state, viewModel)
            }
            CalcMode.CUSTOM_DEAL -> {
                NumberField(state, viewModel, InputField.CUSTOM_BASE_RECEIVED, R.string.input_custom_base_received)
                NumberField(state, viewModel, InputField.CUSTOM_TARGET_DELIVERED, R.string.input_custom_target_delivered)
                NumberField(state, viewModel, InputField.MARKET_RATE, R.string.input_market_rate, locked = state.ratesLocked)
                AgentCostFields(state, viewModel)
            }
        }
    }
}

@Composable
private fun RateFields(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(
            state, viewModel, InputField.MARKET_RATE, R.string.input_market_rate,
            locked = state.ratesLocked,
            modifier = Modifier.weight(1f),
        )
        NumberField(
            state, viewModel, InputField.CUSTOMER_RATE, R.string.input_customer_rate,
            locked = state.ratesLocked,
            modifier = Modifier.weight(1f),
        )
    }
    if (state.ratesLocked) {
        Text(
            text = stringResource(R.string.rates_locked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun CustomerFeeFields(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(state, viewModel, InputField.FLAT_FEE, R.string.input_flat_fee, modifier = Modifier.weight(1f))
        NumberField(state, viewModel, InputField.PCT_FEE, R.string.input_pct_fee, modifier = Modifier.weight(1f))
    }
    NumberField(state, viewModel, InputField.DELIVERY_FEE, R.string.input_delivery_fee)
}

@Composable
private fun AgentCostFields(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(state, viewModel, InputField.FLAT_AGENT_COST, R.string.input_flat_agent_cost, modifier = Modifier.weight(1f))
        NumberField(state, viewModel, InputField.PCT_AGENT_COST, R.string.input_pct_agent_cost, modifier = Modifier.weight(1f))
    }
    Text(
        text = stringResource(R.string.deduction_base),
        style = MaterialTheme.typography.titleSmall,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        DeductionBase.entries.forEachIndexed { index, base ->
            SegmentedButton(
                selected = state.deductionBase == base,
                onClick = { viewModel.onDeductionBaseChanged(base) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = DeductionBase.entries.size),
            ) {
                Text(
                    text = stringResource(
                        if (base == DeductionBase.ON_RECEIVED) R.string.deduction_on_received
                        else R.string.deduction_on_delivered
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun FeeInclusiveSwitch(state: CalculatorUiState, viewModel: CalculatorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.fee_inclusive),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = state.feeInclusive,
            onCheckedChange = viewModel::onFeeInclusiveChanged,
        )
    }
}

@Composable
private fun NumberField(
    state: CalculatorUiState,
    viewModel: CalculatorViewModel,
    field: InputField,
    labelRes: Int,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
) {
    val raw = state.field(field)
    val fieldError = when {
        raw.isBlank() -> null
        CalculatorViewModel.parse(raw).isNaN() -> stringResource(R.string.error_invalid_number)
        CalculatorViewModel.parse(raw) < 0.0 -> stringResource(R.string.error_negative_value)
        else -> null
    }
    ValidatedNumberField(
        value = raw,
        onValueChange = { viewModel.onFieldChanged(field, it) },
        label = stringResource(labelRes),
        locked = locked,
        errorText = fieldError,
        modifier = modifier,
    )
}

@Composable
private fun modeLabel(mode: CalcMode): String = stringResource(
    when (mode) {
        CalcMode.SEND_EXACT -> R.string.mode_send_exact
        CalcMode.RECEIVE_EXACT -> R.string.mode_receive_exact
        CalcMode.CUSTOM_DEAL -> R.string.mode_custom_deal
    }
)

@Composable
private fun errorLabel(error: CalcError): String = stringResource(
    when (error) {
        CalcError.INVALID_NUMBER -> R.string.error_invalid_number
        CalcError.NEGATIVE_VALUE -> R.string.error_negative_value
        CalcError.FEE_OVERFLOW -> R.string.error_fee_overflow
        CalcError.MISSING_RATE -> R.string.error_missing_rate
    }
)
