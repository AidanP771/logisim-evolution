# UX audit: keyboard shortcuts, mouse interactions and wire editing

This document lists the input bindings that exist in the codebase today, and names the class that
implements each one. Sections 1–5 describe the stock (upstream) behaviour. Section 6 compares it with
the planned fork keymap and records the agreed resolutions. Section 7 lists what the fork has changed
so far, with each feature's on/off flag. Paths are relative to `src/main/java/com/cburch/`.

**Notation:** <kbd>Menu</kbd> is the platform menu-shortcut modifier: <kbd>Cmd</kbd> on macOS and
<kbd>Ctrl</kbd> elsewhere. It comes from `Toolkit.getMenuShortcutKeyMaskEx()`, or <kbd>Alt</kbd> in
headless mode. <kbd>Ctrl</kbd> means the physical Control key on every platform.

## 1. How input is routed

| Layer | Responsibility | Class |
| --- | --- | --- |
| Configurable hotkey defaults | Holds every user-rebindable `KeyStroke` (`HOTKEY_*`) | `logisim/prefs/AppPreferences` |
| Hotkey storage and matching | Persists a stroke; `compare(code, modifiers)` | `logisim/prefs/PrefMonitorKeyStroke` |
| Hotkey preferences UI | Preferences tab for rebinding | `logisim/gui/prefs/HotkeyOptions` |
| Hotkey capture widget | Records a key and runs the conflict check | `logisim/util/JHotkeyInput` |
| Conflict detection | Compares against all `HOTKEY_*` fields and menu accelerators | `AppPreferences.hotkeyCheckConflict` |
| Menu accelerators | Swing `setAccelerator` on menu items | `logisim/gui/menu/Menu*`, `util/WindowMenu` |
| Circuit canvas dispatch | Sends key, mouse and wheel events to a tool | `logisim/gui/main/Canvas` (`MyListener`) |
| Mouse-button to tool mapping | Maps modifier and button combinations to a tool | `logisim/file/MouseMappings` |
| Mouse mapping UI | Project Options, Mouse tab | `logisim/gui/opts/MouseOptions` |
| Default mouse mappings | Mappings in the template file for new projects | `resources/logisim/default.templ` |
| Appearance-editor dispatch | Mouse and key events for the appearance canvas | `draw/canvas/CanvasListener` |
| Global key observer | Double-<kbd>Shift</kbd> opens the search dialog | `logisim/gui/search/DoubleShiftTrigger` |

On the circuit canvas, `Canvas.MyListener.keyPressed` handles <kbd>Ctrl</kbd>+<kbd>+</kbd> and
<kbd>Ctrl</kbd>+<kbd>-</kbd> (zoom) itself. It forwards every other key to `proj.getTool()`, the
tool selected in the toolbar. Mouse events go to `MouseMappings.getToolFor(e)` when a mapping
exists, and to the selected tool otherwise.

## 2. Keyboard shortcuts

### 2.1 Menu accelerators

"Rebindable" means the binding comes from a `HOTKEY_*` preference and can be changed in
Preferences, Hotkeys.

| Menu | Action | Default | Rebindable | Class |
| --- | --- | --- | --- | --- |
| File | New | <kbd>Menu</kbd>+<kbd>N</kbd> | No | `gui/menu/MenuFile` |
| File | Merge | <kbd>Menu</kbd>+<kbd>M</kbd> | No | `gui/menu/MenuFile` |
| File | Open | <kbd>Menu</kbd>+<kbd>O</kbd> | No | `gui/menu/MenuFile` |
| File | Close | <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>W</kbd> | No | `gui/menu/MenuFile` |
| File | Save | <kbd>Menu</kbd>+<kbd>S</kbd> | No | `gui/menu/MenuFile` |
| File | Save As | <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>S</kbd> | No | `gui/menu/MenuFile` |
| File | Export project | <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>E</kbd> | Yes | `gui/menu/MenuFile` |
| File | Print | <kbd>Menu</kbd>+<kbd>P</kbd> | Yes | `gui/menu/MenuFile` |
| File | Quit | <kbd>Menu</kbd>+<kbd>Q</kbd> | No | `gui/menu/MenuFile` |
| Edit | Undo | <kbd>Menu</kbd>+<kbd>Z</kbd> | Yes | `gui/menu/MenuEdit` |
| Edit | Redo | <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>Z</kbd> | Yes | `gui/menu/MenuEdit` |
| Edit | Cut / Copy / Paste | <kbd>Menu</kbd>+<kbd>X</kbd> / <kbd>C</kbd> / <kbd>V</kbd> | No | `gui/menu/MenuEdit` |
| Edit | Delete | <kbd>Delete</kbd> | No | `gui/menu/MenuEdit` |
| Edit | Duplicate | <kbd>Menu</kbd>+<kbd>D</kbd> | Yes | `gui/menu/MenuEdit` |
| Edit | Select All | <kbd>Menu</kbd>+<kbd>A</kbd> | No | `gui/menu/MenuEdit` |
| Edit | Raise / Lower (z-order) | <kbd>Menu</kbd>+<kbd>↑</kbd> / <kbd>↓</kbd> | No | `gui/menu/MenuEdit` (1) |
| Edit | Raise to top / Lower to bottom | <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>↑</kbd>/<kbd>↓</kbd> | No | `MenuEdit` (1) |
| Project | Move circuit up | <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>U</kbd> | Yes | `gui/menu/MenuProject` |
| Project | Move circuit down | <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>D</kbd> | Yes | `gui/menu/MenuProject` |
| Simulate | Auto-propagate on/off | <kbd>Menu</kbd>+<kbd>E</kbd> | Yes | `gui/menu/MenuSimulate` |
| Simulate | Reset simulation | <kbd>Menu</kbd>+<kbd>R</kbd> | Yes | `gui/menu/MenuSimulate` |
| Simulate | Step | <kbd>Menu</kbd>+<kbd>I</kbd> | Yes | `gui/menu/MenuSimulate` |
| Simulate | Tick half cycle | <kbd>Menu</kbd>+<kbd>T</kbd> | Yes | `gui/menu/MenuSimulate` |
| Simulate | Tick full cycle | <kbd>Menu</kbd>+<kbd>F9</kbd> | Yes | `gui/menu/MenuSimulate` |
| Simulate | Ticks enabled | <kbd>Menu</kbd>+<kbd>K</kbd> | Yes | `gui/menu/MenuSimulate` |
| Simulate | Go into nearest sub-state | <kbd>Menu</kbd>+<kbd>→</kbd> | No | `MenuSimulate.recreateStateMenu` |
| Simulate | Go out to parent state | <kbd>Menu</kbd>+<kbd>←</kbd> | No | `MenuSimulate.recreateStateMenu` |
| Window | Minimize | <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>M</kbd> (2) | Yes | `util/WindowMenu` |
| Window | Close window | <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>W</kbd> (2) | Yes | `util/WindowMenu` |
| Help | Search | <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>A</kbd> | Yes | `gui/menu/MenuHelp` |

