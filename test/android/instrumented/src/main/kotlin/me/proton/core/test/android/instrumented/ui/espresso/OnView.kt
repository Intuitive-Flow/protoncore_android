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

package me.proton.core.test.android.instrumented.ui.espresso

import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.actionWithAssertions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.RootMatchers.DEFAULT
import androidx.test.espresso.matcher.ViewMatchers
import me.proton.core.test.android.actions.BetterScrollToAction
import me.proton.core.test.android.NestedScrollViewExtension
import me.proton.core.test.android.instrumented.ConditionWatcher
import me.proton.core.test.android.instrumented.ProtonTest.Companion.commandTimeout
import me.proton.core.test.android.instrumented.matchers.inputFieldMatcher
import me.proton.core.test.android.instrumented.utils.StringUtils.stringFromResource
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.AllOf

/**
 * Builder like class that allows to write [ViewActions] and [ViewAssertion] for single [View].
 */
class OnView : ConditionWatcher {
    private val matchers: ArrayList<Matcher<View>> = arrayListOf()
    private val rootMatchers: ArrayList<Matcher<Root>> = arrayListOf()

    /** [ViewInteraction] wait. **/
    private fun viewInteraction(
        viewAssertion: ViewAssertion = matches(ViewMatchers.isDisplayed()),
        timeout: Long = commandTimeout
    ): ViewInteraction {
        waitForCondition(timeout) { onView(viewMatcher()).inRoot(rootMatcher()).check(viewAssertion) }
        return onView(viewMatcher()).inRoot(rootMatcher())
    }

    /** Matcher wrappers **/
    fun instanceOf(clazz: Class<*>?) = apply {
        matchers.add(CoreMatchers.instanceOf(clazz))
    }

    fun isEnabled() = apply {
        matchers.add(ViewMatchers.isEnabled())
    }

    fun hasSibling(siblingView: OnView) = apply {
        matchers.add(ViewMatchers.hasSibling(siblingView.viewMatcher()))
    }

    fun withAncestor(ancestor: OnView) = apply {
        matchers.add(ViewMatchers.isDescendantOfA(ancestor.viewMatcher()))
    }

    fun withId(@IdRes id: Int) = apply {
        matchers.add(ViewMatchers.withId(id))
    }

    fun withParent(parentView: OnView) = apply {
        matchers.add(ViewMatchers.withParent(parentView.viewMatcher()))
    }

    fun withText(@StringRes textId: Int) = apply {
        matchers.add(ViewMatchers.withText(stringFromResource(textId)))
    }

    fun withText(text: String) = apply {
        matchers.add(ViewMatchers.withText(text))
    }

    fun startsWith(text: String) = apply {
        matchers.add(ViewMatchers.withText(CoreMatchers.startsWith(text)))
    }

    fun isClickable() = apply {
        matchers.add(ViewMatchers.isClickable())
    }

    fun isChecked() = apply {
        matchers.add(ViewMatchers.isChecked())
    }

    fun isCompletelyDisplayed() = apply {
        matchers.add(ViewMatchers.isCompletelyDisplayed())
    }

    fun isDescendantOf(ancestorView: OnView) = apply {
        matchers.add(ViewMatchers.isDescendantOfA(ancestorView.viewMatcher()))
    }

    fun isDisplayingAtLeast(displayedPercentage: Int) = apply {
        matchers.add(ViewMatchers.isDisplayingAtLeast(displayedPercentage))
    }

    fun isDisabled() = apply {
        matchers.add(CoreMatchers.not(ViewMatchers.isEnabled()))
    }

    fun isFocusable() = apply {
        matchers.add(ViewMatchers.isFocusable())
    }

    fun isFocused() = apply {
        matchers.add(ViewMatchers.isFocused())
    }

    /**
     * Match EditText with [id] or child EditText of ProtonInput with [id].
     */
    fun isInputField(@IdRes id: Int) = apply {
        matchers.add(inputFieldMatcher(id))
    }

    fun isNotChecked() = apply {
        matchers.add(ViewMatchers.isNotChecked())
    }

    fun isSelected() = apply {
        matchers.add(ViewMatchers.isSelected())
    }

    fun hasChildCount(childCount: Int) = apply {
        matchers.add(ViewMatchers.hasChildCount(childCount))
    }

