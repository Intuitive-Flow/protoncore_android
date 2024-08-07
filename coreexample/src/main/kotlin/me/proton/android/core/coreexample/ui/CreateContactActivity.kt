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

package me.proton.android.core.coreexample.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.proton.android.core.coreexample.databinding.ActivityCreateContactBinding
import me.proton.android.core.coreexample.viewmodel.CreateContactViewModel
import me.proton.core.presentation.ui.ProtonViewBindingActivity
import me.proton.core.presentation.utils.showToast
import me.proton.core.util.kotlin.exhaustive

@AndroidEntryPoint
class CreateContactActivity : ProtonViewBindingActivity<ActivityCreateContactBinding>(
    ActivityCreateContactBinding::inflate
) {

    private val viewModel: CreateContactViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.createButton.setOnClickListener {
            viewModel.createContact(binding.name.text.toString())
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is CreateContactViewModel.State.Error -> showToast(state.reason)
                        CreateContactViewModel.State.Idling -> binding.createButton.setIdle()
                        CreateContactViewModel.State.Processing -> binding.createButton.setLoading()
                        CreateContactViewModel.State.Success -> {
                            showToast("Success")
                            finish()
                        }
                    }.exhaustive
                }
            }
        }
    }
}
