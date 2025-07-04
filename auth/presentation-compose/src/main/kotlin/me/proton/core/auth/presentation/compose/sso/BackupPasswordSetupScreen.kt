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

package me.proton.core.auth.presentation.compose.sso

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import me.proton.core.auth.presentation.compose.R
import me.proton.core.auth.presentation.compose.sso.PasswordFormError.PasswordTooCommon
import me.proton.core.auth.presentation.compose.sso.PasswordFormError.PasswordTooShort
import me.proton.core.auth.presentation.compose.sso.PasswordFormError.PasswordsDoNotMatch
import me.proton.core.compose.component.ProtonPasswordOutlinedTextFieldWithError
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextFieldError
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.LocalColors
import me.proton.core.compose.theme.LocalShapes
import me.proton.core.compose.theme.LocalTypography
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.util.formatBold
import me.proton.core.domain.entity.UserId
import me.proton.core.passvalidator.presentation.report.PasswordPolicyReport

private val CompanyLogoSize = 56.dp
private val MaxFormWidth = 600.dp

@Composable
public fun BackupPasswordSetupScreen(
    onCloseClicked: () -> Unit,
    onErrorMessage: (String?) -> Unit,
    onSuccess: () -> Unit,
    userId: UserId,
    modifier: Modifier = Modifier,
    viewModel: BackupPasswordSetupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackupPasswordSetupScreen(
        modifier = modifier,
        onCloseClicked = onCloseClicked,
        onContinueClicked = { viewModel.submit(it) },
        onErrorMessage = onErrorMessage,
        onSuccess = onSuccess,
        state = state,
        userId = userId
    )
}

@Composable
public fun BackupPasswordSetupScreen(
    modifier: Modifier = Modifier,
    onCloseClicked: () -> Unit = {},
    onContinueClicked: (BackupPasswordSetupAction.SetPassword) -> Unit = {},
    onErrorMessage: (String?) -> Unit = {},
    onSuccess: () -> Unit = {},
    state: BackupPasswordSetupState,
    userId: UserId
) {
    LaunchedEffect(state) {
        when (state) {
            is BackupPasswordSetupState.Error -> onErrorMessage(state.message)
            is BackupPasswordSetupState.Success -> onSuccess()
            else -> Unit
        }
    }
    BackupPasswordSetupScaffold(
        modifier = modifier,
        onCloseClicked = onCloseClicked,
        onContinueClicked = onContinueClicked,
        organizationAdminEmail = state.data.organizationAdminEmail,
        organizationIcon = state.data.organizationIcon,
        organizationName = state.data.organizationName,
        formError = (state as? BackupPasswordSetupState.FormError)?.cause,
        isLoading = state is BackupPasswordSetupState.Loading,
        userId = userId
    )
}

@Composable
public fun BackupPasswordSetupScaffold(
    modifier: Modifier = Modifier,
    onCloseClicked: () -> Unit = {},
    onContinueClicked: (BackupPasswordSetupAction.SetPassword) -> Unit = {},
    organizationAdminEmail: String? = null,
    organizationIcon: Any? = null,
    organizationName: String? = null,
    formError: PasswordFormError? = null,
    isLoading: Boolean = false,
    userId: UserId
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onCloseClicked) {
                        Icon(
                            painterResource(id = R.drawable.ic_proton_close),
                            contentDescription = stringResource(id = R.string.presentation_close)
                        )
                    }
                },
                backgroundColor = LocalColors.current.backgroundNorm
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(ProtonDimens.DefaultSpacing),
            ) {
                OrganizationAdminInfoHeader(
                    organizationAdminEmail = organizationAdminEmail,
                    organizationIcon = organizationIcon,
                    organizationName = organizationName
                )
                Divider(
                    modifier = Modifier.padding(top = ProtonDimens.MediumSpacing),
                    color = LocalColors.current.separatorNorm
                )

                val error = when (formError) {
                    null -> null
                    PasswordTooShort -> stringResource(R.string.backup_password_setup_password_too_short)
                    PasswordTooCommon -> stringResource(R.string.backup_password_setup_password_too_common)
                    PasswordsDoNotMatch -> stringResource(R.string.backup_password_setup_password_not_matching)
                }

                BackupPasswordSetupForm(
                    backupPasswordError = error,
                    backupPasswordRepeatedError = error?.takeIf { formError is PasswordsDoNotMatch },
                    onContinueClicked = onContinueClicked,
                    isLoading = isLoading,
                    userId = userId,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = ProtonDimens.MediumSpacing)
                )
            }
        }
    }
}

