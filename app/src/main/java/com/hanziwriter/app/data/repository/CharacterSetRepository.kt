package com.hanziwriter.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hanziwriter.app.data.local.CharacterSetInfo
import com.hanziwriter.app.data.local.CharacterSetLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ImportPreview(
    val name: String,
    val isZip: Boolean,
    val collision: CharacterSetInfo?
)

@Singleton
class CharacterSetRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _sets = MutableStateFlow<List<CharacterSetInfo>>(emptyList())
    val sets: StateFlow<List<CharacterSetInfo>> = _sets.asStateFlow()

    private var builtInSets: List<CharacterSetInfo> = emptyList()

    init {
        builtInSets = CharacterSetLoader.listAvailableSets(context.assets)
        Log.d("CharacterSetRepo", "init: builtInSets=${builtInSets.size}")
        refresh()
    }

    fun getCustomSetsDir(): File = File(context.filesDir, "sets")

    fun refresh() {
        val customSets = scanCustomSets()
        val merged = (builtInSets + customSets).sortedBy { it.displayName }
        Log.d("CharacterSetRepo", "refresh: builtInSets=${builtInSets.size} customSets=${customSets.size} merged=${merged.size}")
        _sets.value = merged
    }

    fun findSetInfo(dirName: String): CharacterSetInfo? {
        return _sets.value.find { it.dirName == dirName }
    }

    private fun scanCustomSets(): List<CharacterSetInfo> {
        val dir = getCustomSetsDir()
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { CharacterSetLoader.scanCustomSetDir(it) }
            ?: emptyList()
    }

    suspend fun previewImport(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val name = deriveNameFromUri(uri)
        val isZip = isZipUri(uri)
        val collision = _sets.value.find { it.dirName == name }
        ImportPreview(name = name, isZip = isZip, collision = collision)
    }

    suspend fun confirmImport(uri: Uri, overwrite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val name = deriveNameFromUri(uri)

            if (!overwrite) {
                val existing = _sets.value.find { it.dirName == name }
                if (existing != null) {
                    return@withContext Result.failure(ImportException("Set '$name' already exists"))
                }
            }
            if (builtInSets.any { it.dirName == name }) {
                return@withContext Result.failure(ImportException("Cannot overwrite built-in set '$name'"))
            }

            val dir = File(getCustomSetsDir(), name)

            if (isZipUri(uri)) {
                importZip(uri, name, dir)
            } else {
                importCsv(uri, name, dir)
            }

            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun importCsv(uri: Uri, name: String, dir: File) {
        if (dir.exists()) dir.deleteRecursively()
        dir.mkdirs()

        val csvFile = File(dir, "$name.csv")
        context.contentResolver.openInputStream(uri)?.use { input ->
            csvFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw ImportException("Could not read file")

        val entries = CharacterSetLoader.loadFromCsv(csvFile)
        if (entries.isEmpty()) {
            dir.deleteRecursively()
            throw ImportException("CSV file contains no valid entries")
        }
    }

    private fun importZip(uri: Uri, name: String, dir: File) {
        if (dir.exists()) dir.deleteRecursively()
        dir.mkdirs()

        var csvFound = false

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    val targetName = entryName.substringAfterLast('/')
                    val csvMatch = targetName == "$name.csv" || entryName == "$name.csv"
                    val tomlMatch = targetName == "${name}_properties.toml" || entryName == "${name}_properties.toml"

                    if (csvMatch || tomlMatch) {
                        val targetFile = File(dir, targetName)
                        targetFile.outputStream().use { out -> zip.copyTo(out) }
                        if (csvMatch) csvFound = true
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw ImportException("Could not read file")

        if (!csvFound) {
            dir.deleteRecursively()
            throw ImportException("ZIP file does not contain '$name.csv'")
        }
    }

    suspend fun deleteSet(dirName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val setInfo = _sets.value.find { it.dirName == dirName }
                ?: return@withContext Result.failure(ImportException("Set '$dirName' not found"))

            if (setInfo.isBuiltIn) {
                return@withContext Result.failure(ImportException("Cannot delete built-in set"))
            }

            val dir = File(getCustomSetsDir(), dirName)
            if (dir.exists()) dir.deleteRecursively()

            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deriveNameFromUri(uri: Uri): String {
        val path = uri.lastPathSegment ?: return "imported_set"
        val filename = path.substringAfterLast('/')
        val name = filename.substringBeforeLast(".")
        return name.ifEmpty { "imported_set" }
    }

    private fun isZipUri(uri: Uri): Boolean {
        val path = uri.lastPathSegment?.lowercase() ?: ""
        if (path.endsWith(".zip")) return true
        val mimeType = context.contentResolver.getType(uri) ?: ""
        return mimeType == "application/zip"
    }
}

class ImportException(message: String) : Exception(message)
