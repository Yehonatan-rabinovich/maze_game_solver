# Maze Solver — Project Summary

A Java Swing desktop application that fetches mazes from a REST API, decodes
them pixel-by-pixel, renders them entirely in custom Swing painting code, and
animates a BFS shortest-path solution on demand.

---

## Architecture

```
Main.java
  └── GameWindow.java          (JFrame, CardLayout)
        ├── ConfigPanel.java   (Screen 1 — config display + maze request)
        │     └── API.java     (HTTP client, called via SwingWorker)
        └── MazePanel.java     (Screen 2 — maze drawing + animation)
              └── MazeSolver.java  (BFS solver)
```

### Two-screen flow

```
App starts
    │
    ▼
┌───────────────────────────────┐
│  ConfigPanel  (Screen 1)      │  ← always starts here
│                               │
│  Render Configuration         │  Fetched from API on startup
│  • Wall Cell Color            │
│  • Path Color                 │
│  • Draw Grid                  │
│  • Grid Color                 │
│  • Animation Delay            │
│                               │
│  Width  [30]  Height [30]     │  User inputs, range 5–100
│                               │
│  [Refresh Config] [GET MAZE]  │
└───────────────────────────────┘
    │  GET MAZE clicked
    ▼
┌───────────────────────────────┐
│  MazePanel  (Screen 2)        │
│                               │
│  Maze drawn manually          │  No original PNG shown
│  (white cells + wall color)   │
│                               │
│  [Check Solution] [← Back]    │
└───────────────────────────────┘
    │  Check Solution clicked
    ▼
  BFS runs → path animated cell by cell
```

---

## File Reference

### `Main.java`
Entry point. Bootstraps `GameWindow` on the Swing Event Dispatch Thread via
`SwingUtilities.invokeLater`.

---

### `GameWindow.java`
Root `JFrame`. Uses a `CardLayout` to switch between the two screens.

| Method | Description |
|---|---|
| `GameWindow()` | Creates both panels, calls `configPanel.fetchConfig()` immediately |
| `showMaze(result, config)` | Hands decoded maze + config to `MazePanel`, flips to screen 2 |
| `showConfig()` | Flips back to screen 1 (no re-fetch) |

---

### `API.java`
Blocking HTTP client using OkHttp. All methods must be called from a
background thread (inside `SwingWorker.doInBackground`).

**`RenderConfig` fields**

| Field | Type | Source key |
|---|---|---|
| `wallCellColor` | `String` | `wallCellColor` |
| `pathColor` | `String` | `pathColor` |
| `drawGrid` | `boolean` | `drawGrid` |
| `gridColor` | `String` | `gridColor` |
| `animationDelayMs` | `int` | `animationDelayMs` |

**`MazeResult` fields**

| Field | Type | Description |
|---|---|---|
| `grid` | `boolean[][]` | `grid[row][col]` — true = walkable |
| `rows` | `int` | Height parameter |
| `cols` | `int` | Width parameter |

**Pixel decoding logic** (`parseGrid`): samples the centre pixel of each
logical cell block. R, G, B all > 200 → walkable passage; otherwise → wall.
The original `BufferedImage` is used only here and is never stored or displayed.

---

### `ConfigPanel.java`
Screen 1. All API calls run in `SwingWorker`s so the EDT is never blocked.

**Validation rule:** width/height inputs that are non-numeric, below 5, or
above 100 are silently replaced with 30. The validated value is written back
to the text field so the user always sees what was actually used.

**Button behavior**

| Button | Action |
|---|---|
| Refresh Config | Re-fetches `/get-render-config` only — never loads a maze |
| GET MAZE | Validates dimensions → fetches maze → calls `window.showMaze()` |

Both buttons are disabled while a network request is in progress.

---

### `MazePanel.java`
Screen 2. Contains an inner `DrawPanel` that extends `JPanel` and does all
custom painting inside `paintComponent`.

**Drawing rules (all colors from API — nothing hardcoded except `CELL_SIZE`)**

| Element | Color |
|---|---|
| Walkable passage | Always `Color.WHITE` |
| Wall cell | `config.wallCellColor` |
| Grid lines | `config.gridColor` (only when `config.drawGrid == true`) |
| Solution path | `config.pathColor` (animated) |

**Cell size:** `CELL = 20` px (hardcoded as permitted by spec).

Large mazes (up to 100 × 100 = 2000 × 2000 px) are placed inside a
`JScrollPane` so they can be fully explored.

**Animation**

A `javax.swing.Timer` fires every `config.animationDelayMs` milliseconds. Each
tick increments `pathTip` by one and calls `repaint()`, which paints all
solution cells from index 0 to `pathTip`. The result is the path visibly
growing one cell at a time.

- The Check Solution button is **disabled** while animation runs, preventing
  multiple simultaneous animations.
- Clicking Check Solution again clears the previous path and replays from
  the beginning (BFS result is cached — not re-computed).

---

### `MazeSolver.java`
Stateless BFS solver. No instance needed.

```java
List<int[]> path = MazeSolver.solve(mazeResult.grid);
// Each element: [row, col]
// Returns null if no path exists
```

**Returns null when:**
- Grid is null or empty
- Start cell `(0,0)` is a wall
- End cell `(rows-1, cols-1)` is a wall
- No walkable path connects start to end

**Complexity:** O(rows × cols) time and space.

**Algorithm:**
1. Enqueue `(0, 0)`, mark visited.
2. Pop a cell; if it is the goal, walk parent pointers back to start,
   reverse the list, return.
3. Enqueue all unvisited walkable cardinal neighbours.
4. BFS guarantees the first path found is the shortest.

---

## API Endpoints

Base URL: `https://backend-qcf9.onrender.com/fm1`

| Endpoint | Method | Description |
|---|---|---|
| `/get-render-config` | GET | Returns rendering configuration JSON |
| `/get-maze-image?width=W&height=H` | GET | Returns a PNG maze image |

**`/get-render-config` example response:**
```json
{
  "wallCellColor":    "#222222",
  "pathColor":        "#00AA00",
  "drawGrid":         "true",
  "gridColor":        "#CCCCCC",
  "animationDelayMs": 80
}
```

**`/get-maze-image` parameters**

| Parameter | Range | Default fallback |
|---|---|---|
| `width` | 5–100 | 30 |
| `height` | 5–100 | 30 |

---

## Dependencies

```xml
<!-- Maven -->
<dependency>
  <groupId>com.squareup.okhttp3</groupId>
  <artifactId>okhttp</artifactId>
  <version>4.12.0</version>
</dependency>
<dependency>
  <groupId>org.json</groupId>
  <artifactId>json</artifactId>
  <version>20231013</version>
</dependency>
```

Requires Java 17 or higher.

---

## Key Design Decisions

**Never display the original PNG.** The image from the API is read into a
`BufferedImage` solely to sample pixel colors. It is immediately decoded into
a `boolean[][]` grid and discarded. The maze the user sees is drawn entirely
by `paintComponent` from scratch.

**All rendering values from the API.** Wall color, path color, grid color,
grid visibility, and animation delay are taken from `RenderConfig` at paint
time. The only hardcoded visual constant is `CELL = 20` px.

**Non-blocking UI.** Every HTTP call runs inside a `SwingWorker`. The EDT
handles only Swing operations; `doInBackground` handles all network I/O.

**Single active animation.** The `Check Solution` button is disabled the
moment animation starts and re-enabled only after the last cell is painted,
making simultaneous animations impossible.