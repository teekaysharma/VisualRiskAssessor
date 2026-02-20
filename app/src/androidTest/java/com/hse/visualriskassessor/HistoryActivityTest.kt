package com.hse.visualriskassessor

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.hse.visualriskassessor.ui.history.HistoryActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HistoryActivityTest {

    @Test
    fun historyScreen_showsEmptyState_whenNoAssessments() {
        ActivityScenario.launch(HistoryActivity::class.java).use {
            onView(withId(R.id.emptyView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun historyScreen_toolbar_isDisplayed() {
        ActivityScenario.launch(HistoryActivity::class.java).use {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        }
    }
}
