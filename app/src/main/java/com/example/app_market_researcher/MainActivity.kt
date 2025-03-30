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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiOption = findViewById(R.id.apiOption)
        vehicleOption = findViewById(R.id.vehicleOption)
        chatGptOption = findViewById(R.id.chatGptOption)

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
            setActiveMode("CHAT_GPT")
            loadFragment(ChatGptFragment())
        }

    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun setActiveMode(mode: String) {
        when (mode) {
            "API" -> {
                apiOption.setBackgroundResource(android.R.color.white)
                apiOption.elevation = 8f

                vehicleOption.setBackgroundColor(0)
                vehicleOption.elevation = 0f

                chatGptOption.setBackgroundColor(0)
                chatGptOption.elevation = 0f
            }
            "VEHICLE" -> {
                vehicleOption.setBackgroundResource(android.R.color.white)
                vehicleOption.elevation = 8f

                apiOption.setBackgroundColor(0)
                apiOption.elevation = 0f

                chatGptOption.setBackgroundColor(0)
                chatGptOption.elevation = 0f
            }
            "CHAT_GPT" -> {
                chatGptOption.setBackgroundResource(android.R.color.white)
                chatGptOption.elevation = 8f

                apiOption.setBackgroundColor(0)
                apiOption.elevation = 0f

                vehicleOption.setBackgroundColor(0)
                vehicleOption.elevation = 0f
            }
        }
    }

}