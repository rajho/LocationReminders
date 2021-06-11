package com.udacity.project4.locationreminders.reminderslist


import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.collection.IsCollectionWithSize
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

	private lateinit var remindersListViewModel: RemindersListViewModel
	private lateinit var remindersDataSource: FakeDataSource

	@get:Rule
	var instantExecutorRule = InstantTaskExecutorRule()

	@get:Rule
	var mainCoroutineRule = MainCoroutineRule()

	@Before
	fun setupSaveReminderViewModel() {
		remindersDataSource = FakeDataSource()

		FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
		remindersListViewModel = RemindersListViewModel(remindersDataSource)
	}

	@After
	fun tearDown() {
		stopKoin()
	}

	@Test
	fun loadReminders_loading() {
		mainCoroutineRule.pauseDispatcher()
		remindersListViewModel.loadReminders()

		assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

		mainCoroutineRule.resumeDispatcher()
		assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
	}

	@Test
	fun loadReminders_showDataUpdated() {
		val reminder1 = ReminderDTO("Title1", "Description1",  "Location1", 0.0, 0.0)
		val reminder2 = ReminderDTO("Title2", "Description2",  "Location2", 0.0, 0.0)
		val reminder3 = ReminderDTO("Title3", "Description3",  "Location3", 0.0, 0.0)
		remindersDataSource.addReminders(reminder1, reminder2, reminder3)

		remindersListViewModel.loadReminders()

		assertThat(remindersListViewModel.remindersList.getOrAwaitValue(), `is`(IsCollectionWithSize.hasSize(3)))
	}

	@Test
	fun loadReminders_showNoData() {
		remindersListViewModel.loadReminders()

		assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
	}

	@Test
	fun loadReminders_callErrorToDisplay() = mainCoroutineRule.runBlockingTest {
		remindersDataSource.setReturnError(true)
		remindersListViewModel.loadReminders()

		assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`(notNullValue()))
	}

}