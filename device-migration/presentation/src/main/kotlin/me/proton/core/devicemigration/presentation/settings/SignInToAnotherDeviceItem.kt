/*
 * Copyright (c) 2025 Proton AG
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

package me.proton.core.devicemigration.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.core.compose.activity.rememberLauncher
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.viewmodel.hiltViewModelOrNull
import me.proton.core.devicemigration.presentation.DeviceMigrationInput
import me.proton.core.devicemigration.presentation.DeviceMigrationOutput
import me.proton.core.devicemigration.presentation.R
import me.proton.core.devicemigration.presentation.StartDeviceMigration
import me.proton.core.domain.entity.UserId

public fun interface SignInToAnotherDeviceContent {
    @Composable
    public operator fun invoke(label: String, onClick: () -> Unit)
}

/**
 * @param content The composable content that will be displayed, if Easy Device Migration is available for the user.
 * @param onResult The result of the QR code login.
 */
@Composable
public fun SignInToAnotherDeviceItem(
    content: SignInToAnotherDeviceContent,
    onResult: (DeviceMigrationOutput?) -> Unit = {},
    viewModel: SignInToAnotherDeviceViewModel? = hiltViewModelOrNull()
) {
    val state by viewModel?.state?.collectAsStateWithLifecycle() ?: return
    SignInToAnotherDeviceItem(
        content = content,
        onResult = onResult,
        state = state,
    )
}

@Composable
public fun SignInToAnotherDeviceItem(
    content: SignInToAnotherDeviceContent,
    onResult: (DeviceMigrationOutput?) -> Unit = {},
    state: SignInToAnotherDeviceState,
) {
    val launcher = rememberLauncher(StartDeviceMigration(), onResult = onResult)

    when (state) {
        is SignInToAnotherDeviceState.Hidden -> Unit
        is SignInToAnotherDeviceState.Visible -> {
            content(
                label = stringResource(R.string.intro_origin_sign_in_title),
                onClick = { launcher.launch(DeviceMigrationInput(state.userId)) }
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun SignInToAnotherDeviceItemPreview() {
    ProtonTheme {
        SignInToAnotherDeviceItem(
            content = { label, onClick -> ProtonSettingsItem(name = label, onClick = onClick) },
            state = SignInToAnotherDeviceState.Visible(UserId("user-id"))
        )
    }
}
