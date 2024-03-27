/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.compose.component

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.textNorm
import me.proton.core.presentation.compose.R

/**
 * A full-size [LazyColumn] list styled with [ProtonTheme]
 * which displays the given content
 */
@Composable
fun ProtonSettingsList(
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    Surface(
        color = ProtonTheme.colors.backgroundNorm,
        contentColor = ProtonTheme.colors.textNorm,
        modifier = modifier.fillMaxSize()
    ) {
        val state = rememberLazyListState()

        LazyColumn(
            state = state,
            content = content
        )
    }
}

/**
 * A [TopAppBar] styled with [ProtonTheme] to be used in settings screens
 * By default, shows a back icon and the given title
 * @param onBackClick callback to handle back icon click
 */
@Composable
fun ProtonSettingsTopBar(
    modifier: Modifier = Modifier,
    title: String,
    onBackClick: () -> Unit,
) {
    ProtonTopAppBar(
        modifier = modifier.fillMaxWidth(),
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.presentation_back))
            }
        }
    )
}

@Composable
fun ProtonSettingsHeader(
    modifier: Modifier = Modifier,
    @StringRes title: Int
) {
    ProtonSettingsHeader(
        modifier = modifier,
        title = stringResource(id = title)
    )
}

@Composable
fun ProtonSettingsHeader(
    modifier: Modifier = Modifier,
    title: String
) {
    ProtonListItem(
        modifier = modifier,
        isClickable = false
    ) {
        Text(
            modifier = Modifier.align(Alignment.Bottom),
            text = title,
            color = ProtonTheme.colors.brandNorm,
            style = ProtonTheme.typography.body1Medium
        )
    }
}

@Composable
fun ProtonSettingsItem(
    modifier: Modifier = Modifier,
    name: String,
    hint: String? = null,
    isClickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    ProtonRawListItem(
        modifier = modifier
            .clickable(isClickable, onClick = onClick)
            .padding(
                vertical = ProtonDimens.ListItemTextStartPadding,
                horizontal = ProtonDimens.DefaultSpacing
            )
    ) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier,
                text = name,
                color = ProtonTheme.colors.textNorm,
                style = ProtonTheme.typography.body1Regular
            )
            hint?.let {
                Text(
                    modifier = Modifier.padding(top = ProtonDimens.ExtraSmallSpacing),
                    text = hint,
                    color = ProtonTheme.colors.textHint,
                    style = ProtonTheme.typography.body2Regular
                )
            }
        }
    }
}

@Composable
fun ProtonSettingsToggleItem(
    modifier: Modifier = Modifier,
    name: String,
    hint: String? = null,
    value: Boolean?,
    onToggle: (Boolean) -> Unit = {}
) {
    val isSwitchChecked = value ?: false
    val isViewEnabled = value != null
    Column(
        modifier = modifier
            .toggleable(
                value = isSwitchChecked,
                enabled = isViewEnabled,
                role = Role.Switch
            ) { onToggle(!isSwitchChecked) }
            .padding(horizontal = ProtonDimens.DefaultSpacing),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ProtonRawListItem(
            modifier = Modifier.sizeIn(minHeight = ProtonDimens.ListItemHeight),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                color = ProtonTheme.colors.textNorm(isViewEnabled),
                style = ProtonTheme.typography.body1Regular
            )
            Switch(
                checked = isSwitchChecked,
                onCheckedChange = null,
                enabled = isViewEnabled
            )
        }
        hint?.let {
            Text(
                modifier = Modifier.offset(y = toggleItemNegativeOffset),
                text = hint,
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.body2Regular
            )
        }
    }
}

@Composable
fun ProtonSettingsRadioItem(
    modifier: Modifier = Modifier,
    name: String,
    isSelected: Boolean,
    onItemSelected: (String) -> Unit
) {
    ProtonRawListItem(
        modifier = modifier
            .selectable(selected = isSelected, role = Role.RadioButton) { onItemSelected(name) }
            .sizeIn(minHeight = ProtonDimens.ListItemHeight)
            .padding(horizontal = ProtonDimens.DefaultSpacing),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            color = ProtonTheme.colors.textNorm,
            style = ProtonTheme.typography.body1Regular
        )
        RadioButton(
            selected = isSelected,
            onClick = null
        )
    }
}

@Preview(
    name = "Proton settings top bar light mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_NO
)
@Preview(
    name = "Proton settings top bar dark mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun SettingsTopBarPreview() {
    ProtonSettingsTopBar(title = "Setting", onBackClick = {})
}

@Preview(
    name = "Proton settings item with name and hint",
    showBackground = true
)
@Composable
fun SettingsItemPreview() {
    ProtonSettingsItem(name = "Setting name", hint = "This settings does nothing")
}

@Preview(
    name = "Proton settings toggleable item",
    showBackground = true
)
@Composable
fun SettingsToggleableItemPreview() {
    ProtonSettingsToggleItem(name = "Setting toggle", value = true)
}

@Preview(
    name = "Proton settings toggleable item disabled",
    showBackground = true
)
@Composable
fun DisabledSettingsToggleableItemPreview() {
    ProtonSettingsToggleItem(name = "Setting toggle", value = null)
}

@Preview(
    name = "Proton settings toggleable item with hint",
    showBackground = true
)
@Composable
fun SettingsToggleableItemWithHintPreview() {
    ProtonSettingsToggleItem(
        name = "Setting toggle",
        hint = "Use this space to provide an explanation of what toggling this setting does",
        value = true
    )
}

@Preview(
    name = "Proton settings item with name only",
    showBackground = true
)
@Composable
fun SettingsItemWithNameOnlyPreview() {
    ProtonSettingsItem(name = "Setting name")
}

@Preview(
    name = "Proton settings radio item",
    showBackground = true
)
@Composable
fun SettingsRadioItemPreview() {
    ProtonSettingsRadioItem(
        name = "Setting option",
        isSelected = true,
        onItemSelected = {}
    )
}

@Preview(
    name = "Settings screen light mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_NO
)
@Preview(
    name = "Settings screen dark mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun SettingsPreview() {
    ProtonSettingsList {
        item { ProtonSettingsHeader(title = "Account settings") }
        item { ProtonSettingsItem(name = "Test user", hint = "testuser@proton.ch") {} }
    }
}

private val toggleItemNegativeOffset = (-10).dp
