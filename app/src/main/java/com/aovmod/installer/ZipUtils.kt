package com.aovmod.installer

import java.io.File
import java.util.zip.ZipInputStream

object ZipUtils {

    /**
     * Giải nén toàn bộ [zipFile] vào [destDir].
     * Ném exception kèm message chi tiết nếu có entry bất thường (zip-slip).
     */
    fun extractZip(zipFile: File, destDir: File) {
        destDir.mkdirs()
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            val buffer = ByteArray(8192)
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                val canonicalDest = destDir.canonicalPath + File.separator
                if (!outFile.canonicalPath.startsWith(canonicalDest)) {
                    throw ModInstallException(
                        "File zip chứa entry không an toàn (zip-slip): ${entry.name}"
                    )
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { fos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
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