    fun hasContentDescription() = apply {
        matchers.add(ViewMatchers.hasContentDescription())
    }

    fun hasDescendant(descendantView: OnView) = apply {
        matchers.add(ViewMatchers.hasDescendant(descendantView.viewMatcher()))
    }

    fun hasErrorText(errorText: String) = apply {
        matchers.add(ViewMatchers.hasErrorText(errorText))
    }

    fun hasFocus() = apply {
        matchers.add(ViewMatchers.hasFocus())
    }

    fun hasImeAction(imeAction: Int) = apply {
        matchers.add(ViewMatchers.hasImeAction(imeAction))
    }

    fun hasLinks() = apply {
        matchers.add(ViewMatchers.hasLinks())
    }

    fun supportsInputMethods() = apply {
        matchers.add(ViewMatchers.supportsInputMethods())
    }

    fun withChild(childMatcher: OnView) = apply {
        matchers.add(ViewMatchers.withChild(childMatcher.viewMatcher()))
    }

    fun withClassName(className: String) = apply {
        matchers.add(ViewMatchers.withClassName(CoreMatchers.equalTo(className)))
    }

    fun withContentDesc(contentDescText: String) = apply {
        matchers.add(ViewMatchers.withContentDescription(contentDescText))
    }

    fun withContentDesc(@StringRes contentDescTextId: Int) = apply {
        matchers.add(
            ViewMatchers.withContentDescription(
                stringFromResource(contentDescTextId)
            )
        )
    }

    fun withContentDesc(contentDescMatcher: Matcher<out CharSequence?>?) = apply {
        matchers.add(ViewMatchers.withContentDescription(contentDescMatcher))
    }

    fun withHint(hint: String) = apply {
        matchers.add(ViewMatchers.withHint(hint))
    }

    fun withHint(@StringRes hintId: Int) = apply {
        matchers.add(ViewMatchers.withHint(hintId))
    }

    fun withInputType(inputType: Int) = apply {
        matchers.add(ViewMatchers.withInputType(inputType))
    }

    fun withParentIndex(indexInParent: Int) = apply {
        matchers.add(ViewMatchers.withParentIndex(indexInParent))
    }

    fun withResourceName(resourceName: String) = apply {
        matchers.add(ViewMatchers.withResourceName(resourceName))
    }

    fun withSubstring(substring: String) = apply {
        matchers.add(ViewMatchers.withSubstring(substring))
    }

    fun withSnackbarText(text: String) = apply {
        matchers.add(
            CoreMatchers.allOf(
                ViewMatchers.withId(com.google.android.material.R.id.snackbar_text),
                ViewMatchers.withText(text)
            )
        )
    }

    fun withSnackbar() = apply {
        matchers.add(ViewMatchers.withId(com.google.android.material.R.id.snackbar_text))
    }

    fun withSpinnerText(spinnerText: String) = apply {
        matchers.add(ViewMatchers.withSpinnerText(spinnerText))
    }

    fun withTag(tag: Any) = apply {
        matchers.add(ViewMatchers.withTagValue(CoreMatchers.`is`(tag)))
    }

    fun withTagKey(tagKey: Int) = apply {
        matchers.add(ViewMatchers.withTagKey(tagKey))
    }

    fun withVisibility(visibility: ViewMatchers.Visibility) = apply {
        matchers.add(ViewMatchers.withEffectiveVisibility(visibility))
    }

    fun withCustomMatcher(matcher: Matcher<View>) = apply {
        matchers.add(matcher)
    }

    fun withRootMatcher(matcher: Matcher<Root>) = apply {
        rootMatchers.add(matcher)
    }

    fun inRoot(root: OnRootView) = apply {
        rootMatchers.add(root.matcher())
    }

    fun withTimeout(milliseconds: Long) = apply {
        viewInteraction(timeout = milliseconds)
    }

    /** Final [Matcher] for the view. **/
    fun viewMatcher(): Matcher<View> = AllOf.allOf(matchers)

    /** Final [Matcher] for the root. **/
    private fun rootMatcher(): Matcher<Root> = if (rootMatchers.isEmpty()) DEFAULT else AllOf.allOf(rootMatchers)

