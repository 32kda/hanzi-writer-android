# Hanzi Writer Android — Architecture Overview

## Package Map

```
com.hanziwriter.app/
├── HanziWriterApp.kt              # @HiltAndroidApp entry
├── MainActivity.kt                # Single Activity, sets theme + NavGraph
│
├── di/
│   └── DatabaseModule.kt          # Hilt module: Room DB, DAOs
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt         # Room DB (version 3, prepackaged characters.db)
│   │   ├── CharacterSetLoader.kt  # Scans assets/sets/ for CSV character sets
│   │   ├── CharacterSetEntry.kt   # Data classes for set info/entries
│   │   ├── dao/
│   │   │   ├── CharacterDao.kt    # Char + stroke queries
│   │   │   └── ProgressDao.kt     # Progress/engagement/streak CRUD
│   │   └── entity/
│   │       ├── CharacterEntity.kt
│   │       ├── StrokeDataEntity.kt
│   │       ├── CharacterProgress.kt
│   │       ├── DailyEngagement.kt
│   │       └── StreakRecord.kt
│   ├── model/
│   │   └── CharacterParser.kt     # JSON → domain Character
│   └── repository/
│       ├── CharacterRepository.kt # Entity → domain model mapping
│       └── ProgressRepository.kt  # Progress persistence
│
├── domain/
│   ├── algorithm/
│   │   ├── CharacterSelector.kt   # Spaced-repetition selection
│   │   └── PriorityCalculator.kt  # Priority/heuristic calculation
│   ├── model/
│   │   ├── character/
│   │   │   ├── Character.kt       # Domain char: symbol + strokes
│   │   │   └── Stroke.kt          # SVG path + median points + geometry
│   │   ├── geometry/
│   │   │   ├── Point.kt           # 2D point with vector ops
│   │   │   ├── PathSegment.kt     # Sealed interface (10 SVG command types)
│   │   │   ├── SvgPathParser.kt   # SVG path string → PathSegments
│   │   │   ├── PathSampler.kt     # Segments → discrete Points
│   │   │   └── GeometryUtils.kt   # Distance, Fréchet, normalization, etc.
│   │   ├── quiz/
│   │   │   ├── Quiz.kt            # Stroke quiz state machine
│   │   │   ├── QuizCard.kt        # Flashcard model
│   │   │   ├── CharacterSet.kt    # Named set of characters
│   │   │   └── StrokeMatcher.kt   # Multi-criteria stroke matching
│   │   └── state/
│   │       ├── RenderState.kt     # 3-layer stroke visual state (main/outline/highlight)
│   │       └── Color.kt           # Domain color
│   └── sound/
│       └── SoundManager.kt        # SoundPool (3 SFX) + Vibrator
│
├── ui/
│   ├── theme/
│   │   └── Theme.kt               # Material3 light color scheme
│   ├── components/
│   │   ├── WritingCanvas.kt       # Main drawing surface (strokes, grid, badges)
│   │   ├── StrokeNumberBadge.kt   # Individual stroke number badge
│   │   └── TianZiGe.kt            # 田字格 practice grid
│   ├── navigation/
│   │   └── NavGraph.kt            # 5 routes, NavHost
│   ├── setselector/
│   │   ├── SetSelectorScreen.kt   # Character set picker
│   │   └── SetSelectorViewModel.kt
│   ├── home/
│   │   ├── HomeScreen.kt          # Dashboard (Learn/Drill/Quiz cards)
│   │   └── HomeViewModel.kt
│   └── learn/
│       ├── LearnScreen.kt         # Character learning/drilling/quiz UI
│       └── LearnViewModel.kt      # Session state machine
│
└── util/
    ├── PathUtils.kt               # SVG path segments → android.graphics.Path
    ├── Extensions.kt              # Alias: svgPathToAndroidPath()
    └── EngagementTracker.kt       # Session timer + persistence
```

## Responsibilities by Concern

### Drawing / Rendering Pipeline

| Layer | Class(es) | Role |
|---|---|---|
| **SVG parsing** | `SvgPathParser` (`domain/model/geometry/`) | Tokenizes SVG path data strings into structured `PathSegment` objects. Handles M/L/H/V/C/S/Q/T/A/Z commands (relative & absolute). |
| **Path → Android** | `PathUtils.svgToAndroidPath()` (`util/`) | Walks parsed `PathSegment` list and calls corresponding `android.graphics.Path` methods (`moveTo`, `lineTo`, `cubicTo`, `quadTo`, `arcTo`, `close`). Applies relative/absolute coordinate resolution. |
| **Canvas composable** | `WritingCanvas` (`ui/components/`) | `@Composable` that renders the character on a Compose `Canvas`. Draws in order: (1) TianZiGe grid, (2) reference strokes via `drawSvgStroke()`, (3) completed user strokes via `drawUserPath()`, (4) in-progress stroke, (5) stroke number badges. Viewport maps 1024×1024 character space to canvas pixels. |
| **Per-stroke rendering** | `WritingCanvas.drawSvgStroke()` | Creates native `android.graphics.Paint` (stroke style, round caps/joins, anti-alias). Applies viewport matrix (scale + translate). Optionally clips to `drawPortion` fraction using `Path.Op.INTERSECT`. Draws via `nativeCanvas.drawPath()`. |
| **Grid background** | `TianZiGe` (`ui/components/`) | Draws traditional 田字格 grid: outer border, dashed center lines, dashed diagonals. |
| **Stroke badges** | `WritingCanvas.drawStrokeBadge()` | White circle + colored border + bold number text via native canvas. |
| **Composition** | `MenuScene` composable | Multiple `MenuScene` components are tiled horizontally via `horizontalPager` |
| **Pager/scrolling** | `HorizontalPager` from Accompanist | Each page holds one `MenuScene`; indicator dots show current page |

