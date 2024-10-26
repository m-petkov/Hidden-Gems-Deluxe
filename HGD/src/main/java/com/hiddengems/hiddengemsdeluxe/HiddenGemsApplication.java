package com.hiddengems.hiddengemsdeluxe;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import java.util.Random;

public class HiddenGemsApplication extends Application {

    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 600;
    private static final int NUM_ROWS = 20;
    private static final int NUM_COLS = 8;
    private static final Duration FALL_DURATION = Duration.seconds(1);
    private static final Duration FAST_FALL_DURATION = Duration.seconds(0.1);
    private static final Duration MOVE_DURATION = Duration.millis(75); // Duration for left/right movement
    private int score = 0;

    private Random random = new Random();
    private char[][] gameBoard = new char[NUM_ROWS][NUM_COLS];

    private int width = MIN_WIDTH;
    private int height = MIN_HEIGHT;
    private double cellSize;
    private double fontSize;
    private double boardOffsetX;
    private double boardOffsetY;

    private TripleStone fallingStone;
    private TripleStone nextStone;

    private boolean isMovingLeft = false;
    private boolean isMovingRight = false;

    private boolean isPaused = false;
    private Timeline fallTimeline;
    private Timeline moveTimeline;
    private Timeline fastFallTimeline;
    private Timeline borderAnimation;
    private Timeline pulsatingTimeline;
    private double hue1 = 300; // Initial hue for color 1
    private double hue2 = 180; // Initial hue for color 2

    // Class fields for the border colors
    private Color borderColor1 = Color.hsb(hue1, 1.0, 1.0);
    private Color borderColor2 = Color.hsb(hue2, 1.0, 1.0);
    private boolean isFastFalling = false; // За контрол на бързото падане

    private double scoreFontSize;
    private double pauseFontSize;

    private Canvas canvas; // Декларация на canvas като член на класа
    private GraphicsContext gc;

    @Override

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hidden Gems Deluxe");

