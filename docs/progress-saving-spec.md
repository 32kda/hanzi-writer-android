# Progress Saving Specification

## Overview

Progress is persisted locally using Room (SQLite) across three tables, managed by `ProgressDao` and orchestrated by `ProgressRepository`. There are two independent save paths: **per-stroke attempts** and **session-level engagement/streak**.

---

## Data Model

### 1. `character_progress` (`CharacterProgress`)

Tracks per-character writing progress.

| Column | Type | Description |
|---|---|---|
| `unicode` | `Int` (PK) | Unicode codepoint of the character; FK to `CharacterEntity` |
| `accuracy` | `Double` | correctAttempts / totalAttempts |
| `totalAttempts` | `Int` | Total stroke attempts made |
| `correctAttempts` | `Int` | Number of correct stroke attempts |
| `consecutiveCorrect` | `Int` | Consecutive correct strokes (resets to 0 on wrong) |
| `lastPracticed` | `Long` | Timestamp (epoch ms) of last practice |
| `lastResult` | `String` | `"CORRECT"` or `"WRONG"` |
| `averageResponseTimeMs` | `Long` | Unused (always 0) |
| `hintUsageCount` | `Int` | Unused (always 0) |
| `introducedDate` | `Long` | Timestamp when first encountered |
| `isLearned` | `Boolean` | Unused (always false) |
| `activeSetName` | `String` | Character set name (hardcoded `"hsk1_en"` for new entries) |

### 2. `daily_engagement` (`DailyEngagement`)

Tracks time spent and activity types per calendar day.

| Column | Type | Description |
|---|---|---|
| `date` | `String` (PK) | ISO date string (e.g. `"2026-06-25"`) |
| `totalTimeMinutes` | `Int` | Accumulated session minutes for the day |
| `engagementLevel` | `String` | `"LIGHT"` (<10 min), `"MODERATE"` (10–19 min), `"STRONG"` (≥20 min) |
| `activitiesCompleted` | `String` | Comma-separated activity type names appended over sessions |
| `charactersLearned` | `Int` | Always 0 (currently unused) |
| `charactersDrilled` | `Int` | Always 0 (currently unused) |
| `charactersQuizzed` | `Int` | Always 0 (currently unused) |
| `quizScore` | `Int?` | Nullable, currently unused |

### 3. `streak` (`StreakRecord`)

A singleton row (`id = 1`) tracking login streak.

| Column | Type | Description |
|---|---|---|
| `id` | `Int` (PK) | Always `1` |
| `currentStreak` | `Int` | Consecutive days with activity |
| `longestStreak` | `Int` | All-time high streak |
| `lastActiveDate` | `String` | ISO date of last recorded activity |

---

## Save Paths

### Path A: Per-Stroke Save (real-time)

**Trigger:** Each time a user finishes drawing a stroke (end stroke event in `BaseSessionViewModel.onStrokeEnd()` → `view.kt:178`).

**Flow:**
1. `BaseSessionViewModel` calls `progressRepository.saveStrokeAttempt(unicode, result.isCorrect, timestamp)`
2. `ProgressRepository.saveStrokeAttempt()` reads the existing `CharacterProgress` for the given `unicode`
3. **If existing record found:** increments counters, recalculates accuracy, resets or increments `consecutiveCorrect`
4. **If no record:** creates a new `CharacterProgress` with initial values and hardcoded `activeSetName = "hsk1_en"`
5. Calls `progressDao.upsertProgress(updated)` which uses `REPLACE` conflict strategy

**Concurrency:** All DAO methods are `suspend` — Room executes them sequentially on its internal dispatcher. No explicit transaction is used for the read-then-write, which means two concurrent strokes on the same character could theoretically race (though in practice one ViewModel processes strokes sequentially).

### Path B: Session-Level Save (on session end)

**Trigger:** When a learning session ends → `EngagementTracker.endSession(activityType)`.

**Flow:**
1. `EngagementTracker` computes elapsed minutes from `sessionStartTime` + `accumulatedTimeMs`, floors to minutes (minimum 1)
2. Calls `progressRepository.addActivity(today, activityType, minutes)` → updates or creates `DailyEngagement` row
3. Calls `progressRepository.updateStreak(today)` → increments `currentStreak` if `lastActiveDate != today`, updates `longestStreak` if needed

