package com.dzworks.locator42.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dzworks.locator42.R
import com.dzworks.locator42.databinding.FragmentLocatorBinding
import com.dzworks.locator42.formatString
import com.google.android.gms.location.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import com.google.android.gms.location.FusedLocationProviderClient
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils

@AndroidEntryPoint
@SuppressLint("MissingPermission")
class LocatorFragment : Fragment(), EasyPermissions.PermissionCallbacks {

    lateinit var binding: FragmentLocatorBinding

    val viewModel: LocatorViewModel by viewModels()

    var map: MapboxMap? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null

    companion object {
        const val RC_LOCATION_PERM = 122
        const val  SOURCE_ID = "SOURCE_ID"
        const val  ICON_ID = "ICON_ID"
        const val  LAYER_ID = "LAYER_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        buildLocationRequest()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Mapbox.getInstance(requireActivity(), getString(R.string.mapbox_access_token))

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_locator, container, false)

        binding.mapView.onCreate(savedInstanceState)

        loadMap()

        binding.btnNav.setOnClickListener {
            getLastLocation()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }


    private fun loadMap(){
        binding.mapView.getMapAsync { mapboxMap ->
            map = mapboxMap
            updateMap(-1.2921, 36.8219)

            getLocationPermissions()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(requireContext(), "Please give location permissions", Toast.LENGTH_SHORT).show()
    }


    @AfterPermissionGranted(RC_LOCATION_PERM)
    private fun getLocationPermissions() {
        if (EasyPermissions.hasPermissions(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            EasyPermissions.requestPermissions(this, "Please grant Location Permissions",
                RC_LOCATION_PERM, Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest?.interval = 100
        locationRequest?.fastestInterval = 100
        locationRequest?.smallestDisplacement = 1f

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val latitude = (location.latitude)
                    val longitude = (location.longitude)

                    viewModel.longitude.value = "Longitude : ${longitude.formatString(3)}"
                    viewModel.latitude.value = "Latitude : ${latitude.formatString(3)}"

                    if(map != null)
                       updateMap(latitude, longitude)
                }
            }
        }
    }


    private fun getLastLocation(){
            fusedLocationClient.lastLocation.addOnCompleteListener {
                if(it.isSuccessful){
                    val location = it.result

                    val latitude = (location.latitude)
                    val longitude = (location.longitude)

                    viewModel.longitude.value = "Longitude : ${longitude.formatString(3)}"
                    viewModel.latitude.value = "Latitude : ${latitude.formatString(3)}"

                    if(map != null)
                       updateMap(latitude, longitude)
                }
            }
    }


    fun updateMap(lat: Double, lon: Double){
        val symbolLayers = ArrayList<Feature>()
        symbolLayers.add(Feature.fromGeometry(Point.fromLngLat(lon, lat)))
        map?.setStyle(
            Style.Builder().fromUri(Style.MAPBOX_STREETS)
                .withImage(ICON_ID, BitmapUtils
                    .getBitmapFromDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_mapbox_marker_icon_blue))!!)
                .withSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(symbolLayers)))
                .withLayer(
                    SymbolLayer(LAYER_ID, SOURCE_ID)
                    .withProperties
                        (iconImage(ICON_ID), iconSize(1.0f), iconAllowOverlap(true), iconIgnorePlacement(true)))
        )
        {
            map?.cameraPosition = CameraPosition.Builder()
                .target(LatLng(lat, lon))
                .zoom(16.0)
                .tilt(60.0)
                .build()
        }
    }

}