import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class Java2048 extends JPanel {
    private static final Color BG_COLOR = new Color(0xbbada0);
    private static final String FONT_NAME = "Arial";
    private static final int TILE_SIZE = 64;
    private static final int TILES_MARGIN = 16;

    private int gridSize; // Instance variable for grid size
    private int targetScore; // Winning score set by the user
    private boolean undoEnabled; // Flag to determine if Undo is enabled

    private Tile[] myTiles;
    boolean myWin = false;
    boolean myLose = false;
    boolean hasShownWinDialog = false; // Flag to check if win dialog has been shown
    int myScore = 0;

    // Stack to store previous game states for Undo functionality
    private Stack<GameState> previousStates;

    public Java2048(int gridSize, int targetScore, boolean undoEnabled) {
        this.gridSize = gridSize;
        this.targetScore = targetScore;
        this.undoEnabled = undoEnabled;

        if (undoEnabled) {
            previousStates = new Stack<>();
        }

        int preferredWidth = gridSize * (TILE_SIZE + TILES_MARGIN) + TILES_MARGIN;
        int preferredHeight = preferredWidth + 100; // Extra space for score and messages
        setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        setFocusable(true);
        setLayout(null); // Using absolute positioning for simplicity

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    resetGame();
                }
                if (!canMove()) {
                    myLose = true;
                }

                if (!myLose) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                            move(Direction.LEFT);
                            break;
                        case KeyEvent.VK_RIGHT:
                            move(Direction.RIGHT);
                            break;
                        case KeyEvent.VK_DOWN:
                            move(Direction.DOWN);
                            break;
                        case KeyEvent.VK_UP:
                            move(Direction.UP);
                            break;
                        case KeyEvent.VK_U:
                            if (undoEnabled) {
                                performUndo();
                            }
                            break;
                    }
                }

                if (!myWin && !canMove()) {
                    myLose = true;
                }

                // Show win dialog if necessary
                if (myWin && !hasShownWinDialog) {
                    hasShownWinDialog = true;
                    SwingUtilities.invokeLater(() -> showWinDialog());
                }

                repaint();
            }
        });
        resetGame();
    }

    /**
     * Enum to represent movement directions
     */
    private enum Direction {
        LEFT, RIGHT, UP, DOWN
    }

    /**
     * Class to store the game state for Undo functionality
     */
    private static class GameState {
        Tile[] tiles;
        int score;
        boolean win;
        boolean lose;

        public GameState(Tile[] tiles, int score, boolean win, boolean lose) {
            // Deep copy of tiles
            this.tiles = new Tile[tiles.length];
            for (int i = 0; i < tiles.length; i++) {
                this.tiles[i] = new Tile(tiles[i].value);
            }
            this.score = score;
            this.win = win;
            this.lose = lose;
        }
    }

    /**
     * Perform the Undo action
     */
    private void performUndo() {
        if (!previousStates.isEmpty()) {
            GameState prevState = previousStates.pop();
            // Restore tiles
            for (int i = 0; i < myTiles.length; i++) {
                myTiles[i].value = prevState.tiles[i].value;
            }
            // Restore score and flags
            myScore = prevState.score;
            myWin = prevState.win;
            myLose = prevState.lose;

            repaint();
        }
    }

    /**
     * Show the Win Dialog with options to Continue or Restart
     */
    private void showWinDialog() {
        int option = JOptionPane.showOptionDialog(
                this,
                "You reached " + targetScore + "! Do you want to continue playing?",
                "You Win!",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"Continue", "Restart"},
                "Continue");
        if (option == JOptionPane.NO_OPTION) {
            resetGame();
        } else {
            myWin = false; // Continue playing
        }
    }

    /**
     * Reset the game to initial state
     */
    public void resetGame() {
        myScore = 0;
        myWin = false;
        myLose = false;
        hasShownWinDialog = false;
        myTiles = new Tile[gridSize * gridSize];
        for (int i = 0; i < myTiles.length; i++) {
            myTiles[i] = new Tile();
        }
        if (undoEnabled) {
            previousStates.clear();
        }
        addTile();
        addTile();
    }

    /**
     * Perform a move in the specified direction
     */
    private void move(Direction direction) {
        // Save current state before move if Undo is enabled
        if (undoEnabled) {
            previousStates.push(new GameState(myTiles, myScore, myWin, myLose));
        }

        switch (direction) {
            case LEFT:
                left();
                break;
            case RIGHT:
                rotate(180);
                left();
                rotate(180);
                break;
            case UP:
                rotate(270);
                left();
                rotate(90);
                break;
            case DOWN:
                rotate(90);
                left();
                rotate(270);
                break;
        }
    }

    /**
     * Move tiles to the left
     */
    public void left() {
        boolean needAddTile = false;
        for (int i = 0; i < gridSize; i++) {
            Tile[] line = getLine(i);
            Tile[] merged = mergeLine(moveLine(line));
            setLine(i, merged);
            if (!needAddTile && !compare(line, merged)) {
                needAddTile = true;
            }
        }

        if (needAddTile) {
            addTile();
        }
    }

    /**
     * Rotate the grid by the specified angle (90, 180, 270 degrees)
     */
    private void rotate(int angle) {
        Tile[] newTiles = new Tile[gridSize * gridSize];
        int offsetX = gridSize - 1;
        int offsetY = gridSize - 1;
        if (angle == 90) {
            offsetY = 0;
        } else if (angle == 270) {
            offsetX = 0;
        }

        int cos = 0;
        int sin = 0;
        angle = angle % 360;
        if (angle == 0) {
            cos = 1;
            sin = 0;
        } else if (angle == 90) {
            cos = 0;
            sin = 1;
        } else if (angle == 180) {
            cos = -1;
            sin = 0;
        } else if (angle == 270) {
            cos = 0;
            sin = -1;
        }

        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                int newX = (x * cos) - (y * sin) + offsetX;
                int newY = (x * sin) + (y * cos) + offsetY;
                newTiles[newX + newY * gridSize] = tileAt(x, y);
            }
        }
        myTiles = newTiles;
    }

    /**
     * Move a single line to the left (remove empty tiles)
     */
    private Tile[] moveLine(Tile[] oldLine) {
        LinkedList<Tile> l = new LinkedList<>();
        for (int i = 0; i < gridSize; i++) {
            if (!oldLine[i].isEmpty())
                l.addLast(oldLine[i]);
        }
        if (l.isEmpty()) {
            return oldLine;
        } else {
            Tile[] newLine = new Tile[gridSize];
            ensureSize(l, gridSize);
            for (int i = 0; i < gridSize; i++) {
                newLine[i] = l.removeFirst();
            }
            return newLine;
        }
    }

    /**
     * Merge tiles in a line
     */
    private Tile[] mergeLine(Tile[] oldLine) {
        LinkedList<Tile> list = new LinkedList<>();
        for (int i = 0; i < gridSize && !oldLine[i].isEmpty(); i++) {
            int num = oldLine[i].value;
            if (i < gridSize - 1 && oldLine[i].value == oldLine[i + 1].value) {
                num *= 2;
                myScore += num;
                if (num == targetScore) {
                    myWin = true;
                }
                i++;
            }
            list.add(new Tile(num));
        }
        if (list.isEmpty()) {
            return oldLine;
        } else {
            ensureSize(list, gridSize);
            return list.toArray(new Tile[gridSize]);
        }
    }

    /**
     * Ensure the list has the required size by adding empty tiles
     */
    private static void ensureSize(List<Tile> l, int s) {
        while (l.size() < s) {
            l.add(new Tile());
        }
    }

    /**
     * Get a line of tiles from the grid
     */
    private Tile[] getLine(int index) {
        Tile[] result = new Tile[gridSize];
        for (int i = 0; i < gridSize; i++) {
            result[i] = tileAt(i, index);
        }
        return result;
    }

    /**
     * Set a line of tiles in the grid
     */
    private void setLine(int index, Tile[] re) {
        System.arraycopy(re, 0, myTiles, index * gridSize, gridSize);
    }

    /**
     * Add a new tile to a random empty position
     */
    private void addTile() {
        List<Tile> list = availableSpace();
        if (!list.isEmpty()) {
            int index = (int) (Math.random() * list.size()) % list.size();
            Tile emptyTile = list.get(index);
            emptyTile.value = Math.random() < 0.9 ? 2 : 4;
        }
    }

    /**
     * Get a list of all empty tiles
     */
    private List<Tile> availableSpace() {
        final List<Tile> list = new ArrayList<>(gridSize * gridSize);
        for (Tile t : myTiles) {
            if (t.isEmpty()) {
                list.add(t);
            }
        }
        return list;
    }

    /**
     * Check if the grid is full
     */
    private boolean isFull() {
        return availableSpace().isEmpty();
    }

    /**
     * Check if the player can make any move
     */
    boolean canMove() {
        if (!isFull()) {
            return true;
        }
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                Tile t = tileAt(x, y);
                if ((x < gridSize - 1 && t.value == tileAt(x + 1, y).value)
                        || (y < gridSize - 1 && t.value == tileAt(x, y + 1).value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Compare two lines to check if they are the same
     */
    private boolean compare(Tile[] line1, Tile[] line2) {
        if (line1 == line2) {
            return true;
        } else if (line1.length != line2.length) {
            return false;
        }

        for (int i = 0; i < line1.length; i++) {
            if (line1[i].value != line2[i].value) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the tile at the specified position
     */
    private Tile tileAt(int x, int y) {
        return myTiles[x + y * gridSize];
    }

    /**
     * Paint the game grid and UI components
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, this.getSize().width, this.getSize().height);

        // Draw tiles
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                drawTile(g, myTiles[x + y * gridSize], x, y);
            }
        }

        // Draw Win/Lose overlay
        if (myWin || myLose) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(255, 255, 255, 30));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(new Color(78, 139, 202));
            g2d.setFont(new Font(FONT_NAME, Font.BOLD, 48));
            if (myWin) {
                g2d.drawString("You won!", getWidth() / 2 - 100, getHeight() / 2 - 50);
                g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 24));
                g2d.drawString("Press ESC to restart or", getWidth() / 2 - 130, getHeight() / 2);
                g2d.drawString("continue playing!", getWidth() / 2 - 100, getHeight() / 2 + 30);
            }
            if (myLose) {
                g2d.drawString("Game over!", getWidth() / 2 - 120, getHeight() / 2 - 50);
                g2d.drawString("You lose!", getWidth() / 2 - 100, getHeight() / 2);
            }
        }

        // Draw Score
        g.setColor(new Color(0x776e65));
        g.setFont(new Font(FONT_NAME, Font.PLAIN, 18));
        g.drawString("Score: " + myScore, TILES_MARGIN, 50);
    }

    /**
     * Draw a single tile
     */
    private void drawTile(Graphics g2, Tile tile, int x, int y) {
        Graphics2D g = (Graphics2D) g2;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int value = tile.value;
        int xOffset = offsetCoors(x);
        int yOffset = offsetCoors(y) + 60; // Shift down to make space for score
        g.setColor(tile.getBackground());
        g.fillRoundRect(xOffset, yOffset, TILE_SIZE, TILE_SIZE, 14, 14);
        g.setColor(tile.getForeground());

        final int size = value < 100 ? 36 : value < 1000 ? 32 : 24;
        final Font font = new Font(FONT_NAME, Font.BOLD, size);
        g.setFont(font);

        String s = value > 0 ? String.valueOf(value) : "";
        final FontMetrics fm = g.getFontMetrics(font);
        final int w = fm.stringWidth(s);
        final int h = fm.getAscent();

        if (value != 0)
            g.drawString(s, xOffset + (TILE_SIZE - w) / 2, yOffset + (TILE_SIZE + h) / 2 - 2);
    }

    /**
     * Calculate the pixel offset for a tile coordinate
     */
    private int offsetCoors(int arg) {
        return arg * (TILES_MARGIN + TILE_SIZE) + TILES_MARGIN;
    }

    /**
     * Tile class representing each tile on the grid
     */
    static class Tile {
        int value;

        public Tile() {
            this(0);
        }

        public Tile(int num) {
            value = num;
        }

        public boolean isEmpty() {
            return value == 0;
        }

        public Color getForeground() {
            return value < 16 ? new Color(0x776e65) : new Color(0xf9f6f2);
        }

        public Color getBackground() {
            switch (value) {
                case 2:
                    return new Color(0xeee4da);
                case 4:
                    return new Color(0xede0c8);
                case 8:
                    return new Color(0xf2b179);
                case 16:
                    return new Color(0xf59563);
                case 32:
                    return new Color(0xf67c5f);
                case 64:
                    return new Color(0xf65e3b);
                case 128:
                    return new Color(0xedcf72);
                case 256:
                    return new Color(0xedcc61);
                case 512:
                    return new Color(0xedc850);
                case 1024:
                    return new Color(0xedc53f);
                case 2048:
                    return new Color(0xedc22e);
                case 4096:
                    return Color.DARK_GRAY;
                default:
                    if (value >= 8192 && value <= 131072) {
                        // Shades of purple
                        int index = (int) (Math.log(value) / Math.log(2)) - 13;
                        Color[] purpleShades = {
                                new Color(0xB19CD9), // Lavender
                                new Color(0x8B008B), // Dark Magenta
                                new Color(0x800080), // Purple
                                new Color(0x4B0082), // Indigo
                                new Color(0x2E0854)  // Dark Purple
                        };
                        index = Math.min(index, purpleShades.length - 1);
                        return purpleShades[index];
                    } else if (value >= 262144 && value <= 4194304) {
                        // Shades of blue
                        int index = (int) (Math.log(value) / Math.log(2)) - 18;
                        Color[] blueShades = {
                                new Color(0xADD8E6), // Light Blue
                                new Color(0x0000FF), // Blue
                                new Color(0x0000CD), // Medium Blue
                                new Color(0x00008B), // Dark Blue
                                new Color(0x000080)  // Navy
                        };
                        index = Math.min(index, blueShades.length - 1);
                        return blueShades[index];
                    } else if (value >= 4194304) {
                        return Color.GRAY;
                    } else {
                        return new Color(0xcdc1b4); // Default tile color
                    }
            }
        }
    }

    /**
     * Start Screen Class
     */
    public static class StartScreen extends JFrame {
        public StartScreen() {
            setTitle("2048 Game - Start Screen");
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setSize(400, 350);
            setResizable(false);
            setLocationRelativeTo(null);
            setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel titleLabel = new JLabel("Welcome to 2048 Game", JLabel.CENTER);
            titleLabel.setFont(new Font(FONT_NAME, Font.BOLD, 24));
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(10, 10, 20, 10);
            add(titleLabel, gbc);

            gbc.insets = new Insets(5, 10, 5, 10);
            gbc.gridwidth = 1;

            JLabel gridSizeLabel = new JLabel("Grid Size (e.g., 4 for 4x4): ");
            gbc.gridx = 0;
            gbc.gridy = 1;
            add(gridSizeLabel, gbc);

            JTextField gridSizeField = new JTextField("5");
            gbc.gridx = 1;
            gbc.gridy = 1;
            add(gridSizeField, gbc);

            JLabel targetScoreLabel = new JLabel("Target Score (e.g., 2048): ");
            gbc.gridx = 0;
            gbc.gridy = 2;
            add(targetScoreLabel, gbc);

            JTextField targetScoreField = new JTextField("2048");
            gbc.gridx = 1;
            gbc.gridy = 2;
            add(targetScoreField, gbc);

            JCheckBox undoCheckBox = new JCheckBox("Enable Undo");
            undoCheckBox.setSelected(true); // Default to enabled
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            add(undoCheckBox, gbc);

            JButton startButton = new JButton("Start Game");
            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(20, 10, 10, 10);
            add(startButton, gbc);

            startButton.addActionListener(e -> {
                try {
                    int gridSize = Integer.parseInt(gridSizeField.getText());
                    int targetScore = Integer.parseInt(targetScoreField.getText());
                    boolean undoEnabled = undoCheckBox.isSelected();

                    if (gridSize < 2) {
                        JOptionPane.showMessageDialog(this, "Grid size must be at least 2.");
                        return;
                    }
                    if (targetScore <= 0 || (targetScore & (targetScore - 1)) != 0) {
                        JOptionPane.showMessageDialog(this, "Target score must be a power of 2.");
                        return;
                    }
                    dispose();
                    SwingUtilities.invokeLater(() -> startGame(gridSize, targetScore, undoEnabled));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter valid numbers.");
                }
            });
        }
    }

    /**
     * Start the game with the specified settings
     */
    public static void startGame(int gridSize, int targetScore, boolean undoEnabled) {
        int preferredWidth = gridSize * (TILE_SIZE + TILES_MARGIN) + TILES_MARGIN;
        int preferredHeight = preferredWidth + 100;
        JFrame game = new JFrame();
        game.setTitle("2048 Game");
        game.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        game.setSize(preferredWidth, preferredHeight);
        game.setResizable(false);
        game.setLayout(null); // Using absolute positioning

        Java2048 gamePanel = new Java2048(gridSize, targetScore, undoEnabled);
        gamePanel.setBounds(0, 0, preferredWidth, preferredHeight);
        game.add(gamePanel);

        game.setLocationRelativeTo(null);
        game.setVisible(true);
    }

    /**
     * Main method to launch the start screen
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StartScreen().setVisible(true));
    }
}