(1) These items are enabled only in the appearance editor, by `gui/appear/AppearanceEditHandler`.
`gui/main/LayoutEditHandler` disables them in the layout editor.

(2) This is the initial default. `AppPreferences.resetHotkeys()` resets it to a different value.
See finding F1.

### 2.2 Global, non-menu keys

| Key | Action | Class |
| --- | --- | --- |
| <kbd>Menu</kbd>+<kbd>1</kbd> … <kbd>0</kbd> | Select toolbar tool 1–10 (11–14 unbound) | `KeyboardToolSelection` |
| <kbd>Shift</kbd> twice within 300 ms | Open search (pref. `SEARCH_DOUBLE_SHIFT`) | `gui/search/DoubleShiftTrigger` |
| <kbd>Ctrl</kbd>+<kbd>F1</kbd> | Swing "show tooltip"; reserved | `AppPreferences.hotkeyCheckConflict` |

`KeyboardToolSelection` binds its hotkeys in the toolbar's `WHEN_IN_FOCUSED_WINDOW` input map, so
they work from anywhere in the main window.

### 2.3 Circuit canvas: view

| Input | Action | Class |
| --- | --- | --- |
| <kbd>Ctrl</kbd>+<kbd>+</kbd> (main or numpad) | Zoom in, anchored at the mouse | `gui/main/Canvas` (`keyPressed`, `doZoom`) |
| <kbd>Ctrl</kbd>+<kbd>-</kbd> (main or numpad) | Zoom out, anchored at the mouse | `gui/main/Canvas` |

### 2.4 Circuit canvas: Edit tool (default arrow tool)

`tools/EditTool` handles some keys itself and passes the rest to `tools/SelectTool` or
`tools/WiringTool`.

| Key | Action | Class |
| --- | --- | --- |
| <kbd>Delete</kbd> / <kbd>Backspace</kbd> | Delete the selection. See F15. | `EditTool`, `WiringTool` |
| <kbd>Insert</kbd> | Duplicate the selection (`HOTKEY_EDIT_TOOL_DUPLICATE`) | `tools/EditTool` |
| Arrows (`HOTKEY_DIR_*`) | Set the facing of the selected components | `EditTool.attemptReface` |
| <kbd>Ctrl</kbd>+<kbd>Space</kbd> | Rotate the selection | `EditTool.attemptRotate` |
| <kbd>Alt</kbd> (press or release) | Swap wire-start and select. See section 4.3. | `EditTool.updateLocation` |
| <kbd>Shift</kbd> while moving a selection | Invert "keep connected" | `SelectTool.shouldConnect` |
| <kbd>S</kbd>/<kbd>N</kbd>, <kbd>M</kbd>, <kbd>W</kbd> | Gate size: small, medium, wide | `GateKeyboardModifier` |
| <kbd>=</kbd>/<kbd>+</kbd>, <kbd>-</kbd> | Add or remove a gate input | `std/gates/GateKeyboardModifier` |
| <kbd>L</kbd> <kbd>T</kbd> <kbd>V</kbd> <kbd>H</kbd> <kbd>A</kbd> | Auto-label open/toggle/view/hide/stop | `AutoLabel` |
| Digits | Set an integer attribute, such as a gate's input count | `tools/key/IntegerConfigurator` |
| <kbd>Alt</kbd>+digits | Set the bit width (`StdAttr.WIDTH`) | `tools/key/BitWidthConfigurator` |
| <kbd>Alt</kbd>+arrows | Set the label location | `tools/key/DirectionConfigurator` |

