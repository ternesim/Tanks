package com.game.tanks.app;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Javafx extends Application {

    private static Map<ImageView, TranslateTransition> bulletsAnimations;
    private static List<ImageView> bulletsList;
    private static List<ImageView> bulletsListEnemy;
    private static ImageView enemyLifeBar;
    private static ImageView playerLifeBar;

    private static int port = 33;
    private static String host = "83.147.246.223";
    private static Socket socket;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;
    private static final Pane root = new Pane();
    private static ImageView player; 
    private static ImageView enemy;
    private static ImageView explosion;
    private static String userId;
    private static String name;

    public static void setUserId(String userId) {
        Javafx.userId = userId;
    }

    private enum Direction {
        LEFT, RIGHT
    }

    static int HEIGHT = 1042;
    static int WIDTH = 1042;
    static int LIFE_BAR_WIDTH = WIDTH / 3;

    static int PLAYER_SIZE = WIDTH / 10;
    static int BULLET_WIDTH = PLAYER_SIZE / 5;
    static double ANIMATION_SPEED = .2;

    @Override
    public void start(Stage stage) {
        serverAddressForm();
        try {
            Handshake();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Connection problem");
            stage.close();
            return;
        }
        Initialize();
        ReceivePacket();
        stage.setTitle("World of Tanks");
        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        Scene scene = bindKeys(root);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
           launch();
   }

    private void serverAddressForm() {
        Stage stage = new Stage();

        Text text = new Text();
        text.setText("Enter server IP address");
        text.setX(10);
        text.setY(20);

        TextField addressField = new TextField("83.147.246.223:33");
        addressField.setTranslateX(10);
        addressField.setTranslateY(30);

        TextField userNameField = new TextField("Unknown warrior");
        userNameField.setTranslateX(10);
        userNameField.setTranslateY(60);

        Button button = new Button("Connect");
        button.setTranslateX(10);
        button.setTranslateY(100);
        button.setOnAction(e -> {
            String[] portAndHost = addressField.getText().split(":");
            host = portAndHost[0];
            port = Integer.parseInt(portAndHost[1]);
            name = userNameField.getText();
            stage.close();
        });

        Group root = new Group(text, addressField, userNameField, button);
        Scene scene = new Scene(root, 200, 150);
        stage.setTitle("Server connection");
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static void Handshake() throws Exception {
        socket = new Socket(host, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "handshake");

        oos.writeObject(jsonObject.toString());
        oos.flush();
        String response = (String) ois.readObject();
        jsonObject = new JSONObject(response);
        if(jsonObject.has("status") && jsonObject.get("status").equals("200")) System.out.println("Connected");
        else throw new Exception("Connection failed");
    }

    private void Initialize() {
        initBackground(root);
        player = initPlayer(root);
        enemy = initEnemy(root);
        initPlayerHealthBar(root);
        initEnemyHealthBar(root);
        initBullets(root);
        initExplosion(root);

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "initialize");
            jsonObject.put("HEIGHT", HEIGHT);
            jsonObject.put("WIDTH", WIDTH);
            jsonObject.put("name", name);

            oos.writeObject(jsonObject.toString());
            oos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ReceivePacket() {
        Thread receiveThread = new Thread(() -> {
            try {
                String message = "";
                while (!message.equals("exit")) {
                    message = (String) ois.readObject();
                    JSONObject jsonObject = new JSONObject(message);
                    String type;

                    System.out.println(jsonObject);

                    if(!jsonObject.has("type"))  continue;

                    type = jsonObject.get("type").toString();

                    if (type.equals("wait")) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Waiting for another player");
                            alert.setHeaderText("Waiting for another player");
                            alert.show();
                        });
                    }

                    if(type.equals("health")) {
                        int playerHealth = jsonObject.getInt("enemy_health");
                        int enemyHealth = jsonObject.getInt("player_health");
                        if (playerHealth < 1) playerHealth = 1;
                        if (enemyHealth < 1) enemyHealth = 1;
                        playerLifeBar.setFitWidth(LIFE_BAR_WIDTH / 100.0 * playerHealth);
                        enemyLifeBar.setFitWidth(LIFE_BAR_WIDTH / 100.0 * enemyHealth);
                        if (playerHealth < 5) {
                            explosion.setX(player.getX() + player.getTranslateX());
                            explosion.setY(player.getY() + player.getTranslateY());
                        }
                        if (enemyHealth < 5) {
                            explosion.setX(enemy.getX() + enemy.getTranslateX());
                            explosion.setY(enemy.getY() + enemy.getTranslateY());
                        }
                    }

                    if(type.equals("move")) {
                        player.setX(Integer.parseInt(jsonObject.get("player_coord").toString()));
                        enemy.setX(Integer.parseInt(jsonObject.get("enemy_coord").toString()));
                    }
                    
                    if(type.equals("action")) {
                        movePlayerBullets(jsonObject);
                        moveEnemyBullets(jsonObject);
                    }

                    if (type.equals("statistics")) {
                        Platform.runLater(() -> {
                            int shots = jsonObject.getInt("shots");
                            int hits = jsonObject.getInt("hits");

                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Statistic");
                            alert.setHeaderText("Your statistic for this round");
                            alert.setContentText("Shot: " + shots + "\nHit: " + hits + "\nMissed: " + (shots - hits));
                            alert.setOnCloseRequest(event -> {
                                Platform.exit();
                                System.exit(0);
                            });
                            alert.showAndWait().ifPresent(response -> {
                                Platform.exit();
                                System.exit(0);
                            });
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receiveThread.start();
    }
    
    private static void movePlayerBullets(JSONObject jsonObject){
        if(jsonObject.has("CoordsBulletPlayerX") && jsonObject.has("CoordsBulletPlayerY")){
                            
            String coordX = jsonObject.get("CoordsBulletPlayerX").toString().replace("[", "").replace("]", "");
            String coordY = jsonObject.get("CoordsBulletPlayerY").toString().replace("[", "").replace("]", "");
            
            String[] x = coordX.split(",");
            String[] y = coordY.split(",");

            int size = x.length;
            int[] coordsX = new int[size];
            int[] coordsY = new int[size];
            for(int i=0; i<size; i++) {
                coordsX[i] = Integer.parseInt(x[i]);
                coordsY[i] = Integer.parseInt(y[i]);
            }

            for(int i=0; i<size; i++) {
                ImageView bullet = bulletsList.get(i);
                bulletsAnimations.get(bullet).stop();
                bullet.setTranslateY(0);

                bullet.setX(coordsX[i]);
                bullet.setY(coordsY[i]);
                bulletsAnimations.get(bullet).play();
            }
        }
    }

    private static void moveEnemyBullets(JSONObject jsonObject) {
        if(jsonObject.has("CoordsBullerEnemyX") && jsonObject.has("CoordsBullerEnemyY")){
            String coordX = jsonObject.get("CoordsBullerEnemyX").toString().replace("[", "").replace("]", "");
            String coordY = jsonObject.get("CoordsBullerEnemyY").toString().replace("[", "").replace("]", "");
            String[] x = coordX.split(",");
            String[] y = coordY.split(",");
            int size = x.length;
            int[] coordsX = new int[size];
            int[] coordsY = new int[size];
            for(int i=0; i<size; i++) {
                coordsX[i] = Integer.parseInt(x[i]);
                coordsY[i] = Integer.parseInt(y[i]);
            }
            
            for(int i=0; i<size; i++) {
                ImageView bullet = bulletsListEnemy.get(i);
                bulletsAnimations.get(bullet).stop();
                bullet.setTranslateY(0);
                
                bullet.setX(coordsX[i]);
                bullet.setY(coordsY[i]);
                bulletsAnimations.get(bullet).play();
            }
        }
    }

    private void SendPacket(Direction direction) throws Exception {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "move");
        if(direction == Direction.LEFT) jsonObject.put("direction", "left");
        else if(direction == Direction.RIGHT) jsonObject.put("direction", "right");
        jsonObject.put("coord", player.getTranslateX());
        oos.writeObject(jsonObject.toString());
        oos.flush();
    }

    private void SendPacket() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", userId);
        jsonObject.put("type", "action");
        oos.writeObject(jsonObject.toString());
        oos.flush();
    }

    private Scene bindKeys(Pane root) {
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        final boolean[] spaceIsPressed = {false};
        scene.setOnKeyPressed(event -> {
            try {
                if (event.getCode() == KeyCode.A) SendPacket(Direction.LEFT);
                else if(event.getCode() == KeyCode.D) SendPacket(Direction.RIGHT);
                else if (event.getCode() == KeyCode.SPACE) {
                    if (!spaceIsPressed[0]) {
                        SendPacket();
                        spaceIsPressed[0] = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                spaceIsPressed[0] = false;
            }
        });

        return scene;
    }

    private void initExplosion(Pane root) {
        ImageView imageView = new ImageView("fail.png");
        imageView.setX(-100);
        imageView.setY(-100);
        imageView.setFitWidth(PLAYER_SIZE);
        imageView.setFitHeight(PLAYER_SIZE);
        root.getChildren().add(imageView);
        explosion = imageView;
    }

    private void initPlayerHealthBar(Pane root) {
        ImageView border = new ImageView("border.png");

        int width = WIDTH / 3;
        int height = HEIGHT / 20;
        int marginY = 80;
        int marginX = 40;

        border.setFitWidth(width + 8);
        border.setFitHeight(height);
        border.setX(marginX);
        border.setY(HEIGHT - marginY);

        ImageView life = new ImageView("life.png");
        life.setFitWidth(width);
        life.setFitHeight(height);
        life.setX(marginX);
        life.setY(HEIGHT - marginY);
        playerLifeBar = life;

        root.getChildren().add(life);
        root.getChildren().add(border);
    }

    private void initEnemyHealthBar(Pane root) {
        ImageView border = new ImageView("border.png");

        int width = WIDTH / 3;
        int height = HEIGHT / 20;
        int marginY = 30;
        int marginX = 40;

        border.setFitWidth(width + 8);
        border.setFitHeight(height);
        border.setX(WIDTH - marginX - width);
        border.setY(marginY);

        ImageView life = new ImageView("life.png");
        life.setFitWidth(width);
        life.setFitHeight(height);
        life.setX(WIDTH - marginX - width);
        life.setY(marginY);
        enemyLifeBar = life;

        root.getChildren().add(life);
        root.getChildren().add(border);
    }

    private void initBullets(Pane root) {
        bulletsAnimations = new HashMap<>();
        bulletsList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ImageView bullet = new ImageView("playerBullet.png");
            bullet.setFitWidth(BULLET_WIDTH);
            bullet.setFitHeight(PLAYER_SIZE / 3.0);
            bullet.setX(-100);
            root.getChildren().add(bullet);

            TranslateTransition transition = new TranslateTransition(Duration.seconds(ANIMATION_SPEED), bullet);
            bulletsAnimations.put(bullet, transition);
            bulletsList.add(bullet);
        }

        bulletsListEnemy = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ImageView bullet = new ImageView("enemyBullet.png");
            bullet.setFitWidth(BULLET_WIDTH);
            bullet.setFitHeight(PLAYER_SIZE / 3.0);
            bullet.setX(-100);
            root.getChildren().add(bullet);

            TranslateTransition transition = new TranslateTransition(Duration.seconds(ANIMATION_SPEED), bullet);
            bulletsAnimations.put(bullet, transition);
            bulletsListEnemy.add(bullet);
        }
    }

    private void initBackground(Pane root) {
        try {
            ImageView backgroundImageView = new ImageView("/field.png");
            root.getChildren().add(backgroundImageView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ImageView initPlayer(Pane root) {
        ImageView player = new ImageView(new Image("player.png"));
        player.setX(WIDTH / 2.0);
        player.setY(HEIGHT - PLAYER_SIZE - 100);
        player.setFitWidth(PLAYER_SIZE);
        player.setFitHeight(PLAYER_SIZE);
        root.getChildren().add(player);
        return player;
    }

    private ImageView initEnemy(Pane root) {
        ImageView player = new ImageView(new Image("enemy.png"));
        player.setX(WIDTH / 2.0);
        player.setY(100);
        player.setFitWidth(PLAYER_SIZE);
        player.setFitHeight(PLAYER_SIZE);
        root.getChildren().add(player);
        return player;
    }
}