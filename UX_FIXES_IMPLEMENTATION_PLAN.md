# UX fixes implementation plan

Handoff doc for implementing the six fixes approved from the UX audit / mockup review. Each section is self-contained: problem, exact files, target state shape, and the UI wiring. Work through them in order — they're independent of each other, so they can also be split across sessions.

## Ground rules (apply to every fix below)

- Follow `CLAUDE.md` / `DEVELOPMENT_CONTEXT.md` conventions already established in this codebase:
  - `UiState` data classes keep `isLoading: Boolean` + `errorMessage: String?`.
  - User events go through a sealed `XxxAction` interface and a single `onAction()`.
  - Dialog/sheet visibility lives in `UiState` as a boolean flag — never a local `remember`.
  - Screens receive `uiState` + `onAction`; ViewModels never receive nav lambdas — navigation actions are dispatched by the Screen composable's `onAction` wrapper (see how `TestingGridAction.OnNavigateBack` etc. are intercepted in `TestingGridScreen.kt`), not the ViewModel.
- **Stay scoped.** Several ViewModels touched below (`TestingGridViewModel`, `StopwatchViewModel`) already have known, documented tech debt (direct repository access instead of use cases — see `DEVELOPMENT_CONTEXT.md` §6). Do not refactor that as a side effect of these fixes. Only touch what's specified.
- **Do not fix out-of-scope items** noticed while in these files: `Screen.Insights`/`Screen.TestsHub` dead routes, orphaned `AthletesScreen.kt`, unreachable `StopwatchAction.OnUndo`/`OnResetAthlete`, the `errorMessage`-carries-success-text smell in `SettingsViewModel`, duplicated percentile-color logic. These are real but separate cleanup items — flag them, don't fix them here, unless explicitly asked.
- Reuse existing theme tokens (`MaterialTheme.colorScheme.*`, `PerformanceGreen/Yellow/Red/Grey` families in `ui/theme/Color.kt`) — no new hardcoded hex colors.
- Build with `gradlew assembleDebug` (no leading `./` on Windows) after each fix and fix any compile errors before moving to the next.

---

## Fix 1 — Persistent error banner for TestingGrid + Stopwatch (Critical)

**Problem:** In [TestingGridViewModel.kt](app/src/main/java/com/example/alearning/ui/testing/TestingGridViewModel.kt), `saveScore()` and `deleteResult()` catch blocks set `errorMessage`, but [TestingGridScreen.kt](app/src/main/java/com/example/alearning/ui/testing/TestingGridScreen.kt)'s `when` block only reads `errorMessage` when `gridData == null` (initial load). Once the grid has loaded, a failed save/delete is invisible. Stopwatch has the opposite but equally broken behavior: [StopwatchScreen.kt](app/src/main/java/com/example/alearning/ui/testing/stopwatch/StopwatchScreen.kt)'s `when` shows `ErrorBox` for **any** `errorMessage`, at any point — a failed edit mid-session blanks out the entire live grid.

### 1a. New shared composable

Create `ui/components/InlineErrorBanner.kt`:

```kotlin
@Composable
fun InlineErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(message, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (retryLabel != null && onRetry != null) {
                    TextButton(onClick = onRetry) { Text(retryLabel) }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                }
            }
        }
    }
}
```

### 1b. TestingGrid — per-cell retry

In `TestingGridViewModel.kt`:

1. Add a sealed type for what failed, and a field for it:
   ```kotlin
   sealed interface FailedGridAction {
       data class Save(val athlete: Individual, val test: FitnessTest, val rawScore: Double, val moveToNext: Boolean) : FailedGridAction
       data class Delete(val resultId: String, val athlete: Individual, val test: FitnessTest) : FailedGridAction
   }
   ```
   Add `val failedAction: FailedGridAction? = null` to `TestingGridUiState`.