@Composable
private fun OrganizationAdminInfoHeader(
    organizationAdminEmail: String?,
    organizationIcon: Any?,
    organizationName: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(LocalShapes.current.large)
        ) {
            val defaultLogo = painterResource(R.drawable.default_org_logo)
            AsyncImage(
                model = organizationIcon,
                error = defaultLogo,
                fallback = defaultLogo,
                placeholder = defaultLogo,
                modifier = Modifier.size(CompanyLogoSize),
                contentDescription = null,
            )
        }

        if (organizationName != null) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = ProtonDimens.MediumSpacing),
                style = LocalTypography.current.headline,
                text = stringResource(id = R.string.backup_password_setup_title, organizationName)
            )
        }

        if (organizationAdminEmail != null) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = ProtonDimens.SmallSpacing),
                style = LocalTypography.current.body2Medium,
                textAlign = TextAlign.Center,
                text = stringResource(R.string.backup_password_setup_subtitle).formatBold(
                    organizationAdminEmail
                )
            )
        }
    }
}

@Composable
private fun BackupPasswordSetupForm(
    backupPasswordError: String?,
    backupPasswordRepeatedError: String?,
    isLoading: Boolean,
    onContinueClicked: (BackupPasswordSetupAction.SetPassword) -> Unit,
    userId: UserId,
    modifier: Modifier = Modifier,
) {
    var backupPassword by rememberSaveable { mutableStateOf("") }
    var repeatBackupPassword by rememberSaveable { mutableStateOf("") }
    var isPasswordValid by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.widthIn(max = MaxFormWidth)
    ) {
        Text(
            color = LocalColors.current.textWeak,
            style = LocalTypography.current.body2Regular,
            text = stringResource(id = R.string.backup_password_setup_description)
        )
        ProtonPasswordOutlinedTextFieldWithError(
            text = backupPassword,
            onValueChanged = { backupPassword = it },
            enabled = !isLoading,
            singleLine = true,
            label = { Text(text = stringResource(id = R.string.backup_password_setup_password_label)) },
            errorText = backupPasswordError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.padding(top = ProtonDimens.MediumSpacing),
            errorContent = { errorMsg ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = ProtonDimens.ExtraSmallSpacing),
                ) {
                    if (errorMsg != null) {
                        ProtonTextFieldError(errorText = errorMsg)
                    }
                    PasswordPolicyReport(
                        password = backupPassword,
                        userId = userId,
                        onResult = { isPasswordValid = it != null },
                        modifier = Modifier.padding(top = ProtonDimens.ExtraSmallSpacing)
                    )
                }
            }
        )
        ProtonPasswordOutlinedTextFieldWithError(
            text = repeatBackupPassword,
            onValueChanged = { repeatBackupPassword = it },
            enabled = !isLoading,
            singleLine = true,
            label = { Text(text = stringResource(id = R.string.backup_password_setup_repeat_password_label)) },
            errorText = backupPasswordRepeatedError,
            modifier = Modifier.padding(top = ProtonDimens.SmallSpacing)
        )
        ProtonSolidButton(
            contained = false,
            enabled = isPasswordValid,
            loading = isLoading,
            modifier = Modifier
                .padding(top = ProtonDimens.MediumSpacing)
                .height(ProtonDimens.DefaultButtonMinHeight),
            onClick = { onContinueClicked(BackupPasswordSetupAction.SetPassword(backupPassword, repeatBackupPassword)) }
        ) {
            Text(
                text = stringResource(id = R.string.backup_password_setup_continue_action)
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BackupPasswordSetupScreenPreview() {
    ProtonTheme {
        BackupPasswordSetupScreen(
            state = BackupPasswordSetupState.Idle(
                data = BackupPasswordSetupData(
                    organizationAdminEmail = "admin@company.test",
                    organizationName = "The Company",
                )
            ),
            userId = UserId("user-id")
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BackupPasswordSetupScreenFormErrorPreview() {
    ProtonTheme {
        BackupPasswordSetupScreen(
            state = BackupPasswordSetupState.FormError(
                data = BackupPasswordSetupData(
                    organizationAdminEmail = "admin@company.test",
                    organizationName = "The Company",
                ),
                cause = PasswordsDoNotMatch
            ),
            userId = UserId("user-id")
        )
    }
}
