# Reports Redesign — Implementation Manual (for Opus 4.8)

**Audience:** An Opus 4.8 coding agent implementing the "Reports Redesign - Standalone.html" mockup into the live ALearning app.
**Goal:** Adopt the mockup's UI/layout for the Reports section **without removing or breaking any existing behaviour** (AI Coach, CSV export, delete, session switcher, navigation, medical alerts, absent/missing handling, group trends, flags).
**Read first:** `CLAUDE.md` and `DEVELOPMENT_CONTEXT.md`. Obey Clean Architecture and the terminology rules there.

---

## 0. TL;DR — the single most important fact

**The app already implements the mockup's information architecture.** The Reports hub (`ui/report/ReportScreen.kt`) already has the two tabs **"Athlete Profile"** and **"Event Report"**, and it already renders almost every section the mockup shows. This is a **visual + layout refresh of existing composables**, *not* a new feature build.

Do **not** rebuild the screens from scratch. You are restyling and reorganising two shared composables:

| Mockup tab | Composable you edit | File |
|---|---|---|
| Athlete Profile | `AthleteBody(...)` | `ui/athlete/AthleteDashboardScreen.kt` |
| Event Report | `SessionReportBody(...)` | `ui/session/SessionReportScreen.kt` |

> ⚠️ **These two bodies are shared.** `AthleteBody` is rendered both by the standalone `AthleteDashboardScreen` **and** by the Reports hub's Athlete Profile tab. `SessionReportBody` is rendered both by the standalone `SessionReportScreen` **and** by the hub's Event Report tab. Editing the body once updates *both* entry points — which is what we want — but you **must smoke-test both** (see §8).

---

## 1. Files in scope

Primary (edit):
- `ui/athlete/AthleteDashboardScreen.kt` — `AthleteBody` and its private card composables (`PremiumAthleteMetaCard`, `LatestSessionCard`, `CategoryRadarCard`, `PerTestHeader`, `TestTile`, `OutstandingTestsCard`, `AthleteAlertCard`).
- `ui/session/SessionReportScreen.kt` — `SessionReportBody` and helpers (`MetaChip`, `StatSummaryItem`, `TrendBars`, etc.).
- `ui/report/ReportScreen.kt` — the hub host (tabs, top bar, pickers, AI FAB, export/delete/switcher wiring). **Structure stays; only touch if a shared component signature changes.**

Supporting (read; edit only where §5/§6 say so):
- `ui/report/components/` — `ZoneChip.kt`, `PercentileChip.kt`, `DeltaArrow.kt`, `MiniSparkline.kt`, `SessionPill.kt`, `AthleteLeaderRow.kt`, `DistributionBar.kt`, `NormBandLineChart.kt`.
- `ui/components/charts/` — `RadarChart`, `HorizontalBarChart`.
- `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` — theme tokens. **See §4 — `Color.kt` is auto-generated; do not hand-edit it.**
- `domain/model/reports/ReportsModels.kt` — `AthleteDashboardData`, `AthleteTestTile`, `LeaderboardRow`, `SessionReportData`.
- `domain/usecase/reports/ObserveAthleteDashboardUseCase.kt` — builds `tiles`; the one place to add the per-test delta (see §6-A3).
- `domain/usecase/testing/GetAthleteRadarDataUseCase.kt` — `AthleteRadarData.axisScores` (per-category label + `normalizedScore`); source for the new Strongest/Focus callouts.

---

## 2. Non-negotiables — preserve every existing feature

Do **not** drop any of these while restyling. They are the acceptance baseline.

**Athlete Profile tab / `AthleteBody`:**
1. **Medical alert / restricted banner** (`AthleteAlertCard`) — red card at top when `medicalAlert != null || isRestricted`. Keep it above everything.
2. **Average percentile gauge/number** with `%ile` label.
3. **Skill Matrix radar** (`RadarChart`) + the "not enough data" / "record results" empty states.
4. **Individual Test Breakdown** — tappable per-test items that navigate to `AthleteTestDetail` via `AthleteDashboardAction.OnNavigateToTest(testId)`.
5. **Flags** list (`FlagListRow`) — tappable flags launch Quick Test with the right test IDs.
6. **Outstanding/Missing tests** → **Start Quick Test** via `AthleteDashboardAction.OnStartQuickTest(testIds)`.
7. **Export CSV** action (top bar; driven by `OnExportCsv`) and **AI Coach FAB** (`DraggableAiFab`).

