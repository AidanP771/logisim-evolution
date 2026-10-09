# Fork keymap

The UX fork's keyboard shortcuts can be changed without touching any circuit file. Press
<kbd>?</kbd> in the main window to see the bindings that are active right now, any problems found
in your keymap file, and where that file lives.

## Where the file goes

Create `keymap.json` in the fork's config folder:

| Platform | Folder |
| --- | --- |
| macOS | `~/Library/Application Support/Logisim-evolution-fork/` |
| Windows | `%APPDATA%\Logisim-evolution-fork\` |
| Linux | `$XDG_CONFIG_HOME/logisim-evolution-fork/` (usually `~/.config/logisim-evolution-fork/`) |

Changes are read when the app starts, or when you press **Reload keymap** in the cheat sheet.

## Format

```json
{
  "bindings": {
    "tool.and": "Shift+G",
    "view.fit": ["F", "Mod+Shift+F"],
    "tool.xor": null
  }
}
```

- A string sets one binding, a list sets several, and `null` or `[]` unbinds the action.
- Actions you don't list keep their defaults.
- Modifiers: `Mod` (Cmd on macOS, Ctrl elsewhere), `Ctrl`, `Cmd`, `Alt` (or `Option`), `Shift`.
- Keys: letters, digits, `F1`–`F12`, `Up`, `Down`, `Left`, `Right`, `Esc`, `Space`, `Tab`, `Enter`,
  `Backspace`, `Delete`, `Insert`, `Home`, `End`, `PageUp`, `PageDown`, `=`, `-`, `/`, `,`, `.`, `;`,
  `'`, `[`, `]`, `\`, `` ` ``, `+` (as in `Mod++`), `NumpadAdd`, `NumpadSubtract`, `Numpad0`.
- `?` and other characters that need Shift on some keyboards match the typed character, so they
  work on every layout.
- Unknown actions, invalid keys and keys already used by another action are skipped and listed in
  the cheat sheet. They never stop the app from starting.
- Plain-letter shortcuts only work while the circuit canvas has focus and you aren't typing text.
- Every feature can also be switched off on the **Fork UX** preferences tab, which turns its
  shortcuts off too.

Mouse gestures (Space+drag, middle-drag, click-to-route, Alt+click and so on) are listed in the
cheat sheet but can't be rebound.

## Actions

### View

| Id | Action | Default |
| --- | --- | --- |
| `view.zoomIn` | Zoom in at the pointer | `Mod+=`, `Mod++`, `Mod+NumpadAdd` (and the same with `Ctrl`) |
| `view.zoomOut` | Zoom out at the pointer | `Mod+-`, `Mod+NumpadSubtract`, `Ctrl+-`, `Ctrl+NumpadSubtract` |
| `view.fit` | Fit the circuit in the window | `F` |
| `view.fitSelection` | Fit the selection in the window | `Shift+F` |
| `view.resetZoom` | Reset zoom to 100% | `Mod+0`, `Mod+Numpad0` |

### Navigate

| Id | Action | Default |
| --- | --- | --- |
| `nav.parent` | Back to the parent circuit | `Alt+Up` |
| `palette.open` | Command palette | `Mod+Shift+P` |

### Tools

| Id | Action | Default |
| --- | --- | --- |
| `tool.select` | Edit tool / clear selection | `Esc` |
| `tool.wire` | Wiring tool | `W` |
| `tool.and` | AND gate | `A` |
| `tool.or` | OR gate | `O` |
| `tool.not` | NOT gate | `N` |
| `tool.xor` | XOR gate | `X` |
| `tool.nand` | NAND gate | `Shift+A` |
| `tool.nor` | NOR gate | `Shift+O` |
| `tool.inputPin` | Input pin | `I` |
| `tool.outputPin` | Output pin | `P` |
| `tool.text` | Text tool | `T` |

### Edit

| Id | Action | Default |
| --- | --- | --- |
| `edit.rotateCw` | Rotate clockwise | `R` |
| `edit.rotateCcw` | Rotate counter-clockwise | `Shift+R` |
| `edit.nudgeLeft` | Nudge left | `Left` |
| `edit.nudgeRight` | Nudge right | `Right` |
| `edit.nudgeUp` | Nudge up | `Up` |
| `edit.nudgeDown` | Nudge down | `Down` |
| `edit.nudgeLeftBig` | Nudge left 5 steps | `Shift+Left` |
| `edit.nudgeRightBig` | Nudge right 5 steps | `Shift+Right` |
| `edit.nudgeUpBig` | Nudge up 5 steps | `Shift+Up` |
| `edit.nudgeDownBig` | Nudge down 5 steps | `Shift+Down` |
| `edit.delete` | Delete (with wires left dangling) | `Delete`, `Backspace` |

### Arrange

| Id | Action | Default |
| --- | --- | --- |
| `arrange.alignLeft` | Align left edges | `Mod+Alt+Left` |
| `arrange.alignRight` | Align right edges | `Mod+Alt+Right` |
| `arrange.alignTop` | Align top edges | `Mod+Alt+Up` |
| `arrange.alignBottom` | Align bottom edges | `Mod+Alt+Down` |
| `arrange.alignCenterX` | Align centres (vertical line) | — |
| `arrange.alignCenterY` | Align centres (horizontal line) | — |
| `arrange.distributeH` | Distribute horizontally | — |
| `arrange.distributeV` | Distribute vertically | — |

### Wiring

| Id | Action | Default |
| --- | --- | --- |
| `wire.cleanup` | Clean up wires (remove dangling stubs) | — |
| `view.toggleUnconnected` | Show or hide unconnected markers | — |

### Help

| Id | Action | Default |
| --- | --- | --- |
| `help.cheatSheet` | Show this cheat sheet | `?` |
