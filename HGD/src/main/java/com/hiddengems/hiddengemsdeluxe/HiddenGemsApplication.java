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
    // Initial hues for green and gray
    private double hue1 = 120; // Initial hue for bright neon green
    private double hue2 = 210; // Initial hue for dark blue

    // Class fields for the border colors
    private Color borderColor1 = Color.hsb(hue1, 0.9, 0.5); // Darker neon green
    private Color borderColor2 = Color.hsb(hue2, 0.7, 0.2); // Darker grayish-green

    // Add additional color for dark blue or gray if needed
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
                new KeyFrame(Duration.millis(50), e -> updateBorderColors()) // Adjusted to 50 ms for smoother transitions
        );
        borderAnimation.setCycleCount(Timeline.INDEFINITE);
        borderAnimation.play();
    }

    private Color[] borderColors = {
            Color.DARKGREEN,  // Dark Green
            Color.DARKBLUE,   // Dark Blue
            Color.DARKCYAN    // Dark Cyan
    };
    private int currentColorIndex = 0; // Index to track current color

    private double transitionProgress = 0; // Progress for the transition

    private void updateBorderColors() {
        transitionProgress += 0.02; // Increment progress

        if (transitionProgress >= 1) {
            // Move to the next color when the transition is complete
            currentColorIndex = (currentColorIndex + 1) % borderColors.length;
            transitionProgress = 0; // Reset progress
        }

        // Get the next color
        Color nextColor = borderColors[currentColorIndex];

        // Interpolate between current color and next color
        borderColor1 = interpolateColor(borderColors[(currentColorIndex + 1) % borderColors.length], nextColor, transitionProgress);
        borderColor2 = interpolateColor(nextColor, borderColors[(currentColorIndex + 2) % borderColors.length], transitionProgress);

        // Redraw the entire game board with the updated border colors
        drawGameBoard(gc);
    }

    // Interpolation method to blend colors
    private Color interpolateColor(Color colorA, Color colorB, double progress) {
        double red = colorA.getRed() + (colorB.getRed() - colorA.getRed()) * progress;
        double green = colorA.getGreen() + (colorB.getGreen() - colorA.getGreen()) * progress;
        double blue = colorA.getBlue() + (colorB.getBlue() - colorA.getBlue()) * progress;
        return Color.color(red, green, blue);
    }

    private void drawBorder(GraphicsContext gc) {
        double borderWidth = cellSize * 0.3;

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
        // Create a dark green background gradient
        LinearGradient backgroundGradient = new LinearGradient(
                0, 0, 0, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.BLACK),
                new Stop(1, Color.DARKGREEN)
        );
        gc.setFill(backgroundGradient);
        gc.fillRect(0, 0, width, height);

        // Optional: Draw falling code effect
        drawFallingCode(gc);

        // Draw border with the updated colors before clearing the game area
        drawBorder(gc);

        // Clear the game board area
        gc.clearRect(boardOffsetX, boardOffsetY, NUM_COLS * cellSize, NUM_ROWS * cellSize);

        gc.setFill(Color.DARKSEAGREEN);
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

        // Draw "Score" with a green glow effect
        gc.setFont(new javafx.scene.text.Font("Courier New", scoreFontSize));
        String scoreText = "Score: " + score;
        Text text = new Text(scoreText);
        text.setFont(gc.getFont());
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();

        // Calculate score position based on cell size
        double scoreX = boardOffsetX - textWidth - (cellSize * 0.5); // Center it with respect to the cell size
        double scoreY = boardOffsetY + (cellSize * 0.5) + (textHeight / 2); // Vertically center within the cell

        // Draw shadow (black border) for the score text
        gc.setFill(Color.BLACK);
        gc.fillText(scoreText, scoreX + 2, scoreY + 2);  // Black shadow offset
        gc.setFill(Color.LIGHTGREEN); // Matrix-style text color
        gc.fillText(scoreText, scoreX, scoreY);           // Light green text

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

        // Draw "PAUSE" text with gradient and black border if the game is paused
        if (isPaused) {
            LinearGradient pauseGradient = new LinearGradient(
                    0, 0, 1, 0,
                    true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(0, 30, 0)),       // Very dark green
                    new Stop(1, Color.rgb(0, 40, 40))       // Dark teal
            );

            // Calculate a font size based on the cell size and pulsate it
            double pulsatingEffect = Math.sin(System.currentTimeMillis() * 0.005); // Value between -1 and 1
            double pauseFontSize = Math.max(20, cellSize * 0.5 * 4 + pulsatingEffect * 5); // Pulsate around 4 cells

            gc.setFont(new javafx.scene.text.Font("Courier New", pauseFontSize));

            // Calculate PAUSE text width and height
            Text pauseText = new Text("PAUSE");
            pauseText.setFont(gc.getFont());
            double pauseTextWidth = pauseText.getLayoutBounds().getWidth();
            double pauseTextHeight = pauseText.getLayoutBounds().getHeight();

            // Center the PAUSE text based on updated sizes
            double pauseX = (width - pauseTextWidth) / 2; // Center X
            double pauseY = (height - pauseTextHeight) / 2; // Center Y

            // Draw shadow (black border) for the PAUSE text
            gc.setFill(Color.BLACK);
            gc.fillText("PAUSE", pauseX + 2, pauseY + 2); // Black shadow offset
            gc.setFill(pauseGradient);
            gc.fillText("PAUSE", pauseX, pauseY); // Gradient text
        }
    }

    private void drawFallingCode(GraphicsContext gc) {
        gc.setFont(new javafx.scene.text.Font("Courier New", 12));
        String[] characters = {"0", "1", "A", "B", "C", "D", "E", "F"}; // Characters to fall
        int numColumns = (int) Math.ceil(width / 15); // Adjust based on font size

        for (int col = 0; col < numColumns; col++) {
            int fallHeight = (int) (Math.random() * height);
            String fallingCharacter = characters[(int) (Math.random() * characters.length)];
            gc.setFill(Color.GREEN); // Color for falling code
            gc.fillText(fallingCharacter, col * 15, fallHeight); // Draw falling character
        }
    }

    private void drawCell(GraphicsContext gc, char color, double x, double y, double size) {
        // Get the base color using getColor as specified
        Color baseColor = getColor(color);

        // Matrix-inspired gradient with green tones: Keep the base color but add a digital look with the gradient
        RadialGradient gradient = new RadialGradient(
                0, 0, x + size / 2, y + size / 2, size / 2,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, baseColor.brighter().saturate()),   // Bright center for a glow effect
                new Stop(0.5, baseColor),                       // Mid-tone base color
                new Stop(1, baseColor.darker().darker())        // Darker edge for contrast
        );

        // Define points for the diamond shape, centered on (x, y) with size as the dimension
        double halfSize = size / 2;
        double[] xPoints = {x + halfSize, x + size, x + halfSize, x};  // Right, bottom, left, top
        double[] yPoints = {y, y + halfSize, y + size, y + halfSize};

        // Fill the diamond shape with the gradient
        gc.setFill(gradient);
        gc.fillPolygon(xPoints, yPoints, 4);

        // Apply a neon glow effect using a softer green outline based on baseColor
        gc.setGlobalAlpha(0.4); // Reduced opacity for the glow effect
        gc.setStroke(baseColor.brighter().brighter()); // Brighter version of baseColor for glow
        gc.setLineWidth(2.5);
        gc.strokePolygon(xPoints, yPoints, 4); // Outline the diamond shape

        // Darker shadow effect for a deeper look
        gc.setGlobalAlpha(0.25);
        gc.setFill(Color.BLACK); // Black shadow to create digital depth
        double shadowOffset = 3;
        double[] shadowXPoints = {x + halfSize + shadowOffset, x + size + shadowOffset,
                x + halfSize + shadowOffset, x + shadowOffset};
        double[] shadowYPoints = {y + shadowOffset, y + halfSize + shadowOffset,
                y + size + shadowOffset, y + halfSize + shadowOffset};
        gc.fillPolygon(shadowXPoints, shadowYPoints, 4); // Offset shadow for a 3D effect

        // Reset alpha to full opacity
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