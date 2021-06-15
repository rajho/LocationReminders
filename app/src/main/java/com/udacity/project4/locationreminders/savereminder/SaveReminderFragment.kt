package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
	//Get the view model this time as a single to be shared with the another fragment
	override val _viewModel: SaveReminderViewModel by inject()
	private lateinit var binding: FragmentSaveReminderBinding
	private lateinit var geofencingClient: GeofencingClient
	private lateinit var reminderDataItem: ReminderDataItem

	private val runningQOrLater =
		android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

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
		binding =
			DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
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
			checkLocationPermissionsAndSaveReminder()
		}
	}

	override fun onStart() {
		super.onStart()

		_viewModel.addGeofenceEvent.observe(this, Observer {
			if (it) {
				reminderDataItem.apply {
					if (latitude != null && longitude != null) {
						addGeofenceForPoi(
							id,
							latitude!!,
							longitude!!
						)
					}
				}
			}
		})
	}

	private fun checkLocationPermissionsAndSaveReminder() {
		if (hasPermissionsApproved()) {
			checkDeviceLocationEnabledAndSaveReminder()
		} else {
			requestLocationPermission()
		}
	}

	@TargetApi(29)
	private fun hasPermissionsApproved(): Boolean {
		context?.run {
			val foregroundLocationApproved = (PackageManager.PERMISSION_GRANTED ==
				ActivityCompat.checkSelfPermission(
					this,
					Manifest.permission.ACCESS_FINE_LOCATION
				))
			val backgroundPermissionApproved =
				if (runningQOrLater) {
					PackageManager.PERMISSION_GRANTED ==
						ActivityCompat.checkSelfPermission(
							this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
						)
				} else {
					true
				}

			return foregroundLocationApproved && backgroundPermissionApproved
		}

		return false
	}

	@TargetApi(29)
	private fun requestLocationPermission() {
		if (hasPermissionsApproved()) return

		var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
		val resultCode = when {
			runningQOrLater -> {
				permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
				REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
			}
			else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
		}
		requestPermissions(permissionsArray, resultCode)

	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray
	) {
		val fineLocationDenied = grantResults[FOREGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED

		val backgroundLocationDenied = requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
			grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED

		if (grantResults.isEmpty() || backgroundLocationDenied || fineLocationDenied) {
			Snackbar
				.make(
					binding.saveReminder,
					R.string.background_permission_denied_explanation,
					Snackbar.LENGTH_INDEFINITE
				)
				.setAction(R.string.settings) {
					startActivity(Intent().apply {
						action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
						data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
						flags = Intent.FLAG_ACTIVITY_NEW_TASK
					})
				}
				.show()
		} else {
			checkDeviceLocationEnabledAndSaveReminder()
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
			checkDeviceLocationEnabledAndSaveReminder(false)
		}
	}

	private fun checkDeviceLocationEnabledAndSaveReminder(resolve: Boolean = true) {
		val locationRequest = LocationRequest.create().apply {
			priority = LocationRequest.PRIORITY_LOW_POWER
		}
		val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

		context?.let { ctx ->
			val settingsClient = LocationServices.getSettingsClient(ctx)
			val locationSettingsReponseTask = settingsClient.checkLocationSettings(builder.build())

			// if location was NOT enabled on device
			locationSettingsReponseTask.addOnFailureListener { exception ->
				if (exception is ResolvableApiException && resolve) {
					try {
						startIntentSenderForResult(
							exception.resolution.intentSender,
							REQUEST_TURN_DEVICE_LOCATION_ON,
							null,
							0,
							0,
							0,
							null
						)
					} catch (sendEx: IntentSender.SendIntentException) {
						Log.d(
							SaveReminderFragment.TAG,
							"Error getting location settings resolution: " + sendEx.message
						)
					}
				} else {
					Snackbar
						.make(
							binding.saveReminder,
							R.string.location_required_error,
							Snackbar.LENGTH_INDEFINITE
						)
						.setAction(android.R.string.ok) {
							checkDeviceLocationEnabledAndSaveReminder()
						}.show()
				}
			}

			// if location was enabled on device
			locationSettingsReponseTask.addOnCompleteListener {


				if (it.isSuccessful) {
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

					if (_viewModel.validateEnteredData(reminderDataItem)) {
						_viewModel.addGeofenceEvent.value = true
					}
				}
			}
		}
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
					_viewModel.validateAndSaveReminder(reminderDataItem)
				}

				addOnFailureListener {
					_viewModel.addGeofenceEvent.value = false
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
		private val TAG = SaveReminderFragment::class.java.simpleName
		internal const val ACTION_GEOFENCE_EVENT =
			"""SaveReminderFragment.Project4.action.ACTION_GEOFENCE_EVENT"""

	}
}

const val GEOFENCE_RADIUS_IN_METERS = 100f
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 11
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 43
private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 44
private const val FOREGROUND_LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val TAG = "SaveReminderFragment"
