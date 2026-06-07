package com.example.dexplorer.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

class ZipUseCase @Inject constructor() {

    suspend fun compress(sourcePaths: List<String>, destinationZipPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                ZipArchiveOutputStream(File(destinationZipPath)).use { zos ->
                    sourcePaths.forEach { addToZip(zos, File(it), File(it).name) }
                }
            }
        }

    private fun addToZip(zos: ZipArchiveOutputStream, file: File, entryName: String) {
        if (file.isDirectory) {
            zos.putArchiveEntry(ZipArchiveEntry(file, "$entryName/"))
            zos.closeArchiveEntry()
            file.listFiles()?.forEach { child ->
                addToZip(zos, child, "$entryName/${child.name}")
            }
        } else {
            zos.putArchiveEntry(ZipArchiveEntry(file, entryName))
            FileInputStream(file).use { it.copyTo(zos) }
            zos.closeArchiveEntry()
        }
    }

    suspend fun extract(zipPath: String, destinationDir: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val destDir = File(destinationDir).also { it.mkdirs() }
                ZipFile.builder().setFile(File(zipPath)).get().use { zipFile ->
                    zipFile.entries.asSequence().forEach { entry ->
                        val outFile = File(destDir, entry.name)
                        check(outFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                            "Unsafe zip entry: ${entry.name}"
                        }
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            zipFile.getInputStream(entry).use { input ->
                                outFile.outputStream().use { input.copyTo(it) }
                            }
                        }
                    }
                }
            }
        }
}
