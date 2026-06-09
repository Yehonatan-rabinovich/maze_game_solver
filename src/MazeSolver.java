import java.util.*;

/**
 * Stateless BFS solver for a boolean maze grid.
 *
 * Finds the shortest path (minimum steps) from cell (0, 0) to cell
 * (rows-1, cols-1) using cardinal movement only (up / down / left / right).
 *
 * Returns null when:
 *   • the grid is null or empty
 *   • the start cell (0,0) is a wall
 *   • the end cell (rows-1, cols-1) is a wall
 *   • no walkable path connects start to end
 */
public class MazeSolver {

    private static final int[] DR = {-1,  1,  0,  0};  // row deltas
    private static final int[] DC = { 0,  0, -1,  1};  // col deltas

    /**
     * @param maze grid[row][col] — true = walkable passage, false = wall
     * @return ordered list of [row, col] pairs from start to end, or null
     */
    public static List<int[]> solve(boolean[][] maze) {
        if (maze == null || maze.length == 0 || maze[0] == null) return null;

        int rows   = maze.length;
        int cols   = maze[0].length;
        int endRow = rows - 1;
        int endCol = cols - 1;

        // Immediately reject if start or end is a wall
        if (!maze[0][0] || !maze[endRow][endCol]) return null;

        boolean[][] visited = new boolean[rows][cols];

        // parent[r][c] = cell we arrived from; {-1,-1} marks "no parent" (= start cell)
        int[][][] parent = new int[rows][cols][2];
        for (int[][] row : parent)
            for (int[] cell : row)
                Arrays.fill(cell, -1);

        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{0, 0});
        visited[0][0] = true;

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int r = cur[0], c = cur[1];

            // Reached the goal — reconstruct and return the path
            if (r == endRow && c == endCol)
                return buildPath(parent, endRow, endCol);

            for (int d = 0; d < 4; d++) {
                int nr = r + DR[d];
                int nc = c + DC[d];
                if (nr >= 0 && nr < rows
                        && nc >= 0 && nc < cols
                        && maze[nr][nc]
                        && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    parent[nr][nc]  = new int[]{r, c};
                    q.add(new int[]{nr, nc});
                }
            }
        }

        return null;  // no path exists
    }

    /**
     * Walks parent pointers from end back to start, then reverses the list
     * so it runs start → end.
     */
    private static List<int[]> buildPath(int[][][] parent, int endRow, int endCol) {
        List<int[]> path = new ArrayList<>();
        int[] cur = {endRow, endCol};

        // Loop until we reach the start cell whose parent is {-1, -1}
        while (cur[0] != -1 && cur[1] != -1) {
            path.add(cur);
            cur = parent[cur[0]][cur[1]];
        }

        Collections.reverse(path);
        return path;
    }
}
