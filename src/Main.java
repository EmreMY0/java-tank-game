
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;


public class Main extends Application {
    private Pane root;
    private Pane gamePane;
    private PlayerTank player;
    private List<EnemyTank> enemies = new ArrayList<>();
    private List<Wall> walls = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<EnemyBullet> enemyBullets = new ArrayList<>();
    private int score = 0;
    private Text scoreText;
    private Text livesText;
    private boolean isPaused = false;
    private Timeline enemySpawner;
    private boolean isGameOver = false;
    private Text gameOverText;
    private Text pauseText;
    private Text pauseMenuText;
    private double cameraX = 0;
    private double cameraY = 0;
    private final double worldWidth = 1600;
    private final double worldHeight = 1200;
    private final double screenWidth = 1200; 
    private final double screenHeight = 900;  
    private AnimationTimer gameTimer; 

    public static void main(String[] args) {
        // Launches the application
        launch(args);
    }

    public void start(Stage stage) {
        // Initializes the game scene, player, enemies, walls and starts the game
        root = new Pane();
        root.setPrefSize(screenWidth, screenHeight);
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        gamePane = new Pane();
        root.getChildren().add(gamePane);

        createWalls();

        Image playerImg1 = new Image("file:yellowTank1.png");
        Image playerImg2 = new Image("file:yellowTank2.png");
        player = new PlayerTank(worldWidth / 2, worldHeight - 200, playerImg1, playerImg2);
        player.setWalls(walls);
        player.setParent(gamePane);
        player.setGame(this);
        gamePane.getChildren().addAll(player.getView(), player.getCollider());

        scoreText = new Text(50, 70, "Score: 0");
        scoreText.setFill(Color.WHITE);
        livesText = new Text(50, 90, "Lives: 3");
        livesText.setFill(Color.WHITE);
        pauseText = new Text(screenWidth / 2 - 50, screenHeight / 2, "PAUSED");
        pauseText.setFill(Color.YELLOW);
        pauseText.setFont(Font.font("Verdana", FontWeight.BOLD, 30));
        pauseText.setVisible(false);

        pauseMenuText = new Text(screenWidth / 2 - 100, screenHeight / 2 + 50, "Press R to Restart\nPress ESC to Exit");
        pauseMenuText.setFill(Color.YELLOW);
        pauseMenuText.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        pauseMenuText.setVisible(false);

        root.getChildren().addAll(scoreText, livesText, pauseText, pauseMenuText);

        startEnemySpawner();

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> {
            // Handles key press for game control (restart, exit, pause, player movement)
            if (e.getCode() == KeyCode.R) {
                if (isGameOver || isPaused) {
                    restartGame(stage);
                }
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                if (isGameOver || isPaused) {
                    stage.close();
                }
            }
            if (e.getCode() == KeyCode.P && !isGameOver) {
                togglePause();
            }
            if (!isPaused && !isGameOver) {
                player.handleKeyPress(e.getCode());
            }
        });
        scene.setOnKeyReleased(e -> player.handleKeyRelease(e.getCode()));

        gameTimer = new AnimationTimer() {
            public void handle(long now) {
                // Updates game state (player, camera , collisons) each frame if game is not paused or game over
                if (!isPaused && !isGameOver) {
                    player.update();
                    checkTankCollisions(); // Check for tank collisions
                    updateCamera(); 
                }
            }
        };
        gameTimer.start();

