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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.auth.domain.LogTag
import me.proton.core.auth.domain.repository.DeviceSecretRepository
import me.proton.core.auth.domain.usecase.SetupPrimaryKeys
import me.proton.core.auth.domain.usecase.sso.VerifyUnprivatization
import me.proton.core.auth.presentation.compose.DeviceSecretRoutes.Arg.getUserId
import me.proton.core.auth.presentation.compose.sso.BackupPasswordSetupAction.Load
import me.proton.core.auth.presentation.compose.sso.BackupPasswordSetupAction.SetPassword
import me.proton.core.auth.presentation.compose.sso.BackupPasswordSetupState.Error
import me.proton.core.auth.presentation.compose.sso.BackupPasswordSetupState.FormError
import me.proton.core.auth.presentation.compose.sso.BackupPasswordSetupState.Idle
import me.proton.core.auth.presentation.compose.sso.BackupPasswordSetupState.Loading
import me.proton.core.auth.presentation.compose.sso.BackupPasswordSetupState.Success
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.encrypt
import me.proton.core.crypto.common.pgp.Armored
import me.proton.core.domain.entity.UserId
import me.proton.core.observability.domain.ObservabilityContext
import me.proton.core.observability.domain.ObservabilityManager
import me.proton.core.observability.domain.metrics.LoginSsoGetUnprivatizationTotal
import me.proton.core.observability.domain.metrics.LoginSsoLoadOrganizationTotal
import me.proton.core.observability.domain.metrics.LoginSsoSetupPrimaryKeysTotal
import me.proton.core.observability.domain.metrics.LoginSsoVerifyUnprivatizationTotal
import me.proton.core.observability.domain.metrics.LoginSsoVerifyUnprivatizationTotal.VerifyStatus
import me.proton.core.observability.domain.metrics.LoginSsoVerifyUnprivatizationTotal.VerifyStatus.failurePublicAddressKeysError
import me.proton.core.observability.domain.metrics.LoginSsoVerifyUnprivatizationTotal.VerifyStatus.failureUnprivatizeStateError
import me.proton.core.observability.domain.metrics.LoginSsoVerifyUnprivatizationTotal.VerifyStatus.failureVerificationError
import me.proton.core.presentation.utils.InputValidationResult
import me.proton.core.presentation.utils.InvalidPasswordProvider
import me.proton.core.presentation.utils.ValidationType
import me.proton.core.presentation.utils.onFailure
import me.proton.core.presentation.utils.onSuccess
import me.proton.core.usersettings.domain.repository.OrganizationRepository
import me.proton.core.util.kotlin.catchAll
import me.proton.core.util.kotlin.coroutine.flowWithResultContext
import javax.inject.Inject