`SelectTool.keyPressed` tries the handlers in this order: gate modifiers, then auto-label, then
Delete/Backspace, then the component's `KeyConfigurator` (`Tool.processKeyEvent`).

### 2.5 Circuit canvas: Add tool (placing a component)

| Key | Action | Class |
| --- | --- | --- |
| Arrows (`HOTKEY_DIR_*`) | Set the facing of the ghost component | `tools/AddTool` |
| <kbd>R</kbd> (`HOTKEY_ADD_TOOL_ROTATE`) | Rotate the ghost clockwise | `tools/AddTool` |
| <kbd>X</kbd> | Toggle matrix placement, which opens `MatrixPlacerDialog` on click | `tools/AddTool` |
| <kbd>Esc</kbd> | Return to the Edit tool and clear the selection | `tools/AddTool` |
| <kbd>Backspace</kbd> | Undo the last placement, if it was the last action | `tools/AddTool` |
| Gate, auto-label and digit keys | Same as section 2.4, applied to the tool's attributes | `AddTool.keyPressed` |

### 2.6 Circuit canvas: other tools

| Tool | Keys | Class |
| --- | --- | --- |
| Wiring tool | <kbd>Backspace</kbd> undoes the last wire. There is no <kbd>Esc</kbd>. | `tools/WiringTool` |
| Poke tool | Keys go to the active caret, such as a Pin or RAM | `PokeTool`, `instance/InstancePoker` |
| Text tool | Text editing; <kbd>Alt</kbd> combinations ignored | `tools/TextTool`, `comp/TextFieldCaret` |

### 2.7 Other panels

| Area | Keys | Class |
| --- | --- | --- |
| Project explorer tree | <kbd>Delete</kbd>/<kbd>Backspace</kbd> removes the circuit | `ProjectExplorer` |
| Toolbox filter field | <kbd>Esc</kbd> clears the filter | `gui/main/Toolbox` |
| Appearance text tool | <kbd>Enter</kbd> commits, <kbd>Esc</kbd> cancels | `draw/tools/TextTool` |
| Pin value dialogs | <kbd>Enter</kbd> OK, <kbd>Esc</kbd> cancel | `std/wiring/Pin` |
| Assembly viewer | <kbd>F2</kbd> | `gui/menu/AssemblyWindow` |
| Hex editor | Caret navigation and editing | `hex/Caret` |
| Analyzer tabs, K-map | Table caret navigation and editing | `analyze/gui/TableTabCaret`, `KarnaughMapPanel` |
| Chronogram | Signal list and timeline keys | `gui/chrono/LeftPanel`, `RightPanel` |

## 3. Mouse interactions

### 3.1 Default mouse mappings (circuit canvas)

The mappings below come from `resources/logisim/default.templ`. `XmlReader.initMouseMappings` loads
them, and each project can change them in Project Options, Mouse (`gui/opts/MouseOptions`).

| Input | Tool | Effect |
| --- | --- | --- |
| Left button | Active tool | Depends on the tool. See sections 3.3 and 4. |
| Middle button | Poke Tool | Poke a component. Dragging on empty canvas pans. |
| Right button | Menu Tool | Context menu for a component or the selection (`tools/MenuTool`) |
| <kbd>Ctrl</kbd>+left button | Menu Tool | Context menu, the same as right-click |

`Canvas.mousePressed` makes a non-primary mapped tool active for the whole drag. It saves the
previous tool in `tempTool` and restores it on release.

### 3.2 Canvas navigation

| Input | Action | Class |
| --- | --- | --- |
| Wheel | Scroll vertically | `Canvas.mouseWheelMoved` |
| <kbd>Shift</kbd>+wheel | Scroll horizontally | `Canvas.mouseWheelMoved` |
| <kbd>Ctrl</kbd>+wheel | Zoom, anchored at the mouse | `Canvas.mouseWheelMoved`, `doZoom` |
| Wheel over a scrollable poked part | Sends ↑ or ↓ to the Poke tool | `PokeTool.isScrollable` |
| Mouse buttons 4–7 | Scroll horizontally, 10 units | `Canvas.mousePressed` |
| Middle double-click | Centre the circuit | `Canvas.mouseReleased` |
| Click the "auto-zoom" overlay button | Centre and fit the circuit | `Canvas.autoZoomButtonClicked` |
| Drag near the edge | Auto-pan | `Canvas` (auto-pan) |
| Grid button in the zoom control | Toggle the grid | `gui/generic/ZoomControl` |
| Wheel on the scroll pane | Zoom, scroll and shift-scroll | `CanvasPane.ZoomListener` |

### 3.3 Selection and moving (Edit tool, left button)

| Input | Action | Class |
| --- | --- | --- |
| Click a component | Select it, replacing the selection | `SelectTool.mousePressed` |
| <kbd>Shift</kbd>+click a component | Add to, or remove from, the selection | `SelectTool.mousePressed` |
| Drag on empty canvas | Rectangle select (<kbd>Shift</kbd> toggles) | `SelectTool` (`RECT_SELECT`) |
| Drag the selection | Move it; wires stay connected (section 4.2) | `SelectTool`, `MoveGesture` |
| <kbd>Shift</kbd> during the move drag | Invert keep-connected for this move | `SelectTool.shouldConnect` |
| Double-click a labelled component | Open the label edit dialog | `AutoLabel.askAndSetLabel` |
| Click with the Add tool | Place the component, snapped to the grid | `AddTool.mousePressed`, `mouseReleased` |

