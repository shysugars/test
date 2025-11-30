package com.example.helloworld



import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var tvOutput: TextView
    private lateinit var btnRun: Button

    // 定义权限请求结果监听器
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            // 权限授予后自动执行
            runShellCommand()
        } else {
            Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvOutput = findViewById(R.id.tvOutput)
        btnRun = findViewById(R.id.btnRun)

        // 注册监听器
        Shizuku.addRequestPermissionResultListener(permissionListener)

        btnRun.setOnClickListener {
            checkAndStart()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 记得移除监听器，防止内存泄漏
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }

    private fun checkAndStart() {
        // 1. 检查 Shizuku 服务是否存活 (用户是否已激活 Shizuku)
        if (!Shizuku.pingBinder()) {
            tvOutput.text = "Shizuku 服务未运行，请先在 Shizuku APP 中启动服务。"
            return
        }

        // 2. 检查是否有权限
        if (checkPermission()) {
            runShellCommand()
        } else {
            // 3. 请求权限 (requestCode 自定义，这里填 0)
            Shizuku.requestPermission(0)
        }
    }

    private fun checkPermission(): Boolean {
        return if (Shizuku.isPreV11()) {
            false
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun runShellCommand() {
        // 耗时操作必须在子线程执行
        thread {
            try {
                // Shizuku.newProcess 用法和 Runtime.getRuntime().exec() 基本一致
                // 这里的 "sh" 运行在 Shizuku 的高权限进程中
                val process = Shizuku.newProcess(arrayOf("sh", "-c", "ls -l /system"), null, null)
                
                // 读取输出流
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                
                // 等待命令结束
                val exitCode = process.waitFor()
                
                // 更新 UI
                runOnUiThread {
                    tvOutput.text = "Exit Code: $exitCode\n\n$output"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvOutput.text = "执行出错: ${e.message}"
                }
            }
        }
    }
}