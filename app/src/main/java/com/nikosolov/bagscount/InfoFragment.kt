package com.nikosolov.bagscount.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.MediaController
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.nikosolov.bagscount.ImageUploadService
import com.nikosolov.bagscount.R
import com.nikosolov.bagscount.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.edit

class InfoFragment : Fragment(R.layout.fragment_info) {

    private lateinit var previewImage: ImageView
    private lateinit var previewVideo: VideoView
    private lateinit var fileNameTv: TextView
    private lateinit var btnInfo: Button
    private lateinit var infoBlock: LinearLayout

    private lateinit var handbagsTv: TextView
    private lateinit var suitcasesTv: TextView
    private lateinit var backpacksTv: TextView

    companion object {
        private const val PREFS             = "app_prefs"
        private const val KEY_LAST_CODE     = "last_code"
        private const val KEY_LAST_FILENAME = "last_filename"
        private const val KEY_LAST_PATH     = "last_filepath"
    }

    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
    private val api by lazy {
        RetrofitClient.getClient(requireContext()).create(ImageUploadService::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewImage  = view.findViewById(R.id.preview_image)
        previewVideo  = view.findViewById(R.id.preview_video)
        fileNameTv    = view.findViewById(R.id.file_name)
        btnInfo       = view.findViewById(R.id.get_information)
        infoBlock     = view.findViewById(R.id.info_block)

        handbagsTv    = view.findViewById(R.id.handbags_count)
        suitcasesTv   = view.findViewById(R.id.suitcases_count)
        backpacksTv   = view.findViewById(R.id.backpacks_count)

        showLocalFile()

        btnInfo.setOnClickListener {
            val code     = prefs.getString(KEY_LAST_CODE, null)
            val filename = prefs.getString(KEY_LAST_FILENAME, null)
            if (code.isNullOrBlank() || filename.isNullOrBlank()) {
                Toast.makeText(requireContext(),
                    getString(R.string.info_toast_noLastUpload),
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1) Запрашиваем статус
                    val statusResp: Response<com.nikosolov.bagscount.StatusResponse> =
                        api.getStatus(code)

                    if (statusResp.isSuccessful && statusResp.body() != null) {
                        val stats = statusResp.body()!!
                        if (stats.status == "Scanning still going") {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(),
                                    getString(R.string.info_toast_stillScanning),
                                    Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            infoBlock.visibility = View.VISIBLE
                            handbagsTv.text    = "${getString(R.string.info_fragment_handbagCount)}: ${stats.handbags}"
                            suitcasesTv.text   = "${getString(R.string.info_fragment_suitcaseCount)}: ${stats.suitcases}"
                            backpacksTv.text   = "${getString(R.string.info_fragment_backpackCount)}: ${stats.backpacks}"
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(),
                                "${getString(R.string.info_toast_errorStatus)}: ${statusResp.code()}",
                                Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // 2) Только после того, как статус нормальный, скачиваем файл
                    val dlResp: Response<ResponseBody> = api.downloadFile(code)
                    if (dlResp.isSuccessful && dlResp.body() != null) {
                        val body = dlResp.body()!!
                        val outFile = File(requireContext().cacheDir, filename)
                        FileOutputStream(outFile).use { it.write(body.bytes()) }
                        // Сохраняем путь
                        prefs.edit() { putString(KEY_LAST_PATH, outFile.absolutePath) }
                        // И обновляем превью сразу же
                        withContext(Dispatchers.Main) {
                            showFileInView(outFile, filename)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(),
                                "${getString(R.string.info_toast_errorDownload)}: ${dlResp.code()}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(),
                            getString(R.string.info_toast_errorConnection),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showLocalFile() {
        val filename = prefs.getString(KEY_LAST_FILENAME, null)
        val path     = prefs.getString(KEY_LAST_PATH, null)
        if (filename.isNullOrBlank() || path.isNullOrBlank()) {
            Toast.makeText(requireContext(), getString(R.string.info_toast_noLastUpload), Toast.LENGTH_SHORT).show()
            return
        }
        val isVideo = filename.lowercase().endsWith(".mp4")
        if (isVideo)
            fileNameTv.text = "${getString(R.string.info_video)}: ${filename.substringAfter(":")}"
        else
            fileNameTv.text = "${getString(R.string.info_photo)}: ${filename.substringAfter(":")}"

        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(requireContext(), getString(R.string.info_toast_noLocalFile), Toast.LENGTH_SHORT).show()
            return
        }

        previewVideo.stopPlayback()
        previewVideo.visibility = View.GONE
        previewImage.visibility = View.GONE

        if (isVideo) {
            // VideoView
            previewVideo.visibility = View.VISIBLE
            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            requireContext().grantUriPermission(
                "com.android.providers.media",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val mc = MediaController(requireContext())
            mc.setAnchorView(previewVideo)
            previewVideo.setMediaController(mc)
            previewVideo.setOnPreparedListener {
                it.isLooping = false
                previewVideo.seekTo(1)
                previewVideo.start()
            }
            previewVideo.setOnErrorListener { _, what, extra ->
                Toast.makeText(requireContext(),
                    "${getString(R.string.info_toast_errorPlay)} (code=$what, extra=$extra)",
                    Toast.LENGTH_LONG).show()
                true
            }
            previewVideo.setVideoURI(uri)
            previewVideo.requestFocus()
        } else {
            // ImageView с учётом EXIF
            previewImage.visibility = View.VISIBLE
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            val exif = ExifInterface(file.absolutePath)
            val orient = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotation = when (orient) {
                ExifInterface.ORIENTATION_ROTATE_90   -> 90f
                ExifInterface.ORIENTATION_ROTATE_180  -> 180f
                ExifInterface.ORIENTATION_ROTATE_270  -> 270f
                else -> 0f
            }
            val finalBmp: Bitmap = if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            } else {
                bmp
            }
            previewImage.setImageBitmap(finalBmp)
        }
    }

    private fun showFileInView(file: File, filename: String) {
        val isVideo = filename.lowercase().endsWith(".mp4")
        if (isVideo)
            fileNameTv.text = "${getString(R.string.info_video)}: ${filename.substringAfter(":")}"
        else
            fileNameTv.text = "${getString(R.string.info_photo)}: ${filename.substringAfter(":")}"


        previewVideo.stopPlayback()
        previewVideo.visibility = View.GONE
        previewImage.visibility = View.GONE

        if (isVideo) {
            // Видео
            previewVideo.visibility = View.VISIBLE

            val contentUri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            // Разрешаем плееру читать URI
            requireContext().grantUriPermission(
                "com.android.providers.media",
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // Настроим контроллер и старт
            val mc = MediaController(requireContext())
            mc.setAnchorView(previewVideo)
            previewVideo.setMediaController(mc)
            previewVideo.setOnPreparedListener {
                it.isLooping = false
                previewVideo.seekTo(1)
                previewVideo.start()
            }
            previewVideo.setOnErrorListener { _, what, extra ->
                Toast.makeText(requireContext(),
                    "${getString(R.string.info_toast_errorPlay)} (code=$what, extra=$extra)",
                    Toast.LENGTH_LONG).show()
                true
            }
            previewVideo.setVideoURI(contentUri)
            previewVideo.requestFocus()

        } else {
            // Фото
            previewImage.visibility = View.VISIBLE
            // Декодируем вручную, чтобы сразу получить Bitmap
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            previewImage.setImageBitmap(bmp)
        }
    }
}