        stage.setTitle("Tank 2025");
        stage.setScene(scene);
        stage.show();
    }

    private void checkTankCollisions() {
        // Checks for collisions between player and enemy tanks
        if (player.isRespawning() || player.isInvincible()) return;
        for (EnemyTank enemy : new ArrayList<>(enemies)) {
            if (!enemy.isDestroyed() && player.getCollider().getBoundsInParent().intersects(enemy.getCollider().getBoundsInParent())) {
                player.hit(new Image("file:smallExplosion.png"), new Image("file:explosion.png"));
                enemy.destroy(new Image("file:smallExplosion.png"), new Image("file:explosion.png"));
                enemies.remove(enemy);
                gamePane.getChildren().removeAll(enemy.getView(), enemy.getCollider());
                increaseScore(100);
            }
        }
    }

    private void updateCamera() {
        // Adjusts camera position to follow the player within world bounds (Vertical and side scrolling)
        cameraX = player.getView().getX() + player.getView().getFitWidth() / 2 - screenWidth / 2;
        cameraY = player.getView().getY() + player.getView().getFitHeight() / 2 - screenHeight / 2;
        cameraX = Math.max(0, Math.min(cameraX, worldWidth - screenWidth));
        cameraY = Math.max(0, Math.min(cameraY, worldHeight - screenHeight));
        gamePane.setTranslateX(-cameraX);
        gamePane.setTranslateY(-cameraY);
    }

    private void createWalls() {
        // Creates walls (game boundaries and obstacles)
        for (int i = 0; i < worldWidth; i += 40) {
            walls.add(new Wall(i, 0));      
            walls.add(new Wall(i, worldHeight - 40)); 
        }
        for (int i = 40; i < worldHeight - 40; i += 40) {
            walls.add(new Wall(0, i));      
            walls.add(new Wall(worldWidth - 40, i)); 
        }

        for (int i = (int)worldWidth / 5; i < worldWidth - worldWidth / 5; i += 40) {
            walls.add(new Wall(i, worldHeight * 0.4));
            walls.add(new Wall(i, worldHeight * 0.4 + 80));
        }

        for (int i = (int)(worldHeight * 0.3); i < worldHeight - 360; i += 40) {
            walls.add(new Wall(worldWidth * 0.2, i));
            walls.add(new Wall(worldWidth * 0.8 - 40, i));
        }

        walls.add(new Wall(worldWidth * 0.4, worldHeight * 0.75));
        walls.add(new Wall(worldWidth * 0.4 + 40, worldHeight * 0.75));
        walls.add(new Wall(worldWidth * 0.6, worldHeight * 0.75));
        walls.add(new Wall(worldWidth * 0.6 - 40, worldHeight * 0.75));

        for (Wall wall : walls) {
            gamePane.getChildren().add(wall.getView());
        }
    }

    private void startEnemySpawner() {
        // Starts a timeline to periodically spawn enemy tanks
        enemySpawner = new Timeline(new KeyFrame(Duration.seconds(3 + new Random().nextDouble() * 3), e -> spawnEnemy()));
        enemySpawner.setCycleCount(Timeline.INDEFINITE);
        enemySpawner.play();
    }

    private void spawnEnemy() {
        // Spawns an enemy tank at a random valid position
        if (isPaused || isGameOver) return;

        double x, y;
        Random rand = new Random();
        do {
            x = rand.nextInt((int)worldWidth - 100) + 50;
            y = rand.nextInt((int)(worldHeight / 4) - 50) + 50;
        } while (collidesWithWalls(x, y));

        Image enemyImg1 = new Image("file:whiteTank1.png");
        Image enemyImg2 = new Image("file:whiteTank2.png");
        EnemyTank enemy = new EnemyTank(x, y, enemyImg1, enemyImg2);
        enemy.setWalls(walls);
        enemy.setParent(gamePane);
        enemies.add(enemy);
        gamePane.getChildren().addAll(enemy.getView(), enemy.getCollider());
        new EnemyAI(enemy, player, gamePane, walls,
            new Image("file:bullet.png"),
            new Image("file:smallExplosion.png"), 
            new Image("file:explosion.png"),     
            this);
    }

    private boolean collidesWithWalls(double x, double y) {
        // Checks if a position collides with any wall
        for (Wall wall : walls) {
            if (wall.getCollider().getBoundsInParent().intersects(x, y, 40, 40)) {
                return true;
            }
        }
        return false;
    }

    public void increaseScore(int points) {
        // Increases and updates the player's score
        score += points;
        scoreText.setText("Score: " + score);
    }

    public void updateLives(int lives) {
        // Updates the player's lives and triggers game over if lives reach zero
        livesText.setText("Lives: " + lives);
        if (lives <= 0 && !isGameOver) {
            gameOver();
        }
    }

    private void gameOver() {
        // Handles game over state, stopping enemy spawning and displaying game over text
        isGameOver = true;
        enemySpawner.stop();
        for (EnemyTank enemy : enemies) {
            enemy.pauseAI();
        }
        for (Bullet bullet : new ArrayList<>(bullets)) {
            bullet.pause();
        }
        for (EnemyBullet bullet : new ArrayList<>(enemyBullets)) {
            bullet.pause();
        }
        gameOverText = new Text(screenWidth / 2 - 150, screenHeight / 2, "GAME OVER\nScore: " + score + "\nPress R to Restart\nPress ESC to Exit");
        gameOverText.setFill(Color.RED);
        gameOverText.setFont(Font.font("Verdana", FontWeight.BOLD, 30));
        root.getChildren().add(gameOverText);
    }

    private void restartGame(Stage stage) {
        // Resets the game state and restarts the game
        if (gameTimer != null) {
            gameTimer.stop(); // Stop the existing timer to prevent stacking
        }
        gamePane.getChildren().clear();
        root.getChildren().remove(gameOverText);
        enemies.clear();
        walls.clear();
        bullets.clear();
        enemyBullets.clear();
        score = 0;
        isPaused = false;
        isGameOver = false;
        cameraX = 0; 
        start(stage);
    }

    private void togglePause() {
        // Toggles the pause state, pausing/resuming game elements
        isPaused = !isPaused;
        pauseText.setVisible(isPaused);
        pauseMenuText.setVisible(isPaused);
        if (isPaused) {
            enemySpawner.pause();
            for (EnemyTank enemy : enemies) {
                enemy.pauseAI();
            }
            for (Bullet bullet : new ArrayList<>(bullets)) {
                bullet.pause();
            }
            for (EnemyBullet bullet : new ArrayList<>(enemyBullets)) {
                bullet.pause();
            }
        } else {
            enemySpawner.play();
            for (EnemyTank enemy : enemies) {
                enemy.resumeAI();
            }
            for (Bullet bullet : new ArrayList<>(bullets)) {
                bullet.resume();
            }
            for (EnemyBullet bullet : new ArrayList<>(enemyBullets)) {
                bullet.resume();
            }
        }
    }

    public List<EnemyTank> getEnemies() {
        // Returns the list of enemy tanks
        return enemies;
    }

    public void addBullet(Bullet bullet) {
        // Adds a player bullet to the game
        bullets.add(bullet);
    }

    public void removeBullet(Bullet bullet) {
        // Removes a player bullet from the game
        bullets.remove(bullet);
    }

    public void addEnemyBullet(EnemyBullet bullet) {
        // Adds an enemy bullet to the game
        enemyBullets.add(bullet);
    }

    public void removeEnemyBullet(EnemyBullet bullet) {
        // Removes an enemy bullet from the game
        enemyBullets.remove(bullet);
    }
}

