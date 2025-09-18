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

package me.proton.core.label.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.data.arch.ProtonStore
import me.proton.core.data.arch.buildProtonStore
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.label.data.remote.worker.DeleteLabelWorker
import me.proton.core.label.data.remote.worker.FetchLabelWorker
import me.proton.core.label.data.remote.worker.UpdateLabelWorker
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.entity.NewLabel
import me.proton.core.label.domain.entity.toUpdateLabel
import me.proton.core.label.domain.repository.LabelLocalDataSource
import me.proton.core.label.domain.repository.LabelRemoteDataSource
import me.proton.core.label.domain.repository.LabelRepository
import me.proton.core.util.kotlin.CoroutineScopeProvider
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabelRepositoryImpl @Inject constructor(
    private val remoteDataSource: LabelRemoteDataSource,
    private val localDataSource: LabelLocalDataSource,
    private val workManager: WorkManager,
    scopeProvider: CoroutineScopeProvider
) : LabelRepository {

    private data class StoreKey(val userId: UserId, val type: LabelType)

    private val store: ProtonStore<StoreKey, List<Label>> = StoreBuilder.from(
        fetcher = Fetcher.of { key: StoreKey ->
            remoteDataSource.getLabels(key.userId, key.type)
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key: StoreKey ->
                localDataSource.observeLabels(key.userId, key.type).map { labels ->
                    labels.takeIf { it.isNotEmpty() }?.minus(key.getFetchedTagLabel())
                }
            },
            writer = { key, labels ->
                localDataSource.upsertLabel(labels.plus(key.getFetchedTagLabel()))
            },
        )
    ).buildProtonStore(scopeProvider)

    override fun observeLabels(userId: UserId, type: LabelType, refresh: Boolean): Flow<DataResult<List<Label>>> =
        StoreKey(userId = userId, type = type).let { key ->
            store.stream(StoreReadRequest.cached(key, refresh)).map { it.toDataResult() }
        }

    override suspend fun getLabels(userId: UserId, type: LabelType, refresh: Boolean): List<Label> =
        StoreKey(userId = userId, type = type).let { key ->
            if (refresh) store.fresh(key) else store.get(key)
        }

    override suspend fun getLabel(userId: UserId, type: LabelType, labelId: LabelId, refresh: Boolean): Label? =
        // Can't fetch by labelId -> getAll + firstOrNull.
        getLabels(userId, type, refresh).firstOrNull { it.labelId == labelId }

    override suspend fun createLabel(userId: UserId, label: NewLabel) {
        val createdLabel = remoteDataSource.createLabel(userId, label)
        localDataSource.upsertLabel(listOf(createdLabel))
    }

    override suspend fun updateLabel(userId: UserId, label: Label) {
        localDataSource.upsertLabel(listOf(label))
        // Replace any existing [Update|Delete]LabelWorker.
        workManager.enqueueUniqueWork(
            getUniqueWorkName(userId, label.labelId),
            ExistingWorkPolicy.REPLACE,
            UpdateLabelWorker.getRequest(userId, label.type, label.toUpdateLabel())
        )
    }

    override suspend fun updateLabelIsExpanded(userId: UserId, type: LabelType, labelId: LabelId, isExpanded: Boolean) {
        val label = requireNotNull(getLabel(userId, type, labelId))
        updateLabel(userId, label.copy(isExpanded = isExpanded))
    }

    override suspend fun deleteLabel(userId: UserId, type: LabelType, labelId: LabelId) {
        localDataSource.deleteLabel(userId, listOf(labelId))
        // Replace any existing [Update|Delete]LabelWorker.
        workManager.enqueueUniqueWork(
            getUniqueWorkName(userId, labelId),
            ExistingWorkPolicy.REPLACE,
            DeleteLabelWorker.getRequest(userId, type, labelId),
        )
    }

    override fun markAsStale(userId: UserId, type: LabelType) {
        // Replace any existing [Fetch]LabelWorker.
        workManager.enqueueUniqueWork(
            getUniqueWorkName(userId, type),
            ExistingWorkPolicy.REPLACE,
            FetchLabelWorker.getRequest(userId, type),
        )
    }

    private companion object {
        // Currently for: [Update|Delete]LabelWorker (ExistingWorkPolicy.REPLACE).
        private fun getUniqueWorkName(userId: UserId, labelId: LabelId) =
            "${LabelRepositoryImpl::class.simpleName}-$userId-$labelId"

        // Currently for [Fetch]LabelWorker (ExistingWorkPolicy.REPLACE).
        private fun getUniqueWorkName(userId: UserId, type: LabelType) =
            "${LabelRepositoryImpl::class.simpleName}-$userId-$type"

        // Fake Label tagging the repo the labels have been fetched once.
        private fun StoreKey.getFetchedTagLabel() = Label(
            userId = userId,
            labelId = LabelId("fetched-$type"),
            parentId = null,
            name = "fetched",
            type = type,
            path = "",
            color = "",
            order = 0,
            isNotified = null,
            isExpanded = null,
            isSticky = null
        )
    }
}
