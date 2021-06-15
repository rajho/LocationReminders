package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.collection.IsCollectionWithSize
import org.hamcrest.collection.IsEmptyCollection
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

	@get:Rule
	var instantTaskExecutorRule = InstantTaskExecutorRule()

	private lateinit var database: RemindersDatabase

	@Before
	fun initDb() {
		database = Room.inMemoryDatabaseBuilder(
			ApplicationProvider.getApplicationContext(),
			RemindersDatabase::class.java
		).build()
	}

	@After
	fun closeDb() = database.close()

	@Test
	fun insertReminderAndGetById() = runBlockingTest {
		// GIVEN - Insert a task.
		val reminder = ReminderDTO("title", "description", "location", 0.0, 0.0)
		database.reminderDao().saveReminder(reminder)

		// WHEN - Get the task by id from the database.
		val loaded = database.reminderDao().getReminderById(reminder.id)

		// THEN - The loaded data contains the expected values.
		assertThat(loaded, notNullValue())
		assertThat(loaded?.id, `is`(reminder.id))
		assertThat(loaded?.title, `is`(reminder.title))
		assertThat(loaded?.description, `is`(reminder.description))
		assertThat(loaded?.location, `is`(reminder.location))
		assertThat(loaded?.latitude, `is`(reminder.latitude))
		assertThat(loaded?.longitude, `is`(reminder.longitude))
	}

	@Test
	fun insertAndDeleteAllReminders() = runBlockingTest {
		insertReminders()

		val reminders = database.reminderDao().getReminders()
		assertThat(reminders, notNullValue())
		assertThat(reminders, `is`(IsCollectionWithSize.hasSize(3)))

		database.reminderDao().deleteAllReminders()

		val emptyRemindersList = database.reminderDao().getReminders()
		assertThat(emptyRemindersList, notNullValue())
		assertThat(emptyRemindersList.isEmpty(), `is`(true))
	}

	private suspend fun insertReminders() {
		val reminder1 = ReminderDTO("title1", "description1", "location1", 0.0, 0.0)
		val reminder2 = ReminderDTO("title2", "description2", "location2", 0.0, 0.0)
		val reminder3 = ReminderDTO("title3", "description3", "location3", 0.0, 0.0)

		database.reminderDao().saveReminder(reminder1)
		database.reminderDao().saveReminder(reminder2)
		database.reminderDao().saveReminder(reminder3)
	}


}