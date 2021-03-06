package com.udacity.project4

import android.Manifest
import android.app.Activity
import android.app.Application
import androidx.lifecycle.map
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.authentication.FirebaseUserLiveData
import com.udacity.project4.base.DataBindingViewHolder
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get


@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
	AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

	private lateinit var repository: ReminderDataSource
	private lateinit var appContext: Application

	private val dataBindingIdlingResource = DataBindingIdlingResource()

	@get:Rule
	val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

	/**
	 * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
	 * at this step we will initialize Koin related code to be able to use it in out testing.
	 */
	@Before
	fun init() {
		stopKoin()//stop the original app koin
		appContext = getApplicationContext()
		val authenticationState = FirebaseUserLiveData().map {
			RemindersListViewModel.AuthenticationState.AUTHENTICATED
		}
		val myModule = module {
			viewModel {
				RemindersListViewModel(
					get() as ReminderDataSource,
					authenticationState
				)
			}
			single {
				SaveReminderViewModel(
					get() as ReminderDataSource
				)
			}
			single<ReminderDataSource> { RemindersLocalRepository(get()) }
			single { LocalDB.createRemindersDao(appContext) }
		}
		//declare a new koin module
		startKoin {
			modules(listOf(myModule))
		}
		//Get our real repository
		repository = get()

		//clear the data to start fresh
		runBlocking {
			repository.deleteAllReminders()
		}
	}

	@Before
	fun registerIdlingResource() {
		IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
		IdlingRegistry.getInstance().register(dataBindingIdlingResource)
	}

	@After
	fun unregisterIdlingResource() {
		IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
		IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
	}

	@Test
	fun newReminder_isDisplayedInRemindersListScreen() = runBlocking {
		val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
		dataBindingIdlingResource.monitorActivity(activityScenario)

		onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
		onView(withId(R.id.addReminderFAB)).perform(click())
		onView(withId(R.id.selectLocation)).perform(click())
		Thread.sleep(1000L)

		onView(withId(R.id.map)).perform(click())
		onView(withId(R.id.save_button)).perform(click())
		onView(withId(R.id.reminderTitle)).perform(typeText("Buy fruit"))
		onView(withId(R.id.reminderDescription)).perform(typeText("Buy 1 week fruit"))
		closeSoftKeyboard()
		onView(withId(R.id.saveReminder)).perform(click())

		// TOAST TEST
		assert(getActivity(activityScenario) != null)
		getActivity(activityScenario)?.run {
			onView(withText(R.string.reminder_saved))
				.inRoot(withDecorView(not(this.window.decorView)))
				.check(matches(isDisplayed()))
		}

		onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
		onView(withId(R.id.reminderssRecyclerView)).perform(
			RecyclerViewActions.scrollTo<DataBindingViewHolder<ReminderDataItem>>(
				hasDescendant(withText("Buy fruit"))
			)
		)

		activityScenario.close()
	}

	@Test
	fun invalidReminder_onSave_displayNoTitleValidationSnackbar() = runBlocking {
		val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
		dataBindingIdlingResource.monitorActivity(activityScenario)

		onView(withId(R.id.addReminderFAB)).perform(click())
		onView(withId(R.id.saveReminder)).perform(click())
		onView(withId(com.google.android.material.R.id.snackbar_text))
			.check(matches(withText(R.string.err_enter_title)))

		activityScenario.close()
	}

	@Test
	fun invalidReminder_onSave_displayNoLocationValidationSnackbar() = runBlocking {
		val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
		dataBindingIdlingResource.monitorActivity(activityScenario)

		onView(withId(R.id.addReminderFAB)).perform(click())
		onView(withId(R.id.reminderTitle)).perform(typeText("Buy fruit"))
		closeSoftKeyboard()
		onView(withId(R.id.saveReminder)).perform(click())

		onView(withId(com.google.android.material.R.id.snackbar_text))
			.check(matches(withText(R.string.err_select_location)))

		activityScenario.close()
	}



	@Test
	fun selectLocationToast_isDisplayedInSelectReminderLocationScreen() {
		val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
		dataBindingIdlingResource.monitorActivity(activityScenario)

		onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
		onView(withId(R.id.addReminderFAB)).perform(click())
		onView(withId(R.id.selectLocation)).perform(click())
		onView(withId(R.id.save_button)).perform(click())

		assert(getActivity(activityScenario) != null)
		getActivity(activityScenario)?.run {
			onView(withText(R.string.select_poi))
				.inRoot(withDecorView(not(this.window.decorView)))
				.check(matches(isDisplayed()))
		}


		activityScenario.close()
	}

	private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
		var activity: Activity? = null
		activityScenario.onActivity {
			activity = it
		}
		return activity
	}

	@Test
	fun selectLocation_doubleBackButton_displayRemindersListScreen() {
		val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
		dataBindingIdlingResource.monitorActivity(activityScenario)

		onView(withId(R.id.addReminderFAB)).perform(click())
		onView(withId(R.id.selectLocation)).perform(click())
		pressBack()
		pressBack()

		onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

		activityScenario.close()
	}
}
