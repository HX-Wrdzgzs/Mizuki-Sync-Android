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

class LoginActivity : AppCompatActivity() {

    private val REQUEST_CODE_LX = 888

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (getSharedPreferences("mizuki_prefs", Context.MODE_PRIVATE).contains("token")) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_login)

        findViewById<Button>(R.id.btn_get_lx_code).setOnClickListener {
            startActivityForResult(Intent(this, WebViewActivity::class.java), REQUEST_CODE_LX)
        }

        val etToken = findViewById<EditText>(R.id.et_token)
        findViewById<Button>(R.id.btn_login).setOnClickListener {
            val token = etToken.text.toString().trim()
            if (token.isNotEmpty()) loginDF(token)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LX && resultCode == RESULT_OK) {
            val code = data?.getStringExtra("code")
            if (code != null) loginLX(code)
        }
    }

    private fun loginLX(code: String) {
        RetrofitClient.getInstance(this).loginWithLX(code).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) saveAndJump()
                else Toast.makeText(this@LoginActivity, "登录失败", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loginDF(token: String) {
        RetrofitClient.getInstance(this).loginWithDF(token).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) saveAndJump()
                else Toast.makeText(this@LoginActivity, "Token无效", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveAndJump() {
        getSharedPreferences("mizuki_prefs", Context.MODE_PRIVATE).edit()
            .putString("token", "hybrid")
            .apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}