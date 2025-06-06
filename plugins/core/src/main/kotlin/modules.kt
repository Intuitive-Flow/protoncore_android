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

@Suppress("MemberVisibilityCanBePrivate", "unused")
public object Module {

    // region Common
    // Utils
    private const val util = ":util"
    public const val kotlinUtil: String = "$util:util-kotlin"
    private const val androidUtil = "$util:android"
    public const val androidUtilDagger: String = "$androidUtil:util-android-dagger"
    public const val androidUtilDatetime: String = "$androidUtil:util-android-datetime"
    public const val sharedPreferencesUtil: String = "$androidUtil:util-android-shared-preferences"
    public const val workManagersUtil: String = "$androidUtil:util-android-work-manager"
    public const val gradleUtil: String = "$util:util-gradle"
    public const val strictModeUtil: String = "$androidUtil:util-android-strict-mode"
    public const val sentryUtil: String = "$androidUtil:util-android-sentry"
    public const val deviceUtil: String = "$androidUtil:util-android-device"

    // Test
    private const val test = ":test"
    public const val kotlinTest: String = "$test:test-kotlin"
    public const val androidTest: String = "$test:test-android"
    public const val androidInstrumentedTest: String = "$androidTest:test-android-instrumented"
    public const val quark: String = "$test:test-quark"
    public const val mockProxy: String = "$test:test-mock-proxy"
    public const val testPerformance: String = "$test:test-performance"
    public const val testRule: String = "$test:test-rule"
    // endregion

    // region Shared
    public const val corePlatform: String = ":core-platform"
    public const val domain: String = ":domain"
    public const val presentation: String = ":presentation"
    public const val presentationCompose: String = ":presentation-compose"
    public const val data: String = ":data"
    public const val dataRoom: String = ":data-room"
    public const val gopenpgp: String = ":gopenpgp"
    // endregion

    // region Support
    // Network
    public const val network: String = ":network"
    public const val networkDagger: String = "$network:network-dagger"
    public const val networkDomain: String = "$network:network-domain"
    public const val networkData: String = "$network:network-data"
    public const val networkPresentation: String = "$network:network-presentation"
    // endregion

    // Notification
    public const val notification: String = ":notification"
    public const val notificationDagger: String = "$notification:notification-dagger"
    public const val notificationData: String = "$notification:notification-data"
    public const val notificationDomain: String = "$notification:notification-domain"
    public const val notificationPresentation: String = "$notification:notification-presentation"
    public const val notificationTest: String = "$notification:notification-test"

    // region Features

    // Authentication
    public const val auth: String = ":auth"
    public const val authDagger: String = "$auth:auth-dagger"
    public const val authDomain: String = "$auth:auth-domain"
    public const val authPresentation: String = "$auth:auth-presentation"
    public const val authPresentationCompose: String = "$auth:auth-presentation-compose"
    public const val authData: String = "$auth:auth-data"
    public const val authTest: String = "$auth:auth-test"

    // Authentication Fido
    public const val authFido: String = ":auth-fido"
    public const val authFidoDagger: String = "$authFido:auth-fido-dagger"
    public const val authFidoDomain: String = "$authFido:auth-fido-domain"
    public const val authFidoPlay: String = "$authFido:auth-fido-play"

    // Account
    public const val account: String = ":account"
    public const val accountDagger: String = "$account:account-dagger"
    public const val accountDomain: String = "$account:account-domain"
    public const val accountPresentation: String = "$account:account-presentation"
    public const val accountData: String = "$account:account-data"

    // AccountManager
    public const val accountManager: String = ":account-manager"
    public const val accountManagerDomain: String = "$accountManager:account-manager-domain"
    public const val accountManagerPresentation: String = "$accountManager:account-manager-presentation"
    public const val accountManagerPresentationCompose: String = "$accountManager:account-manager-presentation-compose"
    public const val accountManagerData: String = "$accountManager:account-manager-data"
    public const val accountManagerDataDb: String = "$accountManager:account-manager-data-db"
    public const val accountManagerDagger: String = "$accountManager:account-manager-dagger"

    // AccountRecovery
    public const val accountRecovery: String = ":account-recovery"
    public const val accountRecoveryDagger: String = "$accountRecovery:account-recovery-dagger"
    public const val accountRecoveryData: String = "$accountRecovery:account-recovery-data"
    public const val accountRecoveryDomain: String = "$accountRecovery:account-recovery-domain"
    public const val accountRecoveryPresentation: String = "$accountRecovery:account-recovery-presentation"
    public const val accountRecoveryPresentationCompose: String = "$accountRecovery:account-recovery-presentation-compose"
    public const val accountRecoveryTest: String = "$accountRecovery:account-recovery-test"

    // Crypto
    public const val crypto: String = ":crypto"
    public const val cryptoDagger: String = "$crypto:crypto-dagger"
    public const val cryptoCommon: String = "$crypto:crypto-common"
    public const val cryptoAndroid: String = "$crypto:crypto-android"