**Event Report tab / `SessionReportBody`:**
1. **Session pill / header** that opens the **session switcher** (`OnOpenSwitcher` → `SessionSwitcherSheet`).
2. **Meta chips**: `{n} tests`, `{tested}/{total} tested`, `{flagged} flagged` (danger styling).
3. **Per-test selector** (currently `ScrollableTabRow`) driving `OnSelectTest`.
4. **Roster Comparison** (`HorizontalBarChart`).
5. **Session Metrics** — Max / Avg / Min tiles.
6. **Coach's Insight** interpretation text **+ Remediation card** listing `<30 %ile` athletes.
7. **Leaderboard** rows (`AthleteLeaderRow`) — tappable → `OnNavigateToAthlete`.
8. **Absent** subsection and **Missing-data** card → `OnResumeTesting`.
9. **Group trend** ("Need 2+ sessions for trend" empty state) via `TrendBars`.
10. **Delete event** dialog, **Export CSV**, **AI Coach FAB**.

**Architecture rules (from `MEMORY.md` / `CLAUDE.md`) — enforce:**
- UiState holds only domain models + primitives; always `isLoading` + `errorMessage`.
- Screens receive `uiState` + `onAction`, never a ViewModel ref (the bodies already follow this).
- ViewModels use **use cases only** (repos allowed only where no use case exists).
- Never pass navigation callbacks into ViewModels.
- Dialog/expander visibility that must survive process death uses `rememberSaveable`; transient UI (a local "show all" toggle) may use `remember` — match the existing pattern in each body.
- Performance zone colours are **centralised** — use `zoneColors(Classification)` / `ZoneChip` / `PercentileChip`. **Never** hardcode green/yellow/red hex in the redesign.

---

## 3. Mockup → existing-code delta (what actually changes)

The mockup content is ~95% already present. Below is the *only* work. Anything not listed here is a pure restyle (spacing, radius, typography, card colour) — see §4.

### Athlete Profile
| # | Mockup element | Today | Change |
|---|---|---|---|
| A1 | One **header card**: avatar (initials) + name + "{group} · Age {n}" + status line "{class} · Latest session {date} · {n} tests" + large "{avg}ᵗʰ %ile" with expand chevron | Two separate cards: `PremiumAthleteMetaCard` (navy, circular gauge) + `LatestSessionCard` | **Merge** into one header card; add avatar; move session context into the status line; chevron expands the session detail. |
| A2 | **Skill Matrix** shows "Strongest · {cat}" and "Focus · {cat}" chips under the radar | `CategoryRadarCard` has radar + caption only | **Add** two derived callout chips (data already available — §6-A2). |
| A3 | **Individual Test Breakdown** = single-column list rows: name · big value+unit · **trend arrow ▲▼–** · class chip · "{p}ᵗʰ %ile" · "{n} tries"; collapsed shows a few, "Show all {N}" | 2-column `TestTile` grid with `MiniSparkline`; `PerTestHeader` expander | **Reflow** to single-column rows; replace sparkline with `DeltaArrow`; keep tap→test-detail; keep expand/collapse. Requires a small data add (§6-A3). |
| A4 | **Missing Tests** — "{n} remaining", rows "Not attempted", **Start Quick Test ({n})** button | `OutstandingTestsCard` (expandable "Outstanding tests") | **Restyle + rename**; add explicit "Start Quick Test ({n})" button that selects **all** outstanding IDs. Wiring already exists. |

