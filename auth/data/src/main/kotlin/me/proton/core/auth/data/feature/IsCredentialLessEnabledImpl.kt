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

package me.proton.core.auth.data.feature

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import me.proton.core.auth.data.R
import me.proton.core.auth.domain.feature.IsCredentialLessEnabled
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.ExperimentalProtonFeatureFlag
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.entity.FeatureId
import me.proton.core.featureflag.domain.entity.Scope
import javax.inject.Inject
import kotlin.time.Duration

class IsCredentialLessEnabledImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val featureFlagManager: FeatureFlagManager,
) : IsCredentialLessEnabled {

    override suspend fun invoke(userId: UserId?): Boolean {
        return isLocalEnabled() && !awaitIsRemoteDisabled(userId = userId)
    }

    override fun isLocalEnabled(): Boolean {
        return context.resources.getBoolean(R.bool.core_feature_credential_less_enabled)
    }

    @OptIn(ExperimentalProtonFeatureFlag::class)
    override suspend fun awaitIsRemoteDisabled(
        userId: UserId?,
        timeout: Duration
    ): Boolean = withTimeoutOrNull(timeout) {
        featureFlagManager.awaitNotEmptyScope(userId, Scope.Unleash)
    }.run {
        featureFlagManager.getValue(userId, featureId)
    }

    internal companion object {
        val featureId = FeatureId("CredentialLessDisabled")
    }
}