    /** Action wrappers. **/
    fun click() = apply {
        viewInteraction().perform(ViewActions.click())
    }

    fun clearText() = apply {
        viewInteraction().perform(ViewActions.clearText(), ViewActions.closeSoftKeyboard())
    }

    fun customAction(vararg customViewActions: ViewAction) = apply {
        viewInteraction().perform(*customViewActions)
    }

    fun replaceText(text: String) = apply {
        viewInteraction().perform(ViewActions.replaceText(text), ViewActions.closeSoftKeyboard())
    }

    fun swipeDown() = apply {
        viewInteraction().perform(ViewActions.swipeDown())
    }

    fun swipeLeft() = apply {
        viewInteraction().perform(ViewActions.swipeLeft())
    }

    fun swipeRight() = apply {
        viewInteraction().perform(ViewActions.swipeRight())
    }

    fun swipeUp() = apply {
        viewInteraction().perform(ViewActions.swipeUp())
    }

    fun typeText(text: String) = apply {
        viewInteraction().perform(ViewActions.typeText(text), ViewActions.closeSoftKeyboard())
    }

    fun closeKeyboard() = apply {
        viewInteraction().perform(ViewActions.closeSoftKeyboard())
    }

    fun closeDrawer() = apply {
        viewInteraction().perform(DrawerActions.close())
    }

    fun doubleClick() = apply {
        viewInteraction().perform(ViewActions.doubleClick())
    }

    fun longClick() = apply {
        viewInteraction().perform(ViewActions.longClick())
    }

    fun openDrawer() = apply {
        viewInteraction().perform(DrawerActions.open())
    }

    fun pressBack() = apply {
        viewInteraction().perform(ViewActions.pressBack())
    }

    fun pressImeActionBtn() = apply {
        viewInteraction().perform(ViewActions.pressImeActionButton())
    }

    fun scrollTo() = apply {
        viewInteraction(matches(CoreMatchers.anything())).perform(actionWithAssertions(
            BetterScrollToAction()
        ))
    }

    fun scrollToNestedScrollView() = apply {
        viewInteraction(matches(CoreMatchers.anything())).perform(NestedScrollViewExtension())
    }

    /** Assertion wrappers **/
    fun checkIsChecked() = apply {
        viewInteraction(matches(ViewMatchers.isChecked()))
    }

    fun checkIsNotChecked() = apply {
        viewInteraction(matches(CoreMatchers.not(ViewMatchers.isChecked())))
    }

    fun checkDisplayed() = apply {
        viewInteraction(matches(ViewMatchers.isDisplayed()))
    }

    fun checkDisabled() = apply {
        viewInteraction(matches(CoreMatchers.not(ViewMatchers.isEnabled())))
    }

    fun checkEffectiveVisibility(visibility: ViewMatchers.Visibility) = apply {
        viewInteraction(matches(ViewMatchers.withEffectiveVisibility(visibility)))
    }

    fun checkEnabled() = apply {
        viewInteraction(matches(ViewMatchers.isEnabled()))
    }

    fun checkSelected() = apply {
        viewInteraction(matches(ViewMatchers.isSelected()))
    }

    fun checkContains(text: String) = apply {
        viewInteraction(matches(ViewMatchers.withText(CoreMatchers.containsString(text))))
    }

    fun checkContainsAny(vararg text: String) = apply {
        val matchers = text.map { CoreMatchers.containsString(it) }
        viewInteraction(matches(ViewMatchers.withText(CoreMatchers.anyOf(matchers))))
    }

    fun checkContains(@StringRes textId: Int) = apply {
        viewInteraction(matches(ViewMatchers.withText(stringFromResource(textId))))
    }

    fun checkDoesNotExist() = apply {
        viewInteraction(doesNotExist())
    }

    fun checkLengthEquals(length: Int) = apply {
        viewInteraction(
            matches(
                ViewMatchers.withText(object : TypeSafeMatcher<String>(String::class.java) {
                    override fun describeTo(description: Description) {
                        description.appendText("String length equals $length")
                    }

                    override fun matchesSafely(item: String?): Boolean = item?.length == length
                })
            )
        )
    }

    fun checkNotDisplayed() = apply {
        viewInteraction(matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
    }
}
