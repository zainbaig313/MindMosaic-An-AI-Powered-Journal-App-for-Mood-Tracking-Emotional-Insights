package com.example.mindmosaic

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class Home : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        bottomNav = findViewById(R.id.bottomNavigationView)

        // Load the default fragment
        loadFragment(Journal())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_journal -> loadFragment(Journal())
                R.id.nav_insights -> loadFragment(Insights())
                R.id.nav_profile -> loadFragment(Profile())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.homeContentFrame, fragment)
            .commit()
    }
}