    // CryptoValidator
    public const val cryptoValidator: String = ":crypto-validator"
    public const val cryptoValidatorDagger: String = "$cryptoValidator:crypto-validator-dagger"
    public const val cryptoValidatorData: String = "$cryptoValidator:crypto-validator-data"
    public const val cryptoValidatorDomain: String = "$cryptoValidator:crypto-validator-domain"
    public const val cryptoValidatorPresentation: String = "$cryptoValidator:crypto-validator-presentation"

    // Account
    public const val eventManager: String = ":event-manager"
    public const val eventManagerDagger: String = "$eventManager:event-manager-dagger"
    public const val eventManagerDomain: String = "$eventManager:event-manager-domain"
    public const val eventManagerData: String = "$eventManager:event-manager-data"

    // Key
    public const val key: String = ":key"
    public const val keyDagger: String = "$key:key-dagger"
    public const val keyDomain: String = "$key:key-domain"
    public const val keyData: String = "$key:key-data"

    // Label
    public const val label: String = ":label"
    public const val labelDomain: String = "$label:label-domain"
    public const val labelData: String = "$label:label-data"
    public const val labelDagger: String = "$label:label-dagger"

    // Configuration
    public const val configuration: String = ":configuration"
    public const val configurationData: String = "$configuration:configuration-data"
    public const val configurationDaggerStatic: String = "$configuration:configuration-dagger-staticdefaults"
    public const val configurationDaggerContentResolver: String = "$configuration:configuration-dagger-content-resolver"

    // Contact
    public const val contact: String = ":contact"
    public const val contactDomain: String = "$contact:contact-domain"
    public const val contactData: String = "$contact:contact-data"
    public const val contactDagger: String = "$contact:contact-dagger"

    // User
    public const val user: String = ":user"
    public const val userDagger: String = "$user:user-dagger"
    public const val userDomain: String = "$user:user-domain"
    public const val userPresentation: String = "$user:user-presentation"
    public const val userData: String = "$user:user-data"

    // Payment
    public const val payment: String = ":payment"
    public const val paymentDagger: String = "$payment:payment-dagger"
    public const val paymentData: String = "$payment:payment-data"
    public const val paymentDomain: String = "$payment:payment-domain"
    public const val paymentPresentation: String = "$payment:payment-presentation"
    public const val paymentTest: String = "$payment:payment-test"

    // Payment-IAP
    public const val paymentIap: String = ":payment-iap"
    public const val paymentIapDagger: String = "$paymentIap:payment-iap-dagger"
    public const val paymentIapData: String = "$paymentIap:payment-iap-data"
    public const val paymentIapDomain: String = "$paymentIap:payment-iap-domain"
    public const val paymentIapPresentation: String = "$paymentIap:payment-iap-presentation"
    public const val paymentIapTest: String = "$paymentIap:payment-iap-test"

    // Countries
    public const val country: String = ":country"
    public const val countryDagger: String = "$country:country-dagger"
    public const val countryData: String = "$country:country-data"
    public const val countryDomain: String = "$country:country-domain"
    public const val countryPresentation: String = "$country:country-presentation"

    // User Recovery
    public const val userRecovery: String = ":user-recovery"
    public const val userRecoveryDagger: String = "$userRecovery:user-recovery-dagger"
    public const val userRecoveryData: String = "$userRecovery:user-recovery-data"
    public const val userRecoveryDomain: String = "$userRecovery:user-recovery-domain"
    public const val userRecoveryPresentation: String = "$userRecovery:user-recovery-presentation"
    public const val userRecoveryPresentationCompose: String = "$userRecovery:user-recovery-presentation-compose"

    // Settings
    public const val userSettings: String = ":user-settings"
    public const val userSettingsDagger: String = "$userSettings:user-settings-dagger"
    public const val userSettingsDomain: String = "$userSettings:user-settings-domain"
    public const val userSettingsPresentation: String = "$userSettings:user-settings-presentation"
    public const val userSettingsPresentationCompose: String = "$userSettings:user-settings-presentation-compose"
    public const val userSettingsData: String = "$userSettings:user-settings-data"

    // Human Verification
    public const val humanVerification: String = ":human-verification"
    public const val humanVerificationDagger: String = "$humanVerification:human-verification-dagger"
    public const val humanVerificationDomain: String = "$humanVerification:human-verification-domain"
    public const val humanVerificationPresentation: String = "$humanVerification:human-verification-presentation"
    public const val humanVerificationData: String = "$humanVerification:human-verification-data"
    public const val humanVerificationTest: String = "$humanVerification:human-verification-test"

    // Mail Message
    public const val mailMessage: String = ":mail-message"
    public const val mailMessageDagger: String = "$mailMessage:mail-message-dagger"
    public const val mailMessageDomain: String = "$mailMessage:mail-message-domain"
    public const val mailMessagePresentation: String = "$mailMessage:mail-message-presentation"
    public const val mailMessageData: String = "$mailMessage:mail-message-data"

