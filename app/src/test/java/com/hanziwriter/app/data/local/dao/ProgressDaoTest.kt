package com.hanziwriter.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hanziwriter.app.data.local.AppDatabase
import com.hanziwriter.app.data.local.entity.CharacterProgress
import com.hanziwriter.app.data.local.entity.DailyEngagement
import com.hanziwriter.app.data.local.entity.DaysPracticed
import com.hanziwriter.app.data.local.entity.StreakRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProgressDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ProgressDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.progressDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ────────────────────────────────────────────────────────────────────
    // Character Progress tests
    // ────────────────────────────────────────────────────────────────────

    @Test
    fun `save progress for man not day cat and retrieve each individually`() = runTest {
        val now = System.currentTimeMillis()

        val man = CharacterProgress(unicode = 20154, accuracy = 0.8, lastPracticed = now, timesPracticed = 1, activeSetName = "hsk1")
        val bu = CharacterProgress(unicode = 19981, accuracy = 0.9, lastPracticed = now, timesPracticed = 2, activeSetName = "hsk1")
        val tian = CharacterProgress(unicode = 22825, accuracy = 0.7, lastPracticed = now, timesPracticed = 3, activeSetName = "hsk1")
        val mao = CharacterProgress(unicode = 29483, accuracy = 0.95, lastPracticed = now, timesPracticed = 4, activeSetName = "hsk1")

        dao.upsertProgress(man)
        dao.upsertProgress(bu)
        dao.upsertProgress(tian)
        dao.upsertProgress(mao)

        val retrievedMan = dao.getProgress(20154)
        val retrievedBu = dao.getProgress(19981)
        val retrievedTian = dao.getProgress(22825)
        val retrievedMao = dao.getProgress(29483)

        assertNotNull(retrievedMan)
        assertEquals(1, retrievedMan!!.timesPracticed)
        assertEquals(0.8, retrievedMan.accuracy, 0.001)

        assertNotNull(retrievedBu)
        assertEquals(2, retrievedBu!!.timesPracticed)
        assertEquals(0.9, retrievedBu.accuracy, 0.001)

        assertNotNull(retrievedTian)
        assertEquals(3, retrievedTian!!.timesPracticed)
        assertEquals(0.7, retrievedTian.accuracy, 0.001)

        assertNotNull(retrievedMao)
        assertEquals(4, retrievedMao!!.timesPracticed)
        assertEquals(0.95, retrievedMao.accuracy, 0.001)
    }

    @Test
    fun `upsertProgress replaces existing record`() = runTest {
        val now = System.currentTimeMillis()
        val original = CharacterProgress(unicode = 19981, accuracy = 0.5, lastPracticed = now - 1000, timesPracticed = 1, activeSetName = "hsk1")
        dao.upsertProgress(original)

        val updated = original.copy(accuracy = 0.85, lastPracticed = now, timesPracticed = 3)
        dao.upsertProgress(updated)

        val retrieved = dao.getProgress(19981)
        assertNotNull(retrieved)
        assertEquals(3, retrieved!!.timesPracticed)
        assertEquals(0.85, retrieved.accuracy, 0.001)
        assertEquals(now, retrieved.lastPracticed)
    }

    @Test
    fun `getProgress returns null for unknown character`() = runTest {
        val result = dao.getProgress(99999)
        assertNull(result)
    }

    @Test
    fun `upsertProgressBatch saves multiple records at once`() = runTest {
        val now = System.currentTimeMillis()
        val batch = listOf(
            CharacterProgress(unicode = 20154, accuracy = 0.8, lastPracticed = now, timesPracticed = 1, activeSetName = "hsk1"),
            CharacterProgress(unicode = 19981, accuracy = 0.9, lastPracticed = now, timesPracticed = 2, activeSetName = "hsk1"),
            CharacterProgress(unicode = 22825, accuracy = 0.7, lastPracticed = now, timesPracticed = 3, activeSetName = "hsk1"),
            CharacterProgress(unicode = 29483, accuracy = 0.95, lastPracticed = now, timesPracticed = 4, activeSetName = "hsk1"),
        )
        dao.upsertProgressBatch(batch)

        assertEquals(1, dao.getProgress(20154)!!.timesPracticed)
        assertEquals(2, dao.getProgress(19981)!!.timesPracticed)
        assertEquals(3, dao.getProgress(22825)!!.timesPracticed)
        assertEquals(4, dao.getProgress(29483)!!.timesPracticed)
    }

    @Test
    fun `getAllProgressForSet returns only records for given set`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsertProgress(CharacterProgress(unicode = 20154, accuracy = 1.0, lastPracticed = now, timesPracticed = 1, activeSetName = "hsk1"))
        dao.upsertProgress(CharacterProgress(unicode = 19981, accuracy = 1.0, lastPracticed = now, timesPracticed = 1, activeSetName = "hsk1"))
        dao.upsertProgress(CharacterProgress(unicode = 29483, accuracy = 1.0, lastPracticed = now, timesPracticed = 1, activeSetName = "hsk2"))

        val hsk1 = dao.getAllProgressForSet("hsk1")
        val hsk2 = dao.getAllProgressForSet("hsk2")
        val empty = dao.getAllProgressForSet("nonexistent")

        assertEquals(2, hsk1.size)
        assertEquals(1, hsk2.size)
        assertTrue(empty.isEmpty())
    }

    @Test
    fun `observeAllProgressForSet emits updates via Flow`() = runTest {
        val now = System.currentTimeMillis()
        val flow = dao.observeAllProgressForSet("hsk1")

        dao.upsertProgress(CharacterProgress(unicode = 20154, accuracy = 1.0, lastPracticed = now, timesPracticed = 1, activeSetName = "hsk1"))
        dao.upsertProgress(CharacterProgress(unicode = 19981, accuracy = 1.0, lastPracticed = now, timesPracticed = 2, activeSetName = "hsk1"))

        val result = flow.first()
        assertEquals(2, result.size)
        val timesMap = result.associate { it.unicode to it.timesPracticed }
        assertEquals(1, timesMap[20154])
        assertEquals(2, timesMap[19981])
    }

    // ────────────────────────────────────────────────────────────────────
    // Daily Engagement tests
    // ────────────────────────────────────────────────────────────────────

    @Test
    fun `save and retrieve daily engagement`() = runTest {
        val engagement = DailyEngagement(
            date = "2020-07-01",
            totalTimeMinutes = 15,
            engagementLevel = "MODERATE",
            activitiesCompleted = "learn,drill",
            charactersLearned = 2,
            charactersDrilled = 5,
            charactersQuizzed = 0
        )
        dao.upsertDailyEngagement(engagement)

        val retrieved = dao.getDailyEngagement("2020-07-01")
        assertNotNull(retrieved)
        assertEquals(15, retrieved!!.totalTimeMinutes)
        assertEquals("MODERATE", retrieved.engagementLevel)
        assertEquals("learn,drill", retrieved.activitiesCompleted)
        assertEquals(2, retrieved.charactersLearned)
        assertEquals(5, retrieved.charactersDrilled)
    }

    @Test
    fun `upsertDailyEngagement replaces existing record`() = runTest {
        dao.upsertDailyEngagement(DailyEngagement("2020-07-01", 10, "LIGHT", "learn", 1, 0, 0))
        dao.upsertDailyEngagement(DailyEngagement("2020-07-01", 25, "STRONG", "learn,drill", 3, 2, 0))

        val retrieved = dao.getDailyEngagement("2020-07-01")
        assertEquals(25, retrieved!!.totalTimeMinutes)
        assertEquals("STRONG", retrieved.engagementLevel)
    }

    @Test
    fun `getTotalMinutesForDate sums correctly`() = runTest {
        dao.upsertDailyEngagement(DailyEngagement("2020-07-01", 12, "MODERATE", "learn", 1, 0, 0))
        dao.upsertDailyEngagement(DailyEngagement("2020-07-02", 8, "LIGHT", "drill", 0, 1, 0))

        assertEquals(12, dao.getTotalMinutesForDate("2020-07-01"))
        assertEquals(8, dao.getTotalMinutesForDate("2020-07-02"))
        assertEquals(0, dao.getTotalMinutesForDate("2020-07-03"))
    }

    @Test
    fun `getRecentEngagements returns ordered by date DESC`() = runTest {
        dao.upsertDailyEngagement(DailyEngagement("2020-07-01", 10, "LIGHT", "learn", 1, 0, 0))
        dao.upsertDailyEngagement(DailyEngagement("2020-07-03", 20, "STRONG", "quiz", 0, 0, 3))
        dao.upsertDailyEngagement(DailyEngagement("2020-07-02", 15, "MODERATE", "drill", 0, 2, 0))

        val recent = dao.getRecentEngagements()
        assertEquals(3, recent.size)
        assertEquals("2020-07-03", recent[0].date)
        assertEquals("2020-07-02", recent[1].date)
        assertEquals("2020-07-01", recent[2].date)
    }

    // ────────────────────────────────────────────────────────────────────
    // Streak tests
    // ────────────────────────────────────────────────────────────────────

    @Test
    fun `save initial streak and retrieve it`() = runTest {
        val streak = StreakRecord(id = 1, currentStreak = 3, longestStreak = 7, lastActiveDate = "2020-07-07")
        dao.upsertStreak(streak)

        val retrieved = dao.getStreak()
        assertNotNull(retrieved)
        assertEquals(3, retrieved!!.currentStreak)
        assertEquals(7, retrieved.longestStreak)
        assertEquals("2020-07-07", retrieved.lastActiveDate)
    }

    @Test
    fun `getStreak returns null when no streak saved`() = runTest {
        val result = dao.getStreak()
        assertNull(result)
    }

    @Test
    fun `upsertStreak updates current and longest streak`() = runTest {
        dao.upsertStreak(StreakRecord(id = 1, currentStreak = 3, longestStreak = 3, lastActiveDate = "2020-07-05"))

        dao.upsertStreak(StreakRecord(id = 1, currentStreak = 7, longestStreak = 7, lastActiveDate = "2020-07-09"))

        val retrieved = dao.getStreak()
        assertNotNull(retrieved)
        assertEquals(7, retrieved!!.currentStreak)
        assertEquals(7, retrieved.longestStreak)
        assertEquals("2020-07-09", retrieved.lastActiveDate)
    }

    @Test
    fun `longestStreak persists even when currentStreak drops`() = runTest {
        dao.upsertStreak(StreakRecord(id = 1, currentStreak = 10, longestStreak = 15, lastActiveDate = "2020-07-10"))

        dao.upsertStreak(StreakRecord(id = 1, currentStreak = 1, longestStreak = 15, lastActiveDate = "2020-07-12"))

        val retrieved = dao.getStreak()
        assertEquals(1, retrieved!!.currentStreak)
        assertEquals(15, retrieved.longestStreak)
    }

    // ────────────────────────────────────────────────────────────────────
    // Days Practiced tests
    // ────────────────────────────────────────────────────────────────────

    @Test
    fun `save practiced days for July 1-7 2020 and retrieve all`() = runTest {
        val july1to7 = (1..7).map { day ->
            DaysPracticed(java.time.LocalDate.of(2020, 7, day).toEpochDay().toInt())
        }
        july1to7.forEach { dao.insertDaysPracticed(it) }

        val allDays = dao.getAllDaysPracticed()
        assertEquals(7, allDays.size)

        val expectedDays = july1to7.map { it.day }
        assertEquals(expectedDays.sorted(), allDays.sorted())
    }

    @Test
    fun `insertDaysPracticed ignores duplicates via IGNORE strategy`() = runTest {
        val epochDay = java.time.LocalDate.of(2020, 7, 1).toEpochDay().toInt()
        dao.insertDaysPracticed(DaysPracticed(epochDay))
        dao.insertDaysPracticed(DaysPracticed(epochDay))
        dao.insertDaysPracticed(DaysPracticed(epochDay))

        val allDays = dao.getAllDaysPracticed()
        assertEquals(1, allDays.size)
        assertEquals(epochDay, allDays[0])
    }

    @Test
    fun `getAllDaysPracticed returns empty list when no days saved`() = runTest {
        val result = dao.getAllDaysPracticed()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `practiced days are returned in ascending order`() = runTest {
        val days = listOf(
            DaysPracticed(18450), // July 7, 2020
            DaysPracticed(18444), // July 1, 2020
            DaysPracticed(18447), // July 4, 2020
        )
        days.forEach { dao.insertDaysPracticed(it) }

        val result = dao.getAllDaysPracticed()
        assertEquals(3, result.size)
        assertEquals(listOf(18444, 18447, 18450), result)
    }

    @Test
    fun `save practiced days across multiple months`() = runTest {
        val aug1 = DaysPracticed(java.time.LocalDate.of(2020, 8, 1).toEpochDay().toInt())
        val sep1 = DaysPracticed(java.time.LocalDate.of(2020, 9, 1).toEpochDay().toInt())
        val dec25 = DaysPracticed(java.time.LocalDate.of(2020, 12, 25).toEpochDay().toInt())

        dao.insertDaysPracticed(aug1)
        dao.insertDaysPracticed(sep1)
        dao.insertDaysPracticed(dec25)

        val result = dao.getAllDaysPracticed()
        assertEquals(3, result.size)
        assertTrue(result[0] < result[1])
        assertTrue(result[1] < result[2])
    }

    // ────────────────────────────────────────────────────────────────────
    // Transaction test (saveSessionResult)
    // ────────────────────────────────────────────────────────────────────

    @Test
    fun `saveSessionResult saves progress engagement streak and days at once`() = runTest {
        val now = System.currentTimeMillis()
        val progressList = listOf(
            CharacterProgress(unicode = 20154, accuracy = 1.0, lastPracticed = now, timesPracticed = 2, activeSetName = "hsk1"),
            CharacterProgress(unicode = 19981, accuracy = 0.8, lastPracticed = now, timesPracticed = 3, activeSetName = "hsk1"),
        )
        val engagement = DailyEngagement("2020-07-07", 20, "STRONG", "learn,quiz", 2, 0, 3)
        val streak = StreakRecord(id = 1, currentStreak = 5, longestStreak = 10, lastActiveDate = "2020-07-07")
        val day = DaysPracticed(java.time.LocalDate.of(2020, 7, 7).toEpochDay().toInt())

        dao.saveSessionResult(progressList, engagement, streak, day)

        assertEquals(2, dao.getProgress(20154)!!.timesPracticed)
        assertEquals(3, dao.getProgress(19981)!!.timesPracticed)
        assertEquals(20, dao.getDailyEngagement("2020-07-07")!!.totalTimeMinutes)
        assertEquals(5, dao.getStreak()!!.currentStreak)
        assertEquals(listOf(day.day), dao.getAllDaysPracticed())
    }
}
