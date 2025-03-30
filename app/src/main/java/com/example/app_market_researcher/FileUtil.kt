package com.example.app_market_researcher.utils

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Utility class for file operations related to TensorFlow Lite models
 */
object FileUtil {
    private const val TAG = "FileUtil"

    /**
     * Load TF Lite model file from assets
     *
     * @param context Application context
     * @param modelPath Path to the model file in assets folder
     * @return MappedByteBuffer containing the model file
     */
    @Throws(IOException::class)
    fun loadMappedFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Load labels from text file in assets folder
     *
     * @param context Application context
     * @param labelsPath Path to the labels file in assets folder
     * @return Array of labels
     */
    @Throws(IOException::class)
    fun loadLabels(context: Context, labelsPath: String): Array<String> {
        val labels = ArrayList<String>()
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open(labelsPath)))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.trim()?.let {
                    if (it.isNotEmpty()) {
                        labels.add(it)
                    }
                }
            }
            reader.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error reading labels: ${e.message}")
            throw e
        }
        return labels.toTypedArray()
    }

    /**
     * Get file name from a path
     *
     * @param path File path
     * @return File name
     */
    fun getFileName(path: String): String {
        return path.substring(path.lastIndexOf('/') + 1)
    }
}