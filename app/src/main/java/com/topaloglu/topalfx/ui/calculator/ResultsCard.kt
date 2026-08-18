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

            if (mode != CalcMode.CUSTOM_DEAL) {
                HorizontalDivider()
                ResultRow(stringResource(R.string.result_flat_fee), result.flatFee, baseCode)
                ResultRow(stringResource(R.string.result_pct_fee), result.pctFeeBase, baseCode)
                ResultRow(stringResource(R.string.result_delivery_fee_base), result.deliveryFeeBase, baseCode)
            }

            HorizontalDivider()
            ResultRow(stringResource(R.string.result_hidden_spread), result.hiddenSpread, baseCode)
            ResultRow(stringResource(R.string.result_agent_flat), result.agentCostFlat, baseCode)
            ResultRow(stringResource(R.string.result_agent_pct), result.agentCostPctBase.toDouble(), baseCode)

            HorizontalDivider()
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
                    text = "${formatAmount(result.netProfitBase)} $baseCode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (result.netProfitBase >= 0.0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
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
