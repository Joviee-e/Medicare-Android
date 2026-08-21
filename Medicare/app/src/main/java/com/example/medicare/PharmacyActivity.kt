package com.example.medicare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.material.chip.Chip

class PharmacyActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    private lateinit var recyclerPharmacies: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var txtHeader: TextView
    private lateinit var txtSubtitle: TextView
    private lateinit var inputSearch: EditText

    // Search radius in meters
    private var searchRadius = 5000.0

    // Selected place type filter
    private var selectedTypes = listOf("pharmacy")
    private var selectedCategoryName = "Pharmacies"
    private var selectedHue = BitmapDescriptorFactory.HUE_CYAN

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

        // Initialize Places Client
        val apiKey = getString(R.string.google_maps_key)
        if (apiKey.isEmpty() || apiKey == "PLACEHOLDER_MAPS_API_KEY") {
            Toast.makeText(this, "Maps API key is missing. Set it in strings.xml.", Toast.LENGTH_LONG).show()
        } else {
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            }
            placesClient = Places.createClient(this)
        }

        // Setup Map Fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Premium Map style configurations
        mMap.uiSettings.isMyLocationButtonEnabled = false // using custom button
        mMap.uiSettings.isZoomControlsEnabled = true

        mMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            val placeId = marker.tag as? String
            if (placeId != null) {
                val index = placeItemsList.indexOfFirst { it.placeId == placeId }
                if (index != -1) {
                    recyclerPharmacies.smoothScrollToPosition(index)
                }
            }
            true
        }

        // Check permission and set maps configuration
        checkPermissionsAndFetchLocation(zoomToUser = true)
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
        if (!::mMap.isInitialized) return

        try {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    if (zoomToUser) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 15f))
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
            setCategoryFilter(listOf("pharmacy"), "Pharmacies", BitmapDescriptorFactory.HUE_CYAN)
        }
        findViewById<Chip>(R.id.chip_hospital).setOnClickListener {
            setCategoryFilter(listOf("hospital"), "Hospitals", BitmapDescriptorFactory.HUE_RED)
        }
        findViewById<Chip>(R.id.chip_clinic).setOnClickListener {
            setCategoryFilter(listOf("doctor", "medical_clinic"), "Clinics", BitmapDescriptorFactory.HUE_BLUE)
        }
        findViewById<Chip>(R.id.chip_emergency).setOnClickListener {
            // "hospital" covers ER facilities, and we keyword filter if needed
            setCategoryFilter(listOf("hospital"), "Emergencies", BitmapDescriptorFactory.HUE_ORANGE)
        }
        findViewById<Chip>(R.id.chip_lab).setOnClickListener {
            setCategoryFilter(listOf("medical_lab"), "Laboratories", BitmapDescriptorFactory.HUE_GREEN)
        }
        findViewById<Chip>(R.id.chip_dentist).setOnClickListener {
            setCategoryFilter(listOf("dentist"), "Dentists", BitmapDescriptorFactory.HUE_YELLOW)
        }
    }

    private fun setCategoryFilter(types: List<String>, name: String, hue: Float) {
        selectedTypes = types
        selectedCategoryName = name
        selectedHue = hue
        searchRadius = 5000.0 // reset radius
        performNearbySearch()
    }

    private fun performNearbySearch(keyword: String? = null) {
        if (!::placesClient.isInitialized) {
            Toast.makeText(this, "Places API not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading progress
        progressLoading.visibility = View.VISIBLE
        txtHeader.text = "Nearby $selectedCategoryName"
        txtSubtitle.text = "Showing results within ${(searchRadius / 1000).toInt()} km"

        // Search around current map center target so panning works!
        val searchCenter = if (::mMap.isInitialized) mMap.cameraPosition.target else currentLatLng
        if (searchCenter == null) {
            progressLoading.visibility = View.GONE
            showPermissionDeniedMessage()
            return
        }

        val circle = CircularBounds.newInstance(searchCenter, searchRadius)
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.RATING,
            Place.Field.OPENING_HOURS,
            Place.Field.PHONE_NUMBER,
            Place.Field.WEBSITE_URI
        )

        val requestBuilder = SearchNearbyRequest.builder(circle, placeFields)
            .setIncludedTypes(selectedTypes)
            .setMaxResultCount(20)

        // For emergency keyword filtering refinement
        if (selectedCategoryName == "Emergencies") {
            // Places API (New) doesn't support custom query keyword in searchNearby directly,
            // but we can query hospitals and check keywords or display ER locations.
        }

        placesClient.searchNearby(requestBuilder.build())
            .addOnSuccessListener { response ->
                progressLoading.visibility = View.GONE
                val placesList = response.places
                
                placeItemsList.clear()
                mMap.clear()
                markerMap.clear()

                if (placesList.isEmpty()) {
                    showEmptyResults()
                    return@addOnSuccessListener
                }

                for (place in placesList) {
                    val placeId = place.id ?: ""
                    val name = place.name ?: "Healthcare Center"
                    val ratingVal = place.rating?.toString() ?: "N/A"
                    val latLng = place.latLng ?: searchCenter
                    val address = place.address ?: "No address available"
                    val phone = place.phoneNumber
                    val website = place.websiteUri?.toString()
                    val isOpen = if (place.openingHours != null) place.isOpen else null

                    // Calculate distance
                    val results = FloatArray(1)
                    Location.distanceBetween(searchCenter.latitude, searchCenter.longitude, latLng.latitude, latLng.longitude, results)
                    val distanceKm = results[0] / 1000.0
                    val distStr = String.format("%.1f km away", distanceKm)
                    val openStr = if (place.openingHours != null && isOpen != null) {
                        if (isOpen == true) "• Open Now" else "• Closed"
                    } else ""
                    val details = "$distStr $openStr"

                    val item = PharmacyItem(
                        placeId = placeId,
                        name = name,
                        rating = ratingVal,
                        details = details,
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        address = address,
                        phoneNumber = phone,
                        website = website,
                        isOpen = isOpen
                    )
                    placeItemsList.add(item)

                    // Add Map Marker
                    val markerOptions = MarkerOptions()
                        .position(latLng)
                        .title(name)
                        .snippet("Rating: $ratingVal Stars")
                        .icon(BitmapDescriptorFactory.defaultMarker(selectedHue))

                    val marker = mMap.addMarker(markerOptions)
                    if (marker != null) {
                        marker.tag = placeId
                        markerMap[placeId] = marker
                    }
                }

                // Update RecyclerView adapter
                updateResultsAdapter()
            }
            .addOnFailureListener { exception ->
                progressLoading.visibility = View.GONE
                Toast.makeText(this@PharmacyActivity, "Error querying nearby: ${exception.message}", Toast.LENGTH_LONG).show()
                showErrorResults()
            }
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
                // Center Map Camera and show info window
                if (::mMap.isInitialized && item.latitude != 0.0) {
                    val latLng = LatLng(item.latitude, item.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    markerMap[item.placeId]?.showInfoWindow()
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
        details.append("Rating: ${item.rating} Stars\n\n")
        if (item.phoneNumber != null) details.append("Phone: ${item.phoneNumber}\n\n")
        if (item.website != null) details.append("Website: ${item.website}\n\n")
        
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setMessage(details.toString())
            .setPositiveButton("Close", null)
            .apply {
                if (item.phoneNumber != null) {
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
}