class Tank {
    protected ImageView imageView;
    protected Rectangle hitbox;
    protected Image image1;
    protected Image image2;
    protected boolean imageToggle = false;
    protected List<Wall> walls;
    protected static final double worldWidth = 1600;
    protected static final double worldHeight = 1200;

    public Tank(double x, double y, Image image1, Image image2) {
        // Initializes a tank with position, images, and hitbox
        this.image1 = image1;
        this.image2 = image2;
        imageView = new ImageView(image1);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        hitbox = new Rectangle(x + 5, y + 5, 50, 50);
        hitbox.setVisible(false);
    }

    public ImageView getView() {
        // Returns the tank's visual representation
        return imageView;
    }

    public Rectangle getCollider() {
        // Returns the tank's collision hitbox
        return hitbox;
    }

    public void setWalls(List<Wall> walls) {
        // Sets the list of walls for collision detection
        this.walls = walls;
    }

    public void move(double dx, double dy) {
        // Moves the tank if the new position is valid
        if (canMove(dx, dy)) {
            imageView.setX(imageView.getX() + dx);
            imageView.setY(imageView.getY() + dy);
            hitbox.setX(hitbox.getX() + dx);
            hitbox.setY(hitbox.getY() + dy);
            switchImage();
        }
    }

    public void rotateTo(int angle) {
        // Rotates the tank to the specified angle
        imageView.setRotate(angle);
    }

    protected void switchImage() {
        // Toggles between two tank images for animation
        imageToggle = !imageToggle;
        imageView.setImage(imageToggle ? image1 : image2);
    }

