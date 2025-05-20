package com.example.transittracks

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresPermission
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

@Entity
data class Route(
    @PrimaryKey val routeID: String,
    val routeShortName: String,
    val routeLongName: String,
    val routeType: Int,
    val routeColour: String,
    val routeTextColour: String
)

@Entity
data class Trip(
    val routeID: String,
    val serviceID: Int,
    @PrimaryKey val tripID: String,
    val tripHeadSign: String,
    val shapeID: Int,
    val blockID: Int,
    val directionID: Int
)

@Entity(primaryKeys = ["tripID","stopSequence"])
data class StopTime(
    val tripID: String,
    val arrivalTime: String,
    val departureTime: String,
    val stopID: Int,
    val stopSequence: Int,
    val shapeDistTravelled: Int,
    val stopHeadSign: String?,
    val pickupType: Int,
    val dropOffType: Int,
    val timePoint: Int
)

@Entity
data class CalendarDate(
    val serviceID: Int,
    @PrimaryKey val date: String,
    val exceptionType: Int,
)

@Entity(primaryKeys = ["shapeID","shapePtSequence"])
data class Shape(
    val shapeID: Int,
    val shapePtLat: Double,
    val shapePtLon: Double,
    val shapePtSequence: Int,
    val shapeDistTravelled: Int
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

@Database(entities = [Stop::class, Route::class, Trip::class, StopTime::class, CalendarDate::class, Shape::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stopDao(): StopDao

    companion object{
        @Volatile
        private var Instance: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase{
            //checks if database instance already exists
            return Instance ?: synchronized(this){
                Room.databaseBuilder(context, AppDatabase::class.java, "static data")
                    .fallbackToDestructiveMigration(true)
                    .build().also { Instance=it }
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

        val db = AppDatabase.getDatabase(applicationContext)

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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val db = AppDatabase.getDatabase(applicationContext)
        val stopDao = db.stopDao()
        lifecycleScope.launch { //launch separate thread as Room cannot be accessed on main thread
            val stops: List<Stop> = stopDao.getAll()
            for (stop in stops){//adds all stops from database onto map as a Google Maps Marker
                val stopLatLong = LatLng(stop.stopLat,stop.stopLon)
                mMap.addMarker(MarkerOptions().position(stopLatLong).title(stop.stopName))
            }
            val Victoria = LatLng(48.4,-123.3)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(Victoria))
        }

    }
}