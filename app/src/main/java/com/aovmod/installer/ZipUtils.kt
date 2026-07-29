package com.aovmod.installer

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File

object ZipUtils {

    /**
     * Giải nén toàn bộ [zipFile] vào [destDir], hỗ trợ cả zip có mật khẩu.
     *
     * - [password] = null: thử giải nén như zip thường.
     *   Nếu phát hiện zip có mã hóa -> ném [PasswordRequiredException] để UI hiện
     *   hộp thoại nhập mật khẩu rồi gọi lại hàm này với [password] khác null.
     * - [password] khác null: giải nén với mật khẩu đó.
     *   Sai mật khẩu -> ném [PasswordRequiredException] để UI cho nhập lại.
     *
     * zip4j tự kiểm tra path bên trong (chống zip-slip).
     */
    fun extractZip(zipFile: File, destDir: File, password: String? = null) {
        destDir.mkdirs()
        val zf = ZipFile(zipFile)
        try {
            if (zf.isEncrypted) {
                if (password.isNullOrEmpty()) {
                    throw PasswordRequiredException(
                        "File mod này có mật khẩu. Vui lòng nhập mật khẩu để giải nén."
                    )
                }
                zf.setPassword(password.toCharArray())
            }
            zf.extractAll(destDir.absolutePath)
        } catch (e: PasswordRequiredException) {
            throw e
        } catch (e: ZipException) {
            val wrongPassword = e.type == ZipException.Type.WRONG_PASSWORD ||
                (e.message?.contains("wrong password", ignoreCase = true) == true)
            if (wrongPassword) {
                throw PasswordRequiredException(
                    "Sai mật khẩu file mod. Vui lòng nhập lại mật khẩu."
                )
            }
            throw ModInstallException(
                "Lỗi khi giải nén zip: ${e.javaClass.simpleName}: ${e.message}", e
            )
        }
    }

    /**
     * Quét theo tầng (BFS) tìm thư mục tên "Resources" (không phân biệt hoa/thường)
     * gần gốc nhất trong cây thư mục đã giải nén.
     * Xử lý được mọi cấu trúc zip lồng nhau bất kỳ (TH1-TH4 trong yêu cầu gốc).
     */
    fun findResourcesDir(root: File): File? {
        val queue = ArrayDeque<File>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val dir = queue.removeFirst()
            val children = dir.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    if (child.name.equals("Resources", ignoreCase = true)) {
                        return child
                    }
                    queue.add(child)
                }
            }
        }
        return null
    }
}

/** Exception riêng cho lỗi trong quá trình cài mod, để UI hiển thị message rõ ràng. */
class ModInstallException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Ném ra khi zip cần mật khẩu (hoặc mật khẩu vừa nhập sai) để UI hiện lại hộp thoại nhập password. */
class PasswordRequiredException(message: String) : Exception(message)