2. Add `data object OnRetryFailedAction : TestingGridAction` to the sealed interface.
3. Refactor `saveScore(rawScore: Double, moveToNext: Boolean)` to `saveScore(athlete: Individual, test: FitnessTest, rawScore: Double, moveToNext: Boolean)` — pull `athlete`/`test` out of `editingCell` at the two call sites (`OnSaveScore`, `OnSaveAndNext`) instead of inside the function, so `OnRetryFailedAction` can call it directly with the stored failure payload.
4. In `saveScore`'s catch block, in addition to `errorMessage`, set `failedAction = FailedGridAction.Save(athlete, test, rawScore, moveToNext)`. Clear `failedAction = null` at the start of `saveScore` (a fresh attempt supersedes the old failure).
5. In `deleteResult()`'s catch block, set `failedAction = FailedGridAction.Delete(candidate.resultId, candidate.athlete, candidate.test)` alongside the existing `errorMessage`.
6. Handle `OnRetryFailedAction`:
   ```kotlin
   is TestingGridAction.OnRetryFailedAction -> {
       when (val f = _uiState.value.failedAction) {
           is FailedGridAction.Save -> saveScore(f.athlete, f.test, f.rawScore, f.moveToNext)
           is FailedGridAction.Delete -> { /* re-run the same delete-by-id call used in deleteResult(), passing f.resultId */ }
           null -> Unit
       }
   }
   ```
7. `OnDismissError` should also clear `failedAction = null`.

In `TestingGridComponents.kt`:

- `ScoreCell(savedResult, onClick, onLongPress)` → add a `isFailed: Boolean = false` parameter. When true, render the red/needs-attention state regardless of `savedResult` (border + small retry-style icon), so the specific cell that failed is visually flagged even after the banner is dismissed.
- `AthleteRow(...)` → add `isFailed: Boolean` param, pass through to `ScoreCell`.
- In `LiveEntryPhase`, when building each `AthleteRow`, compute:
  ```kotlin
  val isFailed = (uiState.failedAction as? FailedGridAction.Save)?.let { it.athlete.id == athlete.id && it.test.id == selectedTest.id } == true
  ```
  and pass it in.

In `TestingGridScreen.kt` (`TestingGridContent`), inside the `Scaffold` content lambda, wrap the existing `when { ... }` so the banner sits above it without replacing it:

