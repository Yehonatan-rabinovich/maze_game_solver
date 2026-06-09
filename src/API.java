import okhttp3.*;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Blocking HTTP client.  Every public method must be called from a
 * background thread (SwingWorker.doInBackground).
 */
public class API {

    // ── Endpoints ─────────────────────────────────────────────────────────────
    private static final String BASE         = "https://backend-qcf9.onrender.com/fm1";
    private static final String SETTINGS_URL = BASE + "/get-render-config";
    private static final String MAZE_URL     = BASE + "/get-maze-image";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90,    TimeUnit.SECONDS)
            .build();

    // ── DTOs ──────────────────────────────────────────────────────────────────

    /** Rendering configuration returned by /get-render-config. */
    public static class RenderConfig {
        public final String  wallCellColor;
        public final String  pathColor;
        public final boolean drawGrid;
        public final String  gridColor;
        public final int     animationDelayMs;

        public RenderConfig(String wallCellColor, String pathColor,
                            boolean drawGrid, String gridColor, int animationDelayMs) {
            this.wallCellColor    = wallCellColor;
            this.pathColor        = pathColor;
            this.drawGrid         = drawGrid;
            this.gridColor        = gridColor;
            this.animationDelayMs = animationDelayMs;
        }
    }

    /**
     * Decoded maze: a boolean grid and its dimensions.
     * The original PNG image is NOT stored – it was only used during decoding.
     */
    public static class MazeResult {
        /** grid[row][col] – true = walkable passage, false = wall. */
        public final boolean[][] grid;
        public final int         rows;   // == height parameter
        public final int         cols;   // == width  parameter

        public MazeResult(boolean[][] grid, int rows, int cols) {
            this.grid = grid;
            this.rows = rows;
            this.cols = cols;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Fetches /get-render-config and returns a typed RenderConfig. */
    public static RenderConfig getSettings() throws IOException {
        String body = getString(SETTINGS_URL);
        JSONObject j = new JSONObject(body);

        String  wallCellColor    = j.get("wallCellColor").toString();
        String  pathColor        = j.get("pathColor").toString();
        boolean drawGrid         = Boolean.parseBoolean(j.get("drawGrid").toString());
        String  gridColor        = j.get("gridColor").toString();
        int animationDelayMs = Integer.parseInt(j.get("animationDelayMs").toString());

        return new RenderConfig(wallCellColor, pathColor, drawGrid, gridColor, animationDelayMs);
    }

    /**
     * Downloads the maze PNG, decodes every pixel into a walkability grid,
     * and returns the result.  The PNG itself is discarded afterward.
     *
     * @param width  number of columns (clamped to [5, 100])
     * @param height number of rows    (clamped to [5, 100])
     */
    public static MazeResult getMaze(int width, int height) throws IOException {
        width  = Math.max(5, Math.min(100, width));
        height = Math.max(5, Math.min(100, height));

        String url = MAZE_URL + "?width=" + width + "&height=" + height;

        BufferedImage img = getImage(url);
        if (img == null) throw new IOException("Could not decode maze PNG.");

        boolean[][] grid = parseGrid(img, width, height);
        return new MazeResult(grid, height, width);
    }

    // ── Grid decoding ─────────────────────────────────────────────────────────

    /**
     * Each logical cell occupies a rectangular block of pixels.
     * We sample the centre of each block: R,G,B > 200 → walkable, else wall.
     *
     * @param img  raw maze PNG
     * @param cols logical columns  (= width  parameter)
     * @param rows logical rows     (= height parameter)
     */
    private static boolean[][] parseGrid(BufferedImage img, int cols, int rows) {
        boolean[][] grid = new boolean[rows][cols];

        int cellW = Math.max(1, img.getWidth()  / cols);
        int cellH = Math.max(1, img.getHeight() / rows);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int px = Math.min(c * cellW + cellW / 2, img.getWidth()  - 1);
                int py = Math.min(r * cellH + cellH / 2, img.getHeight() - 1);
                Color col = new Color(img.getRGB(px, py));
                // White-ish pixel → open passage; anything darker → wall
                grid[r][c] = col.getRed() == 255 && col.getGreen() == 255 && col.getBlue() == 255;
            }
        }
        return grid;
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private static String getString(String url) throws IOException {
        try (Response r = execute(url)) {
            ok(r, url);
            return r.body().string();
        }
    }

    private static BufferedImage getImage(String url) throws IOException {
        try (Response r = execute(url)) {
            ok(r, url);
            try (InputStream is = r.body().byteStream()) {
                return ImageIO.read(is);
            }
        }
    }

    private static Response execute(String url) throws IOException {
        return CLIENT.newCall(new Request.Builder().url(url).build()).execute();
    }

    private static void ok(Response r, String url) throws IOException {
        if (!r.isSuccessful())
            throw new IOException("HTTP " + r.code() + " from " + url);
    }
}