### 3.4 Poke tool

| Input | Action | Class |
| --- | --- | --- |
| Click a wire | Show the value caret and highlight the connected net | `PokeTool` (`WireCaret`) |
| Click a pokable component | Component action, such as toggling a pin | `InstancePoker` subclasses |
| Double-click a subcircuit | Open the subcircuit's simulation state | `circuit/SubcircuitPoker` |
| Drag on empty space | Pan the view with a hand cursor | `PokeTool.mouseDragged` |

### 3.5 Explorer panels

| Input | Action | Class |
| --- | --- | --- |
| Double-click a circuit in the toolbox | Open that circuit for editing | `gui/main/ToolboxManip.doubleClicked` |
| Right-click a tree node (popup trigger) | Context menu | `gui/generic/ProjectExplorer` |
| Double-click a simulation-tree node | Switch to that circuit state | `gui/main/SimulationExplorer` |

### 3.6 Appearance editor (`draw/**`)

| Input | Action | Class |
| --- | --- | --- |
| Popup trigger (right-click) | Context menu | `CanvasListener`, `AppearanceEditPopup` |
| Middle drag | Pan | `AppearanceCanvas.handleMiddleButtonPanEvent` |
| <kbd>Shift</kbd>+click | Toggle a shape in the selection | `draw/tools/SelectTool` |
| <kbd>Ctrl</kbd> while dragging | Snap to the grid | `draw/tools/*` |
| <kbd>Shift</kbd> drawing a line or curve | Constrain to 8 directions | `LineTool`, `CurveTool`, `PolyTool` |
| <kbd>Shift</kbd> / <kbd>Alt</kbd> on rectangles | Square / from the centre | `RectangularTool` |
| Double-click with the polygon tool | Finish the polygon | `draw/tools/PolyTool` |

## 4. Wire editing behaviour

### 4.1 Creating wires

Wiring is drag-based. There is no click-click polyline mode.

1. **Start:** Pressing the left button snaps to the 10 px grid and records `start`
   (`WiringTool.mousePressed`). If wires already end at that point, the gesture may shorten one of
   them (`startShortening`).
2. **Routing:** The first axis the mouse moves along fixes the orientation of the first leg. The
   preview is an L-shape: horizontal then vertical, or the reverse (`WiringTool.computeMove`). If
   the cursor returns to the start column or row, the orientation can flip.
3. **Release:** The tool creates one straight wire, or two wires meeting at the elbow. It commits
   them as a single `CircuitMutation` labelled "Add Wire" or "Add Wires"
   (`WiringTool.mouseReleased`).
4. **Click without dragging:** Nothing is created. The Edit tool turns the click into a selection
   click (`EditTool.mouseReleased`, `WiringTool.resetClick`).
5. **Undo the last wire:** <kbd>Backspace</kbd> works only while that wire is still the project's
   most recent action.

### 4.2 Shortening, removing and repairing

- **Shorten by dragging back.** Dragging from a wire end back along the same wire shortens it, and
  dragging to its other end removes it. The original wire is hidden while dragging.
  `WiringTool.willShorten`, `performShortening`, `getHiddenComponents`.
- **Endpoint repair at components.** If a wire end lands one grid step inside a component that
  implements `WireRepair`, such as `Splitter`, the end moves to the component's real connection
  point. `WiringTool.checkForRepairs`, `circuit/Splitter.shouldRepairWire`.
- **Automatic merge and split.** After every mutation, collinear overlapping wires are merged and
  wires are split at T-junctions. `circuit/WireRepair`, called from `circuit/CircuitTransaction`.
- **Keep connections when moving.** When a selection moves, attached wires are rerouted. The
  `MOVE_KEEP_CONNECT` preference controls this (default on), and <kbd>Shift</kbd> inverts it.
  `tools/move/MoveGesture`, `SelectTool.shouldConnect`.

### 4.3 Deciding between starting a wire and selecting (Edit tool)

`EditTool.isWiringPoint` and `EditTool.updateLocation` decide what a left press does:

- A press within 6 px (`dx² + dy² < 36`) of a grid point that has a component pin or a wire on it
  starts a **wire**.
- A press in the interior of an already-selected wire, away from its ends, starts a **selection**
  gesture, so the wire can be moved.
- Otherwise the press starts a **selection**.
- Holding <kbd>Alt</kbd> swaps the first and third cases and removes the 6 px distance limit.
- On hover, wiring points are marked with an indicator drawn in `Value.trueColor`
  (`EditTool.draw`, `EditTool.repaintIndicators`).

Commit `a31f929c5` recently adjusted wire selection at instance boundaries.

### 4.4 Inspecting wires

| Behaviour | Class |
| --- | --- |
| Poke a wire to see its value and highlight its net | `PokeTool` (`WireCaret`), `Canvas.setHighlightedWires` |
| Wire colour shows its value and width | `circuit/Wire`, `data/Value` colours |

## 5. Findings and inconsistencies

These are observations only. No code was changed.

