package com.nikosolov.bagscount

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navController = (supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment).navController

        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        navView.setupWithNavController(navController)
    }
}