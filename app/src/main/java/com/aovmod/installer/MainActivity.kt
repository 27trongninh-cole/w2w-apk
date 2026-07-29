package com.aovmod.installer

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.aovmod.installer.databinding.ActivityMainBinding
import com.aovmod.installer.databinding.DialogErrorLogBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pickedZipUri: Uri? = null
    private val logBuilder = StringBuilder()

    private val pickZipLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pickedZipUri = uri
            binding.tvSelectedFile.text = "Đã chọn: ${queryFileName(uri)}"
            binding.btnInstall.isEnabled = ShizukuHelper.currentState() == ShizukuHelper.State.READY
        }
    }

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuHelper.PERMISSION_REQUEST_CODE) {
                refreshShizukuStatus()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        binding.btnGrantShizuku.setOnClickListener { onGrantShizukuClicked() }
        binding.btnPickZip.setOnClickListener {
            pickZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
        }
        binding.btnInstall.setOnClickListener { onInstallClicked() }

        refreshShizukuStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshShizukuStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    private fun onGrantShizukuClicked() {
        when (ShizukuHelper.currentState()) {
            ShizukuHelper.State.NOT_RUNNING -> {
                showErrorDialog(
                    "Shizuku chưa chạy",
                    "Cần cài đặt và khởi động app Shizuku trước:\n\n" +
                        "1. Cài app Shizuku từ Play Store hoặc GitHub (RikkaApps/Shizuku).\n" +
                        "2. Khởi động qua 'Wireless debugging' (Android 11+) hoặc qua quyền root " +
                        "nếu máy đã root.\n" +
                        "3. Quay lại app này và bấm 'Cấp quyền Shizuku' lần nữa."
                )
            }
            ShizukuHelper.State.NOT_GRANTED -> ShizukuHelper.requestPermission()
            ShizukuHelper.State.READY -> refreshShizukuStatus()
        }
    }

    private fun refreshShizukuStatus() {
        val state = ShizukuHelper.currentState()
        binding.tvShizukuStatus.text = when (state) {
            ShizukuHelper.State.NOT_RUNNING -> "Shizuku: CHƯA CHẠY"
            ShizukuHelper.State.NOT_GRANTED -> "Shizuku: chưa cấp quyền"
            ShizukuHelper.State.READY -> "Shizuku: sẵn sàng ✓"
        }
        binding.btnGrantShizuku.isEnabled = state != ShizukuHelper.State.READY
        binding.btnInstall.isEnabled = state == ShizukuHelper.State.READY && pickedZipUri != null
    }

    private fun onInstallClicked(password: String? = null) {
        val uri = pickedZipUri ?: return
        logBuilder.clear()
        setBusy(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ModInstaller.install(applicationContext, uri, password) { step ->
                    logBuilder.appendLine("[${timestamp()}] $step")
                    runOnUiThread { binding.tvLastLog.text = step }
                }
                runOnUiThread {
                    setBusy(false)
                    binding.tvLastLog.text = "Hoàn tất."
                    showSuccessDialog(logBuilder.toString())
                }
            } catch (e: PasswordRequiredException) {
                runOnUiThread {
                    setBusy(false)
                    promptForPassword(e.message ?: "File mod có mật khẩu.")
                }
            } catch (e: Exception) {
                val fullLog = buildFullErrorLog(e)
                runOnUiThread {
                    setBusy(false)
                    binding.tvLastLog.text = "Cài mod thất bại — xem log chi tiết."
                    showErrorDialog("Cài mod thất bại", fullLog)
                }
            }
        }
    }

    private fun promptForPassword(message: String) {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Mật khẩu file mod"
        }
        AlertDialog.Builder(this)
            .setTitle("Cần mật khẩu")
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Cài đặt") { _, _ ->
                onInstallClicked(input.text.toString())
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun buildFullErrorLog(e: Exception): String {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        return buildString {
            appendLine("=== Các bước đã thực hiện ===")
            append(logBuilder)
            appendLine()
            appendLine("=== Lỗi ===")
            appendLine(e.message ?: "(không có message)")
            appendLine()
            appendLine("=== Stack trace ===")
            append(sw.toString())
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.progressBar.visibility = if (busy) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnInstall.isEnabled = !busy && pickedZipUri != null &&
            ShizukuHelper.currentState() == ShizukuHelper.State.READY
        binding.btnPickZip.isEnabled = !busy
    }

    /** Bảng log lỗi chi tiết, cuộn được, text chọn/copy được — không âm thầm bỏ qua lỗi. */
    private fun showErrorDialog(title: String, content: String) {
        val dialogBinding = DialogErrorLogBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvErrorLogContent.text = content
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton("Đóng", null)
            .setNegativeButton("Copy log") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("error_log", content))
            }
            .show()
    }

    private fun showSuccessDialog(content: String) {
        val dialogBinding = DialogErrorLogBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvErrorLogContent.text = content
        AlertDialog.Builder(this)
            .setTitle("Cài mod thành công")
            .setView(dialogBinding.root)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun queryFileName(uri: Uri): String {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else uri.lastPathSegment ?: "mod.zip"
            } ?: (uri.lastPathSegment ?: "mod.zip")
        } catch (e: Exception) {
            uri.lastPathSegment ?: "mod.zip"
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}