- **F1.** The window close and minimize defaults are <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>W</kbd>
  and <kbd>M</kbd>. `resetHotkeys()` sets them to <kbd>Menu</kbd>+<kbd>W</kbd> and <kbd>M</kbd>, so
  after a reset Minimize has the same key as File, Merge (<kbd>Menu</kbd>+<kbd>M</kbd>).
  `AppPreferences` (`HOTKEY_WINDOW_*`, `resetHotkeys`).
- **F2.** With the initial default, Window, Close (<kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>W</kbd>)
  has the same key as File, Close. `MenuFile`, `WindowMenu`.
- **F3.** *(Fixed by the fork; see section 7.)* Canvas zoom uses `isControlDown()` and not the menu
  mask, so on macOS it is <kbd>Ctrl</kbd>+<kbd>+</kbd> and not <kbd>Cmd</kbd>+<kbd>+</kbd>. It
  checks `VK_PLUS` and not `VK_EQUALS`, so on US layouts <kbd>Ctrl</kbd>+<kbd>=</kbd> (unshifted)
  does not zoom in. `Canvas.MyListener.keyPressed`.
- **F4.** *(Partly addressed: the fork adds fit and reset shortcuts.)* Zoom shortcuts and
  zoom-to-fit have no menu items and cannot be rebound. `Canvas`, `ZoomControl`.
- **F5.** By default <kbd>Ctrl</kbd>+left-click opens the Menu Tool on the circuit canvas. In the
  appearance editor, <kbd>Ctrl</kbd>+drag snaps to the grid, so the modifier means different things
  in the two editors. On macOS, Ctrl-click is also the system popup trigger. `default.templ`,
  `draw/tools/*`.
- **F6.** Right-click works differently in the two editors. The circuit canvas uses `MouseMappings`,
  and the appearance editor uses `isPopupTrigger()`. `Canvas`, `draw/canvas/CanvasListener`.
- **F7.** *(Addressed: middle-drag now pans in both editors.)* The middle button pokes components on
  the circuit canvas and pans only on empty space. In the appearance editor it always pans.
  `default.templ`, `AppearanceCanvas`.
- **F8.** The arrow keys in the Edit tool change component facing. There is no keyboard nudge for
  moving a selection. `EditTool.keyPressed`.
- **F9.** Rotate is <kbd>R</kbd> in the Add tool but <kbd>Ctrl</kbd>+<kbd>Space</kbd> in the Edit
  tool, and only the Add tool's key can be rebound. `AddTool`, `EditTool`.
- **F10.** Many canvas keys are hard-coded and are not checked by `hotkeyCheckConflict`, so a user
  can rebind a hotkey onto one of them with no warning. These include <kbd>Ctrl</kbd>+<kbd>±</kbd>,
  <kbd>Ctrl</kbd>+<kbd>Space</kbd>, <kbd>X</kbd>, <kbd>Esc</kbd>, <kbd>Backspace</kbd>,
  <kbd>Delete</kbd>, digits, <kbd>Alt</kbd>+digits and <kbd>Alt</kbd>+arrows. A comment in
  `HotkeyOptions` acknowledges this. `AppPreferences.hotkeyCheckConflict`.
- **F11.** What a single letter does depends on the selection. <kbd>S</kbd>, <kbd>N</kbd>,
  <kbd>M</kbd>, <kbd>W</kbd>, <kbd>+</kbd> and <kbd>-</kbd> act on gates first. <kbd>L</kbd>,
  <kbd>T</kbd>, <kbd>V</kbd>, <kbd>H</kbd> and <kbd>A</kbd> then act as auto-label keys. Nothing in
  the UI shows which applies. `SelectTool.keyPressed`.
- **F12.** `AutoLabel.USED_KEY_STROKES` and `KEY_STROKES` hard-code L, T, V, H and A, and nothing
  reads them. They do not follow user rebinding. `util/AutoLabel`.
- **F13.** The Wiring tool has no <kbd>Esc</kbd> to cancel a drag in progress, but the Add tool uses
  <kbd>Esc</kbd> to return to the Edit tool. `WiringTool`, `AddTool`.
- **F14.** Wires can only be drawn by dragging, so a wire with more than one bend needs several
  drags. `WiringTool`.
- **F15.** <kbd>Backspace</kbd> deletes the selection when something is selected, and undoes the
  last wire when nothing is. <kbd>Delete</kbd> does not undo the last wire. `EditTool.keyPressed`.
- **F16.** The wire preview is drawn in hard-coded `Color.BLACK`, which may be hard to see on a dark
  canvas background (`CANVAS_BG_COLOR`). `WiringTool.draw`.
- **F17.** Horizontal scroll steps differ by input. <kbd>Shift</kbd>+wheel scrolls by
  `blockIncrement`×2, and buttons 4–7 scroll by `unitIncrement`×10. `Canvas.mouseWheelMoved`,
  `Canvas.mousePressed`.

## 6. Conflicts with the planned keymap (UX_PLAN.md section 9)

Status: **Free** means nothing is bound today. **Same** means an existing binding already does the
same thing. **Conflict** means the key does something else today. The resolutions below were
accepted on 2026-10-09, using the recommended option where the notes list several.

