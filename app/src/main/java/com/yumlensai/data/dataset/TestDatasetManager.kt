package com.yumlensai.data.dataset

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Copies the 112 packaged benchmark images (`assets/images/imgNNN.jpg`) to
 * the app's cache directory so they're addressable by `file://` URIs, which
 * is what [com.yumlensai.data.ml.PredictModel] and the multipart upload path
 * both expect.
 */
class TestDatasetManager(private val context: Context) {

    private val datasetDir: File =
        File(context.cacheDir, "$DATASET_FOLDER/$TEST_SUBFOLDER").apply { mkdirs() }

    suspend fun prepareAll() {
        withContext(Dispatchers.IO) {
            for (i in 1..TOTAL_IMAGES) {
                val dest = destFileFor(i)
                if (dest.exists()) continue
                context.assets.open("$ASSETS_IMAGES_DIR/${getImageFileName(i)}").use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
            }
        }
    }

    fun getImageUri(index: Int): Uri {
        require(index in 1..TOTAL_IMAGES) {
            "Test dataset index $index out of range [1, $TOTAL_IMAGES]"
        }
        return destFileFor(index).toUri()
    }

    private fun getImageFileName(index: Int): String = "img%03d.jpg".format(index)

    private fun destFileFor(index: Int): File = File(datasetDir, getImageFileName(index))

    companion object {
        const val TOTAL_IMAGES = 112

        private const val DATASET_FOLDER = "datasets"
        private const val TEST_SUBFOLDER = "test"
        private const val ASSETS_IMAGES_DIR = "images"
    }
}
