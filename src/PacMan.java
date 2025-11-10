import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;
import java.awt.image.BufferedImage;

public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; // U D L R
        int velocityX = 0;
        int velocityY = 0;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        boolean updateDirection(char direction) {
            char prevDirection = this.direction;
            int prevX = this.x;
            int prevY = this.y;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                    return false;
                }
            }
            return this.x != prevX || this.y != prevY;
        }

        void updateVelocity() {
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -tileSize/4;
            }
            else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = tileSize/4;
            }
            else if (this.direction == 'L') {
                this.velocityX = -tileSize/4;
                this.velocityY = 0;
            }
            else if (this.direction == 'R') {
                this.velocityX = tileSize/4;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;

    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;


    private Image backgroundImage;
    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;
    private Image foodImage;
    private int foodWidth;
    private int foodHeight;

    // Knife Feature Variables
    private Image knifeImage;
    private Image pacmanUpKnifeImage;
    private Image pacmanDownKnifeImage;
    private Image pacmanLeftKnifeImage;
    private Image pacmanRightKnifeImage;
    private HashSet<Block> knives;
    private boolean hasWeapon = false;
    private int knifeCount = 0;

    //X = wall, O = skip, P = pac man, ' ' = food
    //Ghosts: b = blue, o = orange, p = pink, r = red
    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X  XXX",
        "O       bpo       O",
        "XXXX X XXXXX   XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX"
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    Block pacman;

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'}; //up down left right
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;

    private SoundManager soundManager;
    private static final String BACKGROUND_MUSIC = "audio/background.wav";
    private static final String FOOD_SOUND = "audio/food.wav";
    private static final String LIFE_LOST_SOUND = "audio/life_lost.wav";
    private static final String MOVE_SOUND = "audio/move.wav";
    private static final String FOOD_IMAGE_RESOURCE = "/goldFood.png";

    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.LIGHT_GRAY);
        addKeyListener(this);
        setFocusable(true);

        //load images
        backgroundImage = new ImageIcon(getClass().getResource("/background.png")).getImage();
        wallImage = new ImageIcon(getClass().getResource("/wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("/blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("/orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("/pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("/redGhost.png")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("/pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("/pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("/pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("/pacmanRight.png")).getImage();

        // Load knife images
        knifeImage = new ImageIcon(getClass().getResource("/knife.png")).getImage();
        pacmanUpKnifeImage = new ImageIcon(getClass().getResource("/pacmanUp-with-knife.png")).getImage();
        pacmanDownKnifeImage = new ImageIcon(getClass().getResource("/pacmanDown-with-knife.png")).getImage();
        pacmanLeftKnifeImage = new ImageIcon(getClass().getResource("/pacmanLeft-with-knife.png")).getImage();
        pacmanRightKnifeImage = new ImageIcon(getClass().getResource("/pacmanRight-with-knife.png")).getImage();

        ImageIcon foodIcon = new ImageIcon(getClass().getResource(FOOD_IMAGE_RESOURCE));
        foodImage = foodIcon.getImage();
        foodWidth = foodIcon.getIconWidth();
        foodHeight = foodIcon.getIconHeight();

        double maxFoodCoverage = 0.6;
        int maxFoodWidth = (int)Math.round(tileSize * maxFoodCoverage);
        int maxFoodHeight = (int)Math.round(tileSize * maxFoodCoverage);
        double widthScale = (double)maxFoodWidth / foodWidth;
        double heightScale = (double)maxFoodHeight / foodHeight;
        double scale = Math.min(1.0, Math.min(widthScale, heightScale));
        if (scale < 1.0) {
            foodWidth = Math.max(1, (int)Math.round(foodWidth * scale));
            foodHeight = Math.max(1, (int)Math.round(foodHeight * scale));
            foodImage = foodImage.getScaledInstance(foodWidth, foodHeight, Image.SCALE_SMOOTH);
        }


        loadMap();
        spawnKnives(3); // spawn 3 knives on the map

        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        //how long it takes to start timer, milliseconds gone between frames
        soundManager = new SoundManager();
        soundManager.playBackgroundLoop(BACKGROUND_MUSIC);

        //how long it takes to start timer, milliseconds gone between frames
        gameLoop = new Timer(50, this); //20fps (1000/50)
        gameLoop.start();

    }

    public void loadMap() {
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        ghosts = new HashSet<Block>();

        boolean[][] wallMatrix = new boolean[rowCount][columnCount];

        for (int r = 0 ; r < rowCount ; r++) {
            for (int c = 0 ; c < columnCount ; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);

                int x = c*tileSize;
                int y = r*tileSize;

                if (tileMapChar == 'X') {
                    wallMatrix[r][c] = true;
                }
                else if (tileMapChar == 'b') {
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'o') {
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'p') {
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'r') {
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'P') {
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                }
                else if (tileMapChar == ' ') {
                    int foodX = x + (tileSize - foodWidth) / 2;
                    int foodY = y + (tileSize - foodHeight) / 2;
                    Block food = new Block(foodImage, foodX, foodY, foodWidth, foodHeight);
                    foods.add(food);
                }
            }
        }
        for (int r = 0 ; r < rowCount ; r++) {
            for (int c = 0 ; c < columnCount ; c++) {
                if (!wallMatrix[r][c]) {
                    continue;
                }

                int x = c*tileSize;
                int y = r*tileSize;
                Image connectedWallImage = createWallTexture(wallMatrix, r, c);
                Block wall = new Block(connectedWallImage, x, y, tileSize, tileSize);
                walls.add(wall);
            }
        }
    }

    public void spawnKnives(int count) {
        knives = new HashSet<>();

        // Convert foods HashSet to array for random selection
        Block[] foodArray = foods.toArray(new Block[0]);

        // If fewer foods than requested knives, limit the count
        count = Math.min(count, foodArray.length);

        int created = 0;
        while (created < count) {
            int index = random.nextInt(foodArray.length);
            Block chosenFood = foodArray[index];

            // Only replace if it still exists in foods (not already replaced)
            if (foods.contains(chosenFood)) {
                // Create a knife exactly where the food was
                Block knife = new Block(knifeImage, chosenFood.x, chosenFood.y, tileSize / 2, tileSize / 2);
                knives.add(knife);
                foods.remove(chosenFood);
                created++;
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        for (Block food : foods) {
            g.drawImage(food.image, food.x, food.y, food.width, food.height, null);
        }

        for (Block knife : knives) {
            g.drawImage(knife.image, knife.x, knife.y, knife.width, knife.height, null);
        }

        for (Block ghost : ghosts){
            g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }

        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        //for score

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf(score), tileSize/2, tileSize/2);
        }
        else{
            g.drawString("x" + lives + " Score: " + score + " Knives: " + knifeCount, tileSize/2, tileSize/2);
        }
    }

    public void move() {
        // Move Pac-Man
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // Wall/frame checks for Pac-Man
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }
        // Frame checks
        if (pacman.x < 0) pacman.x = 0;
        if (pacman.y < 0) pacman.y = 0;
        if (pacman.x + pacman.width > boardWidth) pacman.x = boardWidth - pacman.width;
        if (pacman.y + pacman.height > boardHeight) pacman.y = boardHeight - pacman.height;

        // --- Ghost collision (with knife logic) ---
        Block ghostToRemove = null;
        for (Block ghost : ghosts) {
            if (collision(ghost, pacman)) {
                if (hasWeapon && knifeCount > 0) {
                    knifeCount--;
                    ghostToRemove = ghost;
                    if (knifeCount == 0) {
                        hasWeapon = false;
                        updatePacmanImage();
                    }
                    break; // Only handle 1 collision per tick
                } else {
                    lives -= 1;
                    if (soundManager != null) soundManager.playEffect(LIFE_LOST_SOUND);
                    if (lives == 0) {
                        gameOver = true;
                        return; // End game, stop updating
                    }
                    resetPositions();
                    return; // Immediately exit move(), so Pac-Man resets at center
                }
            }
        }
        if (ghostToRemove != null) ghosts.remove(ghostToRemove);

        // Move ghosts (do this after handling Pac-Man collision)
        for (Block ghost : ghosts) {
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            boolean collided = false;
            // Walls
            for (Block wall : walls) {
                if (collision(ghost, wall)) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    collided = true;
                    break;
                }
            }
            // Frame checks
            if (ghost.x < 0) {
                ghost.x = 0;
                collided = true;
            } else if (ghost.x + ghost.width > boardWidth) {
                ghost.x = boardWidth - ghost.width;
                collided = true;
            }
            if (ghost.y < 0) {
                ghost.y = 0;
                collided = true;
            } else if (ghost.y + ghost.height > boardHeight) {
                ghost.y = boardHeight - ghost.height;
                collided = true;
            }

            if (collided) {
                char newDirection = directions[random.nextInt(directions.length)];
                ghost.updateDirection(newDirection);
            }
        }

        // Food check as before...
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                foodEaten = food;
                score += 10;
                if (soundManager != null) {
                    soundManager.playEffect(FOOD_SOUND);
                }
            }
        }
        if (foodEaten != null) foods.remove(foodEaten);

        // Knife pickup as before...
        Block knifeCollected = null;
        for (Block knife : knives) {
            if (collision(pacman, knife)) {
                knifeCollected = knife;
                hasWeapon = true;
                knifeCount++;
                updatePacmanImage();
            }
        }
        if (knifeCollected != null) knives.remove(knifeCollected);

        // Food collection cleanup (handled above)

        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
            spawnKnives(3);
        }
    }
    public boolean collision(Block a, Block b) {
        return  a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
        }

    public void resetPositions() {
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        pacman.direction = 'R';
        updatePacmanImage();

        // Re-load ghosts to their original state/positions
        ghosts.clear();
        for (int r = 0 ; r < rowCount ; r++) {
            for (int c = 0 ; c < columnCount ; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);

                int x = c * tileSize;
                int y = r * tileSize;

                if (tileMapChar == 'b') {
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                } else if (tileMapChar == 'o') {
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                } else if (tileMapChar == 'p') {
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                } else if (tileMapChar == 'r') {
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
            }
        }

        // Give ghosts random directions
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }

        spawnKnives(3); // (if you want knives to respawn as well)
    }

    private void updatePacmanImage() {
        if (hasWeapon && knifeCount > 0) {
            // assign to with-knife sprite based on direction
            if (pacman.direction == 'U') pacman.image = pacmanUpKnifeImage;
            else if (pacman.direction == 'D') pacman.image = pacmanDownKnifeImage;
            else if (pacman.direction == 'L') pacman.image = pacmanLeftKnifeImage;
            else if (pacman.direction == 'R') pacman.image = pacmanRightKnifeImage;
        } else {
            // assign to normal sprite based on direction
            if (pacman.direction == 'U') pacman.image = pacmanUpImage;
            else if (pacman.direction == 'D') pacman.image = pacmanDownImage;
            else if (pacman.direction == 'L') pacman.image = pacmanLeftImage;
            else if (pacman.direction == 'R') pacman.image = pacmanRightImage;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) {
            loadMap();
            resetPositions();
            spawnKnives(3);
            hasWeapon = false;
            knifeCount = 0;
            lives = 3;
            score = 0;
            gameOver = false;
            gameLoop.start();
        }
        boolean moved = false;
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            moved = pacman.updateDirection('U');
        }
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            moved = pacman.updateDirection('D');
        }
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            moved = pacman.updateDirection('L');
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            moved = pacman.updateDirection('R');
        }

        if (moved && soundManager != null) {
            soundManager.playEffect(MOVE_SOUND);
        }
        updatePacmanImage();
    }

    private Image createWallTexture(boolean[][] wallMatrix, int row, int column) {
        BufferedImage texture = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = texture.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color baseShadow = new Color(70, 50, 20);        // deep warm brown shadow
        Color baseLight = new Color(235, 190, 90);       // main soft yellow light
        Color innerHighlight = new Color(255, 220, 130); // bright lamp glow tone
        Color innerShadow = new Color(120, 90, 40);      // muted golden-brown shadow
        Color accentBright = new Color(255, 210, 100);   // accent light highlight
        Color accentDark = new Color(180, 130, 50);      // rich amber tone
        Color accentHighlight = new Color(255, 235, 180); // warm sunlight reflection


        if (wallImage != null) {
            graphics2D.drawImage(wallImage, 0, 0, tileSize, tileSize, null);
        }

        GradientPaint basePaint = new GradientPaint(0, 0, baseShadow, tileSize, tileSize, baseLight);
        graphics2D.setPaint(basePaint);
        graphics2D.fillRect(0, 0, tileSize, tileSize);

        int borderThickness = Math.max(3, tileSize / 9);
        int accentThickness = Math.max(3, tileSize / 8);
        int cornerDiameter = borderThickness * 2;

        boolean hasTop = row > 0 && wallMatrix[row - 1][column];
        boolean hasBottom = row < rowCount - 1 && wallMatrix[row + 1][column];
        boolean hasLeft = column > 0 && wallMatrix[row][column - 1];
        boolean hasRight = column < columnCount - 1 && wallMatrix[row][column + 1];


        int innerWidth = Math.max(0, tileSize - borderThickness * 2);
        int innerHeight = Math.max(0, tileSize - borderThickness * 2);
        if (innerWidth > 0 && innerHeight > 0) {
            GradientPaint innerPaint = new GradientPaint(0, borderThickness, innerShadow, 0, tileSize - borderThickness, innerHighlight);
            graphics2D.setPaint(innerPaint);
            graphics2D.fillRoundRect(borderThickness, borderThickness, innerWidth, innerHeight, cornerDiameter, cornerDiameter);

            graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            graphics2D.setPaint(new GradientPaint(0, tileSize / 4f, accentBright, 0, tileSize * 3 / 4f, innerShadow));
            graphics2D.fillRoundRect(borderThickness, borderThickness, innerWidth, innerHeight, cornerDiameter, cornerDiameter);
            graphics2D.setComposite(AlphaComposite.SrcOver);

            graphics2D.setColor(new Color(255, 217, 89));
            graphics2D.setStroke(new BasicStroke(Math.max(1, tileSize / 32f)));
            graphics2D.drawRoundRect(borderThickness, borderThickness, innerWidth, innerHeight, cornerDiameter, cornerDiameter);

            int accentLineWidth = Math.max(1, tileSize / 18);
            graphics2D.setColor(new Color(1, 8, 1));
            graphics2D.fillRect(tileSize / 3 - accentLineWidth / 2, borderThickness + accentThickness, accentLineWidth, innerHeight - accentThickness * 2);
            graphics2D.fillRect(tileSize * 2 / 3 - accentLineWidth / 2, borderThickness + accentThickness, accentLineWidth, innerHeight - accentThickness * 2);
        }

        Stroke originalStroke = graphics2D.getStroke();
        graphics2D.setStroke(new BasicStroke(Math.max(1f, accentThickness / 3f)));

        if (!hasTop) {
            graphics2D.setPaint(new GradientPaint(0, 0, accentBright, 0, accentThickness, accentDark));
            graphics2D.fillRect(0, 0, tileSize, accentThickness);

            int segmentWidth = Math.max(3, tileSize / 6);
            int gap = Math.max(2, segmentWidth / 2);
            int yOffset = Math.max(1, accentThickness / 3);
            for (int x = 0; x < tileSize; x += segmentWidth + gap) {
                int width = Math.min(segmentWidth, tileSize - x);
                graphics2D.setColor(accentHighlight);
                graphics2D.fillRect(x, yOffset, width, Math.max(1, accentThickness / 3));
                graphics2D.setColor(accentDark);
                graphics2D.drawLine(x, accentThickness - 1, x + width, accentThickness - 1);
            }
        } else {
            graphics2D.setColor(new Color(44, 16, 94));
            graphics2D.fillRect(0, 0, tileSize, Math.max(2, accentThickness / 3));
        }
        if (!hasBottom) {
            graphics2D.setPaint(new GradientPaint(0, tileSize - accentThickness, accentDark, 0, tileSize, accentBright));
            graphics2D.fillRect(0, tileSize - accentThickness, tileSize, accentThickness);

            int segmentWidth = Math.max(3, tileSize / 6);
            int gap = Math.max(2, segmentWidth / 2);
            int yOffset = tileSize - accentThickness + Math.max(1, accentThickness / 4);
            for (int x = 0; x < tileSize; x += segmentWidth + gap) {
                int width = Math.min(segmentWidth, tileSize - x);
                graphics2D.setColor(accentHighlight);
                graphics2D.fillRect(x, yOffset, width, Math.max(1, accentThickness / 3));
                graphics2D.setColor(accentDark.darker());
                graphics2D.drawLine(x, tileSize - 1, x + width, tileSize - 1);
            }
        } else {
            graphics2D.setColor(new Color(44, 16, 94));
            graphics2D.fillRect(0, tileSize - Math.max(2, accentThickness / 3), tileSize, Math.max(2, accentThickness / 3));
        }
        if (!hasLeft) {
            graphics2D.setPaint(new GradientPaint(0, 0, accentBright, accentThickness, 0, accentDark));
            graphics2D.fillRect(0, 0, accentThickness, tileSize);

            int segmentHeight = Math.max(3, tileSize / 6);
            int gap = Math.max(2, segmentHeight / 2);
            int xOffset = Math.max(1, accentThickness / 3);
            for (int y = 0; y < tileSize; y += segmentHeight + gap) {
                int height = Math.min(segmentHeight, tileSize - y);
                graphics2D.setColor(accentHighlight);
                graphics2D.fillRect(xOffset, y, Math.max(1, accentThickness / 3), height);
                graphics2D.setColor(accentDark);
                graphics2D.drawLine(accentThickness - 1, y, accentThickness - 1, y + height);
            }
        } else {
            graphics2D.setColor(new Color(44, 16, 94));
            graphics2D.fillRect(0, 0, Math.max(2, accentThickness / 3), tileSize);
        }

        if (!hasRight) {
            graphics2D.setPaint(new GradientPaint(tileSize - accentThickness, 0, accentDark, tileSize, 0, accentBright));
            graphics2D.fillRect(tileSize - accentThickness, 0, accentThickness, tileSize);

            int segmentHeight = Math.max(3, tileSize / 6);
            int gap = Math.max(2, segmentHeight / 2);
            int xOffset = tileSize - accentThickness + Math.max(1, accentThickness / 4);
            for (int y = 0; y < tileSize; y += segmentHeight + gap) {
                int height = Math.min(segmentHeight, tileSize - y);
                graphics2D.setColor(accentHighlight);
                graphics2D.fillRect(xOffset, y, Math.max(1, accentThickness / 3), height);
                graphics2D.setColor(accentDark.darker());
                graphics2D.drawLine(tileSize - 1, y, tileSize - 1, y + height);
            }
        } else {
            graphics2D.setColor(new Color(44, 16, 94));
            graphics2D.fillRect(tileSize - Math.max(2, accentThickness / 3), 0, Math.max(2, accentThickness / 3), tileSize);
        }

        graphics2D.setStroke(originalStroke);

        graphics2D.dispose();
        return texture;
    }
}