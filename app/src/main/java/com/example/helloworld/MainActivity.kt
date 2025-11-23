package com.example.helloworld


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvStatus = findViewById<TextView>(R.id.tv_status)
        val btnAuth = findViewById<Button>(R.id.btn_authenticate)

        // 1. 初始化 Executor (用于在主线程回调)
        executor = ContextCompat.getMainExecutor(this)

        // 2. 配置回调处理 (成功、错误、失败)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                
                // 验证错误 (如: 用户取消、尝试次数过多被锁定)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    tvStatus.text = "验证错误: $errString"
                    Toast.makeText(applicationContext, "验证错误: $errString", Toast.LENGTH_SHORT).show()
                }

                // 验证成功
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    tvStatus.text = "验证成功！已通过指纹/生物识别"
                    tvStatus.setTextColor(ContextCompat.getColor(applicationContext, android.R.color.holo_green_dark))
                    Toast.makeText(applicationContext, "验证通过", Toast.LENGTH_SHORT).show()
                }

                // 验证失败 (如: 指纹不匹配，但可以重试)
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    tvStatus.text = "验证失败: 指纹不匹配，请重试"
                    Toast.makeText(applicationContext, "指纹不匹配", Toast.LENGTH_SHORT).show()
                }
            })

        // 3. 配置弹窗信息
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("指纹登录")
            .setSubtitle("请验证指纹以继续")
            .setNegativeButtonText("取消") // 如果允许使用密码，可以使用 setAllowedAuthenticators
            .build()

        // 4. 按钮点击事件
        btnAuth.setOnClickListener {
            checkAndAuthenticate()
        }
    }

    private fun checkAndAuthenticate() {
        val biometricManager = BiometricManager.from(this)
        
        // 检查设备是否支持生物识别
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // 设备支持且已录入指纹 -> 发起验证
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "设备没有生物识别硬件", Toast.LENGTH_LONG).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "生物识别硬件当前不可用", Toast.LENGTH_LONG).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // 提示用户去设置中录入指纹
                Toast.makeText(this, "您尚未录入指纹，请去设置中添加", Toast.LENGTH_LONG).show()
                
                // (可选) 跳转到系统安全设置页面
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG)
                    }
                    startActivityForResult(enrollIntent, 100)
                }
            }
            else -> {
                Toast.makeText(this, "您的设备暂不支持生物识别", Toast.LENGTH_SHORT).show()
            }
        }
    }
}