### Event Report
| # | Mockup element | Today | Change |
|---|---|---|---|
| E1 | **Header card**: group-initials avatar + "{group} · {date}" + chevron (opens switcher) | `SessionPill` | Restyle `SessionPill` into the avatar header card; keep `onTap → OnOpenSwitcher`. |
| E2 | **Test selector = pill chips** | `ScrollableTabRow` | Optional: swap to `FilterChip` row. Cosmetic; keep `OnSelectTest`. Low priority — leave tabs if time-boxed. |
| E3 | Roster Comparison / Session Metrics / Coach's Insight / Leaderboard / Group trend | All present | **Pure restyle** — no structural change. |

That is the entire functional delta. Everything else is visual.

---

## 4. Theme & visual tokens — READ CAREFULLY (decision point)

The mockup is a **theme exploration** with a switcher (Organic, Post-Game Shower, Sports Jersey, Sunlit Grounds, Hike at Dawn). The screenshots use the warm **"Organic"** palette (cream `#f5ead8`, terracotta `#c67139`, olive `#7a8a5e`) with a **display serif font `Caprasimo`** for headings and **`Figtree`** for body.

**The live app does NOT use that palette.** Its real tokens (`ui/theme/Color.kt`) are a bright sport theme: `ElectricBlue #0052FF` (aliased as `NavyPrimary`, `SportBlue`), `DynamicOrange #FF5E00` (`SportOrange`). `Color.kt` carries the banner **"AUTO-GENERATED from design.md - DO NOT EDIT MANUALLY."**

Therefore, treat the palette as a **separate, optional module** and default to this:

**DEFAULT (recommended, low-risk): keep the app's existing theme system.** Implement the *layout/structure/component* changes in §3 and §6 using existing Material 3 tokens (`MaterialTheme.colorScheme.*`) and the centralised `zoneColors`. Do **not** hand-edit `Color.kt`. The redesign's value (merged header, list breakdown, Strongest/Focus, cleaner missing-tests) is fully realisable on the current palette.

**OPTIONAL (only if the user explicitly wants the warm look):** introduce the Organic palette *through the generator*, not by editing `Color.kt` directly — update `design.md` (the source of truth referenced by the auto-gen banner) and regenerate, or add a new theme the app can select. Adding the `Caprasimo` display font is a `Type.kt` change (bundle the font in `res/font`, wire a display `FontFamily`). **Flag this to the user before doing it** — it restyles the whole app, not just Reports, and touches generated files.

**Shape/spacing/elevation tokens worth copying from the mockup regardless of palette** (apply via Material tokens, not new globals):
- Card radius: large cards `24–28dp` (already used by radar/metrics cards), medium `16dp`, chips fully rounded (`999dp`).
- Card style: **flat** (elevation `0dp`) with a tonal surface fill — the app already does this (`surfaceVariant.copy(alpha = 0.5f)`); apply consistently to the new header + breakdown rows.
- Vertical rhythm: `16dp` between sections (already the `Arrangement.spacedBy` in both bodies).
- Section titles: `titleLarge` + `FontWeight.ExtraBold` (matches existing "Skill Matrix"/"Roster Comparison").