        Pane root = new Pane();
        Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);

        canvas = new Canvas(MIN_WIDTH, MIN_HEIGHT); // Инициализация на canvas
        root.getChildren().add(canvas);

        // Initialize gc here, which is now a class-level variable
        gc = canvas.getGraphicsContext2D();

        initializeGameBoard();
        startBorderAnimation(); // This now has access to gc
        calculateNextStone(); // Calculate the initial nextStone

        calculateSizes();
        drawGameBoard(gc); // Draw the game board with the initialized gc

        pulsatingTimeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            drawGameBoard(gc); // Redraw the game board on each tick
        }));
        pulsatingTimeline.setCycleCount(Timeline.INDEFINITE);
        pulsatingTimeline.play();

        fallTimeline = new Timeline(new KeyFrame(FALL_DURATION, event -> {
            if (!isPaused) {
                if (fallingStone == null) {
                    placeNewStones();
                } else {
                    moveStonesDown();
                }
                drawGameBoard(gc);
            }
        }));
        fallTimeline.setCycleCount(Timeline.INDEFINITE);
        fallTimeline.play();

        fastFallTimeline = new Timeline(new KeyFrame(FAST_FALL_DURATION, event -> {
            if (!isPaused && fallingStone != null) {
                moveStonesDown();
                drawGameBoard(gc);
            }
        }));
        fastFallTimeline.setCycleCount(Timeline.INDEFINITE);

        moveTimeline = new Timeline(new KeyFrame(MOVE_DURATION, event -> {
            if (!isPaused) {
                if (isMovingLeft) {
                    moveStonesLeft();
                    drawGameBoard(gc);
                }
                if (isMovingRight) {
                    moveStonesRight();
                    drawGameBoard(gc);
                }
            }
        }));
        moveTimeline.setCycleCount(Timeline.INDEFINITE);
        moveTimeline.play();

        // Add listener for keyboard input
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case LEFT:
                    isMovingLeft = true;
                    break;
                case RIGHT:
                    isMovingRight = true;
                    break;
                case DOWN:
                    if (!isPaused) {
                        fastFallTimeline.play();
                        isFastFalling = true;
                    }
                    break;
                case SPACE:
                    if (fallingStone != null) {
                        fallingStone.shiftUp(); // Shift stones up on SPACE press
                        drawGameBoard(gc);
                    }
                    break;
                case ENTER:
                    togglePause(gc);
                    break;
                default:
                    break;
            }
        });

        scene.setOnKeyReleased(event -> {
            switch (event.getCode()) {
                case LEFT:
                    isMovingLeft = false;
                    break;
                case RIGHT:
                    isMovingRight = false;
                    break;
                case DOWN:
                    fastFallTimeline.stop();
                    isFastFalling = false; // Спрете бързото падане
                    break;
                default:
                    break;
            }
        });

        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            width = newVal.intValue();
            canvas.setWidth(width);
            calculateSizes();
            drawGameBoard(gc);
        });

        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            height = newVal.intValue();
            canvas.setHeight(height);
            calculateSizes();
            drawGameBoard(gc);
        });

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.show();
    }

    private void togglePause(GraphicsContext gc) {
        isPaused = !isPaused;

        if (isPaused) {
            fallTimeline.pause();
            moveTimeline.pause();
            fastFallTimeline.pause();
            isFastFalling = false; // Когато е на пауза, бързото падане също спира
        } else {
            fallTimeline.play();
            moveTimeline.play();
            if (isFastFalling) {
                fastFallTimeline.play(); // Ако е включен бързото падане, възобновете го
            }
        }

        drawGameBoard(gc);
    }

    private void initializeGameBoard() {
        // Initialize game board with empty spaces
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                gameBoard[row][col] = ' ';
            }
        }
    }

    private void calculateNextStone() {
        nextStone = new TripleStone(0, 0, new char[]{getRandomColor(), getRandomColor(), getRandomColor()});
    }

    private int getCenterColumn() {
        return NUM_COLS / 2;
    }

    private void placeNewStones() {
        // Постави новия камък в централната колона
        int centerCol = getCenterColumn();
        fallingStone = new TripleStone(0, centerCol, nextStone.colors);
        calculateNextStone(); // Пресметни следващия камък
    }

    private void moveStonesDown() {
        if (fallingStone != null) {
            boolean moved = fallingStone.moveDown(gameBoard);
            if (!moved) {
                fallingStone = null; // Stone has landed
                checkAndClearMatchesUntilStable(); // Check for matches and clear them until the board is stable
            }
        }
    }

    private void moveStonesLeft() {
        if (fallingStone != null) {
            fallingStone.moveLeft(gameBoard);
        }
    }

    private void moveStonesRight() {
        if (fallingStone != null) {
            fallingStone.moveRight(gameBoard);
        }
    }

    private void checkAndClearMatchesUntilStable() {
        boolean matchesFound;
        do {
            matchesFound = checkAndClearMatches(); // Проверка и пребоядисване на камъните
            if (matchesFound) {
                drawGameBoard(canvas.getGraphicsContext2D()); // Пребоядисване на съвпаденията
                waitAndClearMatches(); // Изчакване и изчистване на камъните
            }
        } while (matchesFound); // Продължава, докато има съвпадения
    }

    private boolean checkAndClearMatches() {
        boolean[][] toClear = new boolean[NUM_ROWS][NUM_COLS];
        boolean matchesFound = false;

        // Проверка за хоризонтални съвпадения
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS - 2; col++) {
                char current = gameBoard[row][col];
                if (isColor(current) && gameBoard[row][col + 1] == current && gameBoard[row][col + 2] == current) {
                    toClear[row][col] = true;
                    toClear[row][col + 1] = true;
                    toClear[row][col + 2] = true;
                    matchesFound = true;
                }
            }
        }

        // Проверка за вертикални съвпадения
        for (int col = 0; col < NUM_COLS; col++) {
            for (int row = 0; row < NUM_ROWS - 2; row++) {
                char current = gameBoard[row][col];
                if (isColor(current) && gameBoard[row + 1][col] == current && gameBoard[row + 2][col] == current) {
                    toClear[row][col] = true;
                    toClear[row + 1][col] = true;
                    toClear[row + 2][col] = true;
                    matchesFound = true;
                }
            }
        }

        // Проверка за диагонални съвпадения (от горе ляво до долу дясно)
        for (int row = 0; row < NUM_ROWS - 2; row++) {
            for (int col = 0; col < NUM_COLS - 2; col++) {
                char current = gameBoard[row][col];
                if (isColor(current) && gameBoard[row + 1][col + 1] == current && gameBoard[row + 2][col + 2] == current) {
                    toClear[row][col] = true;
                    toClear[row + 1][col + 1] = true;
                    toClear[row + 2][col + 2] = true;
                    matchesFound = true;
                }
            }
        }

        // Проверка за диагонални съвпадения (от горе дясно до долу ляво)
        for (int row = 0; row < NUM_ROWS - 2; row++) {
            for (int col = 2; col < NUM_COLS; col++) {
                char current = gameBoard[row][col];
                if (isColor(current) && gameBoard[row + 1][col - 1] == current && gameBoard[row + 2][col - 2] == current) {
                    toClear[row][col] = true;
                    toClear[row + 1][col - 1] = true;
                    toClear[row + 2][col - 2] = true;
                    matchesFound = true;
                }
            }
        }

        // Пребоядисване на маркерите в магента
        if (matchesFound) {
            repaintMarkedStones(toClear);
        }

        return matchesFound;
    }

    private void repaintMarkedStones(boolean[][] toClear) {
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                if (toClear[row][col]) {
                    gameBoard[row][col] = 'M'; // 'M' за Magenta
                }
            }
        }
    }

    private void waitAndClearMatches() {
        PauseTransition pauseTransition = new PauseTransition(Duration.seconds(1));
        pauseTransition.setOnFinished(event -> {
            clearStones(getClearArray()); // Изчистване на камъните
            drawGameBoard(canvas.getGraphicsContext2D()); // Актуализиране на визуализацията
            checkAndClearMatchesUntilStable(); // Проверка и изчистване докато няма повече съвпадения
        });
        pauseTransition.play();
    }

    private boolean[][] getClearArray() {
        boolean[][] toClear = new boolean[NUM_ROWS][NUM_COLS];
        // Създайте масив с информацията за камъните, които трябва да бъдат изчистени
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                if (gameBoard[row][col] == 'M') {
                    toClear[row][col] = true;
                }
            }
        }
        return toClear;
    }

    private void clearStones(boolean[][] toClear) {
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                if (toClear[row][col]) {
                    gameBoard[row][col] = ' '; // Изчисти камъка
                }
            }
        }
        makeStonesFall(); // Преместете камъните след изчистването
        score += 1; // Добавяне на точки
    }

    private void makeStonesFall() {
        for (int col = 0; col < NUM_COLS; col++) {
            int emptyRow = NUM_ROWS - 1;
            for (int row = NUM_ROWS - 1; row >= 0; row--) {
                if (gameBoard[row][col] != ' ') {
                    char temp = gameBoard[row][col];
                    gameBoard[row][col] = ' ';
                    gameBoard[emptyRow][col] = temp;
                    emptyRow--;
                }
            }
        }
    }

    private void calculateSizes() {
        cellSize = Math.min(width / (NUM_COLS + 2), height / (NUM_ROWS + 2));
        fontSize = cellSize * 0.6; // Adjust font size based on cell size
        scoreFontSize = cellSize * 0.8; // Adjust score font size based on cell size
        pauseFontSize = cellSize * 1.0; // Adjust pause font size based on cell size
        boardOffsetX = (width - NUM_COLS * cellSize) / 2;
        boardOffsetY = (height - NUM_ROWS * cellSize) / 2;
    }

    private void startBorderAnimation() {
        // Timeline to animate the border colors continuously
        borderAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, e -> updateBorderColors()),
                new KeyFrame(Duration.millis(16)) // Approximately 60 FPS
        );
        borderAnimation.setCycleCount(Timeline.INDEFINITE);
        borderAnimation.play();
    }

    private void updateBorderColors() {
        // Cycle through hues to animate the border colors
        hue1 = (hue1 + 1) % 360;
        hue2 = (hue2 + 1) % 360;

        // Update border colors based on the new hue values
        borderColor1 = Color.hsb(hue1, 1.0, 1.0);
        borderColor2 = Color.hsb(hue2, 1.0, 1.0);

        // Redraw the entire game board with the updated border colors
        drawGameBoard(gc);
    }

    private void drawBorder(GraphicsContext gc) {
        double borderWidth = 10; // Width of the border

        // Set up a gradient for the border
        LinearGradient borderGradient = new LinearGradient(
                0, 0, width, height,
                true,
                CycleMethod.REFLECT,
                new Stop(0, borderColor1),
                new Stop(1, borderColor2)
        );

        // Set the fill to the border gradient
        gc.setFill(borderGradient);

        // Draw the top border
        gc.fillRect(boardOffsetX - borderWidth, boardOffsetY - borderWidth,
                NUM_COLS * cellSize + 2 * borderWidth, borderWidth);

        // Draw the bottom border
        gc.fillRect(boardOffsetX - borderWidth, boardOffsetY + NUM_ROWS * cellSize,
                NUM_COLS * cellSize + 2 * borderWidth, borderWidth);

        // Draw the left border
        gc.fillRect(boardOffsetX - borderWidth, boardOffsetY,
                borderWidth, NUM_ROWS * cellSize);

        // Draw the right border
        gc.fillRect(boardOffsetX + NUM_COLS * cellSize, boardOffsetY,
                borderWidth, NUM_ROWS * cellSize);
    }

    private void drawGameBoard(GraphicsContext gc) {
        // Background gradient
        LinearGradient backgroundGradient = new LinearGradient(
                0, 0, width, height,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.MIDNIGHTBLUE),
                new Stop(1, Color.DEEPPINK)
        );
        gc.setFill(backgroundGradient);
        gc.fillRect(0, 0, width, height);

        // Draw border with the updated colors
        drawBorder(gc);

        // Clear the game board area
        gc.clearRect(boardOffsetX, boardOffsetY, NUM_COLS * cellSize, NUM_ROWS * cellSize);

        // Draw game board background
        gc.setFill(Color.LIGHTSKYBLUE.brighter());
        gc.fillRect(boardOffsetX, boardOffsetY, NUM_COLS * cellSize, NUM_ROWS * cellSize);

        // Draw existing stones on the board
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                char color = gameBoard[row][col];
                if (isColor(color)) {
                    double x = boardOffsetX + col * cellSize;
                    double y = boardOffsetY + row * cellSize;
                    drawCell(gc, color, x, y, cellSize);
                } else if (color == 'M') {
                    double x = boardOffsetX + col * cellSize;
                    double y = boardOffsetY + row * cellSize;
                    drawCell(gc, 'M', x, y, cellSize);
                }
            }
        }

        // Draw grid lines with a contrasting color
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        for (int row = 0; row <= NUM_ROWS; row++) {
            double y = boardOffsetY + row * cellSize;
            gc.strokeLine(boardOffsetX, y, boardOffsetX + NUM_COLS * cellSize, y);
        }
        for (int col = 0; col <= NUM_COLS; col++) {
            double x = boardOffsetX + col * cellSize;
            gc.strokeLine(x, boardOffsetY, x, boardOffsetY + NUM_ROWS * cellSize);
        }

        // Draw "Score" with a neon glow effect and black border
        gc.setFont(new javafx.scene.text.Font("Comic Sans MS", scoreFontSize));
        String scoreText = "Score: " + score;
        Text text = new Text(scoreText);
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();

        // Calculate score position
        double scoreX = boardOffsetX - textWidth - 20; // 20 pixels away from the left edge
        double scoreY = boardOffsetY + cellSize / 2 + textHeight / 2; // Vertically centered on the cell

        // Draw shadow (black border) for the score text
        gc.setFill(Color.BLACK);
        gc.fillText(scoreText, scoreX + 2, scoreY + 2);  // Black shadow offset
        gc.setFill(Color.CYAN);
        gc.fillText(scoreText, scoreX, scoreY);           // Light blue text

        // Draw falling stone if present
        if (fallingStone != null) {
            fallingStone.draw(gc, boardOffsetX, boardOffsetY, cellSize);
        }

        // Draw next stone preview if present
        if (nextStone != null) {
            double previewOffsetX = boardOffsetX + NUM_COLS * cellSize + cellSize;
            double previewOffsetY = boardOffsetY;
            nextStone.drawPreview(gc, previewOffsetX, previewOffsetY, cellSize);
        }

        // Draw "PAUSE" text with gradient, animation, and black border if the game is paused
        if (isPaused) {
            // Set up a neon-like gradient effect
            LinearGradient pauseGradient = new LinearGradient(
                    0, 0, 1, 0,
                    true, CycleMethod.REPEAT,
                    new Stop(0, Color.DEEPPINK),
                    new Stop(1, Color.CYAN)
            );

            // Create pulsating effect by varying font size
            double pulsatingEffect = Math.sin(System.currentTimeMillis() * 0.005); // Value between -1 and 1
            double pauseFontSize = 30 + pulsatingEffect * 5; // Pulsate around 30 pixels

            gc.setFont(new javafx.scene.text.Font("Comic Sans MS", pauseFontSize));

            // Calculate PAUSE text width and height
            Text pauseText = new Text("PAUSE");
            pauseText.setFont(gc.getFont());
            double pauseTextWidth = pauseText.getLayoutBounds().getWidth();
            double pauseTextHeight = pauseText.getLayoutBounds().getHeight();

            // Draw shadow (black border) for the PAUSE text
            gc.setFill(Color.BLACK);
            gc.fillText("PAUSE", width / 2 - pauseTextWidth / 2 + 2, height / 2 + pauseTextHeight / 4 + 2); // Black shadow offset
            gc.setFill(pauseGradient);
            gc.fillText("PAUSE", width / 2 - pauseTextWidth / 2, height / 2 + pauseTextHeight / 4); // Gradient text
        }
    }

    private void drawCell(GraphicsContext gc, char color, double x, double y, double size) {
        // Радиален градиент за по-реалистичен триизмерен ефект
        Color baseColor = getColor(color);
        RadialGradient gradient = new RadialGradient(
                0, 0, x + size / 2, y + size / 2, size / 2,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, baseColor.brighter()),
                new Stop(1, baseColor.darker())
        );

        // Закръглени ъгли за по-мек вид
        gc.setFill(gradient);
        gc.fillRoundRect(x, y, size, size, size * 0.2, size * 0.2);  // Закръгленост 20% от размера

        // Добавяне на светлосенки
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, size, size, size * 0.2, size * 0.2);  // Същата закръгленост като на формата

        // Лека сянка под камъка
        gc.setGlobalAlpha(0.3);
        gc.setFill(Color.GRAY);
        gc.fillRoundRect(x + 3, y + 3, size, size, size * 0.2, size * 0.2);
        gc.setGlobalAlpha(1.0);
    }

    private Color getColor(char colorChar) {
        switch (colorChar) {
            case 'R':
                return Color.RED;
            case 'G':
                return Color.GREEN;
            case 'B':
                return Color.BLUE;
            case 'Y':
                return Color.YELLOW;
            case 'P':
                return Color.PURPLE;
            case 'M': // Magenta
                return Color.MAGENTA;
            default:
                return Color.BLACK;
        }
    }

    private char getRandomColor() {
        char[] colors = {'R', 'G', 'B', 'Y', 'P'};
        return colors[random.nextInt(colors.length)];
    }

    private boolean isColor(char c) {
        return c == 'R' || c == 'G' || c == 'B' || c == 'Y' || c == 'P';
    }

    private class TripleStone {
        int row;
        int col;
        char[] colors;

        TripleStone(int row, int col, char[] colors) {
            this.row = row;
            this.col = col;
            this.colors = colors;
        }

        void draw(GraphicsContext gc, double offsetX, double offsetY, double size) {
            for (int i = 0; i < colors.length; i++) {
                double x = offsetX + col * size;
                double y = offsetY + (row + i) * size;
                drawCell(gc, colors[i], x, y, size);
            }
        }

        void drawPreview(GraphicsContext gc, double offsetX, double offsetY, double size) {
            for (int i = 0; i < colors.length; i++) {
                double x = offsetX;
                double y = offsetY + i * size;
                drawCell(gc, colors[i], x, y, size);
            }
        }

        boolean moveDown(char[][] board) {
            if (canMoveDown(board)) {
                row++;
                return true;
            }
            placeOnBoard(board);
            return false;
        }

        boolean canMoveDown(char[][] board) {
            for (int i = 0; i < colors.length; i++) {
                if (row + i + 1 >= NUM_ROWS || board[row + i + 1][col] != ' ') {
                    return false;
                }
            }
            return true;
        }

        void moveLeft(char[][] board) {
            if (col > 0 && canMoveLeft(board)) {
                col--;
            }
        }

        void moveRight(char[][] board) {
            if (col < NUM_COLS - 1 && canMoveRight(board)) {
                col++;
            }
        }

        boolean canMoveLeft(char[][] board) {
            for (int i = 0; i < colors.length; i++) {
                if (col - 1 < 0 || board[row + i][col - 1] != ' ') {
                    return false;
                }
            }
            return true;
        }

        boolean canMoveRight(char[][] board) {
            for (int i = 0; i < colors.length; i++) {
                if (col + 1 >= NUM_COLS || board[row + i][col + 1] != ' ') {
                    return false;
                }
            }
            return true;
        }

        void shiftUp() {
            if (colors.length > 1) {
                char temp = colors[0];
                for (int i = 0; i < colors.length - 1; i++) {
                    colors[i] = colors[i + 1];
                }
                colors[colors.length - 1] = temp;
            }
        }

        void placeOnBoard(char[][] board) {
            for (int i = 0; i < colors.length; i++) {
                board[row + i][col] = colors[i];
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}