**Engagement level** is determined on first creation of the day's record and never re-evaluated: it's set based on the minutes of the current session only, not cumulative.

**Streak logic:** The streak increments by 1 per day. If the same date is passed twice, the streak stays the same (no double-count). There is no gap detection — if a day is skipped, `currentStreak` simply increments by 1 the next time activity is recorded, rather than resetting.

---

## DAO Layer (`ProgressDao`)

| Method | Type | Purpose |
|---|---|---|
| `upsertProgress(CharacterProgress)` | `suspend` | Insert or replace a character progress row |
| `upsertProgressBatch(List<CharacterProgress>)` | `suspend` | Batch upsert (unused in codebase) |
| `getProgress(unicode)` | `suspend` | Get single character progress |
| `getAllProgressForSet(setName)` | `suspend` | All progress rows for a set |
| `observeAllProgressForSet(setName)` | `Flow` | Reactive observable of all progress for a set |
| `getLearnedCount(setName)` | `suspend` | Count of rows where `isLearned = 1` |
| `getTotalPracticedCount(setName)` | `suspend` | Total rows for a set |
| `getLeastRecentlyPracticed(setName, limit)` | `suspend` | For scheduling review (unused in codebase) |
| `upsertDailyEngagement(DailyEngagement)` | `suspend` | Insert or replace daily engagement |
| `getDailyEngagement(date)` | `suspend` | Get single day's engagement |
| `getRecentEngagements()` | `suspend` | Last 31 days of engagement |
| `getTotalMinutesForDate(date)` | `suspend` | Sum of minutes for a date |
| `upsertStreak(StreakRecord)` | `suspend` | Upsert the singleton streak record |
| `getStreak()` | `suspend` | Get the singleton streak record |
| `saveSessionResult(...)` | `suspend` + `@Transaction` | Atomic write of progress + engagement + streak (declared but unused in codebase) |

---

## Repository Layer (`ProgressRepository`)

Acts as a thin wrapper over `ProgressDao` with business logic:

| Method | Logic |
|---|---|
| `saveStrokeAttempt()` | Read-modify-write for `CharacterProgress` |
| `getProgress()` / `getAllProgressForSet()` / `observeAllProgressForSet()` | Passthrough to DAO |
| `addActivity()` | Accumulates minutes to existing day's `DailyEngagement`, or creates new; appends activity type string |
| `updateStreak()` | Increments `currentStreak` if date is different, tracks `longestStreak` |
| `getTotalMinutesForDate()` / `getRecentEngagements()` / `getStreak()` / `getLearnedCount()` / `getTotalPracticedCount()` | Passthrough to DAO |

---

## Known Limitations / Gaps

1. **`activeSetName` is hardcoded** to `"hsk1_en"` when creating new `CharacterProgress` rows (`ProgressRepository.kt:40`). No mechanism exists to vary this per session or user.

2. **No streak gap detection:** `updateStreak()` adds 1 regardless of how many days have passed since `lastActiveDate`. A user who skips 5 days then returns will have streak incremented by 1, not reset to 1.

3. **`engagementLevel` re-evaluation bug:** The day's engagement level is set from the first session's minutes and never updated when additional sessions accumulate more minutes later in the same day.

4. **Several fields are unused** (always their default values): `isLearned`, `averageResponseTimeMs`, `hintUsageCount`, `charactersLearned`, `charactersDrilled`, `charactersQuizzed`, `quizScore`.

5. **`saveSessionResult()` DAO method** provides an atomic transaction wrapper but is never called; instead, `EngagementTracker` calls `addActivity()` and `updateStreak()` separately (two non-transactional writes).

6. **Race condition** in `saveStrokeAttempt()`: the read and subsequent write are not wrapped in a transaction, so concurrent stroke attempts for the same character could lose updates.

7. **`activitiesCompleted` string parsing:** Activity types are concatenated with comma prefixes (e.g. `"drill,learn,quiz"`), and duplicates across sessions on the same day are not deduplicated (the logic skips appending only if the type is the sole/current value, not if it already appears in a multi-value string).

8. **`EngagementTracker` is not injectable:** It is constructed manually rather than through DI, making it hard to swap or test.
