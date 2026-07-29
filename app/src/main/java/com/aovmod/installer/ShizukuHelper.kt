package com.aovmod.installer

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuHelper {

    enum class State {
        NOT_RUNNING,   // Shizuku chưa được cài / chưa khởi động service
        NOT_GRANTED,   // Đã chạy nhưng app chưa được cấp quyền
        READY          // Sẵn sàng dùng
    }

    fun currentState(): State {
        if (!Shizuku.pingBinder()) return State.NOT_RUNNING
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            State.READY
        } else {
            State.NOT_GRANTED
        }
    }

    const val PERMISSION_REQUEST_CODE = 1001

    fun requestPermission() {
        Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    /**
     * Chạy một lệnh shell qua Shizuku (tiến trình chạy với quyền UID shell).
     * Trả về Triple(exitCode, stdout, stderr).
     * Ném ModInstallException nếu không thể khởi tạo tiến trình (Shizuku chết giữa chừng, v.v).
     */
    fun runCommand(command: String): Triple<Int, String, String> {
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()
            return Triple(exitCode, stdout, stderr)
        } catch (e: Exception) {
            throw ModInstallException(
                "Không thể chạy lệnh qua Shizuku: ${e.javaClass.simpleName}: ${e.message}",
                e
            )
        }
    }
}
