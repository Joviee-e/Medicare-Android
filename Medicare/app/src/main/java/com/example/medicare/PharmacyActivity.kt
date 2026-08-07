package com.example.medicare

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PharmacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pharmacy)

        // Setup custom bottom navigation
        NavigationHelper.setupNavigation(this, R.id.tab_pharmacy)

        // Setup pharmacies RecyclerView
        val recyclerPharmacies = findViewById<RecyclerView>(R.id.recycler_pharmacies)
        recyclerPharmacies.layoutManager = LinearLayoutManager(this)

        val pharmacyData = listOf(
            PharmacyItem("Walgreens Pharmacy", "4.5", "0.5 miles away • Open until 10 PM"),
            PharmacyItem("CVS Care", "4.8", "1.2 miles away • 24 Hours")
        )
        recyclerPharmacies.adapter = PharmacyAdapter(pharmacyData)

        // VIEW MAP click trigger
        findViewById<TextView>(R.id.btn_view_map)?.setOnClickListener {
            Toast.makeText(this, "Interactive map coming soon", Toast.LENGTH_SHORT).show()
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
}
