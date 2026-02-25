package com.example.mizukisync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mizukisync.network.*
import retrofit2.*

class LoginActivity : AppCompatActivity() {

    private val REQUEST_CODE_LX = 888

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🌟 核心修复1：统一个名字，使用大写的 "MizukiPrefs"
        val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        if (prefs.getString("access_token", null) != null) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        findViewById<Button>(R.id.btn_get_lx_code).setOnClickListener {
            startActivityForResult(Intent(this, WebViewActivity::class.java), REQUEST_CODE_LX)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LX && resultCode == RESULT_OK) {
            val code = data?.getStringExtra("code")
            if (code != null) {
                loginWithBackend(code)
            }
        }
    }

    private fun loginWithBackend(code: String) {
        Toast.makeText(this, "正在验证...", Toast.LENGTH_SHORT).show()

        RetrofitClient.getInstance(this).loginWithLX(code).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // 🌟 核心修复2：统一个名字，使用大写的 "MizukiPrefs"
                    getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE).edit()
                        .putString("access_token", data.token)
                        .putString("refresh_token", data.refresh_token)
                        .apply()

                    goToMain()
                } else {
                    Toast.makeText(this@LoginActivity, "登录失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "连接服务器失败: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}