| Planned key | Planned action | Existing binding | Status | Resolution |
| --- | --- | --- | --- | --- |
| Mod+K | Command palette | Simulate, Ticks enabled | Conflict | Use Mod+Shift+P (free). See note 1. |
| Esc | Select tool / cancel | Add tool: back to Edit tool; Toolbox filter: clear | Same | Extend to Wiring tool (F13). |
| W | Wire tool | Gate size wide, when a gate is selected/armed | Conflict | See note 2. |
| A | AND gate | Auto-label "stop self-numbering", when selected/armed | Conflict | See note 2. |
| O | OR gate | – | Free | |
| N | NOT gate | Gate size narrow (with S), when a gate is selected/armed | Conflict | See note 2. |
| X | XOR gate | Add tool: toggle matrix placement | Conflict | Move matrix placement to Shift+X. |
| Shift+A | NAND gate | – (letter hotkeys compare modifiers exactly) | Free | |
| Shift+O | NOR gate | – | Free | |
| I | Input pin | – | Free | |
| P | Output pin | – | Free | |
| T | Text tool | Auto-label toggle, when selected/armed | Conflict | See note 2. |
| 2 to 9 | Gate input count | Digits already set a gate's input count | Same | Keep existing. See note 3. |
| R | Rotate CW | Add tool: rotate ghost; Edit tool: unbound | Same | Add R to Edit tool. |
| Shift+R | Rotate CCW | – | Free | |
| Mod+D | Duplicate | Edit, Duplicate | Same | Check offset and copy selection in Phase 4. |
| Arrows | Nudge 1 grid unit | Set facing (`HOTKEY_DIR_*`) in Edit and Add tools | Conflict | See note 4. |
| Shift+Arrows | Nudge 5 grid units | – | Free | |
| Space+drag | Pan | Ctrl+Space rotates; plain Space unused | Free | Ignore while a Poke caret or text field is active. |
| F | Fit all | – | Free | |
| Shift+F | Fit selection | – | Free | |
| Mod+0 | Reset zoom | Select toolbar tool 10 (`HOTKEY_TOOL_SELECT_10`) | Conflict | Unbind tool 10 by default; keep Mod+1…9. |
| Ctrl+Tab | Circuit switcher | Swing focus traversal (consumed by `KeyboardFocusManager`) | Conflict | See note 5. |
| Alt+Up | Back to parent | Alt+arrows set label position on a selection | Conflict | See note 6. |
| Backspace (no selection) | Back to parent | Undo last wire (Edit) or placement (Add) | Conflict | Drop it; keep Alt+Up. |
| ? | Cheat sheet | – | Free | Match the typed character `?`, not Shift+/, for non-US layouts. |

**Note 1, command palette.** The existing "Find Action" omni-search already provides fuzzy search
over menu actions and components (`gui/search/OmniSearchDialog`, `FuzzyMatcher`, and the providers
`MenuSearchProvider` and `AddToolSearchProvider`). It opens with Mod+Shift+A or by tapping Shift
twice. Extending it with new providers (circuits, `>` and `@` prefixes, trailing parameters,
recents) is likely less code and fewer upstream conflicts than building a separate palette. It
would then be bound to Mod+Shift+P as well.

**Note 2, single letters on a selection.** `SelectTool.keyPressed` and `AddTool.keyPressed` send
S/N/M/W/+/- to the gate-size handler and L/T/V/H/A to the auto-labeler. They do this whenever a gate
or labelled component is selected or armed. With the planned keymap, arming an AND gate with A and
then pressing N or W would resize it instead of switching tool. Options:

- (a) The fork keymap wins. Move the gate-size and auto-label hotkeys to Alt+letter. They are
  already rebindable preferences, so only their defaults change. On macOS, Option+letter still
  produces a key code, so this works, but it should be tested.
- (b) The existing handlers win while something is selected or armed, and tool letters only work
  with an empty selection. This is surprising after placing a component.
- (c) Choose different tool letters.

Accepted: (a).

**Note 3, digits.** Typing a digit on a selected or armed gate already sets the input count.
`NumericConfigurator` collects digits typed in quick succession, so "1", "2" gives 12.
<kbd>Alt</kbd>+digits sets the bit width. Both already match the plan, so no new code is needed.

**Note 4, arrows.** Arrows currently set facing. <kbd>Alt</kbd>+arrows (label position) and
<kbd>Menu</kbd>+<kbd>←</kbd>/<kbd>→</kbd> (simulation state navigation) are taken, so facing can't
simply move to a modifier. Accepted: in the Edit tool, arrows nudge and facing is changed with
R and Shift+R. In the Add tool, where there is nothing to nudge, arrows keep setting the facing.

**Note 5, Ctrl+Tab.** Swing uses Ctrl+Tab for focus traversal, so the canvas must remove it from
its focus traversal keys. It is also deliberately the physical Ctrl key on every platform, because
Cmd+Tab is the macOS app switcher. This is an accepted exception to hard constraint 4.

**Note 6, Alt+Up.** Use Alt+Up only when nothing is selected, so it doesn't clash with label
positioning. The existing Simulate menu item "go out to parent state" (<kbd>Menu</kbd>+<kbd>←</kbd>)
already moves up the simulation hierarchy. Phase 1 should decide whether "back to parent" reuses it
or keeps its own history of viewed circuits.

