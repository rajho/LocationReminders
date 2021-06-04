package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
	//Get the view model this time as a single to be shared with the another fragment
	override val _viewModel: SaveReminderViewModel by inject()
	private lateinit var binding: FragmentSaveReminderBinding
	private lateinit var geofencingClient: GeofencingClient
	private lateinit var reminderDataItem: ReminderDataItem

	private val geofencePendingIntent: PendingIntent? by lazy {
		context?.run {
			val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
			intent.action = ACTION_GEOFENCE_EVENT
			PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
		binding.viewModel = _viewModel

		setDisplayHomeAsUpEnabled(true)

		activity?.let { geofencingClient = LocationServices.getGeofencingClient(it) }

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.lifecycleOwner = this
		binding.selectLocation.setOnClickListener {
			//            Navigate to another fragment to get the user location
			_viewModel.navigationCommand.value =
				NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
		}

		binding.saveReminder.setOnClickListener {
			val title = _viewModel.reminderTitle.value
			val description = _viewModel.reminderDescription.value
			val location = _viewModel.reminderSelectedLocationStr.value
			val latitude = _viewModel.latitude.value
			val longitude = _viewModel.longitude.value
//
			reminderDataItem = ReminderDataItem(
				title,
				description,
				location,
				latitude,
				longitude
			)

			_viewModel.validateAndSaveReminder(reminderDataItem)
		}
	}

	override fun onStart() {
		super.onStart()

		_viewModel.addGeofenceEvent.observe(this, Observer {
			reminderDataItem.apply {
				if (latitude != null && longitude != null) {
					addGeofenceForPoi(
						id,
						latitude!!,
						longitude!!
					)
				}
			}
		})
	}

	@SuppressLint("MissingPermission")
	private fun addGeofenceForPoi(requestId: String, latitude: Double, longitude: Double) {
		val geofence = Geofence.Builder()
			.setRequestId(requestId)
			.setCircularRegion(latitude, longitude, GEOFENCE_RADIUS_IN_METERS)
			.setExpirationDuration(Geofence.NEVER_EXPIRE)
			.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
			.build()


		val geofencingRequest = GeofencingRequest.Builder()
			.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
			.addGeofence(geofence)
			.build()


		geofencePendingIntent?.let { pendingIntent ->
			geofencingClient.addGeofences(geofencingRequest, pendingIntent)?.run {
				addOnSuccessListener {
					Log.d(TAG, "Geofence added " + geofence.requestId)
				}

				addOnFailureListener {
					context?.let { ctx ->
						Toast.makeText(ctx, R.string.geofences_not_added, Toast.LENGTH_SHORT).show()
					}

					it.message?.let { m -> Log.w(TAG, m) }
				}
			}
		}


	}

	override fun onDestroy() {
		super.onDestroy()
		//make sure to clear the view model after destroy, as it's a single view model.
		_viewModel.onClear()
	}

	companion object {
		internal const val ACTION_GEOFENCE_EVENT =
			"""SaveReminderFragment.Project4.action.ACTION_GEOFENCE_EVENT"""
	}
}

const val GEOFENCE_RADIUS_IN_METERS = 100f
private const val TAG = "SaveReminderFragment"
