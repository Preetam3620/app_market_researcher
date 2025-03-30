package com.example.app_market_researcher

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

class ApiFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var btnGDP: Button
    private lateinit var btnCO2: Button
    private lateinit var btnAgriLand: Button
    private lateinit var indicatorContainer: LinearLayout
    private lateinit var apiService: ApiService

    private var currentIndicator = "GDP"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_api, container, false)

        webView = view.findViewById(R.id.webView)
        btnGDP = view.findViewById(R.id.btnGDP)
        btnCO2 = view.findViewById(R.id.btnCO2)
        btnAgriLand = view.findViewById(R.id.btnAgriLand)
        indicatorContainer = view.findViewById(R.id.indicatorContainer)

        webView.settings.javaScriptEnabled = true

        val retrofit = Retrofit.Builder()
            .baseUrl(ApiEndpoints.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        btnGDP.setOnClickListener {
            setActiveIndicator("GDP")
            fetchData("GDP")
        }

        btnCO2.setOnClickListener {
            setActiveIndicator("CO2")
            fetchData("CO2")
        }

        btnAgriLand.setOnClickListener {
            setActiveIndicator("AGRI_LAND")
            fetchData("AGRI_LAND")
        }

        fetchData("GDP")

        return view
    }

    private fun setActiveIndicator(indicator: String) {
        btnGDP.setBackgroundColor(Color.TRANSPARENT)
        btnGDP.setTextColor(Color.BLACK)
        btnCO2.setBackgroundColor(Color.TRANSPARENT)
        btnCO2.setTextColor(Color.BLACK)
        btnAgriLand.setBackgroundColor(Color.TRANSPARENT)
        btnAgriLand.setTextColor(Color.BLACK)

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

    private fun fetchData(indicator: String) {
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

        apiService.getDataFromEndpoint(endpoint).enqueue(object : Callback<List<Any>> {
            override fun onResponse(call: Call<List<Any>>, response: Response<List<Any>>) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body()
                    if (jsonResponse != null && jsonResponse.size >= 2) {
                        val gson = Gson()
                        val jsonElement = gson.toJsonTree(jsonResponse[1])
                        val typeToken = object : TypeToken<List<GDPDataPoint>>() {}.type
                        val dataPoints = gson.fromJson<List<GDPDataPoint>>(jsonElement, typeToken)
                        renderChart(dataPoints, indicatorName)
                    }
                }
            }

            override fun onFailure(call: Call<List<Any>>, t: Throwable) {
                Toast.makeText(requireContext(), "API error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun renderChart(dataPoints: List<GDPDataPoint>, indicatorName: String) {
        val sortedData = dataPoints.sortedBy { it.date }
        val dataArray = StringBuilder("['Year', '$indicatorName'],")

        for (dp in sortedData) {
            dataArray.append("['${dp.date}', ${dp.value}],")
        }

        val html = """
            <html>
            <head>
                <script src='https://www.gstatic.com/charts/loader.js'></script>
                <script>
                    google.charts.load('current', {packages:['corechart']});
                    google.charts.setOnLoadCallback(drawChart);
                    function drawChart() {
                        var data = google.visualization.arrayToDataTable([$dataArray]);
                        var options = { title: '$indicatorName', curveType: 'function' };
                        var chart = new google.visualization.LineChart(document.getElementById('chart_div'));
                        chart.draw(data, options);
                    }
                </script>
            </head>
            <body><div id='chart_div' style='width:100%; height:100vh;'></div></body>
            </html>
        """
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}

interface ApiService {
    @GET
    fun getDataFromEndpoint(@Url url: String): Call<List<Any>>
}