package com.topaloglu.topalfx.ui.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.CalcResult
import com.topaloglu.topalfx.data.Currency
import com.topaloglu.topalfx.data.TransferDirection
import com.topaloglu.topalfx.ui.theme.ProfitDark
import com.topaloglu.topalfx.ui.theme.ProfitLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultsCard(
    result: CalcResult,
    direction: TransferDirection,
    mode: CalcMode,
    lastCalculatedAt: Long,
    modifier: Modifier = Modifier,
) {
    val baseCode = direction.base.name
    val targetCode = direction.target.name
    // The office fund is accounted in EUR; profit rows convert when the base is USD.
    val toEur = result.eurConversionRate
    val profitCode = if (toEur != null) Currency.EUR.name else baseCode
    fun profit(value: Double): Double = if (toEur != null) value * toEur else value

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.results_title),
                style = MaterialTheme.typography.titleMedium,
            )

            ResultRow(stringResource(R.string.result_cash_received), result.cashReceived, baseCode)
            ResultRow(
                stringResource(R.string.result_transfer_amount),
                result.transferAmount,
                baseCode,
            )
            ResultRow(
                stringResource(R.string.result_target_delivered),
                result.targetDelivered,
                targetCode,
            )

            HorizontalDivider()
            Text(
                text = stringResource(R.string.result_profit_section, profitCode),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (mode != CalcMode.CUSTOM_DEAL) {
                ResultRow(
                    stringResource(R.string.result_pct_fee),
                    profit(result.customerFeeBase),
                    profitCode,
                )
                ResultRow(
                    stringResource(R.string.result_delivery_fee_base),
                    profit(result.deliveryFeeBase),
                    profitCode,
                )
            }
            ResultRow(
                stringResource(R.string.result_hidden_spread),
                profit(result.hiddenSpread),
                profitCode,
            )
            ResultRow(
                stringResource(R.string.result_agent_pct),
                profit(result.agentCostBase),
                profitCode,
            )

            if (toEur == null) {
                Text(
                    text = stringResource(R.string.result_eur_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider()
            val netProfit = result.netProfitEur ?: result.netProfitBase
            val profitColor =
                if (netProfit >= 0.0) {
                    if (isSystemInDarkTheme()) ProfitDark else ProfitLight
                } else {
                    MaterialTheme.colorScheme.error
                }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.result_net_profit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${formatAmount(netProfit)} $profitCode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = profitColor,
                )
            }
            // Keep the base-currency figure visible when it differs from the EUR total.
            if (toEur != null && direction.base != Currency.EUR) {
                Text(
                    text = stringResource(
                        R.string.result_net_profit_base,
                        formatAmount(result.netProfitBase),
                        baseCode,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // The red figure above says how much is being lost; this says what to do
            // about it, which is the number needed to re-quote the customer on the spot.
            if (netProfit < 0.0) {
                NegativeProfitWarning(result.breakEvenPctFee)
            }

            if (lastCalculatedAt > 0L) {
                Text(
                    text = stringResource(
                        R.string.result_last_calculated,
                        TIME_FORMAT.format(Date(lastCalculatedAt)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NegativeProfitWarning(breakEvenPctFee: Double?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null)
            Text(
                text = if (breakEvenPctFee != null && breakEvenPctFee.isFinite()) {
                    stringResource(R.string.result_break_even, formatPercent(breakEvenPctFee))
                } else {
                    stringResource(R.string.result_negative_profit)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ResultRow(label: String, value: Double, currencyCode: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${formatAmount(value)} $currencyCode",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.US)
