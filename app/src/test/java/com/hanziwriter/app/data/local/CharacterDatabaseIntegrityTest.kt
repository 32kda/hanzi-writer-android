package com.hanziwriter.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.sql.DriverManager

class CharacterDatabaseIntegrityTest {

    data class ExpectedCharacter(
        val character: String,
        val unicode: Int,
        val pinyin: String,
        val definitionContains: String
    )

    companion object {
        private lateinit var conn: java.sql.Connection

        val expectedCharacters = listOf(
            ExpectedCharacter("我", 25105, "wǒ", "I"),
            ExpectedCharacter("他", 20182, "tā", "he"),
            ExpectedCharacter("是", 26159, "shì", "be"),
            ExpectedCharacter("的", 30340, "de", "possessive"),
            ExpectedCharacter("一", 19968, "yī", "one"),
            ExpectedCharacter("不", 19981, "bù", "no"),
            ExpectedCharacter("人", 20154, "rén", "man"),
            ExpectedCharacter("了", 20102, "le", "particle"),
            ExpectedCharacter("在", 22312, "zài", "at"),
            ExpectedCharacter("有", 26377, "yǒu", "have"),
            ExpectedCharacter("猫", 29483, "māo", "cat"),
            ExpectedCharacter("大", 22823, "dà", "big"),
            ExpectedCharacter("小", 23567, "xiǎo", "small"),
            ExpectedCharacter("好", 22909, "hǎo", "good"),
            ExpectedCharacter("学", 23398, "xué", "learn"),
            ExpectedCharacter("中", 20013, "zhōng", "middle"),
            ExpectedCharacter("国", 22269, "guó", "country"),
            ExpectedCharacter("你", 20320, "nǐ", "you"),
            ExpectedCharacter("们", 20204, "men", "plural"),
            ExpectedCharacter("这", 36825, "zhè", "this"),
        )

        @JvmStatic
        @BeforeClass
        fun setup() {
            val url = CharacterDatabaseIntegrityTest::class.java.classLoader!!
                .getResource("databases/characters.db")
                ?: throw IllegalStateException("characters.db not found in test resources")
            val dbFile = File(url.toURI())
            Class.forName("org.sqlite.JDBC")
            conn = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        }
    }

    @Test
    fun `test resources copy is identical to main assets copy`() {
        val testUrl = CharacterDatabaseIntegrityTest::class.java.classLoader!!
            .getResource("databases/characters.db")
        val testFile = File(testUrl!!.toURI())

        val testPath = testFile.canonicalPath.replace('\\', '/')
        val mainFile = File(testPath.replace("/test/resources/", "/main/assets/"))
        assertTrue(
            "Main assets characters.db not found at ${mainFile.absolutePath}",
            mainFile.exists()
        )

        val testBytes = testFile.readBytes()
        val mainBytes = mainFile.readBytes()

        assertEquals(
            "Test resources characters.db differs from main assets copy. " +
            "Run: Copy-Item 'app/src/main/assets/databases/characters.db' 'app/src/test/resources/databases/characters.db'",
            testBytes.size, mainBytes.size
        )
        for (i in testBytes.indices) {
            assertEquals("Byte mismatch at offset $i — test copy is out of sync with main assets", testBytes[i], mainBytes[i])
        }
    }

    @Test
    fun `database version is 5`() {
        val stmt = conn.prepareStatement("PRAGMA user_version")
        val rs = stmt.executeQuery()
        rs.next()
        assertEquals(5, rs.getInt(1))
        rs.close()
        stmt.close()
    }

    @Test
    fun `characters table has expected minimum row count`() {
        val stmt = conn.prepareStatement("SELECT COUNT(*) FROM characters")
        val rs = stmt.executeQuery()
        rs.next()
        val count = rs.getInt(1)
        assertTrue("Expected at least 5000 characters, got $count", count >= 5000)
        rs.close()
        stmt.close()
    }

    @Test
    fun `stroke_data table has expected minimum row count`() {
        val stmt = conn.prepareStatement("SELECT COUNT(*) FROM stroke_data")
        val rs = stmt.executeQuery()
        rs.next()
        val count = rs.getInt(1)
        assertTrue("Expected at least 50000 stroke records, got $count", count >= 50000)
        rs.close()
        stmt.close()
    }

    @Test
    fun `all expected characters exist in database with correct unicode`() {
        for (expected in expectedCharacters) {
            val stmt = conn.prepareStatement(
                "SELECT char, pinyin, definition FROM characters WHERE unicode = ?"
            )
            stmt.setInt(1, expected.unicode)
            val rs = stmt.executeQuery()
            assertTrue(
                "Character '${expected.character}' (unicode=${expected.unicode}) not found",
                rs.next()
            )
            assertEquals(expected.character, rs.getString("char"))
            assertEquals(expected.pinyin, rs.getString("pinyin"))
            assertTrue(
                "Definition for '${expected.character}' should contain '${expected.definitionContains}'",
                rs.getString("definition").lowercase().contains(expected.definitionContains.lowercase())
            )
            rs.close()
            stmt.close()
        }
    }

    @Test
    fun `all expected characters can be found by character string`() {
        for (expected in expectedCharacters) {
            val stmt = conn.prepareStatement(
                "SELECT unicode, pinyin FROM characters WHERE char = ?"
            )
            stmt.setString(1, expected.character)
            val rs = stmt.executeQuery()
            assertTrue(
                "Character '${expected.character}' not found by char lookup",
                rs.next()
            )
            assertEquals(
                "Unicode mismatch for '${expected.character}'",
                expected.unicode, rs.getInt("unicode")
            )
            assertEquals(
                "Pinyin mismatch for '${expected.character}'",
                expected.pinyin, rs.getString("pinyin")
            )
            rs.close()
            stmt.close()
        }
    }

    @Test
    fun `all expected characters have stroke data`() {
        for (expected in expectedCharacters) {
            val stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM stroke_data WHERE unicode = ?"
            )
            stmt.setInt(1, expected.unicode)
            val rs = stmt.executeQuery()
            rs.next()
            val strokeCount = rs.getInt(1)
            assertTrue(
                "Character '${expected.character}' (unicode=${expected.unicode}) has no stroke data",
                strokeCount > 0
            )
            rs.close()
            stmt.close()
        }
    }

    @Test
    fun `stroke data for specific characters has valid paths`() {
        val testChars = listOf(25105, 19968, 29483) // 我, 一, 猫
        for (unicode in testChars) {
            val stmt = conn.prepareStatement(
                "SELECT stroke_index, path_data FROM stroke_data WHERE unicode = ? ORDER BY stroke_index"
            )
            stmt.setInt(1, unicode)
            val rs = stmt.executeQuery()
            var count = 0
            while (rs.next()) {
                count++
                val pathData = rs.getString("path_data")
                assertTrue(
                    "Stroke $count for unicode $unicode has invalid path: $pathData",
                    pathData.startsWith("M ") || pathData.startsWith("m ")
                )
            }
            assertTrue("Character $unicode has 0 strokes", count > 0)
            rs.close()
            stmt.close()
        }
    }
}