```kotlin
Column {
    if (uiState.errorMessage != null && uiState.gridData != null) {
        InlineErrorBanner(
            message = uiState.errorMessage,
            onDismiss = { onAction(TestingGridAction.OnDismissError) },
            retryLabel = if (uiState.failedAction != null) "Retry" else null,
            onRetry = if (uiState.failedAction != null) { { onAction(TestingGridAction.OnRetryFailedAction) } } else null
        )
    }
    when {
        uiState.isLoading -> LoadingState()
        uiState.errorMessage != null && uiState.gridData == null -> ErrorState(...)  // unchanged, initial-load path
        else -> LiveEntryPhase(uiState, eventId, groupId, onAction, padding)
    }
}
```
(Keep `padding` applied at the right level — currently applied inside `LiveEntryPhase`/`LoadingState`; make sure the new wrapping `Column` doesn't double-apply it.)

### 1c. Stopwatch — banner instead of full-screen wipe

In `StopwatchState.kt`, add `val sessionLoaded: Boolean = false` to `StopwatchUiState`.

In `StopwatchViewModel.kt`'s `loadSession()`, set `sessionLoaded = true` in **both** the `TimingMode.INDIVIDUAL` and `TimingMode.GROUP_START` success branches' `_uiState.update` calls. Leave it `false` in the `catch` block (so initial-load failure still shows the full-screen error).

In `StopwatchScreen.kt` (`StopwatchContent`), change the top-level `when`:
```kotlin
when {
    uiState.isLoading -> LoadingBox(padding)
    !uiState.sessionLoaded -> ErrorBox(uiState.errorMessage ?: "Something went wrong", onAction, padding)
    uiState.isSessionComplete -> { ... unchanged ... }
    uiState.mode == TimingMode.INDIVIDUAL -> IndividualModeContent(uiState, allAthletes, onAction, padding)
    uiState.mode == TimingMode.GROUP_START -> GroupStartModeContent(uiState, onAction, padding)
}
```
Then, immediately above that `when` (inside the same `Scaffold` content lambda), add:
```kotlin
if (uiState.errorMessage != null && uiState.sessionLoaded) {
    InlineErrorBanner(
        message = uiState.errorMessage,
        onDismiss = { onAction(StopwatchAction.OnDismissError) }
    )
}
```
No retry payload needed here: `submitPending()`'s catch block already leaves `pendingResults` intact on failure, so the existing `SubmitBar` "Submit All" button is itself the retry action. Same for edit/clear failures — the banner's Dismiss is enough since there's no safe auto-retry for those.

**Acceptance check:** In TestingGrid, force a save failure (e.g. temporarily throw in `recordTestResult` call) — banner appears, grid stays interactive, the specific cell shows a red "failed" state, tapping Retry re-attempts with the same score. In Stopwatch, force a `submitPending` failure — banner appears, the athlete grid and Submit bar stay visible and usable underneath.

---

## Fix 2 — Confirmation dialog for Settings "Restore Data" (Critical)

**Files:** [SettingsContract.kt](app/src/main/java/com/example/alearning/ui/settings/SettingsContract.kt), [SettingsViewModel.kt](app/src/main/java/com/example/alearning/ui/settings/SettingsViewModel.kt), [SettingsScreen.kt](app/src/main/java/com/example/alearning/ui/settings/SettingsScreen.kt)

In `SettingsContract.kt`:
```kotlin
sealed interface SettingsAction {
    ...
    object RequestRestoreData : SettingsAction   // NEW — button tap
    object DismissRestoreConfirmation : SettingsAction  // NEW
    object RestoreData : SettingsAction           // existing — now only fired by dialog confirm
    ...
}

data class SettingsUiState(
    ...
    val showRestoreConfirmation: Boolean = false,  // NEW
)
```

In `SettingsViewModel.kt`:
```kotlin
is SettingsAction.RequestRestoreData -> _uiState.update { it.copy(showRestoreConfirmation = true) }
is SettingsAction.DismissRestoreConfirmation -> _uiState.update { it.copy(showRestoreConfirmation = false) }
is SettingsAction.RestoreData -> handleRestoreData()
```
In `handleRestoreData()`, clear the flag at the top: `_uiState.update { it.copy(showRestoreConfirmation = false) }` before proceeding (or fold into the existing update calls).

In `SettingsScreen.kt`:
- Change the "Restore Data (Overwrites Local)" button's `onClick` from `{ onAction(SettingsAction.RestoreData) }` to `{ onAction(SettingsAction.RequestRestoreData) }`.
- Add, alongside the existing dialogs pattern used elsewhere in the app (e.g. Roster's delete-athlete `ConfirmationDialog`, matching wording style):
  ```kotlin
  if (uiState.showRestoreConfirmation) {
      AlertDialog(
          onDismissRequest = { onAction(SettingsAction.DismissRestoreConfirmation) },
          title = { Text("Restore from backup?") },
          text = { Text("This replaces every athlete, group, and result on this device with the selected backup. It can't be undone.") },
          confirmButton = {
              TextButton(onClick = { onAction(SettingsAction.RestoreData) }) {
                  Text("Restore", color = MaterialTheme.colorScheme.error)
              }
          },
          dismissButton = {
              TextButton(onClick = { onAction(SettingsAction.DismissRestoreConfirmation) }) { Text("Cancel") }
          }
      )
  }
  ```

**Acceptance check:** Tapping "Restore Data" no longer restores immediately — it opens the dialog first; Cancel does nothing; Restore proceeds exactly as before.

---

## Fix 3 — Working Leaderboard entry point on Dashboard (Medium)

**Problem:** [NavGraph.kt](app/src/main/java/com/example/alearning/ui/navigation/NavGraph.kt)'s `onNavigateToLeaderboard = { /* empty */ }` for the Dashboard route, and no button in `DashboardContent` fires `DashboardAction.OnLeaderboardClick` even though the action and its plumbing already exist.

**Approach:** Add a 4th Quick Action tile that opens a lightweight picker (reusing `uiState.recentEvents`, already loaded) rather than building a new screen.

In `DashboardViewModel.kt` (`DashboardUiState` / `DashboardAction`):
```kotlin
data class DashboardUiState(
    ...
    val showLeaderboardPicker: Boolean = false,  // NEW
)

sealed interface DashboardAction {
    ...
    data object OnLeaderboardClick : DashboardAction        // already exists — now wired
    data object OnDismissLeaderboardPicker : DashboardAction  // NEW
    data class OnPickLeaderboardEvent(val eventId: String, val groupId: String) : DashboardAction  // NEW, navigation-only
}
```
In `DashboardViewModel.onAction`:
```kotlin
DashboardAction.OnLeaderboardClick -> _uiState.update { it.copy(showLeaderboardPicker = true) }
DashboardAction.OnDismissLeaderboardPicker -> _uiState.update { it.copy(showLeaderboardPicker = false) }
is DashboardAction.OnPickLeaderboardEvent -> Unit  // navigation only, handled by the Screen
```

In `DashboardScreen.kt`:
- Change the `onNavigateToLeaderboard` parameter from `() -> Unit` to `(eventId: String, groupId: String, mode: String) -> Unit`.
- In the `onAction` dispatcher, add:
  ```kotlin
  is DashboardAction.OnPickLeaderboardEvent -> {
      viewModel.onAction(DashboardAction.OnDismissLeaderboardPicker)
      onNavigateToLeaderboard(it.eventId, it.groupId, "event")
  }
  ```
- In `DashboardContent`'s Quick Actions row, add a 4th `QuickActionCard`:
  ```kotlin
  item {
      QuickActionCard(
          icon = Icons.Default.EmojiEvents,
          label = "Leaderboard",
          tint = SportOrange,
          onClick = { onAction(DashboardAction.OnLeaderboardClick) }
      )
  }
  ```
- Add a picker sheet/dialog, shown when `uiState.showLeaderboardPicker`:
  ```kotlin
  if (uiState.showLeaderboardPicker) {
      LeaderboardEventPickerSheet(
          events = uiState.recentEvents.filter { it.groupId != null },
          onPick = { event -> onAction(DashboardAction.OnPickLeaderboardEvent(event.id, event.groupId!!)) },
          onDismiss = { onAction(DashboardAction.OnDismissLeaderboardPicker) }
      )
  }
  ```
  `LeaderboardEventPickerSheet` — a `ModalBottomSheet` (or `AlertDialog`, match whatever's more consistent with other pickers in the app, e.g. Roster's `ManageGroupMembersSheet`) listing each event's name/date as a row; if the filtered list is empty, show "No events yet — create one to see a leaderboard."

In `NavGraph.kt`, replace the stub:
```kotlin
onNavigateToLeaderboard = { eventId, groupId, mode ->
    navController.navigate(Screen.Leaderboard.createRoute(eventId, groupId, mode))
},
```

**Acceptance check:** Tapping the new Leaderboard tile with at least one recent event opens the picker; picking an event navigates to that event's leaderboard. With zero events, shows the empty message instead of a blank sheet.

---

## Fix 4 — QuickTest back button skips the Setup step (High)

**Problem:** In [QuickTestScreen.kt](app/src/main/java/com/example/alearning/ui/quicktest/QuickTestScreen.kt), the dispatcher unconditionally does:
```kotlin
is QuickTestAction.OnNavigateBack -> onNavigateBack()
```
which fully exits the screen every time, even though [QuickTestViewModel.kt](app/src/main/java/com/example/alearning/ui/quicktest/QuickTestViewModel.kt)'s `onAction` already has correct logic to step `ENTER_SCORES → SETUP` first. There's also no hardware/gesture `BackHandler`, so system back bypasses both layers.

Fix in `QuickTestScreen.kt`:
```kotlin
val uiState by viewModel.uiState.collectAsState()

BackHandler {
    if (uiState.step == QuickTestStep.ENTER_SCORES) {
        viewModel.onAction(QuickTestAction.OnNavigateBack)
    } else {
        onNavigateBack()
    }
}

QuickTestContent(
    uiState = uiState,
    onAction = { action ->
        when (action) {
            is QuickTestAction.OnNavigateBack -> {
                if (uiState.step == QuickTestStep.ENTER_SCORES) {
                    viewModel.onAction(action)
                } else {
                    onNavigateBack()
                }
            }
            else -> viewModel.onAction(action)
        }
    }
)
```
(needs `import androidx.activity.compose.BackHandler`). No changes required in `QuickTestViewModel.kt` — its existing `OnNavigateBack` handling (step `ENTER_SCORES → SETUP`) becomes reachable as-is. The `COMPLETE` step's "Back to Dashboard" button already calls `onAction(QuickTestAction.OnNavigateBack)` directly per current code, which will now correctly fall through to `onNavigateBack()` since `step != ENTER_SCORES` at that point — verify this still exits cleanly from `COMPLETE`.

**Acceptance check:** From Setup, back exits immediately (unchanged). From Enter Scores, tapping the top-bar back arrow *or* pressing system back returns to Setup with the athlete/test selection intact; a second back tap from Setup exits. From Complete, back still exits to wherever QuickTest was launched from.

---

## Fix 5 — AiCoach needs a top bar (Medium)

**Problem:** [AiCoachScreen.kt](app/src/main/java/com/example/alearning/ui/aicoach/AiCoachScreen.kt) has no `Scaffold`/`AppTopBar` in any of its four status branches, and [NavGraph.kt](app/src/main/java/com/example/alearning/ui/navigation/NavGraph.kt) doesn't even pass it a back callback — the only way out is the hardware/gesture back.

In `AiCoachScreen.kt`:
```kotlin
@Composable
fun AiCoachScreen(
    viewModel: AiCoachViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "AI Coach",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState.status) {
            AiCoachStatus.UNSUPPORTED -> { /* existing content, wrapped with Modifier.padding(padding) */ }
            AiCoachStatus.DOWNLOADING -> { /* existing content, wrapped with Modifier.padding(padding) */ }
            AiCoachStatus.READY -> {
                ChatUI(
                    messages = uiState.messages,
                    isSending = uiState.isSending,
                    onSendMessage = { viewModel.onAction(AiCoachAction.SendMessage(it)) },
                    errorMessage = uiState.errorMessage,
                    onDismissError = { viewModel.onAction(AiCoachAction.OnDismissError) },
                    modifier = Modifier.padding(padding)
                )
            }
            AiCoachStatus.ERROR -> { /* existing content, wrapped with Modifier.padding(padding) */ }
        }
    }
}
```
Add a `modifier: Modifier = Modifier` param to `ChatUI` and apply it to its outer `Column` (`Modifier.fillMaxSize().then(modifier)` or equivalent) so the padding from `Scaffold` is respected. Needs `import androidx.compose.material.icons.automirrored.filled.ArrowBack` and the `AppTopBar`/`IconButton`/`Icon` imports already used elsewhere.

In `NavGraph.kt`:
```kotlin
composable(
    route = Screen.AiCoach.route,
    arguments = listOf(navArgument("context") { nullable = true; type = NavType.StringType })
) {
    AiCoachScreen(
        viewModel = hiltViewModel(),
        onNavigateBack = { navController.popBackStack() }
    )
}
```

**Acceptance check:** AI Coach now shows the same navy top bar with a back arrow as every other screen, in all four status states, and the back arrow works identically to system back.

---

## Fix 6 — ReportsHub tabs match the rest of the app (Low)

**Problem:** [ReportScreen.kt](app/src/main/java/com/example/alearning/ui/report/ReportScreen.kt)'s `ReportsHubContent` (~lines 220–259) implements a bespoke navy pill-segmented control (`Row` + `Box` + `CircleShape`, `NavyPrimary` background, `SportOrange` selected pill) instead of the standard Material 3 tab row used everywhere else with tabs (see `QuickTestScreen.kt`'s `SetupStep` category tabs, or `TestingGridComponents.kt`'s test tabs).

Replace the custom `Row`/`Box` block in `ReportsHubContent` with:
```kotlin
val tabTitles = listOf("Athlete profile", "Event report")

TabRow(
    selectedTabIndex = selectedTab,
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.primary,
    indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = MaterialTheme.colorScheme.primary
        )
    }
) {
    tabTitles.forEachIndexed { index, title ->
        Tab(
            selected = selectedTab == index,
            onClick = { onTabSelected(index) },
            text = {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                )
            }
        )
    }
}
```
No other change — `onTabSelected`, `selectedTab`, and the `when (selectedTab) { 0 -> AthleteProfileTab(...); 1 -> EventReportTab(...) }` dispatch below it stay exactly as-is. After removing the old block, check whether `CircleShape`, `NavyPrimary`, or `SportOrange` imports become unused elsewhere in `ReportScreen.kt` (they're used again lower in the file for the Session Switcher / selected-icon checks — read before deleting any import).

**Acceptance check:** Reports screen visually matches Test Library / Create Event / Session Report / Quick Test's tab style; switching between "Athlete profile" and "Event report" behaves identically to before.

---

## Suggested implementation order

1. Fix 2 (Settings) — smallest, most isolated, highest consequence if skipped.
2. Fix 4 (QuickTest back nav) — small, single-file-ish, fixes a real data-loss bug.
3. Fix 5 (AiCoach top bar) — small, isolated.
4. Fix 6 (ReportsHub tabs) — small, purely visual, no state changes.
5. Fix 3 (Dashboard leaderboard entry) — medium, touches Dashboard + NavGraph + new sheet.
6. Fix 1 (TestingGrid/Stopwatch banners) — largest, touches the most state; do last so the pattern (shared `InlineErrorBanner`) is battle-tested if reused, and so a build break here doesn't block the smaller wins above.

After each fix: `gradlew assembleDebug`, then manually exercise the acceptance check listed in that section before moving on.
