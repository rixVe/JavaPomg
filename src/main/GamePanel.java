package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

import static java.lang.Math.*;

public class GamePanel extends JPanel implements KeyListener, Runnable {

    //game screen dimensions
    private final int GAME_WIDTH = 210;
    private final int GAME_HEIGHT = 160;
    //boarders
    private final int BOARDER_X_PX = 15;
    private final int BOARDER_Y_PX = 15;

    //determine how much a pixel in code is a pixel on screen
    private final int PIXEL_MULTIPLIER = 5;

    //position and velocity of player's pad
    private int playerPadY = GAME_HEIGHT / 2;
    private final int playerPadVelocity = 2;
    //position and velocity of AI's pad
    private int aiPadY = GAME_HEIGHT / 2;
    private double aiPadVelocity = 1;
    //the AI predicts where the ball will hit next
    private double predictedBallY = (double) GAME_HEIGHT / 2;
    private double aiPadDirection = (double) GAME_HEIGHT / 2;
    //pad dimensions
    private final int PAD_HEIGHT = 20;
    private final int PAD_WIDTH = 2;
    private final int PAD_OFFSET = 0;

    //velocity and angle (counterclockwise from y=0) of the ball
    private double ballVelocity = 2;
    private final double ballMaxVelocity = 10;
    private double ballAngle = PI;
    //position of ball
    private double ballX = (double) GAME_WIDTH / 2;
    private double ballY = (double) GAME_HEIGHT / 2;
    //ball dimension
    private final int BALL_DIAMETER = 2;

    //flags used to solve problems with key listening
    private boolean keyUpPressed = false;
    private boolean keyDownPressed = false;

    //size of the net in the middle of the screen
    private final int NET_SIZE_PX = 4;

    //scores
    private int playerScore = 0;
    private int aiScore = 0;

    private enum Input {
        UP,
        DOWN,
        NEUTRAL,
    }

    Input input = Input.NEUTRAL;

    public GamePanel() {
        this.setPreferredSize(new Dimension(PIXEL_MULTIPLIER * GAME_WIDTH + 2 * BOARDER_X_PX, PIXEL_MULTIPLIER * GAME_HEIGHT + 2 * BOARDER_Y_PX));
        this.setBackground(new Color(0, 0, 0));

        addKeyListener(this);
        setFocusable(true); //solves problems with listening to keys

        //game loop starting
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks; // nanoseconds per tick
        double delta = 0;
        newGame();
        while (true) { //main game loop
            requestFocus(true); //key listening problems solved

            long now = System.nanoTime();
            delta += (now - lastTime) / ns; // how many ticks should have happened over the time that has passed
            lastTime = now;
            while (delta >= 1) {
                tick();
                delta--;
            }
            render();
        }
    }

    private void newGame() { //setting up basic game parameters
        playerPadY = GAME_HEIGHT / 2;
        ballVelocity = 2;
        ballAngle = PI;
        aiPadDirection = (double) GAME_HEIGHT / 2;

        predictedBallY = (double) GAME_HEIGHT / 2;

        aiPadY = GAME_HEIGHT / 2;
        aiPadVelocity = 1;
        ballX = (double) GAME_WIDTH / 2;
        ballY = (double) GAME_HEIGHT / 2;
        if(playerScore == 11 || aiScore == 11) {
            playerScore = 0;
            aiScore = 0;
        }
    }

    private void tick() {
        movePads();
        double precision = floor(ballVelocity); //used to increase precision with collision detections
        for (int i = 0; i < (int) precision; i++) {
            ballX += ballVelocity / precision * cos(ballAngle);
            ballY -= ballVelocity / precision * sin(ballAngle);
            checkCollision();
        }
    }

    private void movePads() { //checking if it is not out of bounds
        if (input == Input.UP) {
            playerPadY -= playerPadVelocity;
            if (playerPadY - PAD_HEIGHT / 2 < 0) {
                playerPadY = PAD_HEIGHT / 2;
            }
        }
        if (input == Input.DOWN) {
            playerPadY += playerPadVelocity;
            if (playerPadY + PAD_HEIGHT / 2 > GAME_HEIGHT) {
                playerPadY = GAME_HEIGHT - PAD_HEIGHT / 2;
            }
        }

        if (aiPadDirection > aiPadY) {
            aiPadY += aiPadVelocity;
        }
        if (aiPadDirection < aiPadY) {
            aiPadY -= aiPadVelocity;
        }

        if (aiPadY - PAD_HEIGHT / 2 < 0) {
            aiPadY = PAD_HEIGHT / 2;
        }
        if (aiPadY + PAD_HEIGHT / 2 > GAME_HEIGHT) {
            aiPadY = GAME_HEIGHT - PAD_HEIGHT / 2;
        }
    }

