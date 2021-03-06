package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setTitle
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

	//Use Koin to get the view model of the SaveReminder
	override val _viewModel: SaveReminderViewModel by inject()
	private lateinit var binding: FragmentSelectLocationBinding
	private lateinit var map: GoogleMap
	private lateinit var fusedLocationProvideClient: FusedLocationProviderClient
	private var selectedPoi: PointOfInterest? = null

	private val defaultLocation = LatLng(-12.067986, -77.041844)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {

		binding = DataBindingUtil.inflate(
			inflater,
			R.layout.fragment_select_location,
			container,
			false
		)
		binding.viewModel = _viewModel
		binding.lifecycleOwner = this

		setHasOptionsMenu(true)
		setTitle(getString(R.string.select_location))
		// setDisplayHomeAsUpEnabled(true)

		context?.let {
			fusedLocationProvideClient = LocationServices.getFusedLocationProviderClient(it)
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
		mapFragment.getMapAsync(this)

		binding.saveButton.setOnClickListener {
			if (selectedPoi == null) {
				context?.let { ctx ->
					Toast.makeText(ctx, R.string.select_poi, Toast.LENGTH_LONG).show()
				}
				return@setOnClickListener
			}

			onLocationSelected()
		}

		return binding.root
	}

	private fun onLocationSelected() {
		_viewModel.selectedPOI.value = selectedPoi
		_viewModel.reminderSelectedLocationStr.value = selectedPoi?.name
		_viewModel.latitude.value = selectedPoi?.latLng?.latitude
		_viewModel.longitude.value = selectedPoi?.latLng?.longitude
		findNavController().navigateUp()
	}


	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.map_options, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.normal_map -> {
			map.mapType = GoogleMap.MAP_TYPE_NORMAL
			true
		}
		R.id.hybrid_map -> {
			map.mapType = GoogleMap.MAP_TYPE_HYBRID
			true
		}
		R.id.satellite_map -> {
			map.mapType = GoogleMap.MAP_TYPE_SATELLITE
			true
		}
		R.id.terrain_map -> {
			map.mapType = GoogleMap.MAP_TYPE_TERRAIN
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onMapReady(googleMap: GoogleMap) {
		map = googleMap
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))

		setPoiClick()
		setMapStyle()
        setMapLongClick()
		checkLocationPermissionsAndEnableMyLocation()
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray
	) {
		val fineLocationDenied =
			grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED

		if (grantResults.isEmpty() || fineLocationDenied) {
			Snackbar
				.make(
					binding.saveButton,
					R.string.permission_denied_explanation,
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
			map.isMyLocationEnabled = true
			checkDeviceLocationEnabledAndEnableLocation()
		}
	}

	private fun setPoiClick() {
		map.setOnPoiClickListener { poi ->
			map.clear()

			selectedPoi = poi

			val poiMarker = map.addMarker(
				MarkerOptions()
					.position(poi.latLng)
					.title(poi.name)
			)
			poiMarker?.showInfoWindow()

		}
	}

  private fun setMapLongClick() {
    map.setOnMapLongClickListener { latLng ->
      map.clear()

      val snippet = String.format(
        Locale.getDefault(),
        "%1$.3f, %2$.3f",
        latLng.latitude,
        latLng.longitude
      )

      selectedPoi = PointOfInterest(latLng, snippet, getString(R.string.dropped_pin))
      map.addMarker(
        MarkerOptions()
          .position(latLng)
          .title(getString(R.string.dropped_pin))
          .snippet(snippet)
      )
    }
  }

	private fun setMapStyle() {
		try {
			context?.run {
				map.setMapStyle(
					MapStyleOptions.loadRawResourceStyle(
						this,
						R.raw.map_style
					)
				)
			}
		} catch (e: Resources.NotFoundException) {
			Log.e(TAG, "Can't find style.Error: ", e)
		}
	}


	private fun checkLocationPermissionsAndEnableMyLocation() {
		if (isLocationPermissionGranted()) {
            map.isMyLocationEnabled = true
			checkDeviceLocationEnabledAndEnableLocation()
		} else {
			requestLocationPermission()
		}
	}

	@SuppressLint("MissingPermission")
	private fun moveCameraAndEnableMyLocation() {
		map.isMyLocationEnabled = true
		val locationResult = fusedLocationProvideClient.lastLocation
		locationResult.addOnCompleteListener { task ->
			if (task.isSuccessful) {
				val lastKnownLocation = task.result
				lastKnownLocation?.run {
					map.moveCamera(
						CameraUpdateFactory.newLatLngZoom(
							LatLng(this.latitude, this.longitude),
							DEFAULT_ZOOM.toFloat()
						)
					)
				}
			}
		}
	}

	@TargetApi(29)
	private fun isLocationPermissionGranted(): Boolean {
		context?.let {
			val fineLocationPermission =
				ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)

			return fineLocationPermission == PackageManager.PERMISSION_GRANTED
		}

		return false
	}

	@TargetApi(29)
	private fun requestLocationPermission() {
		if (isLocationPermissionGranted()) return

		val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
		val resultCode = REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE

		requestPermissions(permissionsArray, resultCode)
	}

	private fun checkDeviceLocationEnabledAndEnableLocation(resolve: Boolean = true) {
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
						Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
					}
				} else {
					Snackbar
						.make(
							binding.saveButton,
							R.string.location_required_error,
							Snackbar.LENGTH_INDEFINITE
						)
						.setAction(android.R.string.ok) {
							checkDeviceLocationEnabledAndEnableLocation()
                            it.visibility = View.GONE
						}.show()
				}
			}

			// if location was enabled on device
			locationSettingsReponseTask.addOnCompleteListener {
				if (it.isSuccessful) {
					moveCameraAndEnableMyLocation()
				}
			}
		}
	}


	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {

			// Setting current location right after granting Location Permission
			if (Activity.RESULT_OK == resultCode) {
				val locationCallback = object : LocationCallback() {
					override fun onLocationResult(locationResult: LocationResult) {
						map.isMyLocationEnabled = true
						map.moveCamera(
							CameraUpdateFactory.newLatLngZoom(
								LatLng(
									locationResult.lastLocation.latitude,
									locationResult.lastLocation.longitude
								),
								DEFAULT_ZOOM.toFloat()
							)
						)
					}
				}

				with(LocationRequest()) {
					priority = LocationRequest.PRIORITY_HIGH_ACCURACY
					interval = 0
					fastestInterval = 0
					numUpdates = 1
					fusedLocationProvideClient.requestLocationUpdates(
						this,
						locationCallback,
						Looper.myLooper()
					)
				}
			} else {
				checkDeviceLocationEnabledAndEnableLocation(false)
			}
		}
	}

	companion object {
		private val TAG = SelectLocationFragment::class.java.simpleName
		private const val DEFAULT_ZOOM = 16
	}
}

private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 45
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 11
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
