package com.yumlensai.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PredictModel(private val context: Context) {

    companion object {
        private const val MODEL_FILE = "models/fruits.tflite"
        private const val INPUT_SIZE = 256
        private const val CONFIDENCE_THRESHOLD = 0.7f

        // Index i holds the label for class id (i + 1); class id 0 is background and has no slot.
        private val CLASS_NAMES = arrayOf("apple", "banana", "kiwi", "lemon", "orange", "strawberry")
    }

    private var interpreter: Interpreter? = null

    // Guards interpreter reads/writes so close() (called from onCleared(), main thread)
    // can never run concurrently with runInference() (called from a background dispatcher) —
    // without this, close() can free the native interpreter mid-inference and crash with SIGSEGV.
    private val interpreterLock = Any()

    sealed class State {
        object Loading : State()
        object Loaded : State()
        object Error : State()
    }

    var state: State = State.Loading
        private set

    fun load() {
        synchronized(interpreterLock) {
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
        val rawBitmap = loadBitmap(imageUri) ?: return emptyList()
        val bitmap = centerCropToSquare(rawBitmap)
        val rotationDegrees = readExifRotationDegrees(imageUri)
        val inputTensor = preprocess(bitmap, rotationDegrees)

        synchronized(interpreterLock) {
            val interpreter = interpreter ?: return emptyList()

            // Output tensor 1 shape is [1, numDetections, numClasses] — must match exactly
            val outputTensorInfo = interpreter.getOutputTensor(1)
            val outputBuffer = TensorBuffer.createFixedSize(outputTensorInfo.shape(), outputTensorInfo.dataType())

            interpreter.runForMultipleInputsOutputs(
                arrayOf(inputTensor.buffer),
                mapOf(1 to outputBuffer.buffer)
            )

            return getDetectedClasses(outputBuffer, outputTensorInfo.shape())
        }
    }

    // Non-square photos (any real camera shot) get squeezed out of proportion by a plain
    // resize-to-square. Cropping to the largest centered square first — matching how the
    // bundled square training/test images were framed — avoids distorting the subject.
    private fun centerCropToSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    private fun preprocess(bitmap: Bitmap, rotationDegrees: Int): TensorImage {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(Rot90Op(-rotationDegrees / 90))
            .add(NormalizeOp(0f, 255f))
            .build()

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage)
    }

    // Reads how many degrees the raw image must be rotated clockwise to appear upright,
    // matching what Coil already applies automatically when displaying the same photo.
    private fun readExifRotationDegrees(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val orientation = ExifInterface(stream)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // Camera photos can be several thousand pixels wide (e.g. 3072x4080). Decoding them at full
    // resolution just to immediately shrink to 256x256 wastes tens of MB per photo and can throw
    // OutOfMemoryError (an Error, not an Exception — a bare catch(Exception) around the full-size
    // decode silently swallows the OOM further up the call chain and looks like "nothing detected").
    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, INPUT_SIZE)
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var inSampleSize = 1
        val smallestSide = minOf(width, height)
        while (smallestSide / (inSampleSize * 2) >= targetSize) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    // Same rule as the RN version (argmax per detection, skip background, 0.7 threshold),
    // reimplemented with unboxed array indexing instead of a Map<Int, String> lookup.
    private fun getDetectedClasses(outputBuffer: TensorBuffer, shape: IntArray): List<String> {
        val numDetections = shape[1]
        val numClasses = shape[2]
        val scores = outputBuffer.floatArray

        val detected = mutableSetOf<String>()
        for (d in 0 until numDetections) {
            val base = d * numClasses
            var bestScore = scores[base]
            var bestClass = 0
            for (j in 1 until numClasses) {
                val score = scores[base + j]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = j
                }
            }
            if (bestClass > 0 && bestScore >= CONFIDENCE_THRESHOLD) {
                detected.add(CLASS_NAMES[bestClass - 1])
            }
        }
        return detected.toList()
    }

    fun close() {
        synchronized(interpreterLock) {
            interpreter?.close()
            interpreter = null
        }
    }
}