**Rule:** every colour you introduce must come from `MaterialTheme.colorScheme`, an existing named token in `Color.kt`, or `zoneColors()`. No new raw hex in composables (the one legacy exception `Color(0xFFFFF8E1)` in `MissingDataCard` should ideally be replaced with a token while you're there, not copied).

---

## 5. Classification thresholds — do not regress

Two threshold systems exist in the codebase; the mockup's labels match the **engine** one. Keep engine semantics for anything you touch:
- **Engine (`Classification`):** Superior ≥ 70, Healthy 35–69, Needs Improvement < 35, else No-Data. The mockup's "52ⁿᵈ → Healthy", "95ᵗʰ → Superior", "10ᵗʰ → Needs Improvement" confirm engine thresholds. Use `ZoneChip(classification)` / `zoneLabel()` — never recompute labels inline.
- **Legacy zone (`LatestSessionCard`)** uses ≥60/≥30. When you merge that card into the new header (A1), **prefer the engine classification** already carried on the data (`tile.classification`, `LeaderboardRow.classification`) so the merged header agrees with the chips shown below it. Do not invent a third threshold.

---

## 6. Detailed implementation steps

### A. Athlete Profile (`AthleteBody`, `ui/athlete/AthleteDashboardScreen.kt`)

Current `AthleteBody` item order: `headerContent?` → [AlertCard, PremiumAthleteMetaCard, LatestSessionCard, CategoryRadarCard] → PerTestHeader → (grid) → Flags → OutstandingTestsCard.

Target order to match mockup: `headerContent?` → **AlertCard** → **MergedHeaderCard (A1)** → **SkillMatrixCard w/ Strongest·Focus (A2)** → **TestBreakdownList (A3)** → **Flags** (keep) → **MissingTestsCard (A4)**.

**A1 — Merge header.** Create one card that replaces `PremiumAthleteMetaCard` + `LatestSessionCard`:
- Left: circular **avatar** with initials from `data.athlete.fullName` (compute initials in the composable; there is no avatar asset — draw an initials circle like the mockup's "AM").
- Title: `data.athlete.fullName`.
- Subtitle 1: `"{group} · Age {age}"` — group = `data.groups.firstOrNull()?.name ?: "No Group"`, age = `data.athlete.currentAge`.
- Subtitle 2 (status line): `"{classification} · Latest session {date} · {n} tests"` using `data.contextSession?.date` (format `MMM d, yyyy`) and `data.sessionTestCount`. Classification from `data.athleteSessionAvgPctile` mapped via engine thresholds (§5).
- Right: large `"{avg}ᵗʰ"` + `"%ile"` (from `data.athleteSessionAvgPctile`, may be null → show `—`). Keep the existing circular gauge *or* the mockup's plain big number — either is acceptable; the mockup uses the big number + a chevron.
- **Chevron**: an expand toggle (`rememberSaveable` boolean local to the card) that reveals/hides the detailed session context (date, test count, avg). Keep it optional and non-blocking; if unsure, default expanded=false with the status line always visible.
- Preserve the visual weight of the old navy card by using `colorScheme.primary`/`primaryContainer` (current palette) as the card fill; keep white/appropriate on-colour text.

**A2 — Strongest / Focus callouts in `CategoryRadarCard`.** Data is already available in `uiState.radarData.axisScores` (each `RadarAxisScore` has `label` = category name, `normalizedScore` 0–1, `testCount`). Below the radar (before/after the caption), add two small chips:
- **Strongest** = axis with the **max** `normalizedScore` among `testCount > 0`.
- **Focus** = axis with the **min** `normalizedScore` among `testCount > 0`.
- If fewer than 2 scored axes, render neither (guard against the empty/one-axis case). Compute this in the composable from `radarData` (no new use case, no VM change). Label format: `"Strongest · {label}"`, `"Focus · {label}"`.

**A3 — Test Breakdown as a list with trend arrows.**
- Replace the 2-column `TestTile` grid (the `items(data.tiles.chunked(2)) { ... }` block) with single-column rows inside the same expand/collapse controlled by `PerTestHeader` (keep `PerTestHeader`, keep its scroll-into-view behaviour; you may relabel its subtitle to match the mockup's "{N} tests recorded").
- Collapsed state should show a few rows (e.g. first 4) with a **"Show all {N}"** row/button that expands to the full list — matching the mockup. You can implement this with the existing `perTestExpanded` toggle (collapsed = first N, expanded = all) rather than hide-all/show-all, to better match the mockup which shows content when collapsed.
- Each row: `test.name` · big raw value (`tile.latestResult` formatted like the existing `TestTile`: integer if whole, else 1 dp) + `test.unit` · **trend arrow** · `ZoneChip(tile.classification, label = tile.latestResult?.classification ?: zoneLabel(...))` · `PercentileChip(tile.latestResult?.percentile)` · `"{n} tries"` (`tile.rawSparkline.size`).
- Row `onClick` → `onAction(AthleteDashboardAction.OnNavigateToTest(tile.test.id))` (unchanged).
- **Trend arrow data gap (important):** `AthleteTestTile` currently has `sparkline`/`rawSparkline` but **no percentile delta**, and raw direction alone is ambiguous (some tests are lower-is-better). The correct, direction-safe signal is the **change in percentile** across the two most recent attempts (percentile is always "higher = better"). Do this:
  1. Add `val deltaPercentile: Int? = null` to `AthleteTestTile` in `domain/model/reports/ReportsModels.kt`.
  2. Populate it in `domain/usecase/reports/ObserveAthleteDashboardUseCase.kt` where tiles are built (the same place that already produces `rawSparkline`/`sparkline` from attempt history) as `latest.percentile - previous.percentile`, or `null` when there are `< 2` attempts or a percentile is missing.
  3. Render with the **existing** `DeltaArrow(deltaPercentile = tile.deltaPercentile)` component (already handles null → "–", up/green, down/red). Do not build a new arrow.
  - This keeps domain logic in the use case (architecture rule) and the composable purely presentational.
- Keep `MiniSparkline` available; if you prefer, you may keep a small sparkline *and* the arrow, but the mockup shows only the arrow.

**A4 — Missing Tests card** (replaces `OutstandingTestsCard`):
- Title `"Missing Tests"`, subtitle `"{data.outstandingTests.size} remaining"`.
- One row per `data.outstandingTests` test: `test.name` + muted `"Not attempted"`.
- Prominent **`Start Quick Test ({n})`** button at the bottom → `onAction(AthleteDashboardAction.OnStartQuickTest(data.outstandingTests.map { it.id }))`. (This is exactly what the existing flag path does; the wiring in `ReportScreen`/`AthleteDashboardScreen` already routes `OnStartQuickTest` correctly, incl. the comma-separated `testIds` param on `Screen.QuickTest`.)
- Only show the card when `outstandingTests.isNotEmpty()` (unchanged guard).

### B. Event Report (`SessionReportBody`, `ui/session/SessionReportScreen.kt`)

**E1 — Header card** from `SessionPill`: render an avatar (group initials from `data.group.name`), `"{group.name} · {date}"` (date `MMM d, yyyy` or existing `EEEE, MMM d`), and a trailing chevron. Keep `onTap = { onAction(SessionReportAction.OnOpenSwitcher) }`. You can restyle `SessionPill` itself (it's shared with nothing risky) or wrap it — prefer editing `SessionPill` so both standalone and hub match.

**E2 — Test selector pills (optional).** Swap `ScrollableTabRow` for a horizontally scrollable `FilterChip` row bound to the same `data.tests` / `activeTestId` / `OnSelectTest`. Keep the single-test fallback (plain title) when `data.tests.size == 1`. **This is cosmetic**; if the change set is getting large, leave the tab row — it does not block the redesign.

**E3 — Restyle only.** Roster Comparison, Session Metrics (Max/Avg/Min), Coach's Insight + Remediation, Leaderboard, Absent, Group trend, Missing-data card: keep all logic and structure; apply the shared card radius/flat-surface/typography from §4. Do not touch the interpretation-text logic or the `<30 %ile` remediation computation.

---

## 7. Components — add vs. reuse

**Reuse (do not reinvent):** `ZoneChip`, `zoneColors`, `zoneLabel`, `PercentileChip`, `DeltaArrow`, `MiniSparkline`, `RadarChart`, `HorizontalBarChart`, `SessionSwitcherSheet`, `DraggableAiFab` / `AiFloatingActionButton`, `CsvExporter`.

**New small composables (keep private to their file, follow existing style):**
- `AthleteInitialsAvatar(name: String)` — circle with 1–2 letter initials (used A1 + E1; put a shared version in `ui/report/components/` if used by both).
- `MergedAthleteHeaderCard(...)` (A1).
- `SkillCalloutChips(radarData)` (A2) — or inline in `CategoryRadarCard`.
- `TestBreakdownRow(tile, onClick)` (A3).
- `MissingTestsCard(tests, onStartQuickTest)` (A4).

**Data model change (only one):** add `deltaPercentile: Int?` to `AthleteTestTile` (§6-A3). No other domain/data changes are required — everything else the mockup shows is already carried by `AthleteDashboardData` and `SessionReportData`.

---

## 8. Verification & regression checklist

Build first (Windows, per `MEMORY.md`):
```
set JAVA_HOME=C:\Users\APF\.jdks\openjdk-24
gradlew assembleDebug
```
Then run the app and verify **both entry points for each body**:

**Reports hub (`ReportScreen`):**
- [ ] Athlete Profile tab: pick an athlete → merged header, Skill Matrix + Strongest/Focus, breakdown list with arrows, Missing Tests + Start Quick Test.
- [ ] Event Report tab: pick an event → header opens switcher, chips, test selector, roster bars, metrics, insight, leaderboard, trend.
- [ ] Export CSV works on both tabs; Delete-event dialog works; AI FAB opens with correct context string; session switcher sheet works.

**Standalone screens (same bodies, different hosts):**
- [ ] `AthleteDashboardScreen` (opened from Roster / TestingGrid / SessionReport leaderboard tap): back button, export, AI FAB, tap-through to `AthleteTestDetail`.
- [ ] `SessionReportScreen` (opened from TestingGrid "End Session"): delete, export, switcher, resume-testing from Absent/Missing, tap-through to athlete.

**Regression specifics:**
- [ ] Medical-alert/restricted athlete still shows the red banner **first**.
- [ ] Athlete with **no results** → empty states (radar "record results", no breakdown, correct Missing Tests).
- [ ] Test with **1 attempt** → trend arrow shows "–" (no crash on `deltaPercentile == null`).
- [ ] `<30 %ile` athletes still listed in the Event Report Remediation card.
- [ ] Zone colours come only from `zoneColors`; no hardcoded hex introduced.
- [ ] `Color.kt` untouched (unless the user approved the §4 optional theme module).

Optional but recommended: run `/verify` or drive the two flows in an emulator to confirm no recomposition/scroll regressions (lists still keyed by `it.test.id` / `it.individualId`).

---

## 9. Risks, gotchas & guardrails

1. **Shared bodies (§0).** The biggest trap: forgetting that `AthleteBody`/`SessionReportBody` power both the hub tab and the standalone screen. Test both.
2. **Don't hand-edit `Color.kt`** — it's generated. Palette change goes through `design.md`/regeneration and needs user sign-off (§4).
3. **Trend arrow correctness** — use **percentile delta**, not raw-score delta (lower-is-better tests would show inverted arrows otherwise). Compute in the use case, not the composable (§6-A3).
4. **Threshold consistency** — merged header classification must use engine thresholds so it agrees with the chips below it (§5).
5. **`GetTestingGridDataUseCase.invoke` gotcha and similar** — unrelated to this work, but per `MEMORY.md` some invocations are non-operator; don't refactor call sites you don't need to.
6. **Keep `onAction`/UiState contracts intact.** No new navigation callbacks into ViewModels; nav still flows through the host screens' `onAction` translation (see `ReportScreen`'s `when(action)` block).
7. **QuickTest pre-selection** already supports a list of test IDs via `Screen.QuickTest` comma-separated `testIds`; A4's "Start Quick Test ({n})" just passes `outstandingTests.map { it.id }`. Don't re-implement the parsing.
8. **Scope creep.** Items E2 (pill chips) and the A1 chevron-expand are the two "nice-to-have, not load-bearing" pieces. If time-boxed, ship A1(merge)/A2/A3/A4 + E1 + E3 restyle first; they deliver the redesign's substance.

---

## 10. Suggested commit slicing

1. Domain: add `AthleteTestTile.deltaPercentile` + populate in `ObserveAthleteDashboardUseCase` (build-green, no UI change yet).
2. Athlete Profile A1 (merged header) + A2 (Strongest/Focus).
3. Athlete Profile A3 (breakdown list + `DeltaArrow`) + A4 (Missing Tests card).
4. Event Report E1 (header card) + E3 (restyle) [+ E2 pills if in scope].
5. Final pass: token cleanup (remove stray hex), verification checklist, screenshots.

Each step should compile and run. Do not merge steps that leave the build red.
