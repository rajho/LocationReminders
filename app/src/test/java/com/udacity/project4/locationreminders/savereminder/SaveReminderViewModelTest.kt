package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SaveReminderViewModelTest {

	private lateinit var saveReminderViewModel: SaveReminderViewModel
	private lateinit var remindersRepository: FakeDataSource
	private lateinit var reminderDataItem: ReminderDataItem

	@get:Rule
	var instantExecutorRule = InstantTaskExecutorRule()

	@get:Rule
	var mainCoroutineRule = MainCoroutineRule()

	@Before
	fun setupSaveReminderViewModel() {
		remindersRepository = FakeDataSource()
		saveReminderViewModel = SaveReminderViewModel(remindersRepository)
		reminderDataItem = ReminderDataItem(
			"Buy toilet paper",
			"Supermarket",
			"Plaza Vea",
			-12.0895,
			-77.0753
		)
	}

	@Test
	fun validateAndSaveReminder_noTitleReminder_displaysError() {
		reminderDataItem.title = null

	    saveReminderViewModel.validateAndSaveReminder(reminderDataItem)

	    assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
	}

	@Test
	fun validateAndSaveReminder_noLocationReminder_displaysError() {
		reminderDataItem.location = null

		saveReminderViewModel.validateAndSaveReminder(reminderDataItem)

		assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
	}

	@Test
	fun validateAndSaveReminder_savesNewReminder() {
		saveReminderViewModel.validateAndSaveReminder(reminderDataItem)

		assertThat(
			remindersRepository.reminders?.firstOrNull { r -> r.id == reminderDataItem.id },
			`is`(notNullValue())
		)
	}

	@Test
	fun check_loading() {
		mainCoroutineRule.pauseDispatcher()
		saveReminderViewModel.validateAndSaveReminder(reminderDataItem)

		assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

		mainCoroutineRule.resumeDispatcher()
		assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
		assertThat(saveReminderViewModel.addGeofenceEvent.getOrAwaitValue(), `is`(false))
		assertThat(saveReminderViewModel.showToastInt.getOrAwaitValue(), `is`(R.string.reminder_saved))
		assertThat(saveReminderViewModel.navigationCommand.getOrAwaitValue(), `is`(NavigationCommand.Back))
	}

	@Test
	fun onClear_resetsLiveDataObjects() {
		saveReminderViewModel.apply {
			reminderTitle.value = reminderDataItem.title
			reminderDescription.value = reminderDataItem.description
			reminderSelectedLocationStr.value = reminderDataItem.location
			selectedPOI.value = PointOfInterest(LatLng(reminderDataItem.latitude ?: 0.0, reminderDataItem.longitude ?: 0.0), reminderDataItem.location, reminderDataItem.location)
			latitude.value = reminderDataItem.latitude
			longitude.value = reminderDataItem.longitude
		}

		saveReminderViewModel.onClear()

		assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
		assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
		assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
		assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(nullValue()))
		assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
		assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
	}

}