package jp.muo.syncmemories.fragments

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.muo.syncmemories.ControllersFragment
import jp.muo.syncmemories.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControllersFragmentTest {
    private fun launchController() = launchFragmentInContainer<ControllersFragment>()

    @Test
    fun launchFragment() {
        launchController()
        onView(withId(R.id.btn_testshot))
            .check(matches(withText(R.string.btn_label_trigger_sync)))
    }
}