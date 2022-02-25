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

package me.proton.core.plan.presentation.entity

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import kotlinx.parcelize.Parcelize
import me.proton.core.plan.domain.entity.Plan
import me.proton.core.presentation.utils.PRICE_ZERO
import me.proton.core.presentation.utils.Price
import java.util.Date

sealed class PlanDetailsItem(
    open val name: String,
    open val displayName: String,
    open val storage: Long,
    open val addresses: Int,
    open val connections: Int,
    open val domains: Int,
    open val members: Int,
    open val calendars: Int,
    open val cycle: PlanCycle
) : Parcelable {

    @Parcelize
    data class CurrentPlanDetailsItem(
        override val name: String,
        override val displayName: String,
        override val storage: Long,
        override val addresses: Int,
        override val connections: Int,
        override val domains: Int,
        override val members: Int,
        override val calendars: Int,
        override val cycle: PlanCycle,
        val price: PlanPricing?,
        val isAutoRenewal: Boolean,
        val endDate: Date?,
        val progressValue: Int,
        val usedSpace: Long,
        val maxSpace: Long,
        val usedAddresses: Int,
        val usedDomains: Int,
        val usedMembers: Int,
        val usedCalendars: Int
    ) : PlanDetailsItem(
        name,
        displayName,
        storage,
        addresses,
        connections,
        domains,
        members,
        calendars,
        cycle
    )

    @Parcelize
    data class FreePlanDetailsItem(
        override val name: String,
        override val displayName: String,
        override val storage: Long,
        override val members: Int,
        override val addresses: Int,
        override val calendars: Int,
        override val domains: Int,
        override val connections: Int
    ) : PlanDetailsItem(
        name,
        displayName,
        storage,
        addresses,
        connections,
        domains,
        members,
        calendars,
        PlanCycle.FREE
    )

    @Parcelize
    data class PaidPlanDetailsItem(
        override val name: String,
        override val displayName: String,
        override val storage: Long,
        override val members: Int,
        override val addresses: Int,
        override val calendars: Int,
        override val domains: Int,
        override val connections: Int,
        override val cycle: PlanCycle,
        val price: PlanPricing,
        val currency: PlanCurrency,
        val starred: Boolean
    ) : PlanDetailsItem(
        name,
        displayName,
        storage,
        addresses,
        connections,
        domains,
        members,
        calendars,
        cycle
    )

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<PlanDetailsItem>() {
            override fun areItemsTheSame(oldItem: PlanDetailsItem, newItem: PlanDetailsItem) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: PlanDetailsItem, newItem: PlanDetailsItem) =
                oldItem == newItem
        }
    }
}

@Parcelize
data class PlanPricing(
    val monthly: Price,
    val yearly: Price,
    val twoYearly: Price? = null
) : Parcelable {

    companion object {
        fun fromPlan(plan: Plan) =
            plan.pricing?.let {
                PlanPricing(it.monthly.toDouble(), it.yearly.toDouble(), it.twoYearly?.toDouble())
            } ?: run {
                val cycle = PlanCycle.map[plan.cycle]
                val monthly = if (cycle == PlanCycle.MONTHLY) plan.amount else PRICE_ZERO
                val yearly = if (cycle == PlanCycle.YEARLY) plan.amount else PRICE_ZERO
                val twoYears = if (cycle == PlanCycle.TWO_YEARS) plan.amount else PRICE_ZERO
                PlanPricing(monthly.toDouble(), yearly.toDouble(), twoYears.toDouble())
            }
    }
}
