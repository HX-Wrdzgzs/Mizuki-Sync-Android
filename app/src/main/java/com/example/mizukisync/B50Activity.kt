package com.example.mizukisync

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.example.mizukisync.network.UnsafeOkHttpClient
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class B50Activity : AppCompatActivity() {
    private lateinit var pbLoading: ProgressBar
    private lateinit var clImageContainer: ConstraintLayout
    private lateinit var pvFullImage: PhotoView
    private var currentFriendCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_b50)

        pbLoading = findViewById(R.id.pbLoading)
        clImageContainer = findViewById(R.id.cl_image_container)
        pvFullImage = findViewById(R.id.pvFullImage)

        // 从缓存中读取好友码
        val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        val profile = prefs.getString("user_profile_cache", "") ?: ""
        currentFriendCode = if (profile.isNotEmpty()) JSONObject(profile).optString("friend_code", "") else ""

        // 按钮绑定
        findViewById<Button>(R.id.btn_get_high_res).setOnClickListener { downloadAndShowImage() }
        findViewById<ImageButton>(R.id.btnCloseImage).setOnClickListener { clImageContainer.visibility = View.GONE }

        // 🌟 核心修复：绑定保存按钮逻辑
        findViewById<Button>(R.id.btnSaveImage).setOnClickListener { saveToGallery() }
    }

    private fun downloadAndShowImage() {
        if (currentFriendCode.isEmpty()) {
            Toast.makeText(this, "未找到好友码", Toast.LENGTH_SHORT).show()
            return
        }
        pbLoading.visibility = View.VISIBLE
        val url = "https://b50.mizuki.top/api/draw/b50?friend_code=$currentFriendCode&t=${System.currentTimeMillis()}"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = UnsafeOkHttpClient.getUnsafeOkHttpClient()
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes() ?: return@launch
                    withContext(Dispatchers.Main) {
                        Glide.with(this@B50Activity).load(bytes).into(pvFullImage)
                        pbLoading.visibility = View.GONE
                        clImageContainer.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this@B50Activity, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🌟 核心功能：保存图片到相册
    private fun saveToGallery() {
        val drawable = pvFullImage.drawable
        if (drawable == null || drawable !is BitmapDrawable) {
            Toast.makeText(this, "图片尚未加载完成", Toast.LENGTH_SHORT).show()
            return
        }
        val bitmap = drawable.bitmap

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val filename = "Mizuki_B50_${System.currentTimeMillis()}.png"
                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    // 在相册中创建专门的文件夹
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MizukiSync")
                }

                // 插入到系统媒体库
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@B50Activity, "保存成功！请查看相册 MizukiSync 文件夹", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MizukiSync", "保存失败", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@B50Activity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}