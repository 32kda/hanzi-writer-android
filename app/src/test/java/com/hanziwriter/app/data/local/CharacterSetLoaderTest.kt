package com.hanziwriter.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.DriverManager

class CharacterSetLoaderTest {

    private fun resourceFile(path: String): File {
        val url = javaClass.classLoader!!.getResource(path)
            ?: throw IllegalStateException("Resource not found: $path")
        return File(url.toURI())
    }

    @Test
    fun `loads all 10 entries from test CSV`() {
        val csvFile = resourceFile("sets/hsk1_test/characters.csv")
        val entries = CharacterSetLoader.loadFromCsv(csvFile)
        assertEquals(10, entries.size)
    }

    @Test
    fun `first entry is de with correct pinyin and translation`() {
        val csvFile = resourceFile("sets/hsk1_test/characters.csv")
        val entries = CharacterSetLoader.loadFromCsv(csvFile)
        val first = entries.first()
        assertEquals("的", first.character)
        assertEquals("de", first.pinyin)
        assertEquals("possessive, adjectival suffix", first.translation)
        assertEquals(30340, first.unicode)
    }

    @Test
    fun `second entry is yi`() {
        val csvFile = resourceFile("sets/hsk1_test/characters.csv")
        val entries = CharacterSetLoader.loadFromCsv(csvFile)
        val second = entries[1]
        assertEquals("一", second.character)
        assertEquals("yī", second.pinyin)
    }

    @Test
    fun `last entry among 10 is ta`() {
        val csvFile = resourceFile("sets/hsk1_test/characters.csv")
        val entries = CharacterSetLoader.loadFromCsv(csvFile)
        val last = entries.last()
        assertEquals("他", last.character)
        assertEquals("tā", last.pinyin)
        assertEquals("other, another; he, she, it", last.translation)
    }

    @Test
    fun `all entries have non-empty character`() {
        val csvFile = resourceFile("sets/hsk1_test/characters.csv")
        val entries = CharacterSetLoader.loadFromCsv(csvFile)
        entries.forEach { assertTrue(it.character.isNotEmpty()) }
    }

    @Test
    fun `all entries have valid unicode`() {
        val csvFile = resourceFile("sets/hsk1_test/characters.csv")
        val entries = CharacterSetLoader.loadFromCsv(csvFile)
        entries.forEach { assertTrue(it.unicode > 0) }
    }

    @Test
    fun `every character in CSV has stroke data in DB`() {
        val csvFile = resourceFile("sets/hsk1_test/characters.csv")
        val dbFile = resourceFile("databases/characters.db")
        val entries = CharacterSetLoader.loadFromCsv(csvFile)

        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")

        for (entry in entries) {
            val stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM stroke_data WHERE unicode = ?"
            )
            stmt.setInt(1, entry.unicode)
            val rs = stmt.executeQuery()
            rs.next()
            val strokeCount = rs.getInt(1)
            assertTrue("Character '${entry.character}' (unicode=${entry.unicode}) has no stroke data", strokeCount > 0)
            rs.close()
            stmt.close()
        }

        conn.close()
    }

    @Test
    fun `verify stroke data for yi`() {
        val dbFile = resourceFile("databases/characters.db")
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")

        // 一 has unicode 19968
        val stmt = conn.prepareStatement(
            "SELECT stroke_index, path_data FROM stroke_data WHERE unicode = ? ORDER BY stroke_index"
        )
        stmt.setInt(1, 19968)
        val rs = stmt.executeQuery()

        var count = 0
        while (rs.next()) {
            count++
            val pathData = rs.getBytes("path_data")
            assertTrue(pathData != null && pathData.isNotEmpty())
        }
        assertEquals("一 should have 1 stroke", 1, count)

        rs.close()
        stmt.close()
        conn.close()
    }
}
