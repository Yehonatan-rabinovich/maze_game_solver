import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/**
 * First screen shown at startup.
 * Displays render configuration from the API, accepts maze dimensions,
 * and provides Refresh Config / GET MAZE buttons.
 *
 * Refresh Config: re-fetches settings only — never loads a maze.
 * GET MAZE:       validates dimensions, requests the maze, switches to MazePanel.
 */
public class ConfigPanel extends JPanel {

    private final GameWindow window;

    // ─── Config value labels ──────────────────────────────────────────────────
    private final JLabel wallColorVal = valLabel("—");
    private final JLabel pathColorVal = valLabel("—");
    private final JLabel drawGridVal  = valLabel("—");
    private final JLabel gridColorVal = valLabel("—");
    private final JLabel delayVal     = valLabel("—");

    // Color swatches shown next to hex values
    private final JPanel wallSwatch   = swatch();
    private final JPanel pathSwatch   = swatch();
    private final JPanel gridSwatch   = swatch();

    // ─── Dimension inputs ─────────────────────────────────────────────────────
    private final JTextField widthField  = inputField("30");
    private final JTextField heightField = inputField("30");

    // ─── Buttons ─────────────────────────────────────────────────────────────
    private final JButton refreshBtn = styledBtn("Refresh Config", new Color(30, 58, 138));
    private final JButton getMazeBtn = styledBtn("GET MAZE",       new Color(5, 90, 55));

    // ─── Status line ─────────────────────────────────────────────────────────
    private final JLabel statusLabel = new JLabel("Initializing…");

    private API.RenderConfig lastConfig;

    // ─── Constructor ─────────────────────────────────────────────────────────
    public ConfigPanel(GameWindow window) {
        this.window = window;
        setLayout(null);
        setBackground(new Color(8, 11, 20));
        setPreferredSize(new Dimension(1200, 820));
        buildUI();
    }

    // ─── Public: called by GameWindow on startup, and by Refresh button ───────
    public void fetchConfig() {
        setStatus("Fetching configuration…");
        setBtnsEnabled(false);

        new SwingWorker<API.RenderConfig, Void>() {
            @Override
            protected API.RenderConfig doInBackground() throws Exception {
                return API.getSettings();
            }
            @Override
            protected void done() {
                try {
                    lastConfig = get();
                    applyConfig(lastConfig);
                    setStatus("Configuration loaded.");
                } catch (InterruptedException | ExecutionException ex) {
                    setStatus("Failed to load config — click Refresh Config to retry.");
                    refreshBtn.setEnabled(true);   // allow retry; getMazeBtn stays off
                    return;
                }
                setBtnsEnabled(true);
            }
        }.execute();
    }

    // ─── Private: request and switch to maze ──────────────────────────────────
    private void fetchMaze() {
        // Validate — non-numeric or out-of-range [5, 100] → default 30
        int w = validated(widthField.getText());
        int h = validated(heightField.getText());
        widthField .setText(String.valueOf(w));
        heightField.setText(String.valueOf(h));

        if (lastConfig == null) {
            setStatus("Load configuration first.");
            return;
        }

        setBtnsEnabled(false);
        setStatus("Requesting " + w + " × " + h + " maze…");

        final API.RenderConfig cfg = lastConfig;
        new SwingWorker<API.MazeResult, Void>() {
            @Override
            protected API.MazeResult doInBackground() throws Exception {
                return API.getMaze(w, h);
            }
            @Override
            protected void done() {
                try {
                    window.showMaze(get(), cfg);
                    setStatus("Maze loaded.");
                } catch (InterruptedException | ExecutionException ex) {
                    setStatus("Failed to fetch maze — try again.");
                }
                setBtnsEnabled(true);
            }
        }.execute();
    }

