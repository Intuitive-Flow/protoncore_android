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

package me.proton.core.plan.domain.entity

import me.proton.core.domain.entity.AppStore

const val PLAN_PRODUCT = 1
const val PLAN_ADDON = 0

const val MASK_NONE = 0 // bitmap
const val MASK_MAIL = 1 // bitmap
const val MASK_CALENDAR = MASK_MAIL // bitmap
const val MASK_DRIVE = 2 // bitmap
const val MASK_VPN = 4 // bitmap
const val MASK_PASS = 8 // bitmap
const val MASK_ALL = 15 // bitmap

const val PLAN_VENDOR_GOOGLE = "Google"

data class Plan(
    val id: String?,
    val type: Int,
    val cycle: Int?,
    val name: String,
    val title: String,
    val currency: String?,
    val amount: Int,
    val maxDomains: Int,
    val maxAddresses: Int,
    val maxCalendars: Int,
    val maxRewardSpace: Long? = null,
    val maxSpace: Long,
    val maxMembers: Int,
    val maxVPN: Int,
    val services: Int?,
    val features: Int,
    val quantity: Int,
    val maxTier: Int?,
    val enabled: Boolean,
    val pricing: PlanPricing? = null,
    val vendors: Map<AppStore, PlanVendorData> = emptyMap()
)

data class PlanPricing(
    val monthly: Int,
    val yearly: Int,
    val twoYearly: Int? = null
)

/**
 * @param customerId Customer ID used with the vendor.
 * @param names Mapping from plan duration (cycle) to vendor plan name.
 */
data class PlanVendorData constructor(
    val customerId: String,
    val names: Map<PlanDuration, String>
)

@JvmInline
value class PlanDuration(val months: Int)
