package com.example.medicare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
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
import com.google.android.material.chip.Chip
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
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

    // Search radius in meters
    private var searchRadius = 5000.0

    // Selected place category filter
    private var selectedCategory = "healthcare.pharmacy"
    private var selectedCategoryName = "Pharmacies"
    private val USE_FALLBACK_DATA = true

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
            setupLocationEnabledMap()
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
                marker.showInfoWindow(mapLibreMap, mapView)
                val placeId = marker.snippet
                if (placeId != null) {
                    val index = placeItemsList.indexOfFirst { it.placeId == placeId }
                    if (index != -1) {
                        recyclerPharmacies.smoothScrollToPosition(index)
                    }
                }
                true
            }

            // Load Geoapify Osm-Bright Map Style
            val styleUrl = "https://maps.geoapify.com/v1/styles/osm-bright/style.json?apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
            map.setStyle(styleUrl) {
                checkPermissionsAndFetchLocation(zoomToUser = true)
            }
        }

        // Category filter setup
        setupCategoryChips()

        // Location target button
        findViewById<View>(R.id.btn_my_location)?.setOnClickListener {
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
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }

        // Back button navigation
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            finish()
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
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    if (zoomToUser && ::mapLibreMap.isInitialized) {
                        mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 15.0))
                    }
                    performNearbySearch()
                } else {
                    fallbackDefaultLocation()
                }
            }.addOnFailureListener {
                fallbackDefaultLocation()
            }
        } catch (e: SecurityException) {
            fallbackDefaultLocation()
        }
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
            setCategoryFilter("healthcare.clinic_or_praxis", "Clinics")
        }
        findViewById<Chip>(R.id.chip_emergency).setOnClickListener {
            setCategoryFilter("emergency.ambulance_station,emergency.emergency_ward_entrance,healthcare.hospital", "Emergencies")
        }
        findViewById<Chip>(R.id.chip_lab).setOnClickListener {
            setCategoryFilter("healthcare.laboratory", "Laboratories")
        }
        findViewById<Chip>(R.id.chip_dentist).setOnClickListener {
            setCategoryFilter("healthcare.dentist", "Dentists")
        }
    }

    private fun setCategoryFilter(category: String, name: String) {
        selectedCategory = category
        selectedCategoryName = name
        searchRadius = 5000.0 // reset radius
        performNearbySearch()
    }

    private fun performNearbySearch(keyword: String? = null) {
        // Show loading progress
        progressLoading.visibility = View.VISIBLE
        txtHeader.text = "Nearby $selectedCategoryName"
        txtSubtitle.text = "Showing results within ${(searchRadius / 1000).toInt()} km"

        // Search around current map center target so panning works!
        val searchCenter = if (::mapLibreMap.isInitialized) mapLibreMap.cameraPosition.target else currentLatLng
        if (searchCenter == null) {
            progressLoading.visibility = View.GONE
            showPermissionDeniedMessage()
            return
        }

        val filterStr = "circle:${searchCenter.longitude},${searchCenter.latitude},$searchRadius"
        val biasStr = "proximity:${searchCenter.longitude},${searchCenter.latitude}"

        // CLEAR old places and markers immediately to prevent stale results during loading
        placeItemsList.clear()
        if (::mapLibreMap.isInitialized) {
            mapLibreMap.clear()
        }
        markerMap.clear()

        GeoapifyClient.getService().getNearbyPlaces(
            categories = selectedCategory,
            filter = filterStr,
            bias = biasStr,
            limit = 20,
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
                    if (USE_FALLBACK_DATA) {
                        loadFallbackData(searchCenter)
                    } else {
                        showErrorResults()
                    }
                    return
                }

                val features = body.features
                if (features.isEmpty()) {
                    if (USE_FALLBACK_DATA) {
                        loadFallbackData(searchCenter)
                    } else {
                        showEmptyResults()
                    }
                    return
                }

                val defaultIcon = IconFactory.getInstance(this@PharmacyActivity).defaultMarker()

                for (feature in features) {
                    val props = feature.properties
                    val geom = feature.geometry
                    val placeId = props.placeId
                    val name = props.name ?: when (selectedCategoryName) {
                        "Pharmacies" -> "Local Pharmacy"
                        "Hospitals" -> "Hospital / Medical Center"
                        "Clinics" -> "Medical Clinic"
                        "Emergencies" -> "Emergency Care"
                        "Laboratories" -> "Medical Laboratory"
                        "Dentists" -> "Dental Clinic"
                        else -> "Healthcare Center"
                    }
                    val lon = geom.coordinates[0]
                    val lat = geom.coordinates[1]
                    val address = props.formatted ?: "No address available"
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
                        "${distanceMeters.toInt()} m away"
                    } else {
                        String.format(Locale.getDefault(), "%.1f km away", distanceMeters / 1000.0)
                    }

                    val item = PharmacyItem(
                        placeId = placeId,
                        name = name,
                        rating = "N/A", // Geoapify OSM does not natively return a 5-star rating scale
                        details = distStr,
                        latitude = lat,
                        longitude = lon,
                        address = address,
                        phoneNumber = phone,
                        website = website,
                        isOpen = null,
                        isMock = false
                    )
                    placeItemsList.add(item)

                    // Add Map Marker
                    if (::mapLibreMap.isInitialized) {
                        val markerOptions = MarkerOptions()
                            .position(LatLng(lat, lon))
                            .title(name)
                            .snippet(placeId)
                            .icon(defaultIcon)

                        val marker = mapLibreMap.addMarker(markerOptions)
                        if (marker != null) {
                            markerMap[placeId] = marker
                        }
                    }
                }

                // Update RecyclerView adapter
                updateResultsAdapter()
            }

            override fun onFailure(call: Call<GeoapifyPlacesResponse>, t: Throwable) {
                progressLoading.visibility = View.GONE
                if (USE_FALLBACK_DATA) {
                    loadFallbackData(searchCenter)
                } else {
                    Toast.makeText(this@PharmacyActivity, "Error querying nearby: ${t.message}", Toast.LENGTH_LONG).show()
                    showErrorResults()
                }
            }
        })
    }

    private fun loadFallbackData(center: LatLng) {
        placeItemsList.clear()
        if (::mapLibreMap.isInitialized) {
            mapLibreMap.clear()
        }
        markerMap.clear()

        val list = when (selectedCategoryName) {
            "Pharmacies" -> listOf(
                Triple("Medicare Care Pharmacy (Demo)", "123 Health Ave", "9876543210"),
                Triple("Wellness Drugstore (Demo)", "456 Wellness Blvd", "9876543211"),
                Triple("City Life Pharmacy (Demo)", "789 Metro St", "9876543212")
            )
            "Hospitals" -> listOf(
                Triple("Metro General Hospital (Demo)", "100 Hospital Rd", "9876543213"),
                Triple("St. Jude Medical Center (Demo)", "200 Care Ln", "9876543214")
            )
            "Clinics" -> listOf(
                Triple("Apex Medical Clinic (Demo)", "50 Doctor Ave", "9876543215"),
                Triple("CareFirst Clinic (Demo)", "60 Wellness St", "9876543216")
            )
            "Emergencies" -> listOf(
                Triple("Rapid Response Emergency Ward (Demo)", "10 Trauma Rd", "9876543217"),
                Triple("City Ambulance & Trauma Station (Demo)", "20 Emergency Blvd", "9876543218")
            )
            "Laboratories" -> listOf(
                Triple("BioDiagnostics Lab (Demo)", "30 Science Rd", "9876543219"),
                Triple("Precision Medical Lab (Demo)", "40 Test Ave", "9876543220")
            )
            "Dentists" -> listOf(
                Triple("Bright Smile Dental Care (Demo)", "15 Smile Ln", "9876543221"),
                Triple("Perfect Dental Clinic (Demo)", "25 Teeth St", "9876543222")
            )
            else -> listOf(
                Triple("General Healthcare Center (Demo)", "1 Care St", "9876543223")
            )
        }

        val defaultIcon = IconFactory.getInstance(this).defaultMarker()

        val offsets = listOf(
            Pair(0.003, -0.004),
            Pair(-0.004, 0.005),
            Pair(0.006, 0.003),
            Pair(-0.002, -0.005)
        )

        for ((idx, p) in list.withIndex()) {
            val offset = offsets[idx % offsets.size]
            val lat = center.latitude + offset.first
            val lon = center.longitude + offset.second
            val placeId = "fallback_${selectedCategoryName.lowercase()}_$idx"

            val results = FloatArray(1)
            Location.distanceBetween(center.latitude, center.longitude, lat, lon, results)
            val distanceMeters = results[0].toDouble()
            val distStr = if (distanceMeters < 1000) {
                "${distanceMeters.toInt()} m away (Demo)"
            } else {
                String.format(Locale.getDefault(), "%.1f km away (Demo)", distanceMeters / 1000.0)
            }

            val item = PharmacyItem(
                placeId = placeId,
                name = p.first,
                rating = "4.5",
                details = distStr,
                latitude = lat,
                longitude = lon,
                address = p.second,
                phoneNumber = p.third,
                website = "https://example.com",
                isOpen = true,
                isMock = true
            )
            placeItemsList.add(item)

            if (::mapLibreMap.isInitialized) {
                val markerOptions = MarkerOptions()
                    .position(LatLng(lat, lon))
                    .title(p.first)
                    .snippet(placeId)
                    .icon(defaultIcon)

                val marker = mapLibreMap.addMarker(markerOptions)
                if (marker != null) {
                    markerMap[placeId] = marker
                }
            }
        }

        updateResultsAdapter()
        Toast.makeText(this, "No live locations found. Showing demo data.", Toast.LENGTH_SHORT).show()
    }

    private fun showPermissionDeniedMessage() {
        placeItemsList.clear()
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
            onNavigateClick = {},
            onCallClick = {}
        )
    }

    private fun showEmptyResults() {
        placeItemsList.clear()
        val item = PharmacyItem(
            placeId = "empty",
            name = "No Locations Found",
            rating = "0.0",
            details = "Expand search radius to search wider area.",
            latitude = 0.0,
            longitude = 0.0,
            address = "No healthcare facilities found within search circle."
        )
        placeItemsList.add(item)
        
        recyclerPharmacies.adapter = PharmacyAdapter(placeItemsList,
            onItemClick = {
                // Try expanding search radius on tap
                searchRadius *= 2
                performNearbySearch()
            },
            onNavigateClick = {},
            onCallClick = {}
        )
    }

    private fun showErrorResults() {
        placeItemsList.clear()
        val item = PharmacyItem(
            placeId = "error",
            name = "Connection/API Error",
            rating = "0.0",
            details = "Verify your internet and API configuration.",
            latitude = 0.0,
            longitude = 0.0,
            address = "Could not fetch nearby location markers. Click VIEW MAP/REFRESH to retry."
        )
        placeItemsList.add(item)

        recyclerPharmacies.adapter = PharmacyAdapter(placeItemsList,
            onItemClick = { performNearbySearch() },
            onNavigateClick = {},
            onCallClick = {}
        )
    }

    private fun updateResultsAdapter() {
        recyclerPharmacies.adapter = PharmacyAdapter(placeItemsList,
            onItemClick = { item ->
                // Center Map Camera
                if (::mapLibreMap.isInitialized && item.latitude != 0.0) {
                    val latLng = LatLng(item.latitude, item.longitude)
                    mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0))
                    markerMap[item.placeId]?.showInfoWindow(mapLibreMap, mapView)
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
                if (item.latitude != 0.0) {
                    setNegativeButton("Directions") { _, _ ->
                        launchNavigationIntent(item)
                    }
                }
            }
            .show()
    }

    private fun launchNavigationIntent(item: PharmacyItem) {
        if (item.latitude == 0.0) return
        val uri = Uri.parse("google.navigation:q=${item.latitude},${item.longitude}&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${item.latitude},${item.longitude}")
            startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
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
