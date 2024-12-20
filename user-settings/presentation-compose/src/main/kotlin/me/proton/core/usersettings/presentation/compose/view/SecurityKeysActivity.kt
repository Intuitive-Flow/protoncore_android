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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import me.proton.core.compose.theme.AppTheme
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.presentation.ui.ProtonActivity
import me.proton.core.presentation.utils.openBrowserLink
import me.proton.core.usersettings.presentation.compose.R
import javax.inject.Inject

@AndroidEntryPoint
class SecurityKeysActivity : ProtonActivity() {

    @Inject
    lateinit var appTheme: AppTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            appTheme {
                SecurityKeysScreen(
                    onAddSecurityKeyClicked = {
                        openBrowserLink(getString(R.string.add_security_key_link))
                    },
                    onManageSecurityKeysClicked = {
                        openBrowserLink(getString(R.string.manage_security_keys_link))
                    },
                    onBackClick = { finish() }
                )
            }
        }
    }

    companion object {

        fun start(context: Context) =
            context.startActivity(getIntent(context))

        fun getIntent(context: Context): Intent = Intent(context, SecurityKeysActivity::class.java)
    }
}