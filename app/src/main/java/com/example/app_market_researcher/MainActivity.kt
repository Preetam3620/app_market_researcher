package com.example.app_market_researcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {

    private lateinit var apiOption: LinearLayout
    private lateinit var vehicleOption: ImageView
    private lateinit var chatGptOption: ImageView
    private lateinit var chatGpt: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiOption = findViewById(R.id.apiOption)
        vehicleOption = findViewById(R.id.vehicleOption)
        chatGptOption = findViewById(R.id.chatGptOption)
        chatGpt = findViewById(R.id.chatGpt)

        loadFragment(ApiFragment())

        apiOption.setOnClickListener {
            setActiveMode("API")
            loadFragment(ApiFragment())
        }

        vehicleOption.setOnClickListener {
            setActiveMode("VEHICLE")
            loadFragment(VehicleFragment())
        }

        chatGptOption.setOnClickListener {
            setActiveMode("CHAT_GPT_OFFLINE")
            loadFragment(ChatGptFragment())
        }

        chatGpt.setOnClickListener {
            setActiveMode("CHAT_GPT")
            loadFragment(ChatGptFragmentOffline())
        }

    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun setActiveMode(mode: String) {
        // Reset all backgrounds and elevation
        apiOption.setBackgroundColor(0)
        apiOption.elevation = 0f

        vehicleOption.setBackgroundColor(0)
        vehicleOption.elevation = 0f

        chatGptOption.setBackgroundColor(0)
        chatGptOption.elevation = 0f

        chatGpt.setBackgroundColor(0)
        chatGpt.elevation = 0f

        // Apply active style to the selected one
        when (mode) {
            "API" -> {
                apiOption.setBackgroundResource(android.R.color.white)
                apiOption.elevation = 8f
            }
            "VEHICLE" -> {
                vehicleOption.setBackgroundResource(android.R.color.white)
                vehicleOption.elevation = 8f
            }
            "CHAT_GPT_OFFLINE" -> {
                chatGptOption.setBackgroundResource(android.R.color.white)
                chatGptOption.elevation = 8f
            }
            "CHAT_GPT" -> {
                chatGpt.setBackgroundResource(android.R.color.white)
                chatGpt.elevation = 8f
            }
        }
    }

}