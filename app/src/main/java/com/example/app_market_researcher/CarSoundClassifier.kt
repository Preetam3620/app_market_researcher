package com.example.app_market_researcher

// CarSoundClassifier.kt

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.ShortBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.support.common.FileUtil
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class CarSoundClassifier(private val context: Context) {
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
        }
    }

    private fun setupBuffers() {
        // Setup input buffer
        inputBuffer = ByteBuffer.allocateDirect(inputFeatureSize * 4) // 4 bytes per float
        inputBuffer.order(ByteOrder.nativeOrder())

        // Setup output buffer (4 classes)
        outputBuffer = ByteBuffer.allocateDirect(classNames.size * 4)
        outputBuffer.order(ByteOrder.nativeOrder())
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

    // Read WAV file and extract audio data
    fun readWavFile(file: File): ShortArray {
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

    // Audio feature extraction (simplified version)
    fun extractFeatures(audioData: ShortArray): FloatArray {
        // Convert audio data to float
        val floatData = FloatArray(audioData.size)
        for (i in audioData.indices) {
            floatData[i] = audioData[i] / 32768.0f
        }

        // Calculate statistical features
        val features = FloatArray(inputFeatureSize)

        // Calculate basic statistics

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

        // Spectral centroid (simplified)
        val fftSize = 1024
        val spectralCentroid = 0.42f  // Placeholder

        // In a real implementation, you would calculate the full set of features
        // For now, just fill the array with these basic features repeated
        for (i in features.indices) {
            features[i] = when (i % 5) {
                0 -> mean
                1 -> stdDev
                2 -> zcrNormalized
                3 -> rms
                else -> spectralCentroid
            }
        }

        // Feature scaling (simplified)
        // In reality, you'd apply the exact same scaler from your Python code
        for (i in features.indices) {
            features[i] = (features[i] - 0.0f) / 1.0f  // Rough normalization
        }

        return features
    }

    fun classifyWavFile(wavFile: File): CarSoundResult {
        // Read WAV file
        val audioData = readWavFile(wavFile)

        // Extract features
        val features = extractFeatures(audioData)

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

        return CarSoundResult(
            classNames[maxIndex],
            outputArray[maxIndex],
            outputArray
        )
    }

    fun close() {
        tflite?.close()
    }

    companion object {
        private const val TAG = "CarSoundClassifier"
    }

    data class CarSoundResult(
        val className: String,
        val confidence: Float,
        val allConfidences: FloatArray
    )
}