    protected boolean canMove(double dx, double dy) {
        // Checks if the tank can move to the new position without colliding
        double newX = hitbox.getX() + dx;
        double newY = hitbox.getY() + dy;
        if (newX < 0 || newX + hitbox.getWidth() > worldWidth || newY < 0 || newY + hitbox.getHeight() > worldHeight) {
            return false;
        }
        Rectangle nextPosition = new Rectangle(newX, newY, hitbox.getWidth(), hitbox.getHeight());
        for (Wall wall : walls) {
            if (nextPosition.getBoundsInParent().intersects(wall.getCollider().getBoundsInParent())) {
                return false;
            }
        }
        return true;
    }

    public void handleKeyInput(KeyCode code) {
        // Handles key input to move or rotate the tank
        if (code == KeyCode.UP) {
            rotateTo(270);
            move(0, -3); 
        } else if (code == KeyCode.DOWN) {
            rotateTo(90);
            move(0, 3); 
        } else if (code == KeyCode.LEFT) {
            rotateTo(180);
            move(-3, 0); 
        } else if (code == KeyCode.RIGHT) {
            rotateTo(0);
            move(3, 0); 
        }
    }
}

class PlayerTank extends Tank {
    private int lives = 3;
    private final double startX, startY;
    private Pane parent;
    private Main game;  
    private Timeline shootCooldown;
    private boolean isRespawning = false;
    private boolean isInvincible = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean shootPressed = false;
    private int animationCounter = 0; 
    private static final int ANIMATION_STEP = 3; 

    public PlayerTank(double x, double y, Image image1, Image image2) {
        // Initializes the player tank with lives and cooldown
        super(x, y, image1, image2);
        this.startX = x;
        this.startY = y;
        shootCooldown = new Timeline();
        imageView.setVisible(true);
        hitbox.setVisible(false);
    }

    public void setParent(Pane parent) {
        // Sets the parent pane for rendering
        this.parent = parent;
    }

    public void setGame(Main game) {  
        // Sets the game instance for interaction
        this.game = game;
    }

    public int getLives() {
        // Returns the player's remaining lives
        return lives;
    }

    public boolean isRespawning() {
        // Returns whether the player is respawning
        return isRespawning;
    }

    public boolean isInvincible() {
        // Returns whether the player is invincible
        return isInvincible;
    }

