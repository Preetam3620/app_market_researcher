package com.example.app_market_researcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

class ChatGptFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var btnSubmit: Button
    private lateinit var etPrompt: EditText
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var chatGptService: ChatGptService
    private var currentPrompt: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_gpt, container, false)

        etPrompt = view.findViewById(R.id.etPrompt)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        webView = view.findViewById(R.id.webView)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)

        webView.settings.javaScriptEnabled = true
        loadingSpinner.visibility = View.GONE

        val client = OkHttpClient.Builder().addInterceptor { chain: Interceptor.Chain ->
            val request = chain.request().newBuilder().addHeader(
                "Authorization",
                "Bearer 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"
            ).addHeader("OpenAI-Project", "proj_9eXCODLhjzsFAUMpm28nd4y2").build()
            chain.proceed(request)
        }.build()

        val retrofit = Retrofit.Builder().baseUrl("https://api.openai.com/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()

        chatGptService = retrofit.create(ChatGptService::class.java)

        btnSubmit.setOnClickListener {
            val promptText = etPrompt.text.toString().trim()
            if (promptText.isNotEmpty()) {
                currentPrompt = promptText
                sendPrompt(promptText)
            } else {
                Toast.makeText(requireContext(), "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun sendPrompt(prompt: String) {
        loadingSpinner.visibility = View.VISIBLE
        webView.visibility = View.GONE

        val request = ChatGptRequest(
            model = "gpt-3.5-turbo", messages = listOf(Message("user", prompt)), max_tokens = 100
        )
        chatGptService.getCompletion(request).enqueue(object : Callback<ChatGptResponse> {
            override fun onResponse(
                call: Call<ChatGptResponse>, response: Response<ChatGptResponse>
            ) {
                loadingSpinner.visibility = View.GONE
                webView.visibility = View.VISIBLE

                if (response.isSuccessful && response.body() != null) {
                    val content = response.body()!!.choices.firstOrNull()?.message?.content ?: ""
                    if (currentPrompt.contains("time series", ignoreCase = true)) {
                        renderLineChartFromText(content)
                    } else {
                        renderPieChartFromText(content)
                    }
                } else {
                    Toast.makeText(requireContext(), "No response", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ChatGptResponse>, t: Throwable) {
                loadingSpinner.visibility = View.GONE
                webView.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "API error: ${t.message}", Toast.LENGTH_LONG)
                    .show()
            }
        })
    }

    private fun renderPieChartFromText(content: String) {
        val lines = content.lines().filter { it.contains("%") && it.contains("-") }
        val dataArray = StringBuilder("['Label', 'Value'],")

        for (line in lines) {
            val parts = line.split("-").map { it.trim() }
            if (parts.size == 2) {
                val label = parts[0]
                val value = parts[1].replace("%", "").toFloatOrNull() ?: continue
                dataArray.append("['$label', $value],")
            }
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
                        var options = {
                            title: 'ChatGPT Pie Chart',
                            pieSliceText: 'percentage',
                            chartArea: { left: 20, top: 40, width: '60%', height: '75%' },   
                            legend: {
                                position: 'right',
                                alignment: 'center',
                                textStyle: {
                                    fontSize: 12
                                }
                            },
                            chartArea: { width: '90%', height: '75%' },
                        };
                        var chart = new google.visualization.PieChart(document.getElementById('chart_div'));
                        chart.draw(data, options);
                    }
                </script>
            </head>
            <body><div id='chart_div' style='width:100%; height:100vh;'></div></body>
            </html>
        """
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
    private fun renderLineChartFromText(content: String) {
        val lines = content.lines().filter { it.contains("-") && it.contains(Regex("\\d")) }
        val dataArray = StringBuilder("[['Date', 'Value'],")
        for (line in lines) {
            val parts = line.split("-").map { it.trim() }
            if (parts.size == 2) {
                val time = parts[0]
                val value = parts[1].replace("%", "").toFloatOrNull() ?: continue
                dataArray.append("[new Date('$time'), $value],")
            }
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
                        var options = {
                            title: 'ChatGPT Time Series',
                            curveType: 'function',
                            chartArea: { width: '85%', height: '70%' },
                            hAxis: { title: 'Time' },
                            vAxis: { title: 'Value' },
                            legend: { position: 'none' }
                        };
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

interface ChatGptService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    fun getCompletion(@Body request: ChatGptRequest): Call<ChatGptResponse>
}

data class ChatGptRequest(
    val model: String, val messages: List<Message>, val max_tokens: Int
)

data class Message(
    val role: String, val content: String
)

data class ChatGptResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)