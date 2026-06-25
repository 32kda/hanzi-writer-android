# Progress Saving Specification

## Overview

Progress is persisted locally using Room (SQLite) across three tables, managed by `ProgressDao` and orchestrated by `ProgressRepository`. Writes happen only at two points: **character round end** (in-memory stats update) and **session end** (single transactional DB write).

---

## Data Model

### 1. `character_progress` (`CharacterProgress`)

Tracks per-character writing progress. Written at session end.

| Column | Type | Description |
|---|---|---|
| `unicode` | `Int` (PK) | Unicode codepoint of the character; FK to `CharacterEntity` |
| `accuracy` | `Double` | Weighted average of per-session accuracy across `timesPracticed` sessions |
| `lastPracticed` | `Long` | Timestamp (epoch ms) of last practice session |
| `timesPracticed` | `Int` | Number of sessions this character has appeared in |
| `activeSetName` | `String` | Character set name this progress belongs to |

**Accuracy update formula:**
```
sessionAccuracy = correctAttempts / totalAttempts
newAccuracy = (oldAccuracy * timesPracticed + sessionAccuracy) / (timesPracticed + 1)
```

Per-stroke data (`totalAttempts`, `correctAttempts`, `consecutiveCorrect`) is tracked **in-memory only** during a session and never persisted. It is exposed via `LearnUiState.sessionResults` for the post-session result screen.

### 2. `daily_engagement` (`DailyEngagement`)

Tracks time spent and activity counts per calendar day.

| Column | Type | Description |
|---|---|---|
| `date` | `String` (PK) | ISO date string (e.g. `"2026-06-25"`) |
| `totalTimeMinutes` | `Int` | Accumulated session minutes for the day |
| `engagementLevel` | `String` | `"LIGHT"` (<10 min), `"MODERATE"` (10–19 min), `"STRONG"` (≥20 min), re-evaluated each session |
| `activitiesCompleted` | `String` | Comma-separated activity type names |
| `charactersLearned` | `Int` | Characters practiced in "learn" sessions today |
| `charactersDrilled` | `Int` | Characters practiced in "drill" sessions today |
| `charactersQuizzed` | `Int` | Characters practiced in "quiz" sessions today |

### 3. `streak` (`StreakRecord`)

A singleton row (`id = 1`) tracking login streak. Unchanged from before.

---

## Save Path (Session-End Only)

**Trigger:** When all rounds in a session complete → `isComplete = true` in `LearnUiState` → screen `LaunchedEffect` fires.

**Flow:**
1. Each screen calls `viewModel.endSession()` (learn/drill/quiz)
2. `BaseSessionViewModel.endSession()`:
   - Computes elapsed minutes from `sessionStartTime`
   - Reads active set name from `AppPreferences.selectedSetName`
   - Calls `progressRepository.endSession(setName, characterStats, sessionType, minutes, date, timestamp)`
3. `ProgressRepository.endSession()`:
   - For each character: reads existing progress, computes new `accuracy` (weighted avg), increments `timesPracticed`, sets `lastPracticed`
   - Builds/updates `DailyEngagement` with accumulated minutes and incremented character counts
   - Builds/updates `StreakRecord`
   - Calls `progressDao.saveSessionResult(progressList, engagement, streak)` — single `@Transaction`

**Per-stroke tracking (in-memory only):**
- `BaseSessionViewModel.onStrokeEnd()`: updates `sessionStats[unicode]` with totalAttempts/correctAttempts, manages `consecutiveCorrect` (reset on new character in `loadCharacterRound`)
- No DB write occurs per stroke or per character round

---

## Character Selection Algorithm

### `CharacterSelector.select(unicodes, progress, count)`

Located in `domain/algorithm/CharacterSelector.kt`. Pure function, no DB queries.

**Algorithm:**
1. For each unicode, compute `score = max(0, timesPracticed - (daysSinceLastPractice / 14))`. New characters (no progress) get score 0.
2. Build histogram: map of `score → count of characters with that score`.
3. Iterate scores from lowest to highest:
   - If `count ≤ pending`: mark this score as "add all", subtract count from pending.
   - If `count > pending`: mark this score as "add random", break.
4. Build result:
   - Add all characters from "add all" scores.
   - Reservoir sample `pending` characters from the "add random" score bucket.

**Example (N=10):** score 0→3 chars, score 1→2 chars, score 3→20 chars, score 4→15 chars.
→ Take all 3 (score 0) + all 2 (score 1) + 5 random from score 3 = 10 total.

---

## DAO Layer (`ProgressDao`)

| Method | Purpose |
|---|---|
| `upsertProgress(CharacterProgress)` | Insert/replace single progress row |
| `upsertProgressBatch(List<CharacterProgress>)` | Batch upsert progress rows |
| `getProgress(unicode)` | Get single character progress |
| `getAllProgressForSet(setName)` | All progress rows for a set |
| `observeAllProgressForSet(setName)` | Reactive Flow of progress for a set |
| `upsertDailyEngagement(DailyEngagement)` | Insert/replace daily engagement |
| `getDailyEngagement(date)` | Get single day's engagement |
| `getRecentEngagements()` | Last 31 days |
| `getTotalMinutesForDate(date)` | Sum of minutes for a date |
| `upsertStreak(StreakRecord)` | Upsert singleton streak |
| `getStreak()` | Get singleton streak |
| `saveSessionResult(progressList, engagement, streak)` | `@Transaction` — atomic write of all session data |

---

## Repository Layer (`ProgressRepository`)

| Method | Logic |
|---|---|
| `endSession()` | Computes progress updates, engagement, streak; calls `saveSessionResult` |
| `getProgress()` / `getAllProgressForSet()` / `observeAllProgressForSet()` | Passthrough to DAO |
| `getStreak()` / `getTotalMinutesForDate()` / `getRecentEngagements()` | Passthrough to DAO |

---

## Key Changes from Previous Version

- **Removed:** `saveStrokeAttempt()`, `totalAttempts`, `correctAttempts`, `consecutiveCorrect`, `lastResult`, `averageResponseTimeMs`, `hintUsageCount`, `introducedDate`, `isLearned` from `CharacterProgress`
- **Removed:** `quizScore` from `DailyEngagement`
- **Removed:** `PriorityCalculator` class (replaced by score-based histogram algorithm)
- **Removed:** `EngagementTracker` class (logic folded into `BaseSessionViewModel`)
- **Removed:** `getLearnedCount`, `getTotalPracticedCount`, `getLeastRecentlyPracticed` from `ProgressDao`
- **Added:** `timesPracticed` field to `CharacterProgress`
- **Added:** `SessionCharacterStats` data class for in-memory session tracking
- **Added:** `BaseSessionViewModel.endSession()` + `sessionType` abstract property
- **Database version:** bumped to 5 (destructive migration)
