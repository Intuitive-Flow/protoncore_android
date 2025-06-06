/*
 * Copyright (c) 2020 Proton Technologies AG
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

package me.proton.core.accountmanager.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.auth.data.db.AuthDatabase
import me.proton.core.challenge.data.db.ChallengeDatabase
import me.proton.core.contact.data.local.db.ContactDatabase
import me.proton.core.eventmanager.data.db.EventMetadataDatabase
import me.proton.core.featureflag.data.db.FeatureFlagDatabase
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.key.data.db.KeySaltDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.keytransparency.data.local.KeyTransparencyDatabase
import me.proton.core.label.data.local.LabelDatabase
import me.proton.core.mailsettings.data.db.MailSettingsDatabase
import me.proton.core.notification.data.local.db.NotificationDatabase
import me.proton.core.observability.data.db.ObservabilityDatabase
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.push.data.local.db.PushDatabase
import me.proton.core.telemetry.data.db.TelemetryDatabase
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.user.data.db.UserKeyDatabase
import me.proton.core.userrecovery.data.db.DeviceRecoveryDatabase
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsDatabase

object AccountManagerDatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_2.migrate(database)
            UserDatabase.MIGRATION_0.migrate(database)
            AddressDatabase.MIGRATION_0.migrate(database)
            KeySaltDatabase.MIGRATION_0.migrate(database)
            PublicAddressDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AddressDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_3.migrate(database)
            HumanVerificationDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MailSettingsDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            OrganizationDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ContactDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AddressDatabase.MIGRATION_2.migrate(database)
            PublicAddressDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            EventMetadataDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_4.migrate(database)
            AddressDatabase.MIGRATION_3.migrate(database)
            UserDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            LabelDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            FeatureFlagDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            OrganizationDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(database: SupportSQLiteDatabase) {
            FeatureFlagDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ChallengeDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ChallengeDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(database: SupportSQLiteDatabase) {
            PushDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(database: SupportSQLiteDatabase) {
            FeatureFlagDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(database: SupportSQLiteDatabase) {
            FeatureFlagDatabase.MIGRATION_3.migrate(database)
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(database: SupportSQLiteDatabase) {
            HumanVerificationDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(database: SupportSQLiteDatabase) {
            HumanVerificationDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(database: SupportSQLiteDatabase) {
            PaymentDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_5.migrate(database)
        }
    }

    val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ObservabilityDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(database: SupportSQLiteDatabase) {
            OrganizationDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AddressDatabase.MIGRATION_4.migrate(database)
            PublicAddressDatabase.MIGRATION_2.migrate(database)
            KeyTransparencyDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(database: SupportSQLiteDatabase) {
            NotificationDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(database: SupportSQLiteDatabase) {
            NotificationDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_32_33 = object : Migration(32, 33) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_33_34 = object : Migration(33, 34) {
        override fun migrate(database: SupportSQLiteDatabase) {
            ContactDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_34_35 = object : Migration(34, 35) {
        override fun migrate(database: SupportSQLiteDatabase) {
            EventMetadataDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_35_36 = object : Migration(35, 36) {
        override fun migrate(database: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_6.migrate(database)
            UserDatabase.MIGRATION_3.migrate(database)
        }
    }

    val MIGRATION_36_37 = object : Migration(36, 37) {
        override fun migrate(database: SupportSQLiteDatabase) {
            TelemetryDatabase.MIGRATION_0.migrate(database)
            UserSettingsDatabase.MIGRATION_3.migrate(database)
        }
    }

    val MIGRATION_37_38 = object : Migration(37, 38) {
        override fun migrate(database: SupportSQLiteDatabase) {
            EventMetadataDatabase.MIGRATION_2.migrate(database)
        }
    }

    val MIGRATION_38_39 = object : Migration(38, 39) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_4.migrate(database)
        }
    }

    val MIGRATION_39_40 = object : Migration(39, 40) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_5.migrate(database)
        }
    }

    val MIGRATION_40_41 = object : Migration(40, 41) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserKeyDatabase.MIGRATION_0.migrate(database)
        }
    }

    val MIGRATION_41_42 = object : Migration(41, 42) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserDatabase.MIGRATION_4.migrate(database)
        }
    }

    val MIGRATION_42_43 = object : Migration(42, 43) {
        override fun migrate(database: SupportSQLiteDatabase) {
            UserDatabase.MIGRATION_5.migrate(database)
            AccountDatabase.MIGRATION_7.migrate(database)
        }
    }

    val MIGRATION_43_44 = object : Migration(43, 44) {
        override fun migrate(database: SupportSQLiteDatabase) {
            PaymentDatabase.MIGRATION_1.migrate(database)
        }
    }

    val MIGRATION_44_45 = object : Migration(44, 45) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_6.migrate(db)
        }
    }

    val MIGRATION_45_46 = object : Migration(45, 46) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DeviceRecoveryDatabase.MIGRATION_0.migrate(db)
            UserKeyDatabase.MIGRATION_1.migrate(db)
            DeviceRecoveryDatabase.MIGRATION_1.migrate(db)
        }
    }

    val MIGRATION_46_47 = object : Migration(46, 47) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_8.migrate(db)
            UserSettingsDatabase.MIGRATION_7.migrate(db)
        }
    }

    val MIGRATION_47_48 = object : Migration(47, 48) {
        override fun migrate(db: SupportSQLiteDatabase) {
            EventMetadataDatabase.MIGRATION_3.migrate(db)
        }
    }

    val MIGRATION_48_49 = object : Migration(48, 49) {
        override fun migrate(db: SupportSQLiteDatabase) {
            PublicAddressDatabase.MIGRATION_3.migrate(db)
        }
    }

    val MIGRATION_49_50 = object : Migration(49, 50) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ContactDatabase.MIGRATION_2.migrate(db)
        }
    }

    val MIGRATION_50_51 = object : Migration(50, 51) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AuthDatabase.MIGRATION_0.migrate(db)
            AuthDatabase.MIGRATION_1.migrate(db)
        }
    }

    val MIGRATION_51_52 = object : Migration(51, 52) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AuthDatabase.MIGRATION_2.migrate(db)
        }
    }

    val MIGRATION_52_53 = object : Migration(52, 53) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AuthDatabase.MIGRATION_3.migrate(db)
        }
    }

    val MIGRATION_53_54 = object : Migration(53, 54) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AuthDatabase.MIGRATION_4.migrate(db)
        }
    }

    val MIGRATION_54_55 = object : Migration(54, 55) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AuthDatabase.MIGRATION_5.migrate(db)
        }
    }

    val MIGRATION_55_56 = object : Migration(55, 56) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MailSettingsDatabase.MIGRATION_1.migrate(db)
        }
    }

    val MIGRATION_56_57 = object : Migration(56, 57) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserDatabase.MIGRATION_6.migrate(db)
            AccountDatabase.MIGRATION_9.migrate(db)
        }
    }

    val MIGRATION_57_58 = object : Migration(57, 58) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MailSettingsDatabase.MIGRATION_2.migrate(db)
        }
    }

    val MIGRATION_58_59 = object : Migration(58, 59) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MailSettingsDatabase.MIGRATION_3.migrate(db)
        }
    }

    val MIGRATION_59_60 = object : Migration(59, 60) {
        override fun migrate(db: SupportSQLiteDatabase) {
            UserSettingsDatabase.MIGRATION_8.migrate(db)
        }
    }

    val MIGRATION_60_61 = object : Migration(60, 61) {
        override fun migrate(db: SupportSQLiteDatabase) {
            AccountDatabase.MIGRATION_10.migrate(db)
        }
    }
}
