/*
 * Copyright (c) 2023 Proton AG
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

import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

plugins {
    protonAndroidLibrary
    id("kotlin-parcelize")
}

protonBuild {
    apiModeDisabled()
}

protonCoverage {
    branchCoveragePercentage.set(8)
    lineCoveragePercentage.set(11)
}

publishOption.shouldBePublishedAsLib = true

android {
    namespace = "me.proton.core.crypto.android"
}

dependencies {
    api(
        project(Module.cryptoCommon),
    )

    implementation(
        project(Module.kotlinUtil),
        `android-ktx`,
        `room-runtime`,
    )

    compileOnly(project(Module.gopenpgp))

    testImplementation(
        junit,
        `kotlin-test`,
        mockk
    )

    androidTestImplementation(project(Module.androidTest)) {
        exclude(mockk) // We're including `mock-android` instead.
    }

    androidTestImplementation(
        project(Module.androidInstrumentedTest),
        project(Module.androidTest),
        project(Module.kotlinTest),
        project(Module.gopenpgp),
        `kotlin-test`,
        `mockk-android`,
    )
}

dependencyAnalysis {
    issues {
        onUnusedDependencies {
            exclude(Module.gopenpgp)
        }
    }
}
