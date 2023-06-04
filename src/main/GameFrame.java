package main;

import javax.swing.*;

public class GameFrame extends JFrame {
    public GameFrame() {

        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setTitle("Pong");
        this.add(new GamePanel());
        this.pack();
        this.setResizable(false);


        this.setLocationRelativeTo(null);

        this.setVisible(true);
    }
}