    // ─── UI construction ─────────────────────────────────────────────────────
    private void buildUI() {
        final int LX = 300;   // label column x
        final int VX = 530;   // value column x
        final int SX = 750;   // swatch x

        // Title
        JLabel title = new JLabel("MAZE SOLVER");
        title.setFont(new Font("Monospaced", Font.BOLD, 30));
        title.setForeground(new Color(0, 212, 255));
        title.setBounds(LX, 28, 500, 50);
        add(title);

        addDivider(LX, 86, 620);

        // ── Render Config section ───────────────────────────────────────────
        addSection("RENDER CONFIGURATION", LX, 108);

        String[] names = {"Wall Cell Color", "Path Color", "Draw Grid", "Grid Color", "Animation Delay"};
        JLabel[] vals  = {wallColorVal, pathColorVal, drawGridVal, gridColorVal, delayVal};
        JPanel[] swts  = {wallSwatch, pathSwatch, null, gridSwatch, null};

        for (int i = 0; i < names.length; i++) {
            int y = 138 + i * 48;
            addRowLabel(names[i], LX, y);
            vals[i].setBounds(VX, y, 200, 28);
            add(vals[i]);
            if (swts[i] != null) {
                swts[i].setBounds(SX, y + 4, 22, 20);
                add(swts[i]);
            }
        }

        // ── Dimensions section ──────────────────────────────────────────────
        addDivider(LX, 384, 620);
        addSection("MAZE DIMENSIONS", LX, 400);

        addRowLabel("Width  (5–100)", LX, 428);
        widthField.setBounds(VX, 424, 150, 36);
        add(widthField);

        addRowLabel("Height (5–100)", LX, 478);
        heightField.setBounds(VX, 474, 150, 36);
        add(heightField);

        // ── Buttons ─────────────────────────────────────────────────────────
        refreshBtn.setBounds(LX,       540, 200, 44);
        getMazeBtn.setBounds(LX + 220, 540, 200, 44);
        add(refreshBtn);
        add(getMazeBtn);
        refreshBtn.addActionListener(e -> fetchConfig());
        getMazeBtn.addActionListener(e -> fetchMaze());

        // ── Status ──────────────────────────────────────────────────────────
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(107, 127, 163));
        statusLabel.setBounds(LX, 604, 700, 24);
        add(statusLabel);

        setBtnsEnabled(false);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private void applyConfig(API.RenderConfig cfg) {
        wallColorVal.setText(cfg.wallCellColor);
        pathColorVal.setText(cfg.pathColor);
        drawGridVal .setText(String.valueOf(cfg.drawGrid));
        gridColorVal.setText(cfg.gridColor);
        delayVal    .setText(cfg.animationDelayMs + " ms");
        setSwatch(wallSwatch, cfg.wallCellColor);
        setSwatch(pathSwatch, cfg.pathColor);
        setSwatch(gridSwatch, cfg.gridColor);
        repaint();
    }

    private static void setSwatch(JPanel p, String hex) {
        try { p.setBackground(Color.decode(hex)); }
        catch (Exception ignored) {}
    }

    private void setBtnsEnabled(boolean on) {
        refreshBtn.setEnabled(on);
        getMazeBtn.setEnabled(on);
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }

    /** Returns 30 for non-numeric or out-of-range [5, 100] input. */
    private static int validated(String s) {
        try {
            int v = Integer.parseInt(s.trim());
            return (v >= 5 && v <= 100) ? v : 30;
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    // ─── Layout helpers ───────────────────────────────────────────────────────
    private void addSection(String text, int x, int y) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.BOLD, 11));
        l.setForeground(new Color(107, 127, 163));
        l.setBounds(x, y, 400, 20);
        add(l);
    }

    private void addRowLabel(String text, int x, int y) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 14));
        l.setForeground(new Color(160, 180, 210));
        l.setBounds(x, y, 220, 28);
        add(l);
    }

    private void addDivider(int x, int y, int w) {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(30, 45, 61));
        sep.setBounds(x, y, w, 2);
        add(sep);
    }

    // ─── Component factories ──────────────────────────────────────────────────
    private static JLabel valLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Monospaced", Font.BOLD, 14));
        l.setForeground(new Color(0, 212, 255));
        return l;
    }

    private static JPanel swatch() {
        JPanel p = new JPanel();
        p.setBackground(new Color(30, 45, 61));
        p.setBorder(BorderFactory.createLineBorder(new Color(50, 70, 90)));
        return p;
    }

    private static JTextField inputField(String val) {
        JTextField f = new JTextField(val);
        f.setFont(new Font("Monospaced", Font.BOLD, 18));
        f.setForeground(new Color(0, 212, 255));
        f.setBackground(new Color(13, 17, 23));
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(30, 45, 61), 2),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        f.setHorizontalAlignment(JTextField.CENTER);
        return f;
    }

    private static JButton styledBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Monospaced", Font.BOLD, 14));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
