package com.hse.visualriskassessor

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.hse.visualriskassessor.ui.MainActivity
import com.hse.visualriskassessor.ui.history.HistoryActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun mainScreen_displaysAllButtons() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btnTakePhoto)).check(matches(isDisplayed()))
            onView(withId(R.id.btnChoosePhoto)).check(matches(isDisplayed()))
            onView(withId(R.id.historyCard)).check(matches(isDisplayed()))
            onView(withId(R.id.aboutCard)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun historyCard_click_opensHistoryActivity() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.historyCard)).perform(click())
            Intents.intended(hasComponent(HistoryActivity::class.java.name))
        }
    }
}
