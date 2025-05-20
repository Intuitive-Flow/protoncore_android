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

@file:Suppress("unused") // Public APIs

package me.proton.core.presentation.utils

import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import me.proton.core.presentation.ui.view.ProtonInput
import me.proton.core.util.kotlin.orEmpty
import me.proton.core.util.kotlin.times

/**
 * Shortcut for [AdapterView.setOnItemSelectedListener]
 * This allow us to pass a simple lambda, instead of an anonymous class of
 * [AdapterView.OnItemSelectedListener]
 *
 * @param block takes *position* [Int]
 */
inline fun <T : Adapter> AdapterView<T>.onItemSelected(crossinline block: (position: Int) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

        /**
         * Callback method to be invoked when an item in this view has been
         * selected. This callback is invoked only when the newly selected
         * position is different from the previously selected position or if
         * there was no selected item.
         *
         * Implementers can call getItemAtPosition(position) if they need to access the
         * data associated with the selected item.
         *
         * @param parent The AdapterView where the selection happened
         * @param view The view within the AdapterView that was clicked
         * @param position The position of the view in the adapter
         * @param id The row id of the item that is selected
         */
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            block(position)
        }

        /**
         * Callback method to be invoked when the selection disappears from this
         * view. The selection can disappear for instance when touch is activated
         * or when the adapter becomes empty.
         *
         * @param parent The AdapterView that now contains no selected item.
         */
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // Noop
        }
    }
}

/**
 * Clears the edit text content.
 */
fun EditText.clearText() = setText("")

/**
 * Clears edit text content and attempt to clear text memory (use for password fields).
 */
fun EditText.clearTextAndOverwriteMemory() {
    with(text) {
        // Override memory and clear - undocumented behavior.
        replace(0, length, " " * length)
        clear()
    }
    clearComposingText()
}

/** Execute the [textChangeListener] on [TextWatcher.onTextChanged] */
inline fun ProtonInput.onTextChange(
    crossinline textChangeListener: (CharSequence) -> Unit = {},
    crossinline afterTextChangeListener: ProtonInput.(Editable) -> Unit = {}
): TextWatcher {
    val watcher = object : TextWatcher {
        override fun afterTextChanged(editable: Editable) {
            afterTextChangeListener(editable)
        }

        override fun beforeTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
            /* Do nothing */
        }

        override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
            textChangeListener(text)
        }
    }
    addTextChangedListener(watcher)
    return watcher
}

/** Returns a flow of [ProtonInput.text] each time it changes (including the initial value). */
fun ProtonInput.textChanges(): Flow<CharSequence> = callbackFlow {
    send(text.orEmpty())
    val watcher = onTextChange { text ->
        trySendBlocking(text)
    }
    awaitClose { removeTextChangedListener(watcher) }
}

/**
 * Shortcut for [View.setOnClickListener]
 * This also allow us to pass a function KProperty as argument
 * e.g. `` view.onClick(::doSomething) ``
 */
inline fun View.onClick(crossinline block: () -> Unit) {
    setOnClickListener { block() }
}

/**
 * Inflate a [LayoutRes] from the receiver [ViewGroup]
 * @param attachToRoot Default is `false`
 * @return [View]
 */
fun ViewGroup.inflate(@LayoutRes layoutId: Int, attachToRoot: Boolean = false): View =
    LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.removeOnAttachStateChangeListener(this)
                view.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

fun View.doOnApplyWindowInsets(block: (View, WindowInsetsCompat, InitialMargin, InitialPadding) -> Unit) {
    val initialMargin = recordInitialMarginForView(this)
    val initialPadding = recordInitialPaddingForView(this)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        block(view, insets, initialMargin, initialPadding)
        insets
    }
    requestApplyInsetsWhenAttached()
}

data class InitialMargin(val start: Int, val top: Int, val end: Int, val bottom: Int)

private fun recordInitialMarginForView(view: View) = InitialMargin(
    view.marginStart, view.marginTop, view.marginEnd, view.marginBottom
)

data class InitialPadding(val start: Int, val top: Int, val end: Int, val bottom: Int)

private fun recordInitialPaddingForView(view: View) = InitialPadding(
    view.paddingStart, view.paddingTop, view.paddingEnd, view.paddingBottom
)

fun ViewGroup.saveChildViewStates(): SparseArray<Parcelable> {
    val childViewStates = SparseArray<Parcelable>()
    children.forEach { child -> child.saveHierarchyState(childViewStates) }
    return childViewStates
}

fun ViewGroup.restoreChildViewStates(childViewStates: SparseArray<Parcelable>) {
    children.forEach { child -> child.restoreHierarchyState(childViewStates) }
}
