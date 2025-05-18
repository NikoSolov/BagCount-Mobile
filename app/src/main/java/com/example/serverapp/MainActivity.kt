package com.example.serverapp

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.create

class MainActivity : ComponentActivity() {
    private lateinit var getImageLauncher: ActivityResultLauncher<String>
    private lateinit var apiService: ImageUploadService
    // ActivityResultLauncher для получения изображения с камеры (если нужно, здесь нужен FileProvider)
    // private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

    private var imageUri: Uri? = null  // будет хранить путь к выбранному изображению

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val baseUrlEditText = findViewById<EditText>(R.id.baseUrlEditText)
        val submitButton = findViewById<Button>(R.id.submitButton)

        submitButton.setOnClickListener {
            val baseUrl = "http://" + baseUrlEditText.text.toString().trim() + ":5000"
            if (baseUrl.isNotEmpty()) {
                // Получение экземпляра Retrofit с введённым BASE_URL
                val retrofit = RetrofitClient.getClient(baseUrl)
                // Теперь вы можете использовать retrofit для создания вашего API интерфейса
                apiService = retrofit.create(ImageUploadService::class.java)

                // Добавьте свои действия с apiService здесь
            } else {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }



        // Инициализация ActivityResultLauncher для выбора изображения
        getImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                imageUri = uri
                Toast.makeText(this, "Изображение выбрано!", Toast.LENGTH_SHORT).show()
            }
        }

        // Кнопка для выбора изображения
        val btnChooseImage = findViewById<Button>(R.id.btnChooseImage)
        btnChooseImage.setOnClickListener {
            // Запускаем выбор изображения с фильтром на изображения
            getImageLauncher.launch("image/*")
        }

        // Кнопка для загрузки изображения на сервер
        val btnUpload = findViewById<Button>(R.id.btnUploadImage)
        btnUpload.setOnClickListener {
            if (imageUri != null) {
                // Запуск загрузки изображения на сервер (в отдельной корутине)
                CoroutineScope(Dispatchers.IO).launch {
                    uploadImage(imageUri!!)
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Сначала выберите изображение", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Функция для работы с Retrofit
    private suspend fun uploadImage(uri: Uri) {
        try {
            // 1. Получаем экземпляр сервиса Retrofit
//            val apiService = RetrofitClient.getRetrofitInstance().create(ImageUploadService::class.java)

            // 2. Получаем код доступа (GET запрос на "/")
            val codeResponse = apiService.getAccessCode()
            if (!codeResponse.isSuccessful || codeResponse.body().isNullOrEmpty()) {
                // Обработка ошибки
                runOnUiThread {
                    Toast.makeText(this, "Ошибка получения кода доступа", Toast.LENGTH_SHORT).show()
                }
                return
            }
            val accessCode = codeResponse.body()!!

            // 3. Преобразуем изображение по URI в RequestBody
            // Здесь чтение байтов изображения через contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val imageBytes = inputStream?.readBytes()
            inputStream?.close()

            if (imageBytes == null) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка чтения изображения", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // Используем extension-функции Kotlin для создания RequestBody
            val requestBody = imageBytes.toRequestBody("image/*".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("image", "upload.jpg", requestBody)

            // 4. Отправляем изображение (POST запрос на "/{код}")
            val uploadResponse = apiService.uploadImage(accessCode, multipartBody)
            if (!uploadResponse.isSuccessful) {
                runOnUiThread {
                    Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // 5. Получаем информацию по изображению (GET запрос на "/{код}")
            val infoResponse = apiService.getImageInfo(accessCode)
            if (infoResponse.isSuccessful) {
                val imageInfo = infoResponse.body()
                // Обновляем UI или обрабатываем полученные данные
                runOnUiThread {
                    Toast.makeText(this, "Изображение загружено: ${imageInfo}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}