import javax.swing.*;
import java.awt.*;

/**
 * Root JFrame. Owns a CardLayout that switches between the configuration
 * panel and the maze panel.
 */
public class GameWindow extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     cardPanel  = new JPanel(cardLayout);

    private final ConfigPanel configPanel;
    private final MazePanel   mazePanel;

    public GameWindow() {
        super("Maze Solver");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        configPanel = new ConfigPanel(this);
        mazePanel   = new MazePanel(this);

        cardPanel.add(configPanel, "CONFIG");
        cardPanel.add(mazePanel,   "MAZE");

        add(cardPanel);
        setPreferredSize(new Dimension(1200, 820));
        pack();
        setLocationRelativeTo(null);

        // Start on config and kick off the initial settings fetch immediately
        cardLayout.show(cardPanel, "CONFIG");
        configPanel.fetchConfig();
    }

    /** Called by ConfigPanel's SwingWorker when a maze has been decoded. */
    public void showMaze(API.MazeResult result, API.RenderConfig config) {
        mazePanel.loadMaze(result, config);
        cardLayout.show(cardPanel, "MAZE");
    }

    /** Called by MazePanel's Back button. */
    public void showConfig() {
        cardLayout.show(cardPanel, "CONFIG");
    }
}
