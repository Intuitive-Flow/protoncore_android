package me.proton.core.gradle.plugins.coverage.rules

import kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig

internal fun KoverReportFiltersConfig.roomDbRules() {
    excludes {
        annotatedBy(
            "androidx.room.Dao",
            "androidx.room.Database",
            "androidx.room.Entity",
            "javax.annotation.processing.Generated"
        )
        classes(
            "*Dao_Impl",
            "*Dao_Impl\$*"
        )
    }
}
