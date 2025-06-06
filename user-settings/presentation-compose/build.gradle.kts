/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
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

import Module.kotlinTest
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

plugins {
    protonComposeUiLibrary
    protonDagger
}

protonBuild {
    apiModeDisabled()
}

protonCoverage {
    branchCoveragePercentage.set(21)
    lineCoveragePercentage.set(49)
}

publishOption.shouldBePublishedAsLib = true

android {
    namespace = "me.proton.core.usersettings.presentation.compose"
}

dependencies {
    api(
        project(Module.presentationCompose),
        project(Module.userSettingsDomain),

        `compose-foundation`,
        `compose-foundation-layout`,
        `compose-material`,
        `compose-runtime`,
        `compose-ui`,
        `compose-ui-graphics`,
        `compose-ui-text`,
        `coroutines-core`,
        `hilt-android`,
        `hilt-navigation-compose`,
        `lifecycle-common`,
        `lifecycle-viewModel`,
    )

    implementation(
        project(Module.presentation),

        `android-ktx`,
        `appcompat`,
        `compose-animation-core`,
        `compose-material-icons-core`,
        `compose-material3`,
        `compose-ui-tooling-preview`,
        `compose-ui-unit`,
        `lifecycle-runtime`,
    )

    debugImplementation(
        `compose-ui-tooling`,
    )

    androidTestImplementation(
        `android-test-runner`,
        `compose-ui-test`,
        `compose-ui-test-junit`,
        `compose-ui-test-manifest`,
        `junit`,
        `junit-ktx`,
        `kotlin-test`
    )

    testImplementation(
        project(kotlinTest),
        `coroutines-test`,
        `kotlin-test`,
        mockk,
        turbine,
    )
}