@HiltViewModel
public class BackupPasswordSetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val context: CryptoContext,
    private val invalidPasswordProvider: InvalidPasswordProvider,
    private val deviceSecretRepository: DeviceSecretRepository,
    private val verifyUnprivatization: VerifyUnprivatization,
    private val setupPrimaryKeys: SetupPrimaryKeys,
    private val organizationRepository: OrganizationRepository,
    override val observabilityManager: ObservabilityManager,
) : ViewModel(), ObservabilityContext {

    init {
        viewModelScope.launch { invalidPasswordProvider.init() }
    }

    private val userId: UserId by lazy { savedStateHandle.getUserId() }

    private val mutableAction = MutableStateFlow<BackupPasswordSetupAction>(Load())

    public val state: StateFlow<BackupPasswordSetupState> = mutableAction.flatMapLatest { action ->
        when (action) {
            is Load -> onLoad()
            is SetPassword -> onSetPassword(action)
        }
    }.stateIn(viewModelScope, WhileSubscribed(stopTimeoutMillis), Idle(BackupPasswordSetupData()))

    public fun submit(action: BackupPasswordSetupAction): Job = viewModelScope.launch {
        mutableAction.emit(action)
    }

    private fun onLoad() = flowWithResultContext {
        onCompleteEnqueueObservability { LoginSsoLoadOrganizationTotal(this) }

        var data = state.value.data
        emit(Loading(data))
        val organization = organizationRepository.getOrganization(userId)
        data = data.copy(organizationName = organization.displayName)
        emit(Loading(data))

        val settings = organizationRepository.getOrganizationSettings(userId)
        val logo = settings.logoId?.let { id -> organizationRepository.getOrganizationLogo(userId, id) }
        data = data.copy(organizationIcon = logo)
        emit(Idle(data))

        emitAll(onVerifyUnprivatization())
    }.catchAll(LogTag.ORGANIZATION_LOAD) {
        emit(Error(state.value.data, it.message))
    }

    private fun onVerifyUnprivatization() = flowWithResultContext {
        onResultEnqueueObservability("getUnprivatizationInfo") { LoginSsoGetUnprivatizationTotal(this) }

        emit(Loading(state.value.data))
        when (val result = verifyUnprivatization.invoke(userId)) {
            is VerifyUnprivatization.Result.UnprivatizeUserSuccess -> {
                enqueueObservability(LoginSsoVerifyUnprivatizationTotal(VerifyStatus.success))
                val data = state.value.data.copy(
                    organizationAdminEmail = result.adminEmail,
                    organizationPublicKey = result.organizationPublicKey
                )
                emit(Idle(data))
            }

            is VerifyUnprivatization.Result.PublicAddressKeysError -> {
                enqueueObservability(LoginSsoVerifyUnprivatizationTotal(failurePublicAddressKeysError))
                emit(Error(state.value.data, null))
            }
            is VerifyUnprivatization.Result.UnprivatizeStateError -> {
                enqueueObservability(LoginSsoVerifyUnprivatizationTotal(failureUnprivatizeStateError))
                emit(Error(state.value.data, null))
            }
            is VerifyUnprivatization.Result.VerificationError -> {
                enqueueObservability(LoginSsoVerifyUnprivatizationTotal(failureVerificationError))
                emit(Error(state.value.data, null))
            }
        }
    }.catchAll(LogTag.UNPRIVATIZE_USER) {
        emit(Error(state.value.data, it.message))
    }

    private fun onSetPassword(action: SetPassword) = flow {
        emit(Loading(state.value.data))
        InputValidationResult(
            text = action.backupPassword,
            validationType = ValidationType.PasswordMinLength
        ).onFailure {
            emit(FormError(state.value.data, PasswordFormError.PasswordTooShort))
        }.onSuccess {
            InputValidationResult(
                text = action.backupPassword,
                validationType = ValidationType.InvalidPassword,
                provider = invalidPasswordProvider
            ).onFailure {
                emit(FormError(state.value.data, PasswordFormError.PasswordTooCommon))
            }.onSuccess {
                InputValidationResult(
                    text = action.backupPassword,
                    validationType = ValidationType.PasswordMatch,
                    additionalText = action.repeatBackupPassword
                ).onFailure {
                    emit(FormError(state.value.data, PasswordFormError.PasswordsDoNotMatch))
                }.onSuccess {
                    when (val organizationPublicKey = state.value.data.organizationPublicKey) {
                        null -> emit(Error(state.value.data, null))
                        else -> emitAll(onSetupPrimaryKeys(action.backupPassword, organizationPublicKey))
                    }
                }
            }
        }
    }

    private fun onSetupPrimaryKeys(
        backupPassword: String,
        organizationPublicKey: Armored
    ) = flowWithResultContext {
        onCompleteEnqueueObservability { LoginSsoSetupPrimaryKeysTotal(this) }

        emit(Loading(state.value.data))
        val password = backupPassword.encrypt(context.keyStoreCrypto)
        val deviceSecret = requireNotNull(deviceSecretRepository.getByUserId(userId)?.secret)
        setupPrimaryKeys.invoke(
            userId = userId,
            password = password,
            accountType = AccountType.External,
            internalDomain = null,
            organizationPublicKey = organizationPublicKey,
            deviceSecret = deviceSecret
        )
        emit(Success(state.value.data))
    }.catchAll(LogTag.SETUP_KEYS) {
        emit(Error(state.value.data, it.message))
    }
}
