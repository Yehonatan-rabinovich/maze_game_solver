# Maze Game

A Java Swing desktop application that fetches mazes from a live REST API,
renders them in a graphical window, and solves them in one click using
Breadth-First Search.

---

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Architecture](#architecture)
4. [State Machine](#state-machine)
5. [Project Structure](#project-structure)
6. [Getting Started](#getting-started)
7. [API Reference](#api-reference)
8. [Component Reference](#component-reference)
9. [Configuration](#configuration)
10. [Troubleshooting](#troubleshooting)

---

## Overview

Maze Game connects to a backend server to pull render configuration and maze
images. The user sets the maze dimensions (5–100 cells wide / tall), generates
a new maze, and can instantly find the shortest path from the top-left corner
to the bottom-right corner.

All network calls run on a background thread so the UI never freezes. A
five-state state machine controls exactly which buttons appear and what each
one does at every point in the flow.

---

## Features

| Feature | Details |
|---|---|
| Live settings | Fetches wall color, path color, grid toggle, and animation delay from the server on startup |
| Custom dimensions | User-supplied width and height, each clamped to the range [5, 100] |
| Non-blocking UI | Every HTTP call runs inside a `SwingWorker`; the Event Dispatch Thread is never blocked |
| Loading states | Dedicated "loading" states disable buttons and show a status message while the network is busy |
| BFS solver | Guarantees the shortest possible path; draws it as a red overlay with green/red start/end markers |
| Automatic scaling | The maze image scales down to fit the window for any dimension up to 100×100 |

---

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Main.java                           │
│  Wraps startup in SwingUtilities.invokeLater, creates       │
│  JFrame + GamePanel, calls gamePanel.startGame()            │
└────────────────────────────┬────────────────────────────────┘
                             │ owns
┌────────────────────────────▼────────────────────────────────┐
│                       GamePanel.java                        │
│                                                             │
│  • Drives the state machine (5 states)                      │
│  • Manages all child components (buttons, settings panel)   │
│  • Dispatches SwingWorker background tasks                  │
│  • Paints maze image + solution path overlay                │
└──────────────┬──────────────────────────┬───────────────────┘
               │ calls (background thread) │ calls (EDT)
┌──────────────▼──────────┐  ┌────────────▼───────────────────┐
│       API.java          │  │        MazeSolver.java          │
│                         │  │                                 │
│ OkHttp + org.json       │  │ BFS on Boolean[][]              │
│ getSettings()           │  │ Returns List<int[]> path        │
│ getMaze(w, h)           │  │ or null if unsolvable           │
│  └─ returns MazeResult  │  │                                 │
│     .image (PNG)        │  └─────────────────────────────────┘
│     .grid  (Boolean[][])│
└─────────────────────────┘
```

### Thread Model

```
Event Dispatch Thread (EDT)
  │
  ├─ All Swing component reads/writes
  ├─ paintComponent()
  ├─ Button ActionListeners
  └─ SwingWorker.done()  ◄── safe to touch Swing here

Background Thread (SwingWorker.doInBackground)
  │
  ├─ API.getSettings()   — HTTP GET + JSON parse
  └─ API.getMaze()       — HTTP GET + PNG decode + grid parse
```

### Technology Stack

| Dependency | Version | Purpose |
|---|---|---|
| Java Swing | JDK 17+ | GUI framework |
| OkHttp | 4.x | HTTP client |
| org.json | 20231013+ | JSON parsing |

---

## State Machine

The application has five states. Only the transitions shown below are valid.
Any invalid button click is silently ignored.

```
  app launch
      │
      ▼
┌─────────────────────┐
│  LOADING_SETTINGS   │  Buttons: disabled ("...")
└──────────┬──────────┘
           │  API returns settings
           ▼
┌─────────────────────┐
│      SETTINGS       │  Left: "Refresh"  │  Right: "Generate Maze"
└──────────┬──────────┘
    │       │
    │       │ right button clicked
    │       ▼
    │  ┌─────────────────────┐
    │  │    LOADING_MAZE     │  Buttons: disabled ("...")
    │  └──────────┬──────────┘
    │             │  API returns maze
    │             ▼
    │  ┌─────────────────────┐
    │  │     MAZE_READY      │  Left: "Solve"  │  Right: "New Maze"
    │  └──────────┬──────────┘
    │             │  left button clicked
    │             ▼
    │  ┌─────────────────────┐
    │  │     MAZE_SOLVED     │  Left: "Back"   │  Right: "New Maze"
    │  └──────────┬──────────┘
    │             │
    │ left ("Back") or right ("New Maze") resets the flow
    └─────────────┴──────────────► back to SETTINGS / LOADING_MAZE
```

### Button Matrix

| State | Left button | Right button |
|---|---|---|
| `LOADING_SETTINGS` | `...` — disabled | `...` — disabled |
| `SETTINGS` | `Refresh` | `Generate Maze` |
| `LOADING_MAZE` | `...` — disabled | `...` — disabled |
| `MAZE_READY` | `Solve` | `New Maze` |
| `MAZE_SOLVED` | `Back` | `New Maze` |

---

## Project Structure

```
maze-game/
├── Main.java          Entry point — creates JFrame, starts the game
├── GamePanel.java     Central controller — state machine, layout, painting
├── API.java           HTTP client — settings fetch and maze fetch
├── MazeSolver.java    BFS shortest-path algorithm
├── Button.java        Styled JButton (500 × 100 px)
├── Label.java         Styled JLabel  (400 × 80 px)
├── TextField.java     Styled JTextField for width / height input
└── README.md          This file
```

> **Note:** All source files are in the default (unnamed) package.
> If you move them into a named package (e.g., `com.example.mazegame`),
> add the matching `package` declaration to every file.

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven **or** Gradle with the following runtime dependencies:

**Maven** — add to `pom.xml`:

```xml
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

**Gradle** — add to `build.gradle`:

```groovy
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'org.json:json:20231013'
```

### Running

Run `Main.main()` from your IDE, or from the command line:

```bash
# Maven
mvn compile exec:java -Dexec.mainClass="Main"

# Gradle
gradle run
```

The 1400 × 800 window opens, immediately begins fetching settings,
and shows a loading message until the server responds.

> **First launch may take 30–60 seconds** if the backend is on Render's free
> tier and has gone to sleep. Click **Refresh** if the first attempt times out.

---

## API Reference

**Base URL:** `https://backend-qcf9.onrender.com/fm1`

---

### `GET /get-render-config`

Returns the current render configuration as a JSON object.

**Example response:**

```json
{
  "wallCellColor":    "#000000",
  "pathColor":        "#ffffff",
  "drawGrid":         "false",
  "gridColor":        "#888888",
  "animationDelayMs": "50"
}
```

**Mapped to `String[]` returned by `API.getSettings()`:**

| Index | JSON key | Description |
|---|---|---|
| `[0]` | `wallCellColor` | Wall cell render colour |
| `[1]` | `pathColor` | Open path colour |
| `[2]` | `drawGrid` | Whether to draw grid lines (`"true"` / `"false"`) |
| `[3]` | `gridColor` | Grid line colour |
| `[4]` | `animationDelayMs` | Animation step delay in milliseconds |

---

### `GET /get-maze-image?width=W&height=H`

Returns a PNG image of a randomly generated maze.

| Parameter | Type | Range | Description |
|---|---|---|---|
| `width` | `int` | 5–100 | Number of cell columns |
| `height` | `int` | 5–100 | Number of cell rows |

**Image encoding:**

Each logical cell is rendered as a square pixel block (typically **16 × 16 px**).

| Pixel colour | Meaning |
|---|---|
| White (R, G, B > 200) | Walkable path |
| Dark | Wall |

`API.getMaze()` samples the **centre pixel** of each block to build the
`Boolean[][]` grid, which is then passed to `MazeSolver.solve()`.

---

## Component Reference

### `GamePanel`

The application's single `JPanel`. All UI coordination lives here.

| Method | Visibility | Description |
|---|---|---|
| `startGame()` | public | Called once after the JFrame is shown. Triggers the first settings fetch. |
| `transitionTo(AppState)` | private | Moves to a new state, updates button labels/enabled state, shows/hides the settings panel, calls `repaint()`. |
| `fetchSettings()` | private | Spawns a `SwingWorker` that calls `API.getSettings()` and populates value labels on completion. |
| `generateMaze()` | private | Reads width/height fields, spawns a `SwingWorker` that calls `API.getMaze()`. |
| `solveMaze()` | private | Calls `MazeSolver.solve(mazeGrid)` synchronously on the EDT (fast), then transitions to `MAZE_SOLVED`. |
| `paintComponent(Graphics)` | protected | Dispatches to `drawCenteredText`, `drawMaze`, or `drawSolution` based on current state. |

**Layout constants:**

| Constant | Value | Description |
|---|---|---|
| `SCREEN_WIDTH` | `1400` | Window width |
| `SCREEN_HEIGHT` | `800` | Window height |
| `BUTTON_Y` | `SCREEN_HEIGHT - 150` | Vertical position of both buttons |
| `SETTINGS_X` | `SCREEN_WIDTH / 2 - 400` | Left edge of the settings panel |

---

### `API`

Static HTTP client. All public methods are blocking; call them only from a
background thread.

| Method | Returns | Description |
|---|---|---|
| `getSettings()` | `String[5]` | Fetches and parses render config |
| `getMaze(int w, int h)` | `MazeResult` | Downloads the PNG and parses the walkability grid |

**`API.MazeResult` fields:**

| Field | Type | Description |
|---|---|---|
| `image` | `BufferedImage` | The raw PNG from the server |
| `grid` | `Boolean[][]` | `grid[row][col]` — `true` = walkable |

**Bug fix vs original code:**

The original `getMaze` iterated `for (row < width)` and `for (col < height)`,
which swapped the axes on non-square mazes.
The corrected loop is `for (row < rows)` / `for (col < cols)`,
and cell sampling uses the **centre** of each pixel block:

```java
int sampleX = col * cellW + cellW / 2;
int sampleY = row * cellH + cellH / 2;
```

---

### `MazeSolver`

Stateless BFS solver. No instance needed.

```java
List<int[]> path = MazeSolver.solve(mazeGrid);
// path is an ordered list of [row, col] pairs from (0,0) to (rows-1, cols-1)
// path is null if no solution exists
```

**Complexity:** O(rows × cols) time and space — always fast even for 100×100.

**How it works:**

1. Enqueue `(0, 0)`, mark it visited.
2. Pop a cell; if it is the goal, reconstruct the path and return.
3. Enqueue all unvisited walkable neighbours.
4. BFS guarantees the first path found is the shortest.

---

### `Button`

Styled `JButton` (500 × 100 px, green background).

```java
Button b = new Button(x, y);
b.updateText("New label");          // change label at any time
b.addActionListener(e -> { ... });  // wire click handler
b.setEnabled(false);                // disable during loading
```

---

### `Label`

Styled `JLabel` (400 × 80 px, white text). Used for both setting names
(left column) and setting values (right column).

---

### `TextField`

Styled `JTextField` (400 × 80 px, dark background, green text).
Used exclusively for the width and height inputs. Default value: `"30"`.

---

## Configuration

Modify these constants to customise the look and feel without changing logic:

| Constant | File | Default | What it controls |
|---|---|---|---|
| `SCREEN_WIDTH` | `GamePanel` | `1400` | Window width in pixels |
| `SCREEN_HEIGHT` | `GamePanel` | `800` | Window height in pixels |
| `BUTTON_Y` | `GamePanel` | `SCREEN_HEIGHT - 150` | How high the buttons sit |
| `Button.WIDTH` | `Button` | `500` | Button width |
| `Button.HEIGHT` | `Button` | `100` | Button height |
| `Label.WIDTH` | `Label` | `400` | Label / settings column width |
| `Label.HEIGHT` | `Label` | `80` | Row height for settings |
| `TextField.WIDTH` | `TextField` | `400` | Width of dimension input fields |
| `BASE_URL` | `API` | `https://backend-qcf9.onrender.com/fm1` | Backend base URL |

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| "Failed to load settings" on startup | Render free-tier backend is cold-starting (can take up to 60 s) | Wait, then click **Refresh** |
| Maze appears entirely black or white | `parseGrid` sample coordinates are off (e.g., cell size changed) | Log `image.getWidth() / cols` and verify it matches the actual cell size |
| `MazeSolver.solve()` returns `null` | No path exists in the generated maze | Click **New Maze** to regenerate |
| Width / height input is ignored | Non-numeric text in the field | Clear the field and type a number between 5 and 100 |
| Text in buttons is clipped | Font size too large for button dimensions | Decrease font size in `Button.java` (currently 40 pt) |
| Settings panel overlaps the maze | `SETTINGS_X` / `SETTINGS_W` need adjusting after layout changes | Recalculate using `SCREEN_WIDTH / 2 ± Label.WIDTH` |
