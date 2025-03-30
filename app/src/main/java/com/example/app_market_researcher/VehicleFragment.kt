package com.example.app_market_researcher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class VehicleFragment : Fragment() {

    private lateinit var btnSelectFile: Button
    private lateinit var btnAnalyze: Button
    private lateinit var tvSelectedFileName: TextView
    private lateinit var tvAnalysisResult: TextView
    private lateinit var audioAnalyzer: AudioAnalyzer
    private val PICK_AUDIO_FILE = 100
    private var selectedAudioFileUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_vehicle, container, false)

        btnSelectFile = view.findViewById(R.id.btnSelectFile)
        btnAnalyze = view.findViewById(R.id.btnAnalyze)
        tvSelectedFileName = view.findViewById(R.id.tvSelectedFileName)
        tvAnalysisResult = view.findViewById(R.id.tvAnalysisResult)

        audioAnalyzer = AudioAnalyzer(requireContext())

        btnSelectFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_AUDIO_FILE)
        }

        btnAnalyze.setOnClickListener {
            selectedAudioFileUri?.let { uri ->
                btnAnalyze.isEnabled = false
                btnAnalyze.text = "Analyzing..."
                thread {
                    val result = audioAnalyzer.analyzeAudio(uri)
                    Handler(Looper.getMainLooper()).post {
                        btnAnalyze.isEnabled = true
                        btnAnalyze.text = "Analyze"
                        showAnalysisResult(result)
                    }
                }
            } ?: Toast.makeText(requireContext(), "Please select a file", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_FILE && resultCode == Activity.RESULT_OK) {
            selectedAudioFileUri = data?.data
            tvSelectedFileName.text = getFileNameFromUri(selectedAudioFileUri!!)
            btnAnalyze.isEnabled = true
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "Unknown"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = it.getString(index)
            }
        }
        return name
    }

    private fun showAnalysisResult(result: Map<String, Any>) {
        val predictedClass = result["predicted_class"] as String
        val confidence = result["confidence"] as Float
        val message = """
            Predicted Class: $predictedClass
            Confidence: ${String.format("%.5f", confidence)}
        """.trimIndent()
        AlertDialog.Builder(requireContext()).setTitle("Result").setMessage(message).setPositiveButton("OK", null).show()
        tvAnalysisResult.text = message
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer.close()
    }
}