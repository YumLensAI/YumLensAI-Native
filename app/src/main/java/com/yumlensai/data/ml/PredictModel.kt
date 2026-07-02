package com.yumlensai.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PredictModel(private val context: Context) {

    companion object {
        private const val MODEL_FILE = "models/fruits.tflite"
        private const val INPUT_SIZE = 256
        private const val CONFIDENCE_THRESHOLD = 0.7f
        private const val NUM_CLASSES = 7

        private val CLASS_NAMES = mapOf(
            1 to "apple",
            2 to "banana",
            3 to "kiwi",
            4 to "lemon",
            5 to "orange",
            6 to "strawberry"
        )
    }

    private var interpreter: Interpreter? = null

    sealed class State {
        object Loading : State()
        object Loaded : State()
        object Error : State()
    }

    var state: State = State.Loading
        private set

    fun load() {
        if (interpreter != null) {
            state = State.Loaded
            return
        }
        state = try {
            val model = loadModelFile()
            interpreter = Interpreter(model)
            logTensorInfo()
            State.Loaded
        } catch (e: Exception) {
            State.Error
        }
    }

    private fun logTensorInfo() {
        val interp = interpreter ?: return
        repeat(interp.outputTensorCount) { i ->
            val t = interp.getOutputTensor(i)
            Log.d("PredictModel", "output[$i] name=${t.name()} shape=${t.shape().toList()} elements=${t.numElements()}")
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun runInference(imageUri: Uri): List<String> {
        val interpreter = interpreter ?: return emptyList()

        val bitmap = loadBitmap(imageUri) ?: return emptyList()
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val inputBuffer = bitmapToByteBuffer(resized)

        // Output tensor 1 shape is [1, numDetections, numClasses] — must match exactly
        val shape = interpreter.getOutputTensor(1).shape()
        val outputScores = Array(shape[0]) { Array(shape[1]) { FloatArray(shape[2]) } }

        interpreter.runForMultipleInputsOutputs(
            arrayOf(inputBuffer),
            mapOf(1 to outputScores)
        )

        return getDetectedClasses(outputScores)
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }

    // Mirrors React Native's getClassNames: argmax per detection, skip background (class 0)
    private fun getDetectedClasses(scores: Array<Array<FloatArray>>): List<String> {
        val detected = mutableSetOf<String>()
        for (detection in scores[0]) {
            var bestScore = detection[0]
            var bestClass = 0
            for (j in 1 until detection.size) {
                if (detection[j] > bestScore) {
                    bestScore = detection[j]
                    bestClass = j
                }
            }
            if (bestScore >= CONFIDENCE_THRESHOLD && bestClass > 0) {
                CLASS_NAMES[bestClass]?.let { detected.add(it) }
            }
        }
        return detected.toList()
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