    private void checkCollision() {
        //top and bottom walls
        if (ballY + (double) BALL_DIAMETER / 2 >= GAME_HEIGHT || ballY - (double) BALL_DIAMETER / 2 <= 0) {
            ballAngle = 2 * PI - ballAngle;
            if (ballAngle > 2 * PI)
                ballAngle -= 2 * PI; //removing the unnecessary 360 degrees, it doesn't change anything
        }

        if (ballX + (double) BALL_DIAMETER / 2 >= GAME_WIDTH) { //right wall collision
            playerScore++;
            newGame();
        }
        if (ballX - (double) BALL_DIAMETER / 2 <= 0) { //left wall collision
            aiScore++;
            newGame();
        }

        if (ballX - (double) BALL_DIAMETER / 2 <= ((double) PAD_WIDTH) + PAD_OFFSET) { //left pad collision
            if (ballY + (double) BALL_DIAMETER / 2 >= playerPadY - (double) PAD_HEIGHT / 2 && ballY - (double) BALL_DIAMETER / 2 <= playerPadY + (double) PAD_HEIGHT / 2) { //the Y value is in between the pad Y top and bottom
                //calculating the reflection angle
                double percent = (ballY - playerPadY) / ((double) PAD_HEIGHT / 2); //the further from the middle of paddle the larger the angle
                if (percent > 1) percent = 1;
                if (percent < -1) percent = -1;
                ballAngle = -percent * PI / 4; //setting the angle (max is 45 degrees)

                //incrementing the velocity
                if (ballMaxVelocity > ballVelocity) ballVelocity = ballVelocity + 0.03 * ballVelocity;

                predictNextBallLocation();
            }
        }

        if (ballX + (double) BALL_DIAMETER / 2 >= GAME_WIDTH - (double) PAD_WIDTH - PAD_OFFSET) { //right pad collision
            if (ballY + (double) BALL_DIAMETER / 2 >= aiPadY - (double) PAD_HEIGHT / 2 && ballY - (double) BALL_DIAMETER / 2 <= aiPadY + (double) PAD_HEIGHT / 2) {  //the Y value is in between the pad Y top and bottom
                //calculating the reflection angle
                double percent = (ballY - aiPadY) / ((double) PAD_HEIGHT / 2); //the further from the middle of paddle the larger the angle
                if (percent > 1) percent = 1;
                if (percent < -1) percent = -1;
                ballAngle = PI + percent * PI / 4; //setting the angle (max is 45 degrees)


                //incrementing the velocity
                if (ballMaxVelocity > ballVelocity)
                    ballVelocity = ballVelocity + 0.03 * ballVelocity;

                //once the ball is reflected AI returns to the center of the board
                aiPadDirection = (double) GAME_HEIGHT / 2;
            }
        }

    }

    private void predictNextBallLocation() {
        double width = GAME_WIDTH - 2 * (PAD_OFFSET + PAD_WIDTH); //the ball travels only between the pads
        double travelledY = -width * tan(ballAngle); //negative because tangent rules
        double bounces = floor((travelledY + ballY) / ((double) GAME_HEIGHT)); //this is how many bounces will occur

        if ((int) bounces % 2 == 0) { //if the number of bounces is odd you have to do a mirror reflection
            predictedBallY = (travelledY + ballY - bounces * ((double) GAME_HEIGHT));
        } else {
            predictedBallY = ((double) GAME_HEIGHT + bounces * (double) GAME_HEIGHT - travelledY - ballY);
        }
        Random random = new Random();

        aiPadDirection = predictedBallY + (0.5-random.nextDouble())*((double) PAD_HEIGHT);
    }

    private void render() {
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.WHITE);