### GUI / Screen Hierarchy

| Screen | File | Content |
|---|---|---|
| **SetSelector** | `SetSelectorScreen.kt` | `LazyColumn` of character set cards (loaded from `assets/sets/`). On tap → navigates to Home. |
| **Home** | `HomeScreen.kt` | Dashboard with set name, streak info, and 3 activity cards: **Learn** (up to 2 new chars), **Drill** (5 review chars), **Quiz** (10 test chars). Each card navigates to LearnScreen with the first character's unicode. |
| **Learn / Drill / Quiz** | `LearnScreen.kt` | Shared screen for all 3 modes. Shows character symbol text, `WritingCanvas`, stroke counter, and Back button. All 3 routes (`/learn/{unicode}`, `/drill/{unicode}`, `/quiz/{unicode}`) point to this same composable. |
| **Results** | Placeholder `Text` in `NavGraph.kt` | Simple results screen (not yet implemented). |

### User Input Handling

| Stage | Class | Method / Mechanism |
|---|---|---|
| **Touch capture** | `WritingCanvas` | `Modifier.pointerInput` with `detectDragGestures` |
| **Drag start** | `onDragStart` callback | `LearnViewModel.onStrokeStart(offset)` → converts `Offset` to domain `Point`, calls `quiz.startUserStroke()`, stores point in `currentUserPoints` |
| **Drag move** | `onDrag` callback | `LearnViewModel.onStrokeMove(position)` → appends absolute `change.position` to `currentUserPoints`, delegates to `quiz.continueUserStroke()` |
| **Drag end** | `onDragEnd` callback | `LearnViewModel.onStrokeEnd()` → calls `quiz.endUserStroke()`, clears `currentUserPoints` |
| **Stroke matching** | `StrokeMatcher.checkMatch()` | Multi-criteria: distance (avg ≤ 350×leniency×0.5), start/end proximity (≤250px), direction (cosine similarity), shape (Fréchet distance ≤0.4 on normalized curves), length (user ≥35% of target). |
| **Quiz decisions** | `Quiz.endUserStroke()` | If match → increment `currentStrokeIndex`, reveal stroke via `RenderState`, fire `onCorrectStroke`. If backwards + `acceptBackwardsStrokes` → same as correct. If miss → increment `mistakesOnStroke`, fire `onMistake`. After `showHintAfterMisses` misses, reveal highlight. When all strokes done → `onComplete`. |

### Data Flow

```
SQLite (prepackaged characters.db)
  → Room DAOs (CharacterDao, ProgressDao)
    → Repositories (CharacterRepository, ProgressRepository)
      → ViewModels (LearnViewModel, HomeViewModel, SetSelectorViewModel)
        → Compose Screens (LearnScreen, HomeScreen, SetSelectorScreen)
          → Components (WritingCanvas, TianZiGe, StrokeNumberBadge)
```

**Learn session data path:**
1. `LearnScreen` receives `unicode` from navigation
2. `LearnViewModel.startLearn(unicode)` launches coroutine:
   - `CharacterDao.getCharacterByUnicode()` → `CharacterEntity`
   - `CharacterDao.getStrokesForCharacter()` → `List<StrokeDataEntity>`
   - `CharacterRepository.buildDomainCharacter()` → domain `Character` (parses median points JSON, calls `Stroke.parseSvgPath()`)
   - Creates `RenderState(character)` (initializes per-stroke opacity/display maps)
   - Creates `Quiz(character, renderState)`, calls `quiz.start()`
   - Emits `LearnUiState` via `StateFlow`
3. `LearnScreen` builds `DrawableStroke` list from `renderState.mainStrokes`, passes to `WritingCanvas`
4. `WritingCanvas` renders grid, strokes, badges on Compose `Canvas`

### Sound System

| Class | File | Mechanism |
|---|---|---|
| `SoundManager` | `domain/sound/SoundManager.kt` | `@Singleton` using `SoundPool.Builder` (max 3 streams, `USAGE_GAME`). Pre-loads 3 OGG files: `negative_2_short.ogg` (mistake SFX), `positive_short.ogg` (correct stroke SFX), `positive.ogg` (lesson complete SFX). Also manages `Vibrator` (100ms haptic feedback). |
| **Callers** | `LearnViewModel` | `onMistake` callback → `playMistakeSound()` + `vibrate()`. `onComplete` callback → `playCharacterCompleteSound()`. `playLessonCompleteSound()` → called on screen exit after completion. |

### State Management

| Layer | Mechanism |
|---|---|
| **ViewModel → UI** | `MutableStateFlow<UiState>` → `collectAsState()` in composables |
| **Quiz state** | Mutable fields on `Quiz` object (currentStrokeIndex, mistakes, isActive) |
| **Render state** | `RenderState` with per-stroke `StrokeState(opacity, displayPortion)` across 3 layers (main, outline, highlight). Exposes `StateFlow<RenderState>`. |
| **Navigation** | Jetpack `NavController` with typed route arguments |
| **DI** | Hilt `@Singleton`, `@HiltViewModel`, `@Module` |
