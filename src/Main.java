import javax.swing.*;

void main() {
    SwingUtilities.invokeLater(() -> {
        GameWindow window = new GameWindow();
        window.setVisible(true);
    });
}
