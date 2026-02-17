package com.example.mizukisync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mizukisync.network.*
import retrofit2.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class LoginActivity : AppCompatActivity() {

    private val REQUEST_CODE_LX = 888
    private lateinit var etServerUrl: EditText
    private var discoverySocket: DatagramSocket? = null
    private var isDiscovering = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("mizuki_prefs", Context.MODE_PRIVATE)
        if (prefs.getString("access_token", null) != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_login)

        etServerUrl = findViewById(R.id.et_server_url)
        val savedUrl = prefs.getString("server_url", "http://10.0.2.2:8000")
        etServerUrl.setText(savedUrl)

        findViewById<Button>(R.id.btn_get_lx_code).setOnClickListener {
            saveServerUrl()
            startActivityForResult(Intent(this, WebViewActivity::class.java), REQUEST_CODE_LX)
        }

        // --- 启动自动发现 ---
        startAutoDiscovery()
    }

    private fun startAutoDiscovery() {
        Thread {
            try {
                // 监听 8888 端口
                discoverySocket = DatagramSocket(8888)
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isDiscovering) {
                    discoverySocket?.receive(packet)
                    val msg = String(packet.data, 0, packet.length)

                    // 如果收到暗号
                    if (msg.startsWith("MIZUKI_SYNC")) {
                        val port = msg.split("|")[1]
                        val ip = packet.address.hostAddress
                        val newUrl = "http://$ip:$port"

                        // 更新 UI
                        runOnUiThread {
                            if (etServerUrl.text.toString() != newUrl) {
                                etServerUrl.setText(newUrl)
                                Toast.makeText(this, "已自动发现局域网服务器！\n$newUrl", Toast.LENGTH_LONG).show()
                                saveServerUrl() // 自动保存
                            }
                        }
                        break // 找到了就停止
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        isDiscovering = false
        discoverySocket?.close()
    }

    private fun saveServerUrl() {
        val url = etServerUrl.text.toString().trim()
        if (url.isNotEmpty()) {
            getSharedPreferences("mizuki_prefs", Context.MODE_PRIVATE).edit()
                .putString("server_url", url)
                .apply()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LX && resultCode == RESULT_OK) {
            val code = data?.getStringExtra("code")
            if (code != null) loginWithBackend(code)
        }
    }

    private fun loginWithBackend(code: String) {
        Toast.makeText(this, "登录中...", Toast.LENGTH_SHORT).show()
        RetrofitClient.getInstance(this).loginWithLX(code).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    getSharedPreferences("mizuki_prefs", Context.MODE_PRIVATE).edit()
                        .putString("access_token", data.token)
                        .apply()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}