package com.example.transittracks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.io.IOException

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.transittracks.databinding.ActivityMapsBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        var string = ""
        try{
            val input = InputStreamReader(assets.open("BCTransitVictoria/stops.txt"))
            val reader = BufferedReader(input)
            var line = ""
            reader.readLine() //Clear info line at the top
            while(reader.readLine().also{line = it} != null){
                val row : List<String> = line.split(",")
                val stopLatLong = LatLng(row[2].toDouble(),row[3].toDouble())
                mMap.addMarker(MarkerOptions().position(stopLatLong).title(row[1]))
            }
        }catch (e: IOException){
            e.printStackTrace()
        }
        // Add a marker in Sydney and move the camera
        val Victoria = LatLng(48.4,-123.3)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Victoria))
    }
}