        //boarders
        g.fillRect(BOARDER_X_PX, BOARDER_Y_PX - NET_SIZE_PX, GAME_WIDTH * PIXEL_MULTIPLIER, NET_SIZE_PX);
        g.fillRect(BOARDER_X_PX, BOARDER_Y_PX + GAME_HEIGHT * PIXEL_MULTIPLIER, GAME_WIDTH * PIXEL_MULTIPLIER, NET_SIZE_PX);

        //net
        for (int i = NET_SIZE_PX / 2; i < GAME_HEIGHT * PIXEL_MULTIPLIER; i += 3 * NET_SIZE_PX) {
            g.fillRect(BOARDER_X_PX + PIXEL_MULTIPLIER * GAME_WIDTH / 2 - NET_SIZE_PX / 2, BOARDER_Y_PX + i + NET_SIZE_PX / 2, NET_SIZE_PX, NET_SIZE_PX);
        }

        //scores
        drawCenteredString(g, String.valueOf(playerScore), new Rectangle(BOARDER_X_PX + PIXEL_MULTIPLIER * GAME_WIDTH / 2 - 2 * 40, BOARDER_Y_PX + 20, 2 * 40, 40), new Font("Consolas", Font.PLAIN, 40));
        drawCenteredString(g, String.valueOf(aiScore), new Rectangle(BOARDER_X_PX + PIXEL_MULTIPLIER * GAME_WIDTH / 2, BOARDER_Y_PX + 20, 2 * 40, 40), new Font("Consolas", Font.PLAIN, 40));

        //two pads
        g.fillRect(BOARDER_X_PX + PAD_OFFSET * PIXEL_MULTIPLIER, BOARDER_Y_PX + PIXEL_MULTIPLIER * (playerPadY - PAD_HEIGHT / 2), PAD_WIDTH * PIXEL_MULTIPLIER, PAD_HEIGHT * PIXEL_MULTIPLIER);
        g.fillRect(BOARDER_X_PX + GAME_WIDTH * PIXEL_MULTIPLIER - PAD_OFFSET * PIXEL_MULTIPLIER - PAD_WIDTH * PIXEL_MULTIPLIER, BOARDER_Y_PX + PIXEL_MULTIPLIER * (aiPadY - PAD_HEIGHT / 2), PAD_WIDTH * PIXEL_MULTIPLIER, PAD_HEIGHT * PIXEL_MULTIPLIER);


        //predicted ball location
        //g.setColor(Color.RED);
        //g.fillRect((int) ((double) BOARDER_X_PX + GAME_WIDTH * PIXEL_MULTIPLIER - PAD_WIDTH * PIXEL_MULTIPLIER - PAD_OFFSET * PIXEL_MULTIPLIER - BALL_DIAMETER * PIXEL_MULTIPLIER), (int) ((double) BOARDER_Y_PX + (double) PIXEL_MULTIPLIER * (predictedBallY - (double) BALL_DIAMETER / 2)), PIXEL_MULTIPLIER * BALL_DIAMETER, PIXEL_MULTIPLIER * BALL_DIAMETER);

        //ball
        g.setColor(Color.white);
        g.fillRect((int) ((double) BOARDER_X_PX + (ballX - (double) BALL_DIAMETER / 2) * (double) PIXEL_MULTIPLIER), (int) ((double) BOARDER_Y_PX + (double) PIXEL_MULTIPLIER * (ballY - (double) BALL_DIAMETER / 2)), PIXEL_MULTIPLIER * BALL_DIAMETER, PIXEL_MULTIPLIER * BALL_DIAMETER);
    }

    public void drawCenteredString(Graphics g, String text, Rectangle rect, Font font) {
        // Get the FontMetrics
        FontMetrics metrics = g.getFontMetrics(font);
        // Determine the X coordinate for the text
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        // Set the font
        g.setFont(font);
        // Draw the String
        g.drawString(text, x, y);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN : {
                input = Input.DOWN;
                keyDownPressed = true;
            } break;
            case KeyEvent.VK_UP : {
                input = Input.UP;
                keyUpPressed = true;
            } break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN : {
                keyDownPressed = false;
                if (keyUpPressed) {
                    input = Input.UP;
                }
            }break;
            case KeyEvent.VK_UP : {
                keyUpPressed = false;
                if (keyDownPressed) {
                    input = Input.DOWN;
                }
            }break;
        }

        if (!keyDownPressed && !keyUpPressed) {
            input = Input.NEUTRAL;
        }
    }
}
