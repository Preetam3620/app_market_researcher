package com.example.app_market_researcher


import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.ShortBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class AudioAnalyzer(private val context: Context) {
    private var tflite: Interpreter? = null
    private val modelName = "car_sounds_model.tflite"
    private val classNames = arrayOf("fan", "gearbox", "pump", "valve")

    // Feature extraction parameters
    private val inputFeatureSize = 136 // This should match your feature size from PyAudioAnalysis

    // Buffers for inference
    private lateinit var inputBuffer: ByteBuffer
    private lateinit var outputBuffer: ByteBuffer

    init {
        try {
            tflite = Interpreter(loadModelFile())
            setupBuffers()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model", e)
            setupBuffers()
        }
    }

    private fun setupBuffers() {
        try {
            // Setup input buffer
            inputBuffer = ByteBuffer.allocateDirect(inputFeatureSize * 4) // 4 bytes per float
            inputBuffer.order(ByteOrder.nativeOrder())

            // Setup output buffer (4 classes)
            outputBuffer = ByteBuffer.allocateDirect(classNames.size * 4)
            outputBuffer.order(ByteOrder.nativeOrder())
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up buffers", e)

            // Create minimal buffers to prevent crashes
            inputBuffer = ByteBuffer.allocateDirect(4)
            inputBuffer.order(ByteOrder.nativeOrder())
            outputBuffer = ByteBuffer.allocateDirect(4)
            outputBuffer.order(ByteOrder.nativeOrder())
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
            // Convert Uri to a temporary file
            val tempFile = createTempFileFromUri(uri)

            // Process the file
            val audioData = readWavFile(tempFile)

            // Extract features
            val features = extractFeatures(audioData)

            // Run inference
            val result = runInference(features)

            // Clean up temp file
            tempFile.delete()

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

    // Create a temporary file from Uri
    private fun createTempFileFromUri(uri: Uri): File {
        val tempFile = File.createTempFile("audio_", ".wav", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    // Read WAV file and extract audio data
    private fun readWavFile(file: File): ShortArray {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.path)

            // Get the audio track
            val format = extractor.getTrackFormat(0)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            Log.d(TAG, "WAV file: $sampleRate Hz, $channelCount channels")

            // Read the raw audio data
            val fileInputStream = FileInputStream(file)
            // Skip WAV header (44 bytes)
            fileInputStream.skip(44)

            // Calculate file size and allocate buffer
            val fileSize = file.length() - 44 // Subtract header size
            val samples = (fileSize / 2).toInt() // 2 bytes per sample (16-bit)
            val audioData = ShortArray(samples)

            // Read the data
            val buffer = ByteBuffer.allocate(samples * 2)
            fileInputStream.channel.read(buffer)
            buffer.flip()

            // Convert to short array
            val shortBuffer = buffer.asShortBuffer()
            shortBuffer.get(audioData)
            fileInputStream.close()

            // If stereo, convert to mono by averaging channels
            if (channelCount == 2) {
                val monoData = ShortArray(samples / 2)
                for (i in 0 until samples / 2) {
                    monoData[i] = ((audioData[i * 2].toInt() + audioData[i * 2 + 1].toInt()) / 2).toShort()
                }
                return monoData
            }

            return audioData
        } catch (e: Exception) {
            Log.e(TAG, "Error reading WAV file", e)
            throw e
        }
    }

    // Extract features from audio data
    private fun extractFeatures(audioData: ShortArray): FloatArray {
        // Convert audio data to float
        val floatData = FloatArray(audioData.size)
        for (i in audioData.indices) {
            floatData[i] = audioData[i] / 32768.0f
        }

        // Calculate statistical features
        val features = FloatArray(inputFeatureSize)

        // Basic statistics
        // Mean
        var mean = 0.0f
        for (sample in floatData) {
            mean += sample
        }
        mean /= floatData.size

        // Variance and std dev
        var variance = 0.0f
        for (sample in floatData) {
            variance += (sample - mean).pow(2)
        }
        variance /= floatData.size
        val stdDev = sqrt(variance)

        // Zero crossing rate
        var zcr = 0
        for (i in 1 until floatData.size) {
            if ((floatData[i] >= 0 && floatData[i-1] < 0) ||
                (floatData[i] < 0 && floatData[i-1] >= 0)) {
                zcr++
            }
        }
        val zcrNormalized = zcr.toFloat() / floatData.size

        // Root mean square (energy)
        var rms = 0.0f
        for (sample in floatData) {
            rms += sample.pow(2)
        }
        rms = sqrt(rms / floatData.size)

        // Spectral features would be calculated here in a full implementation
        // For now we'll use placeholder values

        // In a real implementation, this would compute the exact same features as PyAudioAnalysis
        // For now, we'll just fill the array with repeated basic features
        for (i in features.indices) {
            features[i] = when (i % 5) {
                0 -> mean
                1 -> stdDev
                2 -> zcrNormalized
                3 -> rms
                else -> 0.5f // Placeholder for spectral features
            }
        }

        // Feature scaling
        // In a production implementation, this would apply the exact same scaling
        // coefficients as used in training

        return features
    }

    // Run inference with the TFLite model
    private fun runInference(features: FloatArray): Map<String, Any> {
        try {
            // Prepare input buffer
            inputBuffer.rewind()
            for (feature in features) {
                inputBuffer.putFloat(feature)
            }

            // Run inference
            outputBuffer.rewind()
            tflite?.run(inputBuffer, outputBuffer)

            // Process results
            outputBuffer.rewind()
            val outputArray = FloatArray(classNames.size)
            for (i in outputArray.indices) {
                outputArray[i] = outputBuffer.getFloat()
            }

            // Find the class with highest probability
            var maxIndex = 0
            for (i in 1 until outputArray.size) {
                if (outputArray[i] > outputArray[maxIndex]) {
                    maxIndex = i
                }
            }

            // Create result map
            val result = mutableMapOf<String, Any>()
            result["predicted_class"] = classNames[maxIndex]
            result["confidence"] = outputArray[maxIndex]

            // Add all class probabilities
            for (i in classNames.indices) {
                result[classNames[i]] = outputArray[i]
            }

            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error during inference", e)
            return mapOf(
                "error" to e.message.toString(),
                "predicted_class" to "unknown",
                "confidence" to 0.0f
            )
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