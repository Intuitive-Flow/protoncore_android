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
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.serialization")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

libVersion = parent?.libVersion

android()

dependencies {

    implementation(
        project(Module.kotlinUtil),
        project(Module.data),
        project(Module.dataRoom),
        project(Module.domain),
        project(Module.network),
        project(Module.eventManagerDomain),
        project(Module.presentation),

        project(Module.account),
        project(Module.accountManager),
        project(Module.user), // UserEntity

        `android-work-runtime`,
        `hilt-android`,
        `hilt-androidx-workManager`,
        `kotlin-jdk7`,
        `serialization-json`,
        `coroutines-core`,
        `retrofit`,
        `retrofit-kotlin-serialization`,
        `room-ktx`
    )

    kapt(
        `hilt-android-compiler`,
        `hilt-androidx-compiler`
    )

    testImplementation(
        project(Module.androidTest),
        project(Module.crypto),
        project(Module.account),
        project(Module.accountManager),
        project(Module.contact),
        project(Module.key),
    )
    androidTestImplementation(project(Module.androidInstrumentedTest))
}