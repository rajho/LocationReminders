package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.collection.IsCollectionWithSize
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

	@get:Rule
	var instantTaskExecutorRule = InstantTaskExecutorRule()

	private lateinit var localRepository: RemindersLocalRepository
	private lateinit var database: RemindersDatabase

	@Before
	fun setup() {
		database = Room.inMemoryDatabaseBuilder(
			ApplicationProvider.getApplicationContext(),
			RemindersDatabase::class.java
		)
			.allowMainThreadQueries()
			.build()

		localRepository = RemindersLocalRepository(
			database.reminderDao(),
			Dispatchers.Main
		)
	}

	@After
	fun cleanUp() {
		database.close()
	}

	@Test
	fun saveReminder_retrieveReminder() = runBlocking {
		val reminder = ReminderDTO("title", "description", "location", 0.0, 0.0)
		localRepository.saveReminder(reminder)

		val result = localRepository.getReminder(reminder.id)

		result as Result.Success
		MatcherAssert.assertThat(result, Matchers.notNullValue())
		MatcherAssert.assertThat(result.data.id, Matchers.`is`(reminder.id))
		MatcherAssert.assertThat(result.data.title, Matchers.`is`(reminder.title))
		MatcherAssert.assertThat(result.data.description, Matchers.`is`(reminder.description))
		MatcherAssert.assertThat(result.data.location, Matchers.`is`(reminder.location))
		MatcherAssert.assertThat(result.data.latitude, Matchers.`is`(reminder.latitude))
		MatcherAssert.assertThat(result.data.longitude, Matchers.`is`(reminder.longitude))
	}

	@Test
	fun retrieveNonExistentReminder() = runBlocking {
		val result = localRepository.getReminder("0")

		result as Result.Error
		MatcherAssert.assertThat(result, Matchers.notNullValue())
		MatcherAssert.assertThat(result.message, Matchers.`is`("Reminder not found!"))
	}

	@Test
	fun saveReminders_getReminders() = runBlocking {
		insertReminders()

		val resultList = localRepository.getReminders()

		resultList as Result.Success
		MatcherAssert.assertThat(resultList.data, Matchers.notNullValue())
		MatcherAssert.assertThat(resultList.data, Matchers.`is`(IsCollectionWithSize.hasSize(3)))
	}

	private suspend fun insertReminders() {
		val reminder1 = ReminderDTO("title1", "description1", "location1", 0.0, 0.0)
		val reminder2 = ReminderDTO("title2", "description2", "location2", 0.0, 0.0)
		val reminder3 = ReminderDTO("title3", "description3", "location3", 0.0, 0.0)

		localRepository.saveReminder(reminder1)
		localRepository.saveReminder(reminder2)
		localRepository.saveReminder(reminder3)
	}




}