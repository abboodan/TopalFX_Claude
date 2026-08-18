package com.topaloglu.topalfx.ui.ticker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.data.RatePair

@Composable
fun PairEditorDialog(
    pairs: List<RatePair>,
    onSave: (List<RatePair>) -> Unit,
    onDismiss: () -> Unit,
) {
    val edited = remember { pairs.toMutableStateList() }
    var newPairText by remember { mutableStateOf("") }
    var invalidInput by remember { mutableStateOf(false) }

    fun addPair() {
        val parsed = RatePair.parse(newPairText)
        if (parsed == null) {
            invalidInput = true
        } else {
            if (parsed !in edited) edited.add(parsed)
            newPairText = ""
            invalidInput = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ticker_edit_pairs)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                edited.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = pair.label,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { edited.remove(pair) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newPairText,
                        onValueChange = {
                            newPairText = it
                            invalidInput = false
                        },
                        label = { Text(stringResource(R.string.ticker_pair_hint)) },
                        singleLine = true,
                        isError = invalidInput,
                        supportingText = if (invalidInput) {
                            { Text(stringResource(R.string.ticker_invalid_pair)) }
                        } else null,
                        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = ::addPair) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.ticker_add_pair),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(edited.toList()) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