## 7. Fork changes

Each feature below can be switched off on the **Fork UX** preferences tab
(`logisim/fork/gui/ForkUxOptions`). The flags are stored in their own `java.util.prefs` node
(`logisim/fork/ForkPreferences`), never in .circ files. With a flag off, the stock behaviour from
sections 1–5 returns.

### 7.1 Phase 1: canvas navigation

`gui/main/Canvas` (`MyListener`) offers every key, mouse and wheel event to
`logisim/fork/nav/CanvasNavigator` first. The navigator handles it if it's a fork binding;
otherwise the stock handling runs unchanged.

All classes are in `logisim/fork/nav/`.

| Input | Action | Flag | Class |
| --- | --- | --- | --- |
| <kbd>Space</kbd>+drag | Pan; also mid-wire, without ending the wire | `nav.spacePan` | `CanvasNavigator` |
| Middle drag | Pan with any tool; a still click runs the mapped tool | `nav.middlePan` | `CanvasNavigator` |
| <kbd>Cmd</kbd>+click/drag (macOS) | Middle button: poke, pan, double-click centres | `nav.middlePan` | `CanvasNavigator` |
| <kbd>Menu</kbd>/<kbd>Ctrl</kbd>+wheel | Zoom a level; point under cursor stays put | `nav.zoomToCursor` | `ViewActions` |
| <kbd>Menu</kbd>+<kbd>=</kbd>/<kbd>-</kbd> | Zoom in / out at the cursor (fixes F3) | `nav.zoomToCursor` | `ViewActions` |
| Trackpad pinch (macOS) | Smooth zoom towards the fingers | `nav.pinchZoom` | `PinchZoom` |
| <kbd>F</kbd> / <kbd>Shift</kbd>+<kbd>F</kbd> | Fit the circuit / the selection | `nav.fitKeys` | `ViewActions` |
| <kbd>Menu</kbd>+<kbd>0</kbd> | Reset zoom to 100% | `nav.resetZoom` | `ViewActions.resetZoom` |
| <kbd>Ctrl</kbd>+(<kbd>Shift</kbd>+)<kbd>Tab</kbd> | Recent-circuits switcher | `nav.circuitSwitcher` | `CircuitSwitcher` |
| <kbd>Alt</kbd>+<kbd>↑</kbd>, no selection | To parent state, or previously viewed circuit | `nav.backToParent` | `ViewActions` |

Notes:

- **Typing guard.** <kbd>Space</kbd>, <kbd>F</kbd> and <kbd>Alt</kbd>+<kbd>↑</kbd> are ignored while
  the Text tool is editing (`TextTool.isEditing`) or a Poke caret takes keys
  (`PokeTool.isScrollable`).
- **Middle click timing.** With `nav.middlePan` on, a middle click runs on release instead of on
  press, because only then is it clear the click wasn't a pan.
- **Toolbar tool 10.** <kbd>Menu</kbd>+<kbd>0</kbd> is no longer the default for toolbar tool 10
  (`AppPreferences.HOTKEY_TOOL_SELECT_10`). It can still be bound on the Hotkeys tab.
- **Pinch needs a JVM flag.** It uses the internal `com.apple.eawt.event` API, which needs
  `--add-exports java.desktop/com.apple.eawt.event=ALL-UNNAMED`. The jar manifest, the macOS app
  package and `gradle run` on macOS all set it. Without it, pinch is unavailable and a single line is
  logged.
- **Fit near the origin.** The canvas can't scroll above or left of the origin, so a small circuit
  near the top-left is fitted but can't be exactly centred. This matches the toolbar's "Auto" button.

### 7.2 Keymap (Phase 3)

All fork shortcuts come from one registry of named actions (`logisim/fork/keymap/ForkActions`).
`ForkShortcuts`, a `KeyEventDispatcher`, runs a binding only when its action applies, so other keys
reach Logisim unchanged. Bindings can be changed in `keymap.json` (see [keymap.md](keymap.md)).
<kbd>?</kbd> opens a cheat sheet of the active bindings (`logisim/fork/gui/CheatSheet`).

Plain-letter shortcuts only fire while the canvas has focus and nothing is being typed
(`TypingGuard`). To free the letters, Logisim's own defaults moved (`AppPreferences`, resettable on
the Hotkeys tab): gate size is now <kbd>Alt</kbd>+<kbd>S</kbd>/<kbd>N</kbd>/<kbd>M</kbd>/<kbd>W</kbd>,
and auto-label is <kbd>Alt</kbd>+<kbd>L</kbd>/<kbd>T</kbd>/<kbd>V</kbd>/<kbd>H</kbd>/<kbd>A</kbd>.
Settings saved before this change keep their old keys until **Reset** is pressed on the Hotkeys tab.

