# Logisim-evolution Fork: UX Improvement Plan

Personal fork of [logisim-evolution](https://github.com/logisim-evolution/logisim-evolution) focused on wiring, editing, keyboard shortcuts, and navigation. The simulator and file format are left alone.

Drop this file in the repo root. Work through it one phase per Claude Code session, in order.

---

## 1. Goals and hard constraints

**Goals**

- Faster, less fiddly wiring and component placement.
- Keyboard-first workflow: place, switch tools, and navigate without the mouse menus.
- Every new behaviour can be turned off, so a bad feature never blocks real work.

**Hard constraints (apply to every phase)**

1. **No changes to file I/O.** Do not modify the .circ reader/writer or add new elements/attributes to saved files. Any new settings go in a separate user config file.
2. **No changes to simulation.** Propagation, values, clocks, and component behaviour stay identical.
3. **Everything goes through the existing undo system.** Any edit to a circuit (moving, wiring, cleanup) must be applied as a normal undoable action, never by mutating the model directly.
4. **Cross-platform shortcuts.** Use the platform menu modifier (Cmd on macOS, Ctrl elsewhere) via the toolkit's menu shortcut mask, never hardcoded Ctrl.
5. **Feature flags.** Each feature has an on/off toggle in a settings section ("Fork UX"), default on.
6. **Small, focused commits** so upstream merges stay manageable.

---

## 2. Phase 0: Setup, audit, and safety net

Do this phase fully before any feature work.

### 0.1 Build

- Install JDK 21+ (Temurin).
- Confirm `./gradlew run` launches the app and check `docs/developers.md` for the jar/packaging tasks.
- Create branch `ux-fork` off `main`. Add upstream remote: `git remote add upstream https://github.com/logisim-evolution/logisim-evolution.git`.

### 0.2 CLAUDE.md

Have Claude Code create `CLAUDE.md` containing:

- Build/run/test commands.
- Package map: where the canvas, tools (select/edit/wiring), circuit model, undo/action system, preferences, and menus live, with key class names.
- The hard constraints from section 1, copied verbatim.
- A pointer to this plan and to `docs/ux-audit.md`.

### 0.3 UX audit (no code changes)

Write `docs/ux-audit.md` listing:

- Every existing keyboard shortcut and what it does (menu accelerators and canvas key handling, including arrow keys and Alt+number on selections).
- Current mouse behaviour for: placing components, drawing wires, moving selections, what happens to connected wires on move, deleting wires.
- Current zoom and scroll behaviour.
- For each item, the class/method responsible.
- A "conflicts" section: which planned shortcuts (section 9) collide with existing ones, and a proposed resolution.

### 0.4 Regression safety net

Create `test-circuits/` containing:

- The current coursework .circ files.
- A few varied examples (subcircuits, custom appearance, multi-bit wires, a clocked circuit, at least one file from original Logisim 2.7 if available).

Add a script `scripts/regression.sh` that, for each file:

1. Runs the **stock** release jar headless to print the circuit's truth table (logisim-evolution supports a `--tty` command-line mode for this; confirm exact flags with `--help`) and saves it as the expected output.
2. Runs the **fork's** jar the same way and diffs against expected.
3. Opens and re-saves the file with the fork (headless if possible, otherwise a manual step), then runs the **stock** jar on the re-saved file and diffs again.

Acceptance: script passes on unmodified fork code. Run it at the end of every phase.

---

## 3. Phase 1: Canvas navigation

Self-contained, low risk, immediate payoff.

| Feature | Behaviour | Acceptance |
| --- | --- | --- |
| Space+drag pan | Holding Space turns the cursor into a hand; dragging pans. Releasing Space restores the previous tool. | Works mid-wire-drawing without cancelling the wire. |
| Middle-mouse pan | Middle-button drag pans. | Works with any tool active. |
| Zoom to cursor | Cmd/Ctrl+scroll (and trackpad pinch on macOS) zooms toward the point under the cursor, not the canvas origin. | The point under the cursor stays fixed while zooming. |
| Plain scroll | Scroll and Shift+scroll pan vertically/horizontally (keep current behaviour if it already does this). | No regression from audit. |
| Fit to window | `F` fits the whole circuit in view. `Shift+F` fits the current selection. | Works on empty circuits (no-op). |
| Reset zoom | Cmd/Ctrl+0 resets to 100%. | Matches existing zoom control state. |
| Circuit switcher | Ctrl+Tab / Ctrl+Shift+Tab cycles recently used circuits in the project (MRU order). | Shows a small popup with circuit names while held. |
| Back to parent | `Backspace` with nothing selected, or Alt+Up, returns to the previously viewed circuit after descending into a subcircuit. | Doesn't fire while editing text. |

Implementation notes:

- Zoom must update the existing zoom model so the toolbar zoom control stays in sync.
- Space handling must be ignored when a text field or label editor has focus.

---

## 4. Phase 2: Command palette

| Item | Spec |
| --- | --- |
| Trigger | Cmd/Ctrl+K (or Cmd/Ctrl+Shift+P if K conflicts per audit). |
| Search scope | All components in loaded libraries, all circuits in the project (to place as subcircuit or to navigate to), and menu actions. |
| Matching | Fuzzy match on name and library name. Prefix tokens: `>` for actions only, `@` for circuits only. |
| Parameters | Trailing number sets the obvious attribute where it exists: `and 3` = 3-input AND, `mux 4` = 4-input multiplexer (select bits derived), `reg 8` = 8-bit register. Unknown params are ignored, not errors. |
| Result | Selecting a component arms the add-tool with those attributes so the next click places it. Enter on a circuit navigates to it; Shift+Enter places it as a subcircuit. |
| Recents | Most recently placed items rank first on an empty query. |
| UI | Small floating panel near the top of the canvas, keyboard-only navigation (Up/Down/Enter/Esc). |

Acceptance: can build a 4-gate circuit without touching the component tree.

---

## 5. Phase 3: Keymap system and single-key tools

### 5.1 Keymap system (build first)

- Central registry of named actions (`tool.and`, `view.fit`, `edit.duplicate`, ...).
- Defaults defined in code. User overrides in a `keymap.json` in the Logisim user preferences directory (not in any .circ).
- Malformed or conflicting user entries are logged and skipped, never crash startup.
- Cheat sheet overlay: `?` (Shift+/) shows all active bindings grouped by category.
- Optional later: a simple keymap editor in Preferences.

### 5.2 Single-key tool bindings

Active only when the canvas has focus and no text editing is in progress. See section 9 for defaults.

- Pressing a component key arms that component; Esc returns to the select tool.
- Number keys 2 to 9 while a gate is armed (or a gate is selected) set the input count. Check this against existing Alt+number behaviour from the audit and keep whichever is less disruptive.
- `R` rotates the armed or selected component clockwise, `Shift+R` counter-clockwise (using the existing facing attribute, so files stay compatible).

Acceptance: all bindings appear in the cheat sheet, can be remapped via `keymap.json`, and do nothing while typing a label.

---

## 6. Phase 4: Selection editing

| Feature | Behaviour | Acceptance |
| --- | --- | --- |
| Duplicate | Cmd/Ctrl+D duplicates the selection offset by one grid step and selects the copy. | Copies attributes and internal wires; single undo step. |
| Nudge | Arrow keys move the selection one grid unit, Shift+arrow moves 5 units. Relocate any existing arrow-key behaviour (e.g. facing) to a modifier, per audit. | Connected wires follow (uses Phase 5.2 logic once built; until then, existing behaviour). |
| Align | Align left/right/top/bottom/center for 2+ selected components (palette actions plus shortcuts). | Snaps to grid; single undo step. |
| Distribute | Distribute horizontally/vertically for 3+ components. | Even spacing on grid. |
| Snap guides | While dragging, show guide lines when a component's pins line up with pins of nearby components. | Visual only; position still grid-snapped. |
| Select connected | Double-click a wire selects the whole net; Shift+double-click a component selects it plus everything wired to it. | Works across junctions, stops at component boundaries for nets. |

---

## 7. Phase 5: Wiring (largest and riskiest phase)

Build in this order. Each sub-feature behind its own flag.

### 5.1 Net highlighting (read-only, do first)

- Hovering a wire highlights every segment in its net and the connected pins.
- Unconnected component pins and dangling wire ends are drawn with a distinct marker (toggle via `view.showUnconnected`).
- Acceptance: no model changes at all; purely rendering.

### 5.2 Clean rubber-banding

- When moving components, connected wires stay attached using orthogonal (horizontal/vertical) segments only. Never diagonals.
- Rerouting prefers: extend existing segment, then one bend (L), then two bends (Z).
- Wires not attached to anything moved are never touched.
- Acceptance: moving a gate in a dense circuit leaves no diagonals, overlaps, or new disconnections; result is one undo step.

### 5.3 Start wiring from any pin

- With the select tool active, pressing on a component pin (or wire end) starts a wire instead of moving the component. Pressing the component body still moves it.
- Pin hit area slightly larger than the drawn dot; cursor changes to crosshair over pins.
- Acceptance: never possible to accidentally drag a component when aiming at a pin.

### 5.4 Click-to-route wires

- Click a pin to start. Each further click places a waypoint. Preview shows an L-shaped route to the cursor; `Space` (or `/`) flips the bend direction.
- Clicking a pin or wire finishes. Esc cancels. Backspace removes the last waypoint.
- Existing drag-to-wire keeps working unchanged.
- Acceptance: whole route commits as one undo step; produced wires are ordinary segments indistinguishable in the saved file.

### 5.5 Auto-cleanup

- After any wire edit, and on demand via a palette action ("Clean up wires"):
  - Merge collinear touching segments.
  - Remove zero-length segments.
  - Remove dangling stubs that connect to nothing on one end (on-demand action only, not automatic, since dangling wires can be intentional mid-edit).
- Acceptance: cleanup never changes connectivity. Verify by comparing net membership before and after.

### 5.6 Wire deletion helpers

- Alt+click a segment deletes just that segment between junctions.
- Deleting a component offers (setting) to also delete wires that would be left dangling.

---

## 8. Phase 6: Polish

- "Fork UX" settings page listing every feature flag with a short description.
- Status bar hints for modal actions (e.g. "Routing wire: click to add bend, Space to flip, Esc to cancel").
- Update the cheat sheet and `docs/ux-audit.md` to reflect the final state.
- Build a runnable jar/app package for daily use.

---

## 9. Default keymap

Final bindings depend on the Phase 0 conflict audit. Mod = Cmd on macOS, Ctrl elsewhere.

| Key | Action |
| --- | --- |
| Mod+K | Command palette |
| Esc | Select tool / cancel |
| W | Wire tool |
| A | AND gate |
| O | OR gate |
| N | NOT gate |
| X | XOR gate |
| Shift+A | NAND gate |
| Shift+O | NOR gate |
| I | Input pin |
| P | Output pin |
| T | Text tool |
| 2 to 9 | Input count for armed/selected gate |
| R / Shift+R | Rotate CW / CCW |
| Mod+D | Duplicate |
| Arrows / Shift+Arrows | Nudge 1 / 5 grid units |
| Space+drag | Pan |
| F / Shift+F | Fit all / fit selection |
| Mod+0 | Reset zoom |
| Ctrl+Tab | Switch circuit (MRU) |
| Alt+Up | Back to parent circuit |
| ? | Shortcut cheat sheet |

---

## 10. Workflow per phase

1. Start the Claude Code session in plan mode: "Read CLAUDE.md, this plan, and docs/ux-audit.md. Propose an implementation for Phase N with the files you'll touch. Don't write code yet."
2. Review the proposal. Push back on anything touching file I/O or simulation.
3. Implement one feature at a time; build and launch after each.
4. Run `scripts/regression.sh`. Manually exercise the feature on the test circuits.
5. Commit per feature. Merge the phase into `ux-fork`.
6. Every few phases: `git fetch upstream && git merge upstream/main`, rebuild, rerun regression.

---

## 11. Risks

| Risk | Mitigation |
| --- | --- |
| Rubber-banding produces messy or broken wiring | Ship 5.1 to 5.3 first; keep 5.2 behind a flag; verify connectivity before/after in tests. |
| Single-key shortcuts fire while typing labels | Central focus check in the keymap system, tested explicitly. |
| Upstream merges conflict with UI changes | Small commits, new code in new classes where possible, minimal edits to upstream classes. |
| Saved files subtly differ | Regression script round-trips every test file through the stock jar. |
| macOS vs other platforms | Use menu shortcut mask; test trackpad pinch and Cmd bindings on macOS. |
