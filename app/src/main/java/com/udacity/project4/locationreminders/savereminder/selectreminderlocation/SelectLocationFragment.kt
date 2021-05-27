package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

  //Use Koin to get the view model of the SaveReminder
  override val _viewModel: SaveReminderViewModel by inject()
  private lateinit var binding: FragmentSelectLocationBinding
  private lateinit var map: GoogleMap

  private val runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

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
    // setDisplayHomeAsUpEnabled(true)


    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)


//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
    onLocationSelected()

    return binding.root
  }

  private fun onLocationSelected() {
    //        TODO: When the user confirms on the selected location,
    //         send back the selected location details to the view model
    //         and navigate back to the previous fragment to save the reminder and add the geofence
  }


  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.map_options, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
    // TODO: Change the map type based on the user's selection.
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

    val latitude = -12.067986
    val longitude = -77.041844
    val zoomLevel = 16f

    val homeLatLng = LatLng(latitude, longitude)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))

    setPoiClick()
    enableMyLocation()

    // Check fine and background location permissions
    // If granted
    //    TODO check Location on device
    //    If granted
    //      Enable mylocation in GoogleMap
    //      move camera to my location in GoogleMap
    //    If NOT granted
    //      Request resolution for location
    //      OR Notify the user that location services must be enabled
    // If NOT granted
    //    request fine and background location permissions

    // On Location Permissions Result
    // If granted
    //    TODO check Location on device
    //    If granted
    //      Enable mylocation in GoogleMap
    //      move camera to my location in GoogleMap
    //    If NOT granted
    //      Request resolution for location
    //      OR Notify the user that location services must be enabled
    // If NOT granted
    //    Notify user he needs to grant location permission
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    if (requestCode == REQUEST_LOCATION_PERMISSION) {
      if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
        enableMyLocation()
      } else {
        Snackbar
          .make(binding.saveButton, R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE)
          .setAction(R.string.settings) {
            startActivity(Intent().apply {
              action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
              data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            })
          }
          .show()
      }
    }
  }

  private fun setPoiClick() {
    map.setOnPoiClickListener { poi ->
      map.clear()

      val poiMarker = map.addMarker(
        MarkerOptions()
          .position(poi.latLng)
          .title(poi.name)
      )

      poiMarker?.showInfoWindow()
    }
  }

  @SuppressLint("MissingPermission")
  private fun enableMyLocation() {
    if (isPermissionGranted()) {
      map.isMyLocationEnabled = true
    } else {
      requestPermissions(
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        REQUEST_LOCATION_PERMISSION
      )
    }
  }

  private fun isPermissionGranted(): Boolean {
    context?.let {
      val locationPermission =
        ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
      return locationPermission == PackageManager.PERMISSION_GRANTED
    }

    return false
  }

}

private const val REQUEST_LOCATION_PERMISSION = 1
