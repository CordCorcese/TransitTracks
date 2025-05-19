package com.example.transittracks

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.IOException

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.transittracks.databinding.ActivityMapsBinding
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Entity
data class Stop(
    @PrimaryKey val stopId: Int,
    val stopName: String?,
    val stopLat: Double,
    val stopLon: Double,
    val wheelchair: Int,
    val stopCode: Int
)

fun makeStop(stopID: Int, stopName: String?, stopLat: Double, stopLon: Double, wheelchair: Int, stopCode: Int ):Stop{
    val newStop = Stop(
        stopId = stopID,
        stopName = stopName,
        stopLat = stopLat,
        stopLon = stopLon,
        wheelchair = wheelchair,
        stopCode = stopCode
    )
    return newStop
}

@Database(entities = [Stop::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stopDao(): StopDao

    companion object{
        @Volatile
        private var Instance: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase{
            //checks if database instance already exists
            return Instance ?: synchronized(this){
                Room.databaseBuilder(context, AppDatabase::class.java, "static data").build().also { Instance=it }
            }
        }
    }
}

@Dao
interface StopDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: Stop)

    @Delete
    fun delete(stop: Stop)

    @Query("SELECT * FROM Stop")
    suspend fun getAll(): List<Stop>
}

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

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "static data"
        ).build()

        try{
            val input = InputStreamReader(assets.open("BCTransitVictoria/stops.txt"))
            val reader = BufferedReader(input)
            var line = ""
            reader.readLine() //Clear info line at the top
            while(reader.readLine().also{line = it} != null){
                val row : List<String> = line.split(",")
                val stop = makeStop(row[0].toInt(),row[1],row[2].toDouble(),row[3].toDouble(),row[4].toInt(),row[5].toInt())
                lifecycleScope.launch { db.stopDao().insertStop(stop) }
            }
        }catch (e: IOException){
            e.printStackTrace()
        }

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
        /*try{
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
        }*/
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "static data"
        ).build()
        val stopDao = db.stopDao()
        lifecycleScope.launch { //launch separate thread as Room cannot be accessed on main thread
            val stops: List<Stop> = stopDao.getAll()
            for (stop in stops){
                val stopLatLong = LatLng(stop.stopLat,stop.stopLon)
                mMap.addMarker(MarkerOptions().position(stopLatLong).title(stop.stopName))
            }
            val Victoria = LatLng(48.4,-123.3)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(Victoria))
        }

    }
}