    // Mail Settings
    public const val mailSettings: String = ":mail-settings"
    public const val mailSettingsDagger: String = "$mailSettings:mail-settings-dagger"
    public const val mailSettingsDomain: String = "$mailSettings:mail-settings-domain"
    public const val mailSettingsPresentation: String = "$mailSettings:mail-settings-presentation"
    public const val mailSettingsData: String = "$mailSettings:mail-settings-data"

    // Mail Send Preferences
    public const val mailSendPreferences: String = ":mail-send-preferences"
    public const val mailSendPreferencesDagger: String = "$mailSendPreferences:mail-send-preferences-dagger"
    public const val mailSendPreferencesDomain: String = "$mailSendPreferences:mail-send-preferences-domain"
    public const val mailSendPreferencesData: String = "$mailSendPreferences:mail-send-preferences-data"

    // Plan
    public const val plan: String = ":plan"
    public const val planDagger: String = "$plan:plan-dagger"
    public const val planDomain: String = "$plan:plan-domain"
    public const val planData: String = "$plan:plan-data"
    public const val planPresentation: String = "$plan:plan-presentation"
    public const val planPresentationCompose: String = "$plan:plan-presentation-compose"
    public const val planTest: String = "$plan:plan-test"

    public const val push: String = ":push"
    public const val pushDomain: String = "$push:push-domain"
    public const val pushData: String = "$push:push-data"
    public const val pushDagger: String = "$push:push-dagger"

    // Reports
    public const val report: String = ":report"
    public const val reportDomain: String = ":report:report-domain"
    public const val reportData: String = ":report:report-data"
    public const val reportPresentation: String = ":report:report-presentation"
    public const val reportDagger: String = ":report:report-dagger"
    public const val reportTest: String = ":report:report-test"

    // Feature flags
    public const val featureFlag: String = ":feature-flag"
    public const val featureFlagData: String = "$featureFlag:feature-flag-data"
    public const val featureFlagDomain: String = "$featureFlag:feature-flag-domain"
    public const val featureFlagDagger: String = "$featureFlag:feature-flag-dagger"

    // Metrics
    public const val metrics: String = ":metrics"
    public const val metricsDomain: String = "$metrics:metrics-domain"
    public const val metricsData: String = "$metrics:metrics-data"
    public const val metricsDagger: String = "$metrics:metrics-dagger"

    // Challenge
    public const val challenge: String = ":challenge"
    public const val challengeDagger: String = ":challenge:challenge-dagger"
    public const val challengeDomain: String = ":challenge:challenge-domain"
    public const val challengeData: String = ":challenge:challenge-data"
    public const val challengePresentation: String = ":challenge:challenge-presentation"
    public const val challengePresentationCompose: String = ":challenge:challenge-presentation-compose"

    // Observability
    public const val observability: String = ":observability"
    public const val observabilityDagger: String = "$observability:observability-dagger"
    public const val observabilityDomain: String = "$observability:observability-domain"
    public const val observabilityData: String = "$observability:observability-data"

    // Telemetry
    public const val telemetry: String = ":telemetry"
    public const val telemetryDagger: String = "$telemetry:telemetry-dagger"
    public const val telemetryDomain: String = "$telemetry:telemetry-domain"
    public const val telemetryData: String = "$telemetry:telemetry-data"
    public const val telemetryPresentation: String = "$telemetry:telemetry-presentation"

    // Proguard rules
    public const val proguardRules: String = ":proguard-rules"


    // Key transparency
    public const val keyTransparency: String = ":key-transparency"
    public const val keyTransparencyDomain: String = "$keyTransparency:key-transparency-domain"
    public const val keyTransparencyData: String = "$keyTransparency:key-transparency-data"
    public const val keyTransparencyDagger: String = "$keyTransparency:key-transparency-dagger"
    public const val keyTransparencyPresentation: String = "$keyTransparency:key-transparency-presentation"

    // Easy device migration
    public const val deviceMigration: String = ":device-migration"
    public const val deviceMigrationDomain: String = "$deviceMigration:device-migration-domain"
    public const val deviceMigrationData: String = "$deviceMigration:device-migration-data"
    public const val deviceMigrationDagger: String = "$deviceMigration:device-migration-dagger"
    public const val deviceMigrationPresentation: String = "$deviceMigration:device-migration-presentation"

    // Biometric
    public const val biometric: String = ":biometric"
    public const val biometricDomain: String = "$biometric:biometric-domain"
    public const val biometricData: String = "$biometric:biometric-data"
    public const val biometricDagger: String = "$biometric:biometric-dagger"
    public const val biometricPresentation: String = "$biometric:biometric-presentation"

    // Pass validator
    public const val passValidator: String = ":pass-validator"
    public const val passValidatorDomain: String = "$passValidator:pass-validator-domain"
    public const val passValidatorDagger: String = "$passValidator:pass-validator-dagger"
    public const val passValidatorData: String = "$passValidator:pass-validator-data"
    public const val passValidatorPresentation: String = "$passValidator:pass-validator-presentation"

    // endregion
}