| Input | Action | Flag |
| --- | --- | --- |
| <kbd>W</kbd> / <kbd>T</kbd> | Wiring tool / Text tool | `keys.singleKeyTools` |
| <kbd>A</kbd> <kbd>O</kbd> <kbd>N</kbd> <kbd>X</kbd> | AND, OR, NOT, XOR gate | `keys.singleKeyTools` |
| <kbd>Shift</kbd>+<kbd>A</kbd> / <kbd>Shift</kbd>+<kbd>O</kbd> | NAND / NOR gate | `keys.singleKeyTools` |
| <kbd>I</kbd> / <kbd>P</kbd> | Input pin / output pin | `keys.singleKeyTools` |
| <kbd>Esc</kbd> | Edit tool; with the Edit tool, clear the selection | `keys.singleKeyTools` |
| <kbd>R</kbd> / <kbd>Shift</kbd>+<kbd>R</kbd> | Rotate selection or armed part CW / CCW | `keys.rotate` |
| <kbd>2</kbd> … <kbd>9</kbd> | Gate input count (unchanged stock behaviour) | — |
| <kbd>?</kbd> | Cheat sheet | `keys.cheatSheet` |

### 7.3 Command palette (Phase 2)

<kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>P</kbd> (or the stock <kbd>Menu</kbd>+<kbd>Shift</kbd>+<kbd>A</kbd>
or double <kbd>Shift</kbd>) opens Find Action with the fork's providers (`logisim/fork/palette/`).
The built-in component and menu providers are swapped for wrappers; with `palette.extended` off they
behave exactly as before.

- `>` searches only actions (menus and fork commands); `@` searches only circuits.
- A trailing number sets the obvious attribute (`ParameterRule`): `and 3` gives 3 inputs, `mux 4`
  gives 2 select bits, `reg 8` gives 8 bits. A number a part can't take is ignored.
- Circuits: <kbd>Enter</kbd> opens; <kbd>Shift</kbd>+<kbd>Enter</kbd> arms it as a subcircuit.
- On an empty query, recently armed parts come first.
- The dialog opens centred, as the stock one does. The plan's "near the top" placement was not done,
  to avoid changing the upstream dialog.

### 7.4 Selection editing (Phase 4)

| Input | Action | Flag | Class |
| --- | --- | --- | --- |
| Arrows / <kbd>Shift</kbd>+arrows | Nudge selection 1 / 5 steps; wires follow | `select.nudge` | `EditActions` |
| <kbd>Menu</kbd>+<kbd>D</kbd> | Duplicate one step down-right; copy selected | `select.duplicateOffset` | `ArrangeActions` |
| <kbd>Menu</kbd>+<kbd>Alt</kbd>+arrows | Align left / right / top / bottom | `select.align` | `ArrangeActions` |
| Palette | Align centres, distribute horizontally / vertically | `select.align` | `ArrangeActions` |
| Dragging parts | Dashed guides where pins line up (drawing only) | `select.snapGuides` | `SnapGuides` |
| Double-click wire | Select its whole net | `select.connected` | `SelectConnected` |
| <kbd>Shift</kbd>+double-click part | Select it with everything wired to it | `select.connected` | `SelectConnected` |

Arrows only nudge when the Edit tool has a selection; otherwise they keep setting facing. Align and
distribute check pin-to-pin connectivity and undo themselves if parts would land on other wires.

### 7.5 Wiring (Phase 5)

| Input | Action | Flag | Class |
| --- | --- | --- | --- |
| Hover a wire | Highlight its net; ring its pins | `wire.netHighlight` | `NetHover` |
| — | Red ring on unconnected pins, cross on dangling ends | `view.showUnconnected` | `NetHover` |
| Press near a pin | Larger pick-up area, crosshair cursor; starts a wire | `wire.fromPin` | `PinFinder` |
| Click a pin, click bends | Click-to-route; <kbd>Space</kbd> flips, <kbd>Esc</kbd> cancels | `wire.clickRoute` | `WireRouter` |
| <kbd>Alt</kbd>+click wire | Delete the run between junctions | `wire.segmentDelete` | `WireActions` |
| Palette | Clean up wires (dangling stubs) | `wire.cleanup` | `WireActions` |
| <kbd>Delete</kbd> parts | Also delete wires that only led to them | `wire.deleteDangling` | `WireActions` |

- **5.2 rubber-banding** needed no new code. Logisim's `MoveGesture` already reroutes attached wires
  orthogonally when moving, and wires can't be diagonal. Nudge, align and distribute all use it.
- **Clean-up** only removes stubs. Merging touching segments and dropping zero-length ones already
  happens on every edit (`WireRepair`). Clean-up refuses if connectivity would change.
- **Undo:** each gesture is one undo step of ordinary wires; nothing is added to saved files.

### 7.6 Phase 6

- **Fork UX tab** lists every flag with a description, grouped by phase area.
- **Status hint** while routing a wire, in the canvas message area (`ui.statusHints`).
- **Cheat sheet** (<kbd>?</kbd>) shows the active bindings, keymap problems and the keymap file path.
- **Installers:** the `Fork installers` workflow (`.github/workflows/fork-release.yml`) builds them on
  GitHub for Windows (MSI and portable ZIP), macOS (DMG, Apple Silicon and Intel) and Linux (DEB, RPM,
  x86_64 and arm64), after running the tests on all three. Run it from the Actions tab, or push a
  `fork-v*` tag to get a draft release. Locally: `./gradlew createDmg`, `createMsi` or `createDeb` on
  the matching system. Installers use the `forkInstaller` id from `gradle.properties` (their own
  name, bundle id and Windows upgrade code), so they install alongside stock Logisim-evolution. The
  project name is unchanged, because saved files mention it.
