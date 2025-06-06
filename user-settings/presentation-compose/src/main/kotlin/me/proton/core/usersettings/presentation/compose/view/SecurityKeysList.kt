/*
 * Copyright (c) 2024 Proton Technologies AG
 * This file is part of Proton AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.usersettings.presentation.compose.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.core.auth.fido.domain.entity.Fido2RegisteredKey
import me.proton.core.compose.component.DeferredCircularProgressIndicator
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.viewmodel.hiltViewModelOrNull
import me.proton.core.usersettings.presentation.compose.R
import me.proton.core.usersettings.presentation.compose.viewmodel.SecurityKeysInfoViewModel
import me.proton.core.usersettings.presentation.compose.viewmodel.SecurityKeysState

@Composable
fun SecurityKeysList(
    modifier: Modifier = Modifier,
    onManageSecurityKeysClicked: () -> Unit,
    onAddSecurityKeyClicked: () -> Unit,
    viewModel: SecurityKeysInfoViewModel? = hiltViewModelOrNull(),
) {
    if (viewModel == null) return
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecurityKeysList(
        modifier = modifier,
        onManageSecurityKeysClicked = onManageSecurityKeysClicked,
        onAddSecurityKeyClicked = onAddSecurityKeyClicked,
        state = state
    )
}

@Composable
fun SecurityKeysList(
    modifier: Modifier = Modifier,
    onManageSecurityKeysClicked: () -> Unit,
    onAddSecurityKeyClicked: () -> Unit,
    state: SecurityKeysState
) {
    when (state) {
        is SecurityKeysState.Loading -> SecurityKeysInfoProcessing()
        is SecurityKeysState.Error -> SecurityKeyError(state.throwable?.message)
        is SecurityKeysState.Success -> SecurityKeysList(
            modifier = modifier,
            onManageSecurityKeysClicked = onManageSecurityKeysClicked,
            onAddSecurityKeyClicked = onAddSecurityKeyClicked,
            keys = state.keys
        )
    }
}

@Composable
internal fun SecurityKeysList(
    modifier: Modifier = Modifier,
    onManageSecurityKeysClicked: () -> Unit,
    onAddSecurityKeyClicked: () -> Unit,
    keys: List<Fido2RegisteredKey>
) {
    LazyColumn(
        modifier = modifier
            .padding(
                horizontal = ProtonDimens.DefaultSpacing,
                vertical = ProtonDimens.DefaultSpacing
            )
            .fillMaxWidth()
    ) {
        item {
            if (keys.isNotEmpty()) {
                SecurityKeysListHeader()
            } else {
                SecurityKeysEmptyListHeader()
            }
        }
        if (keys.isNotEmpty()) {
            items(keys, { key -> key.name }) { key ->
                SecurityKeyRow(key = key)
            }
        }
        item {
            if (keys.isNotEmpty()) {
                SecurityKeysListFooter(
                    onManageSecurityKeysClicked = onManageSecurityKeysClicked
                )
            } else {
                SecurityKeysEmptyListFooter(
                    onAddSecurityKeyClicked = onAddSecurityKeyClicked
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SecurityKeyRow(
    modifier: Modifier = Modifier,
    key: Fido2RegisteredKey
) {
    ListItem(
        text = {
            Text(text = key.name)
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_proton_security_key),
                contentDescription = null,
                tint = ProtonTheme.colors.iconNorm
            )
        }
    )
}

@Composable
internal fun SecurityKeysInfoProcessing() {
    Surface(modifier = Modifier.fillMaxHeight()) {
        DeferredCircularProgressIndicator(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun SecurityKeyError(
    message: String? = null
) {
    Text(
        modifier = Modifier
            .padding(
                horizontal = ProtonDimens.DefaultSpacing,
                vertical = ProtonDimens.DefaultSpacing
            )
            .fillMaxWidth(),
        text = message ?: stringResource(id = R.string.settings_security_keys_error)
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
internal fun SecurityKeysListPreviewWithStateSuccess() {
    ProtonTheme {
        SecurityKeysList(
            onManageSecurityKeysClicked = {},
            onAddSecurityKeyClicked = {},
            state = SecurityKeysState.Success(
                keys = listOf(
                    Fido2RegisteredKey("format", UByteArray(10), "Test key 1"),
                    Fido2RegisteredKey("format", UByteArray(10), "Test key 2"),
                    Fido2RegisteredKey("format", UByteArray(10), "Test key 3"),
                )
            )
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
internal fun SecurityKeysListPreviewWithStateError() {
    ProtonTheme {
        SecurityKeysList(
            onManageSecurityKeysClicked = {},
            onAddSecurityKeyClicked = {},
            state = SecurityKeysState.Error(
                Error("Test error")
            )
        )
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Preview
@Composable
internal fun SecurityKeysListPreviewWithKeysData() {
    ProtonTheme {
        SecurityKeysList(
            onManageSecurityKeysClicked = {},
            onAddSecurityKeyClicked = {},
            keys = listOf(
                Fido2RegisteredKey("format", UByteArray(10), "Test key 1"),
                Fido2RegisteredKey("format", UByteArray(10), "Test key 2"),
                Fido2RegisteredKey("format", UByteArray(10), "Test key 3"),
            )
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
internal fun SecurityKeysListPreviewNoData() {
    ProtonTheme {
        SecurityKeysList(
            onManageSecurityKeysClicked = {},
            onAddSecurityKeyClicked = {},
            state = SecurityKeysState.Success(keys = emptyList())
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
internal fun PreviewLoading() {
    ProtonTheme {
        SecurityKeysInfoProcessing()
    }
}
