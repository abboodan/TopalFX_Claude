package com.topaloglu.topalfx.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.data.CalcMode
import com.topaloglu.topalfx.data.CalcResult
import com.topaloglu.topalfx.data.Currency
import com.topaloglu.topalfx.data.TransferDirection

@Composable
fun ResultsCard(
    result: CalcResult,
    direction: TransferDirection,
    mode: CalcMode,
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

            ResultRow(stringResource(R.string.result_principal), result.principal, baseCode)
            ResultRow(stringResource(R.string.result_target_delivered), result.targetDelivered, targetCode)
            ResultRow(stringResource(R.string.result_total_paid), result.totalPaidByCustomer, baseCode)
            ResultRow(stringResource(R.string.result_total_received), result.totalReceivedByCustomer, targetCode)

            HorizontalDivider()
            Text(
                text = stringResource(R.string.result_profit_section, profitCode),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (mode != CalcMode.CUSTOM_DEAL) {
                ResultRow(stringResource(R.string.result_flat_fee), profit(result.flatFee), profitCode)
                ResultRow(stringResource(R.string.result_pct_fee), profit(result.pctFeeBase), profitCode)
                ResultRow(
                    stringResource(R.string.result_delivery_fee_base),
                    profit(result.deliveryFeeBase),
                    profitCode,
                )
            }
            ResultRow(stringResource(R.string.result_hidden_spread), profit(result.hiddenSpread), profitCode)
            ResultRow(stringResource(R.string.result_agent_flat), profit(result.agentCostFlat), profitCode)
            ResultRow(
                stringResource(R.string.result_agent_pct),
                profit(result.agentCostPctBase.toDouble()),
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
                    color = if (netProfit >= 0.0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
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
