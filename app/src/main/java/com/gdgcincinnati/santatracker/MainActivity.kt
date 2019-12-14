package com.gdgcincinnati.santatracker

import android.media.SoundPool
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.layout.Column
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Card
import androidx.ui.tooling.preview.Preview
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {
    private var map: GoogleMap? = null
    private var marker: Marker? = null

    private var santaLocation = SantaLocation(LatLng(39.10, -84.51))

    private var locationRef: DatabaseReference? = null
    private val locationListener: ValueEventListener by lazy {
        ValueEventListenerAdapter { dataSnapshot ->
            updateMapAndMarker(with(dataSnapshot) {
                val latitude = dataSnapshot.child("lat").getNonNullValue(Double::class.java)
                val longitude = dataSnapshot.child("lng").getNonNullValue(Double::class.java)
                LatLng(latitude, longitude)
            })
        }
    }
    private var hohohoRef: DatabaseReference? = null
    private val hohohoListener: ValueEventListener by lazy {
        ValueEventListenerAdapter { dataSnapshot ->
            val hohohoing = dataSnapshot.getNonNullValue(Boolean::class.java)
            playSantaIfHohohoing(hohohoing)
        }
    }

    private lateinit var soundPool: SoundPool
    private var soundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            startTrackingSanta()
        }

        soundPool = SoundPool.Builder().build()
        soundId = soundPool.load(this, R.raw.hohoho, 1)
    }

    private fun startTrackingSanta() {
        val database = FirebaseDatabase.getInstance()
        locationRef = database.getReference("current_location").apply {
            addValueEventListener(locationListener)
        }

        hohohoRef = database.getReference("ho_ho_hoing").apply {
            addValueEventListener(hohohoListener)
        }
    }

    override fun onPause() {
        locationRef?.removeEventListener(locationListener)
        hohohoRef?.removeEventListener(hohohoListener)
        super.onPause()
    }

    private fun updateMapAndMarker(position: LatLng) {
        santaLocation.latLng = position
        map?.let { map ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 9f))

            if (marker == null) {
                val options = MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.santa))
                        .position(position)
                marker = map.addMarker(options)
            } else {
                marker?.position = position
            }
        }
    }

    private fun playSantaIfHohohoing(hohohoing: Boolean) {
        if (hohohoing) {
            soundPool.play(soundId, 1f, 1f, 10, 0, 1f)
        }
    }

    private class ValueEventListenerAdapter(val lambda: (DataSnapshot) -> Unit) : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) = lambda(dataSnapshot)
        override fun onCancelled(error: DatabaseError) = Unit
    }

    private fun <T> DataSnapshot.getNonNullValue(type: Class<T>): T = getValue(type)
            ?: throw IllegalStateException()
}


@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Model
data class SantaLocation(var latLng: LatLng)

@Composable
fun SantaLocationCard(santaLocation: SantaLocation) {
    val typography = +MaterialTheme.typography()

    Card(shape = RoundedCornerShape(8.dp)) {
        Padding(padding = 8.dp) {
            Row {
                Column {
                    Text("Santa Location", style = typography.h4)
                    Text("Latitude: ${santaLocation.latLng.latitude}", style = typography.body1)
                    Text("Longitude: ${santaLocation.latLng.longitude}", style = typography.body1)
                }
            }
        }
    }
}

@Preview
@Composable
fun DefaultPreview() {
    MaterialTheme {
        SantaLocationCard(SantaLocation(LatLng(39.10, -84.51)))
    }
}
