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

import configuration.extensions.protonEnvironment
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    protonAndroidTest
    protonDagger
    id("me.proton.core.gradle-plugins.environment-config")
}

protonDagger {
    workManagerHiltIntegration = true
}

publishOption.shouldBePublishedAsLib = false

android {
    namespace = "me.proton.android.core.coreexample.hilttests"

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    defaultConfig {
        targetProjectPath = ":coreexample"
        testInstrumentationRunner = "me.proton.core.test.android.ProtonHiltTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    flavorDimensions.add("env")
    productFlavors {
        register("dev") {
            dimension = "env"
            protonEnvironment {
                host = "proton.black"
            }
        }
        register("localProperties") {
            dimension = "env"
            protonEnvironment {
                useProxy = true
                host = getLocalProperties().getProperty("HOST") ?: "proton.black"
            }
        }
        register("mock") { dimension = "env" }
        register("prod") { dimension = "env" }
    }

    testOptions {
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

dependencies {
    androidTestUtil(`androidx-test-orchestrator`)

    coreLibraryDesugaring(`desugar-jdk-libs`)

    implementation(
        `android-work-runtime`,
        `android-work-testing`,
        fusion,
        `hilt-android-testing`,
        `kotlin-test`,
        `kotlin-test-junit`,
        `mockk-android`,
        mockWebServer,

        project(Module.accountRecoveryTest),
        project(Module.androidInstrumentedTest),
        project(Module.quark),

        project(Module.account),
        project(Module.accountManager),
        project(Module.accountRecovery),
        project(Module.androidUtilDagger),
        project(Module.auth),
        project(Module.authTest),
        project(Module.challenge),
        project(Module.contact),
        project(Module.country),
        project(Module.crypto),
        project(Module.cryptoValidator),
        project(Module.eventManager),
        project(Module.featureFlag),
        project(Module.gopenpgp),
        project(Module.humanVerification),
        project(Module.kotlinUtil),
        project(Module.key),
        project(Module.label),
        project(Module.mailMessage),
        project(Module.mailSettings),
        project(Module.network),
        project(Module.notification),
        project(Module.observabilityDagger),
        project(Module.payment),
        project(Module.sentryUtil),
        project(Module.paymentIap),
        project(Module.plan),
        project(Module.plan),
        project(Module.push),
        project(Module.report),
        project(Module.telemetry),
        project(Module.user),
        project(Module.userRecovery),
        project(Module.userSettings),
        project(Module.keyTransparency),
        project(Module.configurationData),
        project(Module.testRule),
        project(Module.deviceMigration),
        project(Module.biometric),
        project(Module.passValidator)
    )
}

fun getLocalProperties(): Properties {
    return Properties().apply {
        try {
            load(rootDir.resolve("local.properties").inputStream())
        } catch (e: FileNotFoundException) {
            logger.warn("No local.properties found")
        }
    }
}
