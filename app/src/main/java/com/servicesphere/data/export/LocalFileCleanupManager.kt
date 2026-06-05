package com.servicesphere.data.export

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalFileCleanupManager(private val context: Context) {
    suspend fun deleteAllManagedFiles(includeExports: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val directories = buildList {
            add(File(context.filesDir, "job_photos"))
            add(File(context.filesDir, "signatures"))
            add(File(context.filesDir, "pdfs"))
            add(File(context.filesDir, "business"))
            add(File(context.cacheDir, "camera"))
            context.getExternalFilesDir("Pictures/job_photos")?.let(::add)
            context.getExternalFilesDir("Documents/servicesphere_pdfs")?.let(::add)
            if (includeExports) add(File(context.filesDir, "exports"))
        }

        directories.map { directory ->
            runCatching { deleteManagedDirectory(directory) }.getOrDefault(false)
        }.all { it }
    }

    private fun deleteManagedDirectory(directory: File): Boolean {
        if (!isManagedPath(directory)) return false
        if (!directory.exists()) return true
        return directory.deleteRecursively()
    }

    private fun isManagedPath(file: File): Boolean {
        val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return false
        val allowedRoots = listOfNotNull(
            context.filesDir,
            context.cacheDir,
            context.getExternalFilesDir(null)
        ).mapNotNull { root -> runCatching { root.canonicalFile }.getOrNull() }

        return allowedRoots.any { root ->
            canonical.path == root.path || canonical.path.startsWith(root.path + File.separator)
        }
    }
}
