package com.aovmod.installer

import android.content.Context
import android.net.Uri
import java.io.File

object ModInstaller {

    const val GARENA_PACKAGE = "com.garena.game.kgvn"

    /**
     * Cài mod từ [zipUri] vào đúng thư mục Resources gốc của game.
     * Mọi bước lỗi đều ném ModInstallException với message chi tiết
     * (không bao giờ âm thầm bỏ qua) để UI hiển thị đầy đủ cho người dùng.
     *
     * [log] được gọi để ghi lại tiến trình từng bước, hữu ích khi hiện bảng log lỗi.
     */
    fun install(context: Context, zipUri: Uri, log: (String) -> Unit) {
        log("Kiểm tra trạng thái Shizuku...")
        when (ShizukuHelper.currentState()) {
            ShizukuHelper.State.NOT_RUNNING -> throw ModInstallException(
                "Shizuku chưa chạy. Hãy mở app Shizuku, khởi động service (qua Wireless " +
                    "debugging hoặc root) rồi thử lại."
            )
            ShizukuHelper.State.NOT_GRANTED -> throw ModInstallException(
                "App chưa được cấp quyền Shizuku. Hãy cấp quyền ở màn hình chính rồi thử lại."
            )
            ShizukuHelper.State.READY -> log("Shizuku sẵn sàng.")
        }

        val tmpRoot = File(context.getExternalFilesDir(null), "mod_tmp").apply {
            deleteRecursively()
            if (!mkdirs()) throw ModInstallException("Không tạo được thư mục tạm: $absolutePath")
        }
        val zipFile = File(context.cacheDir, "mod_incoming.zip")

        log("Đang đọc file zip đã chọn...")
        try {
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                zipFile.outputStream().use { input.copyTo(it) }
            } ?: throw ModInstallException("Không mở được file zip đã chọn (URI: $zipUri)")
        } catch (e: ModInstallException) {
            throw e
        } catch (e: Exception) {
            throw ModInstallException("Lỗi khi đọc file zip: ${e.javaClass.simpleName}: ${e.message}", e)
        }

        log("Đang giải nén (${zipFile.length()} bytes)...")
        try {
            ZipUtils.extractZip(zipFile, tmpRoot)
        } catch (e: ModInstallException) {
            throw e
        } catch (e: Exception) {
            throw ModInstallException("Lỗi khi giải nén zip: ${e.javaClass.simpleName}: ${e.message}", e)
        }

        log("Đang tìm thư mục Resources trong file mod...")
        val resourcesDir = ZipUtils.findResourcesDir(tmpRoot)
            ?: throw ModInstallException(
                "Không tìm thấy thư mục 'Resources' bên trong file mod. " +
                    "Kiểm tra lại file zip có đúng cấu trúc mod không."
            )
        log("Tìm thấy: ${resourcesDir.absolutePath.removePrefix(tmpRoot.absolutePath)}")

        val targetResources = "/sdcard/Android/data/$GARENA_PACKAGE/files/Resources"

        log("Kiểm tra thư mục đích qua Shizuku...")
        val (checkCode, _, checkErr) = ShizukuHelper.runCommand("[ -d '$targetResources' ] || mkdir -p '$targetResources'")
        if (checkCode != 0) {
            throw ModInstallException(
                "Không thể truy cập/tạo thư mục đích '$targetResources' (exit=$checkCode).\n" +
                    "Chi tiết: ${checkErr.ifBlank { "(không có output lỗi)" }}\n" +
                    "Kiểm tra lại game Liên Quân đã cài và đã chạy ít nhất 1 lần chưa."
            )
        }

        log("Đang copy đè vào $targetResources ...")
        val (exitCode, out, err) = ShizukuHelper.runCommand(
            "cp -rf '${resourcesDir.absolutePath}/.' '$targetResources/' 2>&1"
        )

        tmpRoot.deleteRecursively()
        zipFile.delete()

        if (exitCode != 0) {
            throw ModInstallException(
                "Lệnh copy thất bại (exit code=$exitCode).\n" +
                    "Output: ${out.ifBlank { "(trống)" }}\n" +
                    "Error: ${err.ifBlank { "(trống)" }}"
            )
        }

        log("Copy hoàn tất. Cài mod thành công vào $targetResources")
    }
}