    public void hit(Image explosion1, Image explosion2) {
        // Handles the player being hit, reducing lives and triggering respawn
        if (isInvincible) return;

        lives--;
        game.updateLives(lives);
        if (lives > 0) {
            isRespawning = true;
            isInvincible = true;
            parent.getChildren().removeAll(imageView, hitbox);

            ImageView explosionView = new ImageView(explosion2);
            explosionView.setX(imageView.getX());
            explosionView.setY(imageView.getY());
            explosionView.setFitWidth(60);
            explosionView.setFitHeight(60);
            parent.getChildren().add(explosionView);

            Timeline explosionTimeline = new Timeline(
                new KeyFrame(Duration.millis(200), e -> parent.getChildren().remove(explosionView))
            );
            explosionTimeline.setCycleCount(1);
            explosionTimeline.play();

            Timeline stunInvincibleTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> {
                resetPosition();
                imageView.setVisible(true);
                hitbox.setVisible(false);
                parent.getChildren().addAll(imageView, hitbox);
                isRespawning = false;
                isInvincible = false;
            }));
            stunInvincibleTimer.setCycleCount(1);
            stunInvincibleTimer.play();
        }
    }

    private void resetPosition() {
        // Resets the player to the starting position
        imageView.setX(startX);
        imageView.setY(startY);
        hitbox.setX(startX + 5); 
        hitbox.setY(startY + 5);
        imageView.setRotate(0);
        imageView.setImage(image1);
        imageToggle = false; 
        animationCounter = 0; 
    }

    public void handleKeyPress(KeyCode code) {
        // Handles key press for player movement and shooting
        if (isRespawning || isInvincible) return;
        switch (code) {
            case UP:
                upPressed = true;
                break;
            case DOWN:
                downPressed = true;
                break;
            case LEFT:
                leftPressed = true;
                break;
            case RIGHT:
                rightPressed = true;
                break;
            case X:
                shootPressed = true;
                shoot();
                break;
        }
    }

    public void handleKeyRelease(KeyCode code) {
        // Handles key release for player movement and shooting
        switch (code) {
            case UP:
                upPressed = false;
                break;
            case DOWN:
                downPressed = false;
                break;
            case LEFT:
                leftPressed = false;
                break;
            case RIGHT:
                rightPressed = false;
                break;
            case X:
                shootPressed = false;
                break;
        }
    }

    public void update() {
        // Updates player movement based on pressed keys
        if (isRespawning || isInvincible) return;
        if (upPressed) {
            rotateTo(270);
            move(0, -3); 
        } else if (downPressed) {
            rotateTo(90);
            move(0, 3); 
        } else if (leftPressed) {
            rotateTo(180);
            move(-3, 0); 
        } else if (rightPressed) {
            rotateTo(0);
            move(3, 0); 
        }
        if (shootPressed) {
            shoot();
        }
    }

    private void shoot() {
        // Fires a bullet if the cooldown has expired
        if (shootCooldown.getStatus() == Animation.Status.STOPPED && !isRespawning && !isInvincible) {
            Bullet bullet = new Bullet(
                imageView.getX() + 20,
                imageView.getY() + 20,
                imageView.getRotate(),
                new Image("file:bullet.png"),
                new Image("file:smallExplosion.png"),
                new Image("file:explosion.png"),
                parent,
                walls,
                game.getEnemies()
            );
            bullet.setGame(game);
            game.addBullet(bullet);
            shootCooldown = new Timeline(new KeyFrame(Duration.millis(500), e -> {}));
            shootCooldown.setCycleCount(1);
            shootCooldown.play();
        }
    }

    @Override
    public void move(double dx, double dy) {
        // Moves the tank and controls wheel animation speed
        if (canMove(dx, dy)) {
            imageView.setX(imageView.getX() + dx);
            imageView.setY(imageView.getY() + dy);
            hitbox.setX(hitbox.getX() + dx);
            hitbox.setY(hitbox.getY() + dy);
            animationCounter++;
            if (animationCounter >= ANIMATION_STEP) {
                switchImage();
                animationCounter = 0; 
            }
        }
    }
}

class EnemyTank extends Tank {
    private boolean destroyed = false;
    private Pane parent;
    private EnemyAI ai;

    public EnemyTank(double x, double y, Image image1, Image image2) {
        // Initializes an enemy tank
        super(x, y, image1, image2);
    }

    public void setParent(Pane parent) {
        // Sets the parent pane for rendering
        this.parent = parent;
    }

    public void setAI(EnemyAI ai) {
        // Sets the AI for controlling the enemy tank
        this.ai = ai;
    }

    public boolean isDestroyed() {
        // Returns whether the enemy tank is destroyed
        return destroyed;
    }

    public void destroy(Image explosion1, Image explosion2) {
        // Destroys the enemy tank with an explosion animation
        if (destroyed) return;
        destroyed = true;
        parent.getChildren().removeAll(imageView, hitbox);

        ImageView explosionView = new ImageView(explosion2);
        explosionView.setX(imageView.getX());
        explosionView.setY(imageView.getY());
        explosionView.setFitWidth(60);
        explosionView.setFitHeight(60);
        parent.getChildren().add(explosionView);

        Timeline explosionTimeline = new Timeline(
            new KeyFrame(Duration.millis(200), e -> parent.getChildren().remove(explosionView))
        );
        explosionTimeline.setCycleCount(1);
        explosionTimeline.play();
    }

    public void pauseAI() {
        // Pauses the enemy AI
        if (ai != null) {
            ai.pause();
        }
    }

    public void resumeAI() {
        // Resumes the enemy AI
        if (ai != null) {
            ai.resume();
        }
    }
}

class Wall {
    private ImageView imageView;
    private Rectangle hitbox;

    public Wall(double x, double y) {
        // Initializes a wall with position and hitbox
        imageView = new ImageView("file:wall.png");
        imageView.setX(x);
        imageView.setY(y);
        imageView.setFitWidth(40);
        imageView.setFitHeight(40);
        hitbox = new Rectangle(x, y, 40, 40);
        hitbox.setVisible(false);
    }

