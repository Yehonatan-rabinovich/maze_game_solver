import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Second screen: draws the maze manually from a decoded boolean grid,
 * and animates the BFS solution path on request.
 * Drawing rules (all colors from API — nothing hardcoded except CELL_SIZE):
 *   • Passage cell  → Color.WHITE  (always)
 *   • Wall cell     → config.wallCellColor
 *   • Grid lines    → config.gridColor  (only when config.drawGrid == true)
 *   • Solution path → config.pathColor, animated with config.animationDelayMs delay
 */
public class MazePanel extends JPanel {

    /** Pixel size of one logical maze cell. May be hardcoded per spec. */
    private static final int CELL = 20;

    private final GameWindow window;

    // ─── State ────────────────────────────────────────────────────────────────
    private API.MazeResult   maze;
    private API.RenderConfig config;
    private List<int[]>      solution;     // null until BFS runs
    private boolean          solveRun;     // true once BFS has been attempted
    private int              pathTip = -1; // last cell index painted (-1 = nothing)
    private Timer            anim;

    // ─── UI ───────────────────────────────────────────────────────────────────
    private final DrawPanel drawPanel   = new DrawPanel();
    private final JButton   checkBtn;
    private final JLabel    statusLabel;

    // ─── Constructor ─────────────────────────────────────────────────────────
    public MazePanel(GameWindow window) {
        this.window = window;
        setLayout(new BorderLayout());
        setBackground(new Color(8, 11, 20));

        // Top bar
        JPanel top = bar();
        top.setLayout(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel title = new JLabel("MAZE SOLVER  ·  MAZE VIEW");
        title.setFont(new Font("Monospaced", Font.BOLD, 16));
        title.setForeground(new Color(0, 212, 255));
        top.add(title, BorderLayout.WEST);

        JButton backBtn = btn("← Back to Config", new Color(40, 60, 130));
        backBtn.addActionListener(_ -> goBack());
        top.add(backBtn, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Scroll pane wrapping the draw panel
        JScrollPane scroll = new JScrollPane(drawPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBackground(new Color(8, 11, 20));
        scroll.getViewport().setBackground(new Color(8, 11, 20));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        // Bottom bar
        JPanel bot = bar();
        bot.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 10));

        checkBtn = btn("Check Solution", new Color(5, 90, 55));
        checkBtn.setEnabled(false);
        checkBtn.addActionListener(_ -> checkSolution());
        bot.add(checkBtn);

        statusLabel = new JLabel("Load a maze to begin.");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(107, 127, 163));
        bot.add(statusLabel);

        add(bot, BorderLayout.SOUTH);
    }

    // ─── Called by GameWindow after a maze is fetched ─────────────────────────
    public void loadMaze(API.MazeResult result, API.RenderConfig cfg) {
        stopAnim();
        maze     = result;
        config   = cfg;
        solution = null;
        solveRun = false;
        pathTip  = -1;

        // Size the inner panel so the scroll pane knows the full extent
        drawPanel.setPreferredSize(
            new Dimension(result.cols * CELL, result.rows * CELL));
        drawPanel.revalidate();
        drawPanel.repaint();

        checkBtn.setEnabled(true);
        setStatus("Maze ready — " + result.cols + " × " + result.rows + " cells.");
    }

    // ─── Solution logic ───────────────────────────────────────────────────────
    private void checkSolution() {
        if (maze == null) return;
        stopAnim();

        // Solve once; cache the result so replays don't re-run BFS
        if (!solveRun) {
            solution = MazeSolver.solve(maze.grid);
            solveRun = true;
        }

        // Clear any previously drawn path and repaint the clean maze
        pathTip = -1;
        drawPanel.repaint();

        if (solution == null) {
            setStatus("No solution found.");
            return;
        }

        // Begin cell-by-cell animation
        setStatus("Animating solution…");
        checkBtn.setEnabled(false);   // prevent a second simultaneous animation
        int delay = Math.max(1, config.animationDelayMs);

        anim = new Timer(delay, _ -> {
            pathTip++;
            drawPanel.repaint();
            if (pathTip >= solution.size() - 1) {
                stopAnim();
                setStatus("Solution shown — click Check Solution to replay.");
                checkBtn.setEnabled(true);
            }
        });
        anim.start();
    }

    private void stopAnim() {
        if (anim != null) { anim.stop(); anim = null; }
    }

    private void goBack() {
        stopAnim();
        window.showConfig();
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }

    // ─── Inner panel that does all the custom Swing painting ─────────────────
    private class DrawPanel extends JPanel {

        DrawPanel() { setBackground(new Color(8, 11, 20)); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (maze == null) return;

            Graphics2D  g2   = (Graphics2D) g;
            boolean[][] grid = maze.grid;
            int rows         = maze.rows;
            int cols         = maze.cols;

            Color wallClr   = hex(config.wallCellColor, Color.DARK_GRAY);
            Color gridClr   = hex(config.gridColor,     new Color(80, 80, 80));

            // ── 1. Draw every cell ────────────────────────────────────────────
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    // Passages are always WHITE; walls use the API color.
                    g2.setColor(grid[r][c] ? Color.WHITE : wallClr);
                    g2.fillRect(c * CELL, r * CELL, CELL, CELL);
                }
            }

            // ── 2. Optional grid lines ────────────────────────────────────────
            if (config.drawGrid) {
                g2.setColor(gridClr);
                g2.setStroke(new BasicStroke(0.5f));
                for (int r = 0; r <= rows; r++)
                    g2.drawLine(0, r * CELL, cols * CELL, r * CELL);
                for (int c = 0; c <= cols; c++)
                    g2.drawLine(c * CELL, 0, c * CELL, rows * CELL);
            }

            // ── 3. Animated solution path (grows one cell per timer tick) ─────
            if (solution != null && pathTip >= 0) {
                Color pathClr = hex(config.pathColor, new Color(0, 200, 100));
                g2.setColor(pathClr);
                int limit = Math.min(pathTip, solution.size() - 1);
                for (int i = 0; i <= limit; i++) {
                    int[] p = solution.get(i);
                    // Paint a slightly inset square so the path is visible over
                    // the white passage cell background
                    g2.fillRect(p[1] * CELL + 2, p[0] * CELL + 2,
                                CELL - 4,         CELL - 4);
                }
            }
        }

        /** Decode a hex color string; return fallback on any error. */
        private Color hex(String s, Color fallback) {
            try { return Color.decode(s); }
            catch (Exception e) { return fallback; }
        }
    }

    // ─── Static helpers ───────────────────────────────────────────────────────
    private static JPanel bar() {
        JPanel p = new JPanel();
        p.setBackground(new Color(13, 17, 23));
        return p;
    }

    private static JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Monospaced", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
