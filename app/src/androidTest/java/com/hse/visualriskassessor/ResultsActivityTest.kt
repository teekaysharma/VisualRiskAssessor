package com.hse.visualriskassessor

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.hse.visualriskassessor.ui.results.ResultsActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ResultsActivityTest {

    @Test
    fun resultsActivity_withNoImageUri_finishesImmediately() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            ResultsActivity::class.java
        )
        ActivityScenario.launch<ResultsActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity.isFinishing || activity.isDestroyed)
            }
        }
    }

    @Test
    fun resultsActivity_displaysToolbar() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val intent = Intent(context, ResultsActivity::class.java).apply {
            putExtra(ResultsActivity.EXTRA_IMAGE_URI, "android.resource://${context.packageName}/${R.drawable.ic_launcher_foreground}")
        }
        ActivityScenario.launch<ResultsActivity>(intent).use {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        }
    }
}
