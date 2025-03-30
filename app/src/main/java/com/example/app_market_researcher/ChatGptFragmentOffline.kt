package com.example.app_market_researcher

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ChatGptFragmentOffline : Fragment() {

    private lateinit var promptEditText: EditText
    private lateinit var sendPromptButton: Button
    private lateinit var responseTextView: TextView

    private val huggingFaceUrl = "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.1"
    private val hfApiKey = "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"
    private val ragServerUrl = "https://ebf5-35-245-94-4.ngrok-free.app/ask"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_gpt_offline, container, false)

        promptEditText = view.findViewById(R.id.promptEditText)
        sendPromptButton = view.findViewById(R.id.sendPromptButton)
        responseTextView = view.findViewById(R.id.responseTextView)

        sendPromptButton.setOnClickListener {
            val prompt = promptEditText.text.toString().trim()
            if (prompt.isNotEmpty()) {
                callRagBackend(prompt)
            } else {
                Toast.makeText(requireContext(), "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun callRagBackend(prompt: String) {
        val client = OkHttpClient()

        val json = JSONObject().put("query", prompt)
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(ragServerUrl)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    responseTextView.text = "❌ Request failed: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseCode = response.code
                val responseBody = response.body?.string()

                Log.d("RAG_HTTP", "Status: $responseCode")
                Log.d("RAG_RESPONSE", "Body: $responseBody")

                activity?.runOnUiThread {
                    try {
                        val jsonResponse = JSONObject(responseBody!!)
                        val answer = jsonResponse.getString("answer")

                        // Remove "Context:" header if present
                        var displayAnswer = answer
                        if (answer.contains("Context:")) {
                            displayAnswer = displayAnswer.substringAfter("Context:").trim()
                        }
                        if (displayAnswer.contains("Question:")) {
                            displayAnswer = displayAnswer.substringBefore("Question:").trim()
                        }

                        responseTextView.text = displayAnswer
                    } catch (e: Exception) {
                        responseTextView.text = "❌ Failed to parse RAG response"
                        Log.e("RAG_PARSE", "Error: ${e.message}", e)
                    }
                }
            }
        })
    }
}
