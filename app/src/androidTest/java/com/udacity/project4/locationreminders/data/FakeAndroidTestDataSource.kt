package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result


class FakeAndroidTestDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

	override suspend fun getReminders(): Result<List<ReminderDTO>> {
		reminders?.let { return Result.Success(ArrayList(it)) }
		return Result.Error(
			"Reminders not found"
		)
	}

	override suspend fun saveReminder(reminder: ReminderDTO) {
		reminders?.add(reminder)
	}

	override suspend fun getReminder(id: String): Result<ReminderDTO> {
		reminders?.let { r ->
			r.firstOrNull { it.id == id }?.also { foundReminder ->
				return Result.Success(foundReminder)
			}
		}
		return Result.Error(
			"Reminder not found"
		)
	}

	override suspend fun deleteAllReminders() {
		reminders?.clear()
	}


}