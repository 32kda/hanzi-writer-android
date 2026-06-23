package com.hanziwriter.app.data.local

import android.content.res.AssetManager
import java.io.File
import java.io.InputStream

object CharacterSetLoader {

    private const val SETS_ROOT = "sets"

    fun listAvailableSets(assets: AssetManager): List<CharacterSetInfo> {
        val dirs = try {
            assets.list(SETS_ROOT)?.filter { it.isNotBlank() } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return dirs.mapNotNull { dirName ->
            val csvPath = "$SETS_ROOT/$dirName/$dirName.csv"
            val exists = try {
                assets.open(csvPath).use { true }
            } catch (_: Exception) {
                false
            }
            if (!exists) return@mapNotNull null

            val props = readProperties(assets, dirName)
            CharacterSetInfo(
                dirName = dirName,
                displayName = props?.get("name")?.trim('"') ?: dirName,
                description = props?.get("description")?.trim('"') ?: "",
                isBuiltIn = true
            )
        }.sortedBy { it.displayName }
    }

    fun scanCustomSetDir(dir: File): CharacterSetInfo? {
        val csvFile = File(dir, "${dir.name}.csv")
        if (!csvFile.exists()) return null
        val propsFile = File(dir, "${dir.name}_properties.toml")
        val props = if (propsFile.exists()) {
            parseSimpleToml(propsFile.readText())
        } else null
        return CharacterSetInfo(
            dirName = dir.name,
            displayName = props?.get("name")?.trim('"') ?: dir.name,
            description = props?.get("description")?.trim('"') ?: "",
            isBuiltIn = false
        )
    }

    fun loadFromCsv(csvFile: File): List<CharacterSetEntry> {
        if (!csvFile.exists()) return emptyList()
        return csvFile.readLines().mapNotNull { line -> parseLine(line) }
    }

    fun loadFromAssets(assets: AssetManager, dirName: String): List<CharacterSetEntry> {
        val lines = try {
            assets.open("$SETS_ROOT/$dirName/$dirName.csv")
                .bufferedReader()
                .readLines()
        } catch (_: Exception) {
            return emptyList()
        }
        return lines.mapNotNull { parseLine(it) }
    }

    private fun readProperties(assets: AssetManager, dirName: String): Map<String, String>? {
        val stream: InputStream = try {
            assets.open("$SETS_ROOT/$dirName/${dirName}_properties.toml")
        } catch (_: Exception) {
            return null
        }
        val text = stream.bufferedReader().readText()
        return parseSimpleToml(text)
    }

    private fun parseSimpleToml(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith('#')) continue
            val eq = trimmed.indexOf('=')
            if (eq < 0) continue
            val key = trimmed.substring(0, eq).trim()
            val value = trimmed.substring(eq + 1).trim()
            if (key.isNotEmpty()) result[key] = value
        }
        return result
    }

    private fun parseLine(line: String): CharacterSetEntry? {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return null
        val parts = parseCsvLine(trimmed)
        if (parts.isEmpty()) return null
        val char = parts[0].trim().removeSurrounding("\"")
        if (char.isEmpty()) return null
        val pinyin = if (parts.size > 1) parts[1].trim().removeSurrounding("\"") else ""
        val translation = if (parts.size > 2) parts[2].trim().removeSurrounding("\"") else ""
        return CharacterSetEntry(character = char, pinyin = pinyin, translation = translation)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
