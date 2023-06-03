package main;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {
    public GameFrame() {
        this.setVisible(true);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setTitle("Pong");
        this.add(new GamePanel());
        this.pack();
        this.setResizable(false);

        this.setLocationRelativeTo(null);
    }
}
