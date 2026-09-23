package com.example.medicare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.api.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.chip.Chip
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap as MapLibreMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.annotations.IconFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class PharmacyActivity : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapLibreMap: MapLibreMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var recyclerPharmacies: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var txtHeader: TextView
    private lateinit var txtSubtitle: TextView
    private lateinit var inputSearch: EditText

    // Selected place category filter
    private var selectedCategory = "healthcare.pharmacy"
    private var selectedCategoryName = "Pharmacies"

    // Cache of markers to link selection
    private val markerMap = HashMap<String, Marker>()
    private val placeItemsList = ArrayList<PharmacyItem>()

    // Current location coordinates
    private var currentLatLng: LatLng? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            if (::mapLibreMap.isInitialized) {
                mapLibreMap.style?.let { enableLocationComponent(it) }
            }
            setupLocationEnabledMap(zoomToUser = true)
        } else {
            Toast.makeText(this, "Location permission is required to automatically locate healthcare facilities.", Toast.LENGTH_LONG).show()
            showPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Mapbox before setContentView
        Mapbox.getInstance(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pharmacy)

        // Setup custom bottom navigation
        NavigationHelper.setupNavigation(this, R.id.tab_pharmacy)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Bind layout views
        recyclerPharmacies = findViewById(R.id.recycler_pharmacies)
        recyclerPharmacies.layoutManager = LinearLayoutManager(this)
        
        progressLoading = findViewById(R.id.progress_map_loading)
        txtHeader = findViewById(R.id.txt_header_pharmacies)
        txtSubtitle = findViewById(R.id.txt_subtitle_pharmacies)
        inputSearch = findViewById(R.id.input_search_query)

        // Setup Map View
        mapView = findViewById<MapView>(R.id.map)
        mapView.onCreate(savedInstanceState)
        
        // Touch interception listener for nested scrolling support
        mapView.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                    view.parent.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        mapView.getMapAsync { map ->
            mapLibreMap = map
            
            // Configure controls
            map.uiSettings.isZoomGesturesEnabled = true

            map.setOnMarkerClickListener { marker ->
                // Dismiss any default info windows to prevent map clutter
                marker.hideInfoWindow()
                val markerPos = marker.position
                mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerPos, 16.0))

                val matchedItem = placeItemsList.firstOrNull {
                    Math.abs(it.latitude - markerPos.latitude) < 0.0002 &&
                    Math.abs(it.longitude - markerPos.longitude) < 0.0002
                } ?: placeItemsList.firstOrNull { it.name == marker.title }

                if (matchedItem != null) {
                    val index = placeItemsList.indexOf(matchedItem)
                    if (index != -1) {
                        recyclerPharmacies.smoothScrollToPosition(index)
                    }
                    showPlaceDetailsDialog(matchedItem)
                }
                true
            }

            // Load Geoapify Osm-Bright Map Style
            val styleUrl = "https://maps.geoapify.com/v1/styles/osm-bright/style.json?apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
            map.setStyle(styleUrl) { style ->
                enableLocationComponent(style)
                checkPermissionsAndFetchLocation(zoomToUser = true)
            }
        }

        // Category filter setup
        setupCategoryChips()

        // Location target button
        findViewById<View>(R.id.btn_my_location)?.setOnClickListener {
            if (::mapLibreMap.isInitialized) {
                mapLibreMap.style?.let { enableLocationComponent(it) }
            }
            checkPermissionsAndFetchLocation(zoomToUser = true)
        }

        // Manual Refresh button
        findViewById<View>(R.id.btn_view_map)?.setOnClickListener {
            performNearbySearch()
        }

        // Search edit text enter key listener
        inputSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performNearbySearch(inputSearch.text.toString().trim())
                true
            } else {
                false
            }
        }

        // Search submit icon
        findViewById<View>(R.id.btn_filter_settings)?.setOnClickListener {
            performNearbySearch(inputSearch.text.toString().trim())
        }

        // Notification bell click trigger
        findViewById<ImageView>(R.id.btn_notification)?.setOnClickListener {
            NotificationHelper.show(this)
        }

        // Back button navigation
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val locationComponent = mapLibreMap.locationComponent
                val locationComponentActivationOptions = LocationComponentActivationOptions
                    .builder(this, loadedMapStyle)
                    .useDefaultLocationEngine(true)
                    .build()
                locationComponent.activateLocationComponent(locationComponentActivationOptions)
                locationComponent.isLocationComponentEnabled = true
                locationComponent.cameraMode = CameraMode.NONE
                locationComponent.renderMode = RenderMode.COMPASS
            } catch (e: Exception) {
                // If location engine is not available, ignore gracefully
            }
        }
    }

    private fun checkPermissionsAndFetchLocation(zoomToUser: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocationEnabledMap(zoomToUser)
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun setupLocationEnabledMap(zoomToUser: Boolean = true) {
        if (::mapLibreMap.isInitialized) {
            mapLibreMap.style?.let { enableLocationComponent(it) }
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        onLocationAcquired(location, zoomToUser)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                onLocationAcquired(lastLoc, zoomToUser)
                            } else {
                                tryLocationManagerFallback(zoomToUser)
                            }
                        }.addOnFailureListener {
                            tryLocationManagerFallback(zoomToUser)
                        }
                    }
                }.addOnFailureListener {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            onLocationAcquired(lastLoc, zoomToUser)
                        } else {
                            tryLocationManagerFallback(zoomToUser)
                        }
                    }.addOnFailureListener {
                        tryLocationManagerFallback(zoomToUser)
                    }
                }
        } catch (e: SecurityException) {
            tryLocationManagerFallback(zoomToUser)
        }
    }

    private fun tryLocationManagerFallback(zoomToUser: Boolean) {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLoc = gpsLoc ?: netLoc
            if (bestLoc != null) {
                onLocationAcquired(bestLoc, zoomToUser)
            } else {
                fallbackDefaultLocation()
            }
        } catch (e: SecurityException) {
            fallbackDefaultLocation()
        }
    }

    private fun onLocationAcquired(location: Location, zoomToUser: Boolean) {
        currentLatLng = LatLng(location.latitude, location.longitude)
        if (zoomToUser && ::mapLibreMap.isInitialized) {
            mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 15.0))
        }
        performNearbySearch()
    }

    private fun fallbackDefaultLocation() {
        showLocationUnavailableMessage()
    }

    private fun setupCategoryChips() {
        findViewById<Chip>(R.id.chip_pharmacy).setOnClickListener {
            setCategoryFilter("healthcare.pharmacy", "Pharmacies")
        }
        findViewById<Chip>(R.id.chip_hospital).setOnClickListener {
            setCategoryFilter("healthcare.hospital", "Hospitals")
        }
        findViewById<Chip>(R.id.chip_clinic).setOnClickListener {
            setCategoryFilter("healthcare.clinic_or_praxis", "Doctors & Clinics")
        }
        findViewById<Chip>(R.id.chip_emergency).setOnClickListener {
            setCategoryFilter("emergency.ambulance_station,healthcare.hospital", "Emergencies")
        }
        findViewById<Chip>(R.id.chip_lab).setOnClickListener {
            setCategoryFilter("healthcare.clinic_or_praxis.radiology,healthcare.clinic_or_praxis", "Laboratories")
        }
        findViewById<Chip>(R.id.chip_dentist).setOnClickListener {
            setCategoryFilter("healthcare.dentist", "Dentists")
        }
    }

    private fun setCategoryFilter(category: String, name: String) {
        selectedCategory = category
        selectedCategoryName = name
        performNearbySearch()
    }

    private fun performNearbySearch(keyword: String? = null) {
        // Show loading progress
        progressLoading.visibility = View.VISIBLE
        txtHeader.text = "Nearby $selectedCategoryName"
        txtSubtitle.text = "Searching verified facilities..."

        // Search around current user location, or current map center if user panned
        val mapTarget = if (::mapLibreMap.isInitialized) mapLibreMap.cameraPosition?.target else null
        val searchCenter = currentLatLng ?: if (mapTarget != null && mapTarget.latitude != 0.0) mapTarget else null
        if (searchCenter == null) {
            progressLoading.visibility = View.GONE
            showPermissionDeniedMessage()
            return
        }

        val biasStr = "proximity:${searchCenter.longitude},${searchCenter.latitude}"

        GeoapifyClient.getService().getNearbyPlaces(
            categories = selectedCategory,
            filter = null,
            bias = biasStr,
            limit = 50,
            name = if (keyword.isNullOrEmpty()) null else keyword,
            apiKey = BuildConfig.GEOAPIFY_API_KEY
        ).enqueue(object : Callback<GeoapifyPlacesResponse> {
            override fun onResponse(
                call: Call<GeoapifyPlacesResponse>,
                response: Response<GeoapifyPlacesResponse>
            ) {
                progressLoading.visibility = View.GONE
                val body = response.body()

                if (!response.isSuccessful || body == null) {
                    showErrorResults()
                    return
                }

                val features = body.features
                if (features.isEmpty()) {
                    showEmptyResults()
                    return
                }

                txtSubtitle.text = "Found ${features.size} verified facilities near you"

                // Clear previous markers & list now that verified real results arrived
                placeItemsList.clear()
                if (::mapLibreMap.isInitialized) {
                    mapLibreMap.clear()
                }
                markerMap.clear()

                val defaultIcon = IconFactory.getInstance(this@PharmacyActivity).defaultMarker()

                for (feature in features) {
                    val props = feature.properties
                    val geom = feature.geometry
                    val placeId = props.placeId ?: UUID.randomUUID().toString()
                    val fallbackLocality = props.street ?: props.suburb ?: props.city
                    val name = props.name ?: if (!fallbackLocality.isNullOrEmpty()) {
                        when (selectedCategoryName) {
                            "Pharmacies" -> "Pharmacy - $fallbackLocality"
                            "Hospitals" -> "Hospital - $fallbackLocality"
                            "Doctors & Clinics" -> "Clinic - $fallbackLocality"
                            "Emergencies" -> "Emergency Care - $fallbackLocality"
                            "Laboratories" -> "Laboratory - $fallbackLocality"
                            "Dentists" -> "Dental Clinic - $fallbackLocality"
                            else -> "Healthcare - $fallbackLocality"
                        }
                    } else {
                        when (selectedCategoryName) {
                            "Pharmacies" -> "Local Pharmacy"
                            "Hospitals" -> "Hospital / Medical Center"
                            "Doctors & Clinics" -> "Medical Clinic"
                            "Emergencies" -> "Emergency Care"
                            "Laboratories" -> "Medical Laboratory"
                            "Dentists" -> "Dental Clinic"
                            else -> "Healthcare Facility"
                        }
                    }
                    val lon = geom.coordinates[0]
                    val lat = geom.coordinates[1]
                    val address = props.formatted ?: "Address available on map"
                    val phone = props.contact?.phone
                    val website = props.website

                    // Calculate distance locally if null or use API value
                    val distanceMeters = props.distance ?: run {
                        val results = FloatArray(1)
                        Location.distanceBetween(searchCenter.latitude, searchCenter.longitude, lat, lon, results)
                        results[0].toDouble()
                    }

                    // Format distance user-friendly
                    val distStr = if (distanceMeters < 1000) {
                        "${distanceMeters.toInt()}m"
                    } else {
                        String.format(Locale.getDefault(), "%.1f km", distanceMeters / 1000.0)
                    }

                    val cleanAddress = if (address.startsWith(name, ignoreCase = true)) {
                        address.removePrefix(name).trimStart(',', ' ')
                    } else {
                        address
                    }

                    val item = PharmacyItem(
                        placeId = placeId,
                        name = name,
                        rating = "",
                        details = "$distStr • $cleanAddress",
                        latitude = lat,
                        longitude = lon,
                        address = address,
                        phoneNumber = phone,
                        website = website,
                        isOpen = null,
                        isMock = false
                    )
                    placeItemsList.add(item)

                    // Add Map Marker without random hash code snippet
                    if (::mapLibreMap.isInitialized) {
                        val markerOptions = MarkerOptions()
                            .position(LatLng(lat, lon))
                            .title(name)
                            .icon(defaultIcon)

                        val marker = mapLibreMap.addMarker(markerOptions)
                        if (marker != null) {
                            markerMap[placeId] = marker
                        }
                    }
                }

                // Update RecyclerView adapter
                updateResultsAdapter()

                // Adjust camera bounds to display nearest facilities
                if (::mapLibreMap.isInitialized && placeItemsList.isNotEmpty()) {
                    try {
                        val boundsBuilder = com.mapbox.mapboxsdk.geometry.LatLngBounds.Builder()
                        currentLatLng?.let { boundsBuilder.include(it) }
                        for (item in placeItemsList.take(8)) {
                            boundsBuilder.include(LatLng(item.latitude, item.longitude))
                        }
                        val bounds = boundsBuilder.build()
                        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                    } catch (e: Exception) {
                        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(placeItemsList[0].latitude, placeItemsList[0].longitude), 14.0))
                    }
                }
            }

            override fun onFailure(call: Call<GeoapifyPlacesResponse>, t: Throwable) {
                progressLoading.visibility = View.GONE
                showErrorResults()
            }
        })
    }

    private fun showPermissionDeniedMessage() {
        placeItemsList.clear()
        if (::mapLibreMap.isInitialized) {
            mapLibreMap.clear()
        }
        markerMap.clear()

        val item = PharmacyItem(
            placeId = "permission_denied",
            name = "Location Permission Denied",
            rating = "0.0",
            details = "Tap here to open app Settings.",
            latitude = 0.0,
            longitude = 0.0,
            address = "Medicare requires location permission to auto-detect nearby services. Alternatively, pan/drag the map manually and click REFRESH to query places."
        )
        placeItemsList.add(item)
        updateResultsAdapterForStatus(item)
    }

    private fun showLocationUnavailableMessage() {
        placeItemsList.clear()
        if (::mapLibreMap.isInitialized) {
            mapLibreMap.clear()
        }
        markerMap.clear()

        val item = PharmacyItem(
            placeId = "location_unavailable",
            name = "Location Unavailable",
            rating = "0.0",
            details = "Tap here to retry location lookup.",
            latitude = 0.0,
            longitude = 0.0,
            address = "Make sure GPS/Location services are enabled, or pan the map manually and click REFRESH to query places."
        )
        placeItemsList.add(item)
        updateResultsAdapterForStatus(item)
    }

    private fun updateResultsAdapterForStatus(item: PharmacyItem) {
        recyclerPharmacies.adapter = PharmacyAdapter(placeItemsList,
            onItemClick = { clickedItem ->
                if (clickedItem.placeId == "permission_denied") {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show()
                    }
                } else if (clickedItem.placeId == "location_unavailable") {
                    checkPermissionsAndFetchLocation(zoomToUser = true)
                }
            },
            onNavigateClick = {
                openGoogleMapsSearch("$selectedCategoryName near me")
            },
            onCallClick = {}
        )
    }

    private fun showEmptyResults() {
        placeItemsList.clear()
        if (::mapLibreMap.isInitialized) {
            mapLibreMap.clear()
        }
        markerMap.clear()

        val item = PharmacyItem(
            placeId = "empty",
            name = "No $selectedCategoryName Found Nearby",
            rating = "N/A",
            details = "Tap to search directly on Google Maps.",
            latitude = 0.0,
            longitude = 0.0,
            address = "No facilities were found in this area. Tap Navigate to view live listings on Google Maps."
        )
        placeItemsList.add(item)

        recyclerPharmacies.adapter = PharmacyAdapter(placeItemsList,
            onItemClick = {
                openGoogleMapsSearch("$selectedCategoryName near me")
            },
            onNavigateClick = {
                openGoogleMapsSearch("$selectedCategoryName near me")
            },
            onCallClick = {}
        )
    }

    private fun showErrorResults() {
        placeItemsList.clear()
        if (::mapLibreMap.isInitialized) {
            mapLibreMap.clear()
        }
        markerMap.clear()

        val item = PharmacyItem(
            placeId = "error",
            name = "Unable to Query $selectedCategoryName",
            rating = "N/A",
            details = "Tap to search directly on Google Maps.",
            latitude = 0.0,
            longitude = 0.0,
            address = "Could not fetch nearby location markers. Tap to retry or tap Navigate to open Google Maps."
        )
        placeItemsList.add(item)

        recyclerPharmacies.adapter = PharmacyAdapter(placeItemsList,
            onItemClick = { performNearbySearch() },
            onNavigateClick = {
                openGoogleMapsSearch("$selectedCategoryName near me")
            },
            onCallClick = {}
        )
    }

    private fun openGoogleMapsSearch(query: String) {
        val searchUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, searchUri))
        } catch (e: Exception) {
            Toast.makeText(this, "No browser or maps application available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateResultsAdapter() {
        recyclerPharmacies.adapter = PharmacyAdapter(placeItemsList,
            onItemClick = { item ->
                // Center Map Camera
                if (::mapLibreMap.isInitialized && item.latitude != 0.0) {
                    val latLng = LatLng(item.latitude, item.longitude)
                    mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0))
                }

                // Show Details Dialog
                showPlaceDetailsDialog(item)
            },
            onNavigateClick = { item ->
                launchNavigationIntent(item)
            },
            onCallClick = { item ->
                launchCallIntent(item)
            }
        )
    }

    private fun showPlaceDetailsDialog(item: PharmacyItem) {
        val details = StringBuilder()
        details.append("Address: ${item.address}\n\n")
        if (!item.phoneNumber.isNullOrEmpty()) details.append("Phone: ${item.phoneNumber}\n\n")
        if (!item.website.isNullOrEmpty()) details.append("Website: ${item.website}\n\n")

        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setMessage(details.toString())
            .setPositiveButton("Close", null)
            .apply {
                if (!item.phoneNumber.isNullOrEmpty()) {
                    setNeutralButton("Call") { _, _ ->
                        launchCallIntent(item)
                    }
                }
                setNegativeButton("Directions") { _, _ ->
                    launchNavigationIntent(item)
                }
            }
            .show()
    }

    private fun launchNavigationIntent(item: PharmacyItem) {
        if (item.placeId == "empty" || item.placeId == "error" || item.placeId == "permission_denied" || item.placeId == "location_unavailable") {
            openGoogleMapsSearch("$selectedCategoryName near me")
            return
        }

        if (item.latitude == 0.0 && item.longitude == 0.0) {
            openGoogleMapsSearch(item.name + " " + item.address)
            return
        }

        // Direct navigation with destination coordinates and facility name
        val gmmIntentUri = Uri.parse("geo:0,0?q=${item.latitude},${item.longitude}(${Uri.encode(item.name)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${item.latitude},${item.longitude}&query=${Uri.encode(item.name)}")
            try {
                startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
            } catch (e: Exception) {
                openGoogleMapsSearch(item.name)
            }
        }
    }

    private fun launchCallIntent(item: PharmacyItem) {
        if (item.phoneNumber.isNullOrEmpty()) return
        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phoneNumber}"))
        startActivity(callIntent)
    }

    // MapView Lifecycle Integrations
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
