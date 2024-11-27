/*
 * Copyright (c) 2022 Proton Technologies AG
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

package me.proton.core.challenge.data

import me.proton.core.challenge.domain.ChallengeManager
import me.proton.core.challenge.domain.entity.ChallengeFrameDetails
import me.proton.core.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

public class ChallengeManagerImpl @Inject constructor(
    private val challengeRepository: ChallengeRepository
) : ChallengeManager {

    override suspend fun resetFlow(flow: String) {
        challengeRepository.deleteFrames(flow)
    }

    override suspend fun addOrUpdateFrameToFlow(frame: ChallengeFrameDetails) {
        challengeRepository.insertFrameDetails(frame)
    }

    override suspend fun getFramesByFlowName(flow: String): List<ChallengeFrameDetails> =
        challengeRepository.getFramesByFlow(flow) ?: emptyList()

    override suspend fun getFrameByFlowAndFrameName(flow: String, frame: String): ChallengeFrameDetails? =
        challengeRepository.getFramesByFlowAndFrame(flow, frame)
}
