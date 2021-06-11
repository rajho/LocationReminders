package com.udacity.project4.locationreminders.reminderslist

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.FakeAndroidTestDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

	private lateinit var repository: ReminderDataSource

	@Before
	fun initRepository() {
		repository = FakeAndroidTestDataSource()

	}

//    TODO: test the navigation of the fragments.
//    TODO: test the displayed data on the UI.
//    TODO: add testing for the error messages.
}