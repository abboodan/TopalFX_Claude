package com.topaloglu.topalfx.ui.updater

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.topaloglu.topalfx.R
import com.topaloglu.topalfx.viewmodel.UpdateState
import com.topaloglu.topalfx.viewmodel.UpdateViewModel

@Composable
fun UpdateDialog(viewModel: UpdateViewModel) {
    val state by viewModel.state.collectAsState()
    if (state is UpdateState.Idle) return

    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = {
            Text(
                text = when (state) {
                    is UpdateState.Checking -> stringResource(R.string.update_checking)
                    is UpdateState.UpToDate -> stringResource(R.string.update_up_to_date)
                    is UpdateState.Failed -> stringResource(R.string.update_failed)
                    else -> stringResource(R.string.update_available_title)
                }
            )
        },
        text = {
            when (val current = state) {
                is UpdateState.Available -> Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.update_current_version, viewModel.currentVersion))
                    Text(stringResource(R.string.update_new_version, current.tag))
                    if (current.changelog.isNotBlank()) {
                        Text(
                            text = current.changelog,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is UpdateState.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.update_downloading,
                            (current.progress * 100).toInt(),
                        )
                    )
                    LinearProgressIndicator(
                        progress = { current.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is UpdateState.ReadyToInstall -> Text(stringResource(R.string.update_install_note))
                else -> Text(stringResource(R.string.update_current_version, viewModel.currentVersion))
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Available -> TextButton(onClick = viewModel::download) {
                    Text(stringResource(R.string.update_download))
                }
                is UpdateState.ReadyToInstall -> TextButton(onClick = viewModel::install) {
                    Text(stringResource(R.string.update_install))
                }
                is UpdateState.Failed -> TextButton(onClick = { viewModel.checkForUpdate() }) {
                    Text(stringResource(R.string.update_retry))
                }
                else -> {}
            }
        },
        dismissButton = {
            if (state !is UpdateState.Downloading) {
                TextButton(onClick = viewModel::dismiss) {
                    Text(stringResource(R.string.action_close))
                }
            }
        },
    )
}
