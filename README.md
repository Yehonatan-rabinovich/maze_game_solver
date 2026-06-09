<div align="center">

```
███╗   ███╗ █████╗ ███████╗███████╗    ███████╗ ██████╗ ██╗    ██╗   ██╗███████╗██████╗
████╗ ████║██╔══██╗╚══███╔╝██╔════╝    ██╔════╝██╔═══██╗██║    ██║   ██║██╔════╝██╔══██╗
██╔████╔██║███████║  ███╔╝ █████╗      ███████╗██║   ██║██║    ██║   ██║█████╗  ██████╔╝
██║╚██╔╝██║██╔══██║ ███╔╝  ██╔══╝      ╚════██║██║   ██║██║    ╚██╗ ██╔╝██╔══╝  ██╔══██╗
██║ ╚═╝ ██║██║  ██║███████╗███████╗    ███████║╚██████╔╝███████╗╚████╔╝ ███████╗██║  ██║
╚═╝     ╚═╝╚═╝  ╚═╝╚══════╝╚══════╝    ╚══════╝ ╚═════╝ ╚══════╝ ╚═══╝  ╚══════╝╚═╝  ╚═╝
```

**A Java Swing desktop application that fetches mazes from a live REST API,
decodes them pixel-by-pixel, renders them entirely with custom Swing painting,
and animates a BFS shortest-path solution in real time.**

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/UI-Java%20Swing-5C6BC0?style=flat-square)
![OkHttp](https://img.shields.io/badge/HTTP-OkHttp%204.12-43A047?style=flat-square)
![Algorithm](https://img.shields.io/badge/Algorithm-BFS-0288D1?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [How It Works](#how-it-works)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Usage Guide](#usage-guide)
- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Code Reference](#code-reference)
- [Design Decisions](#design-decisions)
- [Dependencies](#dependencies)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Maze Solver is a full-stack desktop application connecting a live backend to a
hand-crafted Swing front end. The app never renders the raw PNG returned by the
server — it reads that image only as a data source, decodes every pixel into a
boolean walkability grid, and then draws the entire maze from scratch using
`paintComponent`. All visual parameters (colors, grid visibility, animation
speed) are loaded dynamically from the API at startup, making the renderer
fully server-driven.

---

## Features

- **Server-driven rendering** — wall color, path color, grid color, grid
  visibility, and animation delay all come from the API. No visual constant is
  hardcoded except cell size.
- **Pixel-accurate maze decoding** — samples the centre pixel of each logical
  cell block; R, G, B all > 200 → walkable passage, otherwise → wall.
- **Custom Swing renderer** — the original PNG is never displayed. Every cell
  is drawn manually with `Graphics2D.fillRect` inside `paintComponent`.
- **BFS shortest-path solver** — guaranteed minimum-step solution using
  breadth-first search with O(rows × cols) time and space complexity.
- **Cell-by-cell animation** — solution path grows one cell at a time, paced
  by the server-supplied `animationDelayMs` delay.
- **Non-blocking UI** — every HTTP call runs inside a `SwingWorker`;
  the Event Dispatch Thread handles only painting and user events.
- **Scroll support** — mazes up to 100 × 100 (2000 × 2000 px) are wrapped in
  a `JScrollPane` for full exploration.
- **Replay** — clicking Check Solution again clears the previous path and
  re-animates from the beginning. BFS result is cached; the solve runs only
  once per maze load.
- **Input validation** — width/height values that are non-numeric or outside
  [5, 100] are silently replaced with 30 and written back to the input field.

---

## How It Works

```
┌─────────────────────────────────────────────────────────────────────┐
│                         APPLICATION FLOW                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. STARTUP          App opens → GET /get-render-config             │
│                      Five rendering values stored in RenderConfig   │
│                                                                     │
│  2. USER INPUT       User types width + height (5–100, default 30)  │
│                      Clicks GET MAZE                                │
│                                                                     │
│  3. IMAGE FETCH      GET /get-maze-image?width=W&height=H           │
│                      Server returns a PNG                           │
│                                                                     │
│  4. PIXEL DECODE     BufferedImage read; centre pixel sampled        │
│                      per cell block                                 │
│                      RGB > 200 each → true (walkable)               │
│                      Otherwise     → false (wall)                   │
│                      PNG discarded; only boolean[][] grid kept      │
│                                                                     │
│  5. CUSTOM RENDER    paintComponent draws every cell from scratch   │
│                      White   → walkable passage (always)            │
│                      wallCellColor → wall                           │
│                      gridColor    → optional grid lines             │
│                                                                     │
│  6. BFS SOLVE        User clicks Check Solution                     │
│                      BFS from (0,0) to (rows-1, cols-1)             │
│                      No solution → status message                   │
│                                                                     │
│  7. ANIMATION        Timer fires every animationDelayMs ms          │
│                      pathTip++ → repaint → one more cell shown      │
│                      pathColor used for path squares                │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
maze-solver/
├── Main.java            # Entry point — bootstraps GameWindow on the EDT
├── GameWindow.java      # Root JFrame with CardLayout (CONFIG ↔ MAZE)
├── ConfigPanel.java     # Screen 1: config display, dimension inputs, buttons
├── MazePanel.java       # Screen 2: custom maze renderer + animation
├── MazeSolver.java      # Stateless BFS pathfinder
├── API.java             # Blocking HTTP client (OkHttp) + DTOs
└── README.md            # This file
```

### Dependency tree

```
Main
 └── GameWindow  (JFrame, CardLayout)
       ├── ConfigPanel              ← Screen 1
       │     └── API.getSettings()
       │     └── API.getMaze()
       └── MazePanel                ← Screen 2
             └── MazeSolver.solve()
```

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 17 or higher |
| Maven | 3.6+ (or Gradle equivalent) |
| Internet access | Required for API calls |

---

## Installation

### 1. Clone the repository

```bash
git clone https://github.com/your-org/maze-solver.git
cd maze-solver
```

### 2. Add dependencies to `pom.xml`

```xml
<dependencies>

  <!-- HTTP client -->
  <dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
  </dependency>

  <!-- JSON parsing -->
  <dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20231013</version>
  </dependency>

</dependencies>
```

### 3. Build

```bash
mvn clean package
```

---

## Quick Start

```bash
# Build and run in one step
mvn clean package && java -jar target/maze-solver.jar

# Or compile manually (all .java files in one directory)
javac -cp okhttp-4.12.0.jar:json-20231013.jar *.java
java  -cp .:okhttp-4.12.0.jar:json-20231013.jar Main
```

The app opens immediately on the configuration screen and fetches render
settings from the server in the background.

---

## Usage Guide

### Screen 1 — Configuration

```
┌────────────────────────────────────────────────────────┐
│  MAZE SOLVER                                           │
│  ──────────────────────────────────────────────────── │
│  RENDER CONFIGURATION                                  │
│  Wall Cell Color   #222222  ■                          │
│  Path Color        #00AA00  ■                          │
│  Draw Grid         true                                │
│  Grid Color        #CCCCCC  ■                          │
│  Animation Delay   80 ms                               │
│  ──────────────────────────────────────────────────── │
│  MAZE DIMENSIONS                                       │
│  Width  (5–100)    [  30  ]                            │
│  Height (5–100)    [  30  ]                            │
│                                                        │
│  [ Refresh Config ]   [ GET MAZE ]                     │
│                                                        │
│  ● Configuration loaded.                               │
└────────────────────────────────────────────────────────┘
```

| Action | Result |
|---|---|
| **App starts** | Config fetched automatically; values populate |
| **Refresh Config** | Re-fetches render settings only — no maze loaded |
| **Edit Width / Height** | Any value outside [5, 100] or non-numeric resets to 30 |
| **GET MAZE** | Validates inputs → fetches maze image → switches to Screen 2 |

### Screen 2 — Maze View

```
┌────────────────────────────────────────────────────────┐
│  MAZE SOLVER  ·  MAZE VIEW          [ ← Back to Config]│
├────────────────────────────────────────────────────────┤
│                                                        │
│   ░░░░░░░░░  ░░░░░░░░░░░░░░                           │
│   ░         ░             ░                           │
│   ░  ░░░░░  ░  ░░░░░░░░   ░                           │
│   ░      ░  ░          ░  ░                           │
│   ░░░░░  ░░░░░░░░░░░   ░  ░                           │
│         (scroll for large mazes)                       │
│                                                        │
├────────────────────────────────────────────────────────┤
│  [ Check Solution ]   Maze ready — 30 × 30 cells.     │
└────────────────────────────────────────────────────────┘
```

| Action | Result |
|---|---|
| **Check Solution** | BFS runs; if no path → message shown |
| **Check Solution** (with path) | Path animates cell by cell using `pathColor` |
| **Check Solution** (replay) | Previous path clears; animation restarts |
| **← Back to Config** | Returns to Screen 1; maze state preserved |

---

## Architecture

### Threading model

```
Event Dispatch Thread (EDT)
  │
  ├── paintComponent()          ← all drawing happens here
  ├── ActionListeners           ← button clicks
  └── SwingWorker callbacks     ← done(), publish()

Background Thread (SwingWorker.doInBackground)
  ├── API.getSettings()         ← HTTP GET /get-render-config
  └── API.getMaze()             ← HTTP GET /get-maze-image + pixel decode
```

The EDT never touches the network. All I/O runs in `doInBackground`; results
are handed back via `done()` on the EDT.

### Card switching

`GameWindow` owns a single `JPanel` with a `CardLayout` containing exactly
two cards — `"CONFIG"` and `"MAZE"`. Switching is a single call:

```java
cardLayout.show(cardPanel, "MAZE");   // ConfigPanel → MazePanel
cardLayout.show(cardPanel, "CONFIG"); // MazePanel   → ConfigPanel
```

Neither panel is re-created on switch; state is preserved across navigation.

### Animation loop

```
checkSolution() called
  │
  ├── stopAnim()               stop any running Timer
  ├── pathTip = -1             clear previous visual
  ├── drawPanel.repaint()      paint clean maze
  ├── MazeSolver.solve()       BFS (once; result cached)
  │
  └── new Timer(animationDelayMs, …).start()
        │
        └── each tick:
              pathTip++
              drawPanel.repaint()   → paintComponent draws cells 0..pathTip
              if pathTip == path.size()-1  → stop Timer, re-enable button
```

---

## API Reference

**Base URL:** `https://backend-qcf9.onrender.com/fm1`

### `GET /get-render-config`

Returns the rendering configuration that drives all visual output.

**Response**

```json
{
  "wallCellColor":    "#222222",
  "pathColor":        "#00AA00",
  "drawGrid":         "true",
  "gridColor":        "#CCCCCC",
  "animationDelayMs": 80
}
```

| Field | Type | Description |
|---|---|---|
| `wallCellColor` | `String` (hex) | Color used to fill wall cells |
| `pathColor` | `String` (hex) | Color used to fill solution path cells |
| `drawGrid` | `boolean` | Whether to draw cell boundary lines |
| `gridColor` | `String` (hex) | Color of grid lines (when enabled) |
| `animationDelayMs` | `int` | Milliseconds between each animated path cell |

---

### `GET /get-maze-image`

Returns a PNG image where each pixel represents one maze cell.

**Query parameters**

| Parameter | Type | Range | Fallback |
|---|---|---|---|
| `width` | `int` | 5 – 100 | 30 |
| `height` | `int` | 5 – 100 | 30 |

**Example request**

```
GET /get-maze-image?width=40&height=30
```

**Pixel decoding contract**

| Pixel value | Meaning |
|---|---|
| R > 200 AND G > 200 AND B > 200 | Walkable passage (`true`) |
| Anything else | Wall (`false`) |

> **Note:** The wall color inside the PNG is determined by the server and
> may differ from `wallCellColor`. The image is used only for structure
> detection — it is never displayed to the user.

---

## Configuration

All rendering parameters are loaded at runtime from `/get-render-config`.
The table below shows where each value is used in the renderer.

| Config field | Used in | Effect |
|---|---|---|
| `wallCellColor` | `DrawPanel.paintComponent` | Fill color for `grid[r][c] == false` cells |
| `pathColor` | `DrawPanel.paintComponent` | Fill color for animated solution squares |
| `drawGrid` | `DrawPanel.paintComponent` | Gates the grid-line drawing block |
| `gridColor` | `DrawPanel.paintComponent` | Stroke color for grid lines |
| `animationDelayMs` | `MazePanel.checkSolution` | `Timer` interval in milliseconds |

The only hardcoded visual constant is:

```java
private static final int CELL = 20; // px per logical cell
```

---

## Code Reference

### `MazeSolver`

```java
// Returns the shortest path, or null if none exists.
List<int[]> path = MazeSolver.solve(boolean[][] maze);

// Each element is [row, col].
// path.get(0)               == {0, 0}        (start)
// path.get(path.size() - 1) == {rows-1, cols-1} (end)
```

**Returns `null` when:**
- `maze` is `null` or empty
- Start cell `(0, 0)` is a wall
- End cell `(rows-1, cols-1)` is a wall
- No walkable path connects start to end

**Complexity:** O(rows × cols) time and space.

---

### `API.RenderConfig`

```java
public static class RenderConfig {
    public final String  wallCellColor;    // hex, e.g. "#222222"
    public final String  pathColor;        // hex, e.g. "#00AA00"
    public final boolean drawGrid;
    public final String  gridColor;        // hex, e.g. "#CCCCCC"
    public final int     animationDelayMs;
}
```

---

### `API.MazeResult`

```java
public static class MazeResult {
    public final boolean[][] grid; // grid[row][col] — true = walkable
    public final int         rows; // == height parameter
    public final int         cols; // == width  parameter
}
```

---

### `GameWindow` public surface

```java
// Switch to MazePanel and load the given maze + config.
window.showMaze(API.MazeResult result, API.RenderConfig config);

// Switch back to ConfigPanel (no network call).
window.showConfig();
```

---

## Design Decisions

### Never display the original PNG

The image returned by `/get-maze-image` is read into a `BufferedImage` solely
to sample pixel values. Once `parseGrid` has built the `boolean[][]`, the
`BufferedImage` goes out of scope and is garbage-collected. The maze the user
sees is drawn entirely from scratch inside `paintComponent` — every cell,
every grid line, every solution square.

### Server-driven visuals

Hardcoding colors creates brittleness: a server-side theme change would
require a client rebuild. Pulling every visual parameter from `RenderConfig`
means the look can change server-side with no client deployment. The single
exception — `CELL = 20` px — is explicitly permitted by the specification.

### BFS over DFS

BFS guarantees the shortest path in an unweighted grid. DFS would find *a*
path, but not necessarily the shortest one, leading to a longer, less elegant
animation. BFS also has a predictable O(rows × cols) worst-case, which is
acceptable for grids up to 100 × 100.

### SwingWorker for all I/O

Blocking the Event Dispatch Thread produces a frozen UI and risks a
`java.lang.Error` on some platforms. Every HTTP call is wrapped in a
`SwingWorker`; `doInBackground` carries the network work, and `done` delivers
the result back to the EDT for safe Swing updates.

### Single-animation invariant

The Check Solution button is disabled the instant animation starts and
re-enabled only after `pathTip` reaches the final cell. This makes it
structurally impossible to start two overlapping animations. No flag,
lock, or guard is needed.

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| [OkHttp](https://square.github.io/okhttp/) | 4.12.0 | HTTP client for API calls |
| [org.json](https://github.com/stleary/JSON-java) | 20231013 | JSON parsing for config response |

Both are available on Maven Central. No other third-party libraries are used;
all UI, threading, and image-handling code relies on the Java standard library
and the bundled Swing/AWT toolkit.

---

## Contributing

1. Fork the repository and create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Make your changes. Keep each commit focused and the message descriptive.
3. Ensure the app builds cleanly with no warnings:
   ```bash
   mvn clean package -Werror
   ```
4. Open a pull request against `main` with a clear description of what
   changed and why.

**Areas open for contribution:**
- Diagonal movement option in `MazeSolver`
- A* heuristic as an alternative solver
- Zoom in / zoom out controls for the maze view
- Export solution path as PNG or SVG

---

## License

```
MIT License

Copyright (c) 2024 Maze Solver Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

---

<div align="center">

Built with Java · Swing · OkHttp · BFS

</div>