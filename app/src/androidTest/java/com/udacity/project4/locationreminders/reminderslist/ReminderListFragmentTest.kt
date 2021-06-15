package com.udacity.project4.locationreminders.reminderslist

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.map
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.authentication.FirebaseUserLiveData
import com.udacity.project4.base.DataBindingViewHolder
import com.udacity.project4.locationreminders.data.FakeAndroidTestDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

	private lateinit var repository: ReminderDataSource

	@Before
	fun initRepository() {
		stopKoin()

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
			single<ReminderDataSource> { FakeAndroidTestDataSource() }
		}
		//declare a new koin module
		startKoin {
			androidContext(ApplicationProvider.getApplicationContext())
			modules(listOf(myModule))
		}

		repository = get()
	}

	@After
	fun cleanUp() {
		runBlocking {
			repository.deleteAllReminders()
		}
	}

	@Test
	fun reminder_displayedInList() = runBlockingTest {
		val reminder = ReminderDTO("title", "description", "location", 0.0, 0.0)
		repository.saveReminder(reminder)

		launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
		onView(withId(R.id.reminderssRecyclerView)).perform(
			RecyclerViewActions.scrollTo<DataBindingViewHolder<ReminderDataItem>>(
				hasDescendant(withText("title"))
			)
		)
	}

	@Test
	fun noData_displayedInUI() {
		launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
		onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(isDisplayed()))
	}

	@Test
	fun clickAddReminder_navigateToSaveReminderFragment() {
		val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
		val navController = mock(NavController::class.java)

		scenario.onFragment{
			Navigation.setViewNavController(it.view!!, navController)
		}

		onView(withId(R.id.addReminderFAB)).perform(click())

		verify(navController).navigate(
			ReminderListFragmentDirections.toSaveReminder()
		)
	}

	@Test
	fun error_displayedInSnackbar() {
		(repository as FakeAndroidTestDataSource).setReturnError(true)
		launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)

		onView(withId(com.google.android.material.R.id.snackbar_text))
			.check(matches(withText("Test exception")))
	}
}