    public ImageView getView() {
        // Returns the wall's visual representation
        return imageView;
    }

    public Rectangle getCollider() {
        // Returns the wall's collision hitbox
        return hitbox;
    }
}

class Bullet {
    protected ImageView imageView;
    protected ImageView explosionView;
    protected Rectangle hitbox;
    protected double dx, dy;
    protected boolean exploded = false;
    protected Timeline timeline;
    protected Image explosion1;
    protected Image explosion2;
    protected Pane parent;
    protected List<Wall> walls;
    protected List<EnemyTank> enemies;
    protected Main game;
    protected static final double worldWidth = 1600;
    protected static final double worldHeight = 1200;

    public Bullet(double x, double y, double angle, Image bulletImage,
                 Image exp1, Image exp2, Pane parent, List<Wall> walls, List<EnemyTank> enemies) {
        // Initializes a bullet with position, angle, and collision properties
        this.parent = parent;
        this.walls = walls;
        this.enemies = enemies;
        this.explosion1 = exp1;
        this.explosion2 = exp2;

        imageView = new ImageView(bulletImage);
        imageView.setFitWidth(13);
        imageView.setFitHeight(10);
        imageView.setRotate(angle);
        hitbox = new Rectangle(x, y, 10, 10);
        hitbox.setVisible(false);

        if (angle == 0) {
            dx = 5; dy = 0;
            imageView.setX(x + 35); imageView.setY(y + 5);
            hitbox.setX(x + 35); hitbox.setY(y + 5);
        } else if (angle == 90) {
            dx = 0; dy = 5;
            imageView.setX(x + 4); imageView.setY(y + 35);
            hitbox.setX(x + 4); hitbox.setY(y + 35);
        } else if (angle == 180) {
            dx = -5; dy = 0;
            imageView.setX(x - 25); imageView.setY(y + 5);
            hitbox.setX(x - 25); hitbox.setY(y + 5);
        } else if (angle == 270) {
            dx = 0; dy = -5;
            imageView.setX(x + 4); imageView.setY(y - 25);
            hitbox.setX(x + 4); hitbox.setY(y - 25);
        }

        parent.getChildren().addAll(imageView, hitbox);

        timeline = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            if (!exploded) {
                move();
                checkCollision();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void setGame(Main game) {
        // Sets the game instance for interaction
        this.game = game;
    }

    private void move() {
        // Moves the bullet based on its direction
        imageView.setX(imageView.getX() + dx);
        imageView.setY(imageView.getY() + dy);
        hitbox.setX(hitbox.getX() + dx);
        hitbox.setY(hitbox.getY() + dy);
    }

    private void checkCollision() {
        // Checks for bullet collisions with walls or enemies
        if (hitbox.getX() < 0 || hitbox.getX() >= worldWidth || hitbox.getY() < 0 || hitbox.getY() >= worldHeight) {
            explodeSmall(imageView.getX(), imageView.getY());
            return;
        }
        for (Wall wall : new ArrayList<>(walls)) {
            if (hitbox.getBoundsInParent().intersects(wall.getCollider().getBoundsInParent())) {
                explodeSmall(imageView.getX(), imageView.getY()); 
                return;
            }
        }
        for (EnemyTank enemy : new ArrayList<>(enemies)) {
            if (!enemy.isDestroyed() && hitbox.getBoundsInParent().intersects(enemy.getCollider().getBoundsInParent())) {
                explodeLargeOnTank(enemy);
                return;
            }
        }
    }

    protected void explodeLargeOnTank(EnemyTank enemy) {
        // Triggers a large explosion when hitting an enemy tank
        exploded = true;
        timeline.stop();
        parent.getChildren().removeAll(imageView, hitbox);
        game.removeBullet(this);
        enemy.destroy(explosion1, explosion2);
        game.increaseScore(100);
    }

    protected void explodeSmall(double x, double y) {
        // Triggers a small explosion when hitting a wall or boundary
        exploded = true;
        timeline.stop();
        parent.getChildren().removeAll(imageView, hitbox);
        game.removeBullet(this);

        explosionView = new ImageView(explosion1);
        explosionView.setX(x - 5);
        explosionView.setY(y - 5);
        explosionView.setFitWidth(30);
        explosionView.setFitHeight(30);
        parent.getChildren().add(explosionView);

        Timeline explosionTimeline = new Timeline(
            new KeyFrame(Duration.millis(100), e -> parent.getChildren().remove(explosionView))
        );
        explosionTimeline.setCycleCount(1);
        explosionTimeline.play();
    }

    public void pause() {
        // Pauses the bullet's movement
        timeline.pause();
    }

    public void resume() {
        // Resumes the bullet's movement if not exploded
        if (!exploded) {
            timeline.play();
        }
    }
}

class EnemyBullet {
    protected ImageView imageView;
    protected ImageView explosionView;
    protected Rectangle hitbox;
    protected double dx, dy;
    protected boolean exploded = false;
    protected Timeline timeline;
    protected Image explosion1;
    protected Image explosion2;
    protected Pane parent;
    protected List<Wall> walls;
    private PlayerTank player;
    protected Main game;
    protected static final double worldWidth = 1600;
    protected static final double worldHeight = 1200;

    public EnemyBullet(double x, double y, double angle, Image bulletImage, Image exp1, Image exp2,
                      Pane parent, List<Wall> walls, PlayerTank player) {
        // Initializes an enemy bullet with position, angle, and collision properties
        this.parent = parent;
        this.walls = walls;
        this.player = player;
        this.explosion1 = exp1;
        this.explosion2 = exp2;

        imageView = new ImageView(bulletImage);
        imageView.setFitWidth(13);
        imageView.setFitHeight(10);
        imageView.setRotate(angle);
        hitbox = new Rectangle(x, y, 10, 10);
        hitbox.setVisible(false);

        if (angle == 0) {
            dx = 5; dy = 0;
            imageView.setX(x + 35); imageView.setY(y + 5);
            hitbox.setX(x + 35); hitbox.setY(y + 5);
        } else if (angle == 90) {
            dx = 0; dy = 5;
            imageView.setX(x + 4); imageView.setY(y + 35);
            hitbox.setX(x + 4); hitbox.setY(y + 35);
        } else if (angle == 180) {
            dx = -5; dy = 0;
            imageView.setX(x - 25); imageView.setY(y + 5);
            hitbox.setX(x - 25); hitbox.setY(y + 5);
        } else if (angle == 270) {
            dx = 0; dy = -5;
            imageView.setX(x + 4); imageView.setY(y - 25);
            hitbox.setX(x + 4); hitbox.setY(y - 25);
        }

        parent.getChildren().addAll(imageView, hitbox);

        timeline = new Timeline(new KeyFrame(Duration.millis(20), e -> {
            if (!exploded) {
                move();
                checkCollision();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void setGame(Main game) {
        // Sets the game instance for interaction
        this.game = game;
    }

    private void move() {
        // Moves the enemy bullet based on its direction
        imageView.setX(imageView.getX() + dx);
        imageView.setY(imageView.getY() + dy);
        hitbox.setX(hitbox.getX() + dx);
        hitbox.setY(hitbox.getY() + dy);
    }

    protected void checkCollision() {
        // Checks for enemy bullet collisions with walls or the player
        if (exploded) return;
        if (hitbox.getX() < 0 || hitbox.getX() >= worldWidth || hitbox.getY() < 0 || hitbox.getY() >= worldHeight) {
            explodeSmall(imageView.getX(), imageView.getY());
            return;
        }
        for (Wall wall : new ArrayList<>(walls)) {
            if (hitbox.getBoundsInParent().intersects(wall.getCollider().getBoundsInParent())) {
                explodeSmall(imageView.getX(), imageView.getY()); 
                return;
            }
        }
        if (!exploded && !player.isRespawning() && !player.isInvincible() && 
            player.getCollider().getBoundsInParent().intersects(hitbox.getBoundsInParent())) {
            exploded = true;
            explodeLargeOnPlayer(imageView.getX(), imageView.getY());
            player.hit(explosion1, explosion2);
        }
    }

    protected void explodeLargeOnPlayer(double x, double y) {
        // Triggers a large explosion when hitting the player
        exploded = true;
        timeline.stop();
        parent.getChildren().removeAll(imageView, hitbox);
        game.removeEnemyBullet(this);

        explosionView = new ImageView(explosion2);
        explosionView.setX(x - 15);
        explosionView.setY(y - 15);
        explosionView.setFitWidth(60);
        explosionView.setFitHeight(60);
        parent.getChildren().add(explosionView);

        Timeline explosionTimeline = new Timeline(
            new KeyFrame(Duration.millis(200), e -> parent.getChildren().remove(explosionView))
        );
        explosionTimeline.setCycleCount(1);
        explosionTimeline.play();
    }

    protected void explodeSmall(double x, double y) {
        // Triggers a small explosion when hitting a wall or boundary
        exploded = true;
        timeline.stop();
        parent.getChildren().removeAll(imageView, hitbox);
        game.removeEnemyBullet(this);

        explosionView = new ImageView(explosion1);
        explosionView.setX(x - 5);
        explosionView.setY(y - 5);
        explosionView.setFitWidth(30);
        explosionView.setFitHeight(30);
        parent.getChildren().add(explosionView);

        Timeline explosionTimeline = new Timeline(
            new KeyFrame(Duration.millis(100), e -> parent.getChildren().remove(explosionView))
        );
        explosionTimeline.setCycleCount(1);
        explosionTimeline.play();
    }

    public void pause() {
        // Pauses the enemy bullet's movement
        timeline.pause();
    }

    public void resume() {
        // Resumes the enemy bullet's movement if not exploded
        if (!exploded) {
            timeline.play();
        }
    }
}

class EnemyAI {
    private EnemyTank enemy;
    private PlayerTank player;
    private Pane parent;
    private List<Wall> walls;
    private Image bulletImg, exp1, exp2;
    private Random random = new Random();
    private int moveCounter = 0;
    private int currentDirection = -1;
    private Timeline moveTimeline;
    private Timeline shootTimeline;
    private Main game;

    public EnemyAI(EnemyTank enemy, PlayerTank player, Pane parent, List<Wall> walls,
                  Image bulletImg, Image exp1, Image exp2, Main game) {
        // Initializes the AI for an enemy tank
        this.enemy = enemy;
        this.player = player;
        this.parent = parent;
        this.walls = walls;
        this.bulletImg = bulletImg;
        this.exp1 = exp1;
        this.exp2 = exp2;
        this.game = game;
        enemy.setAI(this);
        startAI();
    }

    private void startAI() {
        // Starts timelines for random movement and shooting
        moveTimeline = new Timeline(new KeyFrame(Duration.millis(50), e -> randomMove()));
        moveTimeline.setCycleCount(Timeline.INDEFINITE);
        moveTimeline.play();

        shootTimeline = new Timeline(new KeyFrame(Duration.seconds(1 + random.nextInt(5)), e -> shoot()));
        shootTimeline.setCycleCount(Timeline.INDEFINITE);
        shootTimeline.play();
    }

    private void randomMove() {
        // Moves the enemy tank in a random direction for a set duration
        if (enemy.isDestroyed()) return;
        if (moveCounter <= 0) {
            currentDirection = random.nextInt(4);
            moveCounter = 15 + random.nextInt(8);
        }

        switch (currentDirection) {
            case 0: enemy.rotateTo(270); enemy.move(0, -5); break;
            case 1: enemy.rotateTo(90); enemy.move(0, 5); break;
            case 2: enemy.rotateTo(180); enemy.move(-5, 0); break;
            case 3: enemy.rotateTo(0); enemy.move(5, 0); break;
        }
        moveCounter--;
    }

    private void shoot() {
        // Fires an enemy bullet if the tank is not destroyed
        if (!enemy.isDestroyed()) {
            EnemyBullet bullet = new EnemyBullet(
                enemy.getView().getX() + 20,
                enemy.getView().getY() + 20,
                enemy.getView().getRotate(),
                bulletImg, exp1, exp2,
                parent, walls, player
            );
            bullet.setGame(game);
            game.addEnemyBullet(bullet);
        }
    }

    public void pause() {
        // Pauses the enemy AI timelines
        moveTimeline.pause();
        shootTimeline.pause();
    }

    public void resume() {
        // Resumes the enemy AI timelines if the tank is not destroyed
        if (!enemy.isDestroyed()) {
            moveTimeline.play();
            shootTimeline.play();
        }
    }
}
