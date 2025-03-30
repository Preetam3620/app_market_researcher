package com.example.app_market_researcher

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.IOException
import com.example.app_market_researcher.utils.FileUtil
import com.example.app_market_researcher.utils.WavToFloatConverter

class AudioAnalyzer(private val context: Context) {
    private var tflite: Interpreter? = null
    private val modelName = "audio_model.tflite"
    private val classNames = arrayOf("fan", "gearbox", "pump", "valve")

    init {
        try {
            tflite = Interpreter(loadModelFile())
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model", e)
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Main function to analyze audio from a Uri
    fun analyzeAudio(uri: Uri): Map<String, Any> {
        try {
            // Read audio and process using the WavToFloatConverter (similar to the Java example)
            val input = WavToFloatConverter.readAsFloat3D(context, uri)
            if (input == null || input[0].size != 16000) {
                Log.e(TAG, "Audio must be 1-second WAV (16kHz mono)")
                return mapOf(
                    "error" to "Audio must be 1-second WAV (16kHz mono)",
                    "predicted_class" to "unknown",
                    "confidence" to 0.0f
                )
            }

            // Create output buffer (4 classes)
            val output = Array(1) { FloatArray(classNames.size) }

            // Run inference
            tflite?.run(input, output)

            // Find the class with highest probability
            var maxIndex = 0
            var maxConf = 0f
            for (i in output[0].indices) {
                if (output[0][i] > maxConf) {
                    maxConf = output[0][i]
                    maxIndex = i
                }
            }

            // Create result map
            val result = mutableMapOf<String, Any>()
            result["predicted_class"] = classNames[maxIndex]
            result["confidence"] = maxConf

            // Add all class probabilities
            for (i in classNames.indices) {
                result[classNames[i]] = output[0][i]
            }

            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing audio", e)
            return mapOf(
                "error" to e.message.toString(),
                "predicted_class" to "unknown",
                "confidence" to 0.0f
            )
        }
    }

    // Simplified version similar to the Java implementation
    fun runMLModel(uri: Uri): Pair<String, Float> {
        try {
            val input = WavToFloatConverter.readAsFloat3D(context, uri)
            if (input == null || input[0].size != 16000) {
                return Pair("error", 0.0f)
            }

            val output = Array(1) { FloatArray(classNames.size) }

            tflite?.run(input, output)

            var maxIndex = 0
            var maxConf = 0f
            for (i in output[0].indices) {
                if (output[0][i] > maxConf) {
                    maxConf = output[0][i]
                    maxIndex = i
                }
            }

            return Pair(classNames[maxIndex], maxConf)
        } catch (e: Exception) {
            Log.e(TAG, "Error in runMLModel", e)
            return Pair("error: ${e.message}", 0.0f)
        }
    }

    // Clean up resources
    fun close() {
        tflite?.close()
    }

    companion object {
        private const val TAG = "AudioAnalyzer"
    }
}