package com.example.app_market_researcher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import retrofit2.converter.gson.GsonConverterFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

import retrofit2.http.GET
import retrofit2.http.Url
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnGDP: Button
    private lateinit var btnCO2: Button
    private lateinit var btnAgriLand: Button
    private lateinit var apiOption: LinearLayout
    private lateinit var chatGptOption: ImageView
    private lateinit var vehicleOption: ImageView
    private lateinit var apiService: ApiService
    private lateinit var indicatorContainer: LinearLayout

    // New UI elements for vehicle container
    private lateinit var vehicleContainer: LinearLayout
    private lateinit var btnSelectFile: Button
    private lateinit var tvSelectedFileName: TextView
    private lateinit var btnAnalyze: Button
    private lateinit var tvAnalysisResult: TextView

    // Current active indicator
    private var currentIndicator = "GDP"

    // Audio file URI
    private var selectedAudioFileUri: Uri? = null

    // Request code for file picking
    private val PICK_AUDIO_FILE = 100

    // Audio analyzer
    private lateinit var audioAnalyzer: AudioAnalyzer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        webView = findViewById(R.id.webView)
        btnGDP = findViewById(R.id.btnGDP)
        btnCO2 = findViewById(R.id.btnCO2)
        btnAgriLand = findViewById(R.id.btnAgriLand)
        apiOption = findViewById(R.id.apiOption)
        chatGptOption = findViewById(R.id.chatGptOption)
        vehicleOption = findViewById(R.id.vehicleOption)
        indicatorContainer = findViewById(R.id.indicatorContainer)

        // Initialize new UI elements
        vehicleContainer = findViewById(R.id.vehicleContainer)
        btnSelectFile = findViewById(R.id.btnSelectFile)
        tvSelectedFileName = findViewById(R.id.tvSelectedFileName)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        tvAnalysisResult = findViewById(R.id.tvAnalysisResult)

        // Set up WebView
        webView.settings.javaScriptEnabled = true

        // Initialize audio analyzer
        audioAnalyzer = AudioAnalyzer(this)

        // Setup Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiEndpoints.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Set up click listeners for bottom navigation
        apiOption.setOnClickListener {
            setActiveMode("API")
            // Initially fetch GDP data
            fetchDataForIndicator("GDP")
        }

        chatGptOption.setOnClickListener {
            showApiOptions(false)
            Toast.makeText(this, "ChatGPT option selected", Toast.LENGTH_SHORT).show()
            // Implement ChatGPT integration
        }

        vehicleOption.setOnClickListener {
            setActiveMode("VEHICLE")
        }

        // Set up indicator button click listeners
        btnGDP.setOnClickListener {
            setActiveIndicator("GDP")
            fetchDataForIndicator("GDP")
        }

        btnCO2.setOnClickListener {
            setActiveIndicator("CO2")
            fetchDataForIndicator("CO2")
        }

        btnAgriLand.setOnClickListener {
            setActiveIndicator("AGRI_LAND")
            fetchDataForIndicator("AGRI_LAND")
        }

        // Set up file selection and analyze button
        btnSelectFile.setOnClickListener {
            openAudioFilePicker()
        }

        btnAnalyze.setOnClickListener {
            analyzeAudioFile()
        }

        // Initially show API options and fetch GDP data
        setActiveMode("API")
        fetchDataForIndicator("GDP")
    }

    private fun setActiveMode(mode: String) {
        when (mode) {
            "API" -> {
                // Show API related views
                webView.visibility = View.VISIBLE
                vehicleContainer.visibility = View.GONE
                indicatorContainer.visibility = View.VISIBLE

                // Highlight API option
                apiOption.background = getDrawable(android.R.color.white)
                apiOption.elevation = 8f

                // Reset other options (no elevation)
                vehicleOption.background = null
                vehicleOption.elevation = 0f
                chatGptOption.background = null
                chatGptOption.elevation = 0f
            }
            "VEHICLE" -> {
                // Show Vehicle related views
                webView.visibility = View.GONE
                vehicleContainer.visibility = View.VISIBLE
                indicatorContainer.visibility = View.GONE

                // Highlight Vehicle option
                vehicleOption.background = getDrawable(android.R.color.white)
                vehicleOption.elevation = 8f

                // Reset other options (no elevation)
                apiOption.background = null
                apiOption.elevation = 0f
                chatGptOption.background = null
                chatGptOption.elevation = 0f

                Toast.makeText(this, "Select an audio file to analyze", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showApiOptions(show: Boolean) {
        indicatorContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setActiveIndicator(indicator: String) {
        // Reset all buttons to outlined style
        btnGDP.setBackgroundColor(Color.TRANSPARENT)
        btnGDP.setTextColor(Color.BLACK)
        btnCO2.setBackgroundColor(Color.TRANSPARENT)
        btnCO2.setTextColor(Color.BLACK)
        btnAgriLand.setBackgroundColor(Color.TRANSPARENT)
        btnAgriLand.setTextColor(Color.BLACK)

        // Set active button style
        when (indicator) {
            "GDP" -> {
                btnGDP.setBackgroundColor(Color.parseColor("#4267B2"))
                btnGDP.setTextColor(Color.WHITE)
            }
            "CO2" -> {
                btnCO2.setBackgroundColor(Color.parseColor("#4267B2"))
                btnCO2.setTextColor(Color.WHITE)
            }
            "AGRI_LAND" -> {
                btnAgriLand.setBackgroundColor(Color.parseColor("#4267B2"))
                btnAgriLand.setTextColor(Color.WHITE)
            }
        }

        currentIndicator = indicator
    }

    private fun openAudioFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, PICK_AUDIO_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_AUDIO_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedAudioFileUri = uri

                // Get the file name
                val fileName = getFileNameFromUri(uri)
                tvSelectedFileName.text = fileName

                // Enable the analyze button
                btnAnalyze.isEnabled = true

                // Clear previous analysis result
                tvAnalysisResult.text = ""

                Toast.makeText(this, "Selected: $fileName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "Unknown file"

        // Try to get the display name from the content resolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = it.getString(displayNameIndex)
                }
            }
        }

        return fileName
    }

    private fun analyzeAudioFile() {
        selectedAudioFileUri?.let { uri ->
            // Show loading state
            btnAnalyze.isEnabled = false
            btnAnalyze.text = "Analyzing..."

            // Use a background thread for analysis
            thread {
                // Call the audio analyzer
                val result = audioAnalyzer.analyzeAudio(uri)

                // Update UI on main thread
                Handler(Looper.getMainLooper()).post {
                    btnAnalyze.isEnabled = true
                    btnAnalyze.text = "Analyze"

                    // Display the results
                    showAnalysisResult(result)
                }
            }
        } ?: run {
            Toast.makeText(this, "Please select an audio file first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAnalysisResult(result: Map<String, Any>) {
        val predictedClass = result["predicted_class"] as String
        val confidence = result["confidence"] as Float

        // Display in dialog
        val message = """
            Analysis Complete:
            
            Predicted Class: $predictedClass
            Confidence: ${String.format("%.5f", confidence)}
            
            This sound indicates a potential $predictedClass issue.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Audio Analysis Result")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()

        // Update the TextView
        tvAnalysisResult.text = "Predicted Class: $predictedClass\nConfidence = ${String.format("%.5f", confidence)}"
    }

    private fun fetchDataForIndicator(indicator: String) {
        val endpoint = when (indicator) {
            "GDP" -> ApiEndpoints.GDP_ENDPOINT
            "CO2" -> ApiEndpoints.CO2_ENDPOINT
            "AGRI_LAND" -> ApiEndpoints.AGRI_LAND_ENDPOINT
            else -> ApiEndpoints.GDP_ENDPOINT
        }

        val indicatorName = when (indicator) {
            "GDP" -> "GDP Growth (annual %)"
            "CO2" -> "CO2 Emissions (metric tons per capita)"
            "AGRI_LAND" -> "Agricultural Land (% of land area)"
            else -> "GDP Growth (annual %)"
        }

        Toast.makeText(this, "Fetching $indicatorName data...", Toast.LENGTH_SHORT).show()

        apiService.getDataFromEndpoint(endpoint).enqueue(object : Callback<List<Any>> {
            override fun onResponse(call: Call<List<Any>>, response: Response<List<Any>>) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body()
                    Log.d("API_RESPONSE", "Raw response: $jsonResponse")

                    if (jsonResponse != null && jsonResponse.size >= 2) {
                        try {
                            // Convert the second element (data array) to list of DataPoint
                            val gson = Gson()
                            val jsonElement = gson.toJsonTree(jsonResponse[1])
                            val typeToken = object : TypeToken<List<GDPDataPoint>>() {}.type
                            val dataPoints = gson.fromJson<List<GDPDataPoint>>(jsonElement, typeToken)

                            renderGoogleChart(dataPoints, indicatorName)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error parsing data: ${e.message}")
                            showError("Error parsing data: ${e.message}")

                            // Fallback to sample data for demo purposes
                            renderGoogleChart(createSampleData(indicator), indicatorName)
                        }
                    } else {
                        Log.e("API_RESPONSE", "Invalid JSON structure: $jsonResponse")
                        showError("Invalid response format")

                        // Fallback to sample data for demo purposes
                        renderGoogleChart(createSampleData(indicator), indicatorName)
                    }
                } else {
                    Log.e("API_RESPONSE", "Error code: ${response.code()}")
                    showError("Error: ${response.code()}")

                    // Fallback to sample data for demo purposes
                    renderGoogleChart(createSampleData(indicator), indicatorName)
                }
            }

            override fun onFailure(call: Call<List<Any>>, t: Throwable) {
                Log.e("MainActivity", "Network error: ${t.message}")
                showError("Network error: ${t.message}")

                // Fallback to sample data for demo purposes
                renderGoogleChart(createSampleData(indicator), indicatorName)
            }
        })
    }

    private fun renderGoogleChart(dataPoints: List<GDPDataPoint>, indicatorName: String) {
        // Filter for World data
        val worldData = dataPoints.filter { it.country.value == "World" }

        val chartData = if (worldData.isEmpty()) {
            dataPoints.take(10) // If no World data, take first 10 entries
        } else {
            worldData
        }

        if (chartData.isEmpty()) {
            showError("No data found")
            return
        }

        // Sort by date
        val sortedData = chartData.sortedBy { it.date }

        // Build data array for Google Charts
        val dataArray = StringBuilder()
        dataArray.append("['Year', '$indicatorName'],")

        for (dataPoint in sortedData) {
            dataArray.append("['${dataPoint.date}', ${dataPoint.value}],")
        }

        // Choose chart color based on indicator
        val chartColor = when (currentIndicator) {
            "GDP" -> "#1b73e8"
            "CO2" -> "#e8731b"
            "AGRI_LAND" -> "#4CAF50"
            else -> "#1b73e8"
        }

        // Build the HTML with Google Charts
        val html = """
            <html>
            <head>
                <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
                <script type="text/javascript">
                    google.charts.load('current', {'packages':['corechart']});
                    google.charts.setOnLoadCallback(drawChart);
                    
                    function drawChart() {
                        var data = google.visualization.arrayToDataTable([
                            $dataArray
                        ]);
                        
                        var options = {
                            title: '$indicatorName - World',
                            curveType: 'function',
                            legend: { position: 'top' },
                            colors: ['$chartColor'],
                            chartArea: { width: '80%', height: '70%' },
                            vAxis: { title: '$indicatorName' },
                            hAxis: { title: 'Year', slantedText: true, slantedTextAngle: 45 }
                        };
                        
                        var chart = new google.visualization.LineChart(document.getElementById('chart_div'));
                        chart.draw(data, options);
                    }
                </script>
                <style>
                    body { margin: 0; padding: 0; }
                    #chart_div { width: 100%; height: 100vh; }
                </style>
            </head>
            <body>
                <div id="chart_div"></div>
            </body>
            </html>
        """

        // Load the HTML into WebView
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

        // Show success message
        Toast.makeText(this, "$indicatorName data loaded", Toast.LENGTH_SHORT).show()
    }

    private fun createSampleData(indicatorType: String): List<GDPDataPoint> {
        val sampleData = mutableListOf<GDPDataPoint>()

        // Create sample country and indicator
        val country = Country("1W", "World")
        val indicator = Indicator(
            when (indicatorType) {
                "GDP" -> "NY.GDP.MKTP.KD.ZG"
                "CO2" -> "EN.ATM.CO2E.PC"
                "AGRI_LAND" -> "AG.LND.AGRI.ZS"
                else -> "NY.GDP.MKTP.KD.ZG"
            },
            when (indicatorType) {
                "GDP" -> "GDP Growth (annual %)"
                "CO2" -> "CO2 Emissions (metric tons per capita)"
                "AGRI_LAND" -> "Agricultural Land (% of land area)"
                else -> "GDP Growth (annual %)"
            }
        )

        // Years for sample data
        val years = listOf("1970", "1975", "1980", "1985", "2000", "2005", "2010", "2015")

        // Different sample values based on indicator
        val values = when (indicatorType) {
            "GDP" -> listOf(3.2, 2.8, 4.3, 3.6, 4.3, 2.8, -3.3, 2.8)
            "CO2" -> listOf(4.1, 4.0, 4.1, 4.5, 4.8, 4.9, 4.6, 4.5)
            "AGRI_LAND" -> listOf(38.4, 38.1, 37.6, 37.3, 37.0, 36.8, 36.5, 36.3)
            else -> listOf(3.2, 2.8, 4.3, 3.6, 4.3, 2.8, -3.3, 2.8)
        }

        for (i in years.indices) {
            sampleData.add(
                GDPDataPoint(
                    indicator = indicator,
                    country = country,
                    countryiso3code = "WLD",
                    date = years[i],
                    value = values[i],
                    unit = "",
                    obs_status = "",
                    decimal = 1
                )
            )
        }

        return sampleData
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        audioAnalyzer.close()
    }
}

// Updated API service to handle dynamic endpoints
interface ApiService {
    @GET
    fun getDataFromEndpoint(@Url url: String): Call<List<Any>>
}