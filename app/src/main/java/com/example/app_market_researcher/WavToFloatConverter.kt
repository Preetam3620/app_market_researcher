package com.example.app_market_researcher.utils

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

/**
 * Utility class to convert WAV files to float arrays for TensorFlow Lite processing
 */
object WavToFloatConverter {
    private const val TAG = "WavToFloatConverter"
    private const val SAMPLE_RATE = 16000
    private const val RECORDING_LENGTH = 16000  // 1 second at 16kHz

    /**
     * Reads audio file from Uri and converts to float array for TFLite input
     * @param context Application context
     * @param uri Uri of the audio file
     * @return 3D float array [1][RECORDING_LENGTH][1] for model input
     */
    fun readAsFloat3D(context: Context, uri: Uri): Array<Array<FloatArray>>? {
        try {
            // Create temporary file from Uri
            val tempFile = createTempFileFromUri(context, uri)

            // Extract audio data
            val audioData = extractPCMFromWAV(tempFile)

            // Clean up temp file
            tempFile.delete()

            if (audioData == null || audioData.isEmpty()) {
                Log.e(TAG, "Failed to extract audio data")
                return null
            }

            // Convert to the expected 3D array format for TFLite
            // [batch_size=1][audio_length][channels=1]
            val result = Array(1) {
                Array(RECORDING_LENGTH) {
                    FloatArray(1)
                }
            }

            // Fill the array with normalized values
            // Only use the first second (16000 samples)
            val sampleCount = minOf(audioData.size, RECORDING_LENGTH)
            for (i in 0 until sampleCount) {
                // Normalize to [-1, 1]
                result[0][i][0] = audioData[i] / 32768.0f
            }

            // Zero-pad if audio is shorter than expected
            for (i in sampleCount until RECORDING_LENGTH) {
                result[0][i][0] = 0.0f
            }

            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error converting audio: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Creates a temporary file from the provided Uri
     */
    private fun createTempFileFromUri(context: Context, uri: Uri): File {
        val tempFile = File.createTempFile("audio_", ".wav", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    /**
     * Extracts PCM audio data from WAV file
     * @param wavFile WAV file
     * @return Array of audio samples (16-bit PCM converted to shorts)
     */
    private fun extractPCMFromWAV(wavFile: File): ShortArray? {
        try {
            // For simple WAV files, we can just skip the header
            val fileInputStream = FileInputStream(wavFile)

            // Skip WAV header (usually 44 bytes)
            fileInputStream.skip(44)

            // Calculate file size and allocate buffer
            val fileSize = wavFile.length() - 44
            val samples = (fileSize / 2).toInt() // 2 bytes per sample for 16-bit PCM

            // Read the audio data
            val buffer = ByteBuffer.allocate(samples * 2)
            fileInputStream.channel.read(buffer)
            buffer.rewind()
            buffer.order(ByteOrder.LITTLE_ENDIAN) // WAV files are little-endian

            // Convert to short array
            val audioData = ShortArray(samples)
            val shortBuffer = buffer.asShortBuffer()
            shortBuffer.get(audioData)

            fileInputStream.close()
            return audioData
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting PCM: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Alternative method using MediaExtractor for more complex audio files
     * This handles resampling, channel conversion, etc.
     */
    private fun extractAudioUsingMediaExtractor(context: Context, uri: Uri): ShortArray? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)

            // Find audio track
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)

                if (mime?.startsWith("audio/") == true) {
                    extractor.selectTrack(i)

                    // Get audio properties
                    val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

                    Log.d(TAG, "Audio track found: $sampleRate Hz, $channelCount channels")

                    // For a full implementation, you would use MediaCodec here to decode
                    // and possibly resample the audio to match your model requirements

                    break
                }
            }

            // For now, revert to the simpler approach
            return null
        } catch (e: Exception) {
            Log.e(TAG, "MediaExtractor error: ${e.message}")
            return null
        } finally {
            extractor.release()
        }
    }
}