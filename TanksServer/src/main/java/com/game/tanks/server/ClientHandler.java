package com.game.tanks.server;

import com.game.tanks.services.*;
import com.game.tanks.config.SocketsApplicationConfig;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.*;
import java.net.*;
import java.nio.channels.Channel;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.hibernate.Session;
import org.json.JSONObject;

// import com.jcraft.jsch.*;


public class ClientHandler extends Thread {
    private static ApplicationContext context = new AnnotationConfigApplicationContext(SocketsApplicationConfig.class);
    private StatisticsService statisticsService = (StatisticsService) context.getBean("StatisticsServiceImpl");

    private Socket clientSocket;
    private static Map<Integer,Player> players = new HashMap<>();

    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    private static int HEIGHT = 1042;
    private static int WIDTH = 1042;
    private static int UserId = 0;
    private static int SPEED = 100;
    private static int marginY = 100;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            Handshake();
            ReceivePacket();
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void Handshake() throws Exception{
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ois = new ObjectInputStream(clientSocket.getInputStream());

            String request = (String) ois.readObject();
            JSONObject jsonObject = new JSONObject(request);
            jsonObject.put("status", "200");
            if(players.size() >= 2) jsonObject.put("status", "400");

            oos.writeObject(jsonObject.toString());
            oos.flush();

            if(players.size() >= 2) throw new Exception("Too many players");
    }

    private void ReceivePacket() {
        Thread sendThread = new Thread(() -> {
            try {
                while(clientSocket.isConnected() && !clientSocket.isOutputShutdown()) {
                    JSONObject jsonObject = new JSONObject((String) ois.readObject());
                    String type = jsonObject.get("type").toString();
                    System.out.println(jsonObject);
                    if(type.equals("initialize")) {
                        HEIGHT =(int) jsonObject.get("HEIGHT");
                        WIDTH = (int)jsonObject.get("WIDTH");
                        players.put(++UserId, new Player(jsonObject.get("name").toString(),oos, ois));
                    }

                    if(UserId != 2) {
                        JSONObject jsonObject_new = new JSONObject();
                        jsonObject_new.put("type", "wait");
                        oos.writeObject(jsonObject_new.toString());
                        continue;
                    }

                    if(type.equals("move")) {
                        for(Map.Entry<Integer, Player> entry : players.entrySet()) {
                            if(oos == entry.getValue().getOos()) entry.getValue().PlayerMove(jsonObject.get("direction").toString());
                            else entry.getValue().EnemyMove(jsonObject.get("direction").toString());
                        }
                    }

                    if(type.equals("action")) {
                        for(Map.Entry<Integer, Player> entry : players.entrySet()) {
                            if(oos == entry.getValue().getOos()) entry.getValue().PlayerShoot();
                            else entry.getValue().EnemyShoot();
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("\033[31mClient disconnected: " + clientSocket + "\033[0m");
                players.remove(UserId);
                UserId--;
                for(Map.Entry<Integer, Player> entry : players.entrySet()) entry.getValue().SendEndPacket("win");
            } catch (Exception e) {
                // e.printStackTrace();
            }
        });
        sendThread.start();
    }

    private class Player {
        private List<Bullet> PlayerBullets = new ArrayList<>();
        private List<Bullet> EnemyBullets = new ArrayList<>();
        private int player_coord = (WIDTH / 10)*4;
        private int enemy_coord =  (WIDTH / 10)*4;
        private int PLAYER_SIZE = WIDTH / 10;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        private int player_health = 100;
        private int enemy_health =  100;
        private int shots = 0;
        private int hits = 0;
        private String name;


        public Player(String name, ObjectOutputStream oos, ObjectInputStream ois) {
            this.oos = oos;
            this.ois = ois;
            this.name = name;
            try {
                InitCoords();
                InitCoordsBullet();
                InitHealthBar();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        public void PlayerMove(String direction) throws Exception {
            if(direction.equals("left") && player_coord > 0)  player_coord -= PLAYER_SIZE;
            if(direction.equals("right") && player_coord < WIDTH - 2*PLAYER_SIZE) player_coord += PLAYER_SIZE;
            InitCoords();
        }

        public void EnemyMove(String direction) throws Exception {
            if(direction.equals("left") && enemy_coord > 0)  enemy_coord -= PLAYER_SIZE;
            if(direction.equals("right") && enemy_coord < WIDTH - 2*PLAYER_SIZE) enemy_coord += PLAYER_SIZE;
            InitCoords();
        }

        public ObjectOutputStream getOos() {
            return oos;
        }

        private void SendEndPacket(String res){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "statistics");
            jsonObject.put("shots", shots);
            jsonObject.put("hits", hits);
            jsonObject.put("result", res);
            try {
                oos.writeObject(jsonObject.toString());
                oos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(res.equals("win")) statisticsService.saveStatistic(name, shots, hits, shots - hits);
        }

        private void PlayerShoot(){
            PlayerBullets.add(new Bullet(player_coord, HEIGHT));
            shots++;
            if(PlayerBullets.size() >= 50) PlayerBullets.remove(0);
        }

        private void EnemyShoot(){
             EnemyBullets.add(new Bullet(enemy_coord, 0));
            if(EnemyBullets.size() >= 50) EnemyBullets.remove(0);
        }

        private void InitCoords() throws Exception{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "move");
            jsonObject.put("player_coord", player_coord);
            jsonObject.put("enemy_coord", enemy_coord);
            oos.writeObject(jsonObject.toString());
            oos.flush();
        }

        private void InitHealthBar() throws Exception {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "health");
            jsonObject.put("player_health", player_health);
            jsonObject.put("enemy_health", enemy_health);
            oos.writeObject(jsonObject.toString());
            oos.flush();
            
            if(player_health == 0 ) SendEndPacket("lose");
            if(enemy_health  == 0 ) SendEndPacket("win");
        }
        private void InitCoordsBullet() {
            Thread sendThread = new Thread(() -> {
                try {
                    while(clientSocket.isConnected() && !clientSocket.isOutputShutdown()) {

                        int[] coordsX = new int[PlayerBullets.size()];
                        int[] coordsY = new int[PlayerBullets.size()];
                        int i=0;
                        for(Bullet bullet : PlayerBullets) {
                            coordsX[i] = bullet.GetX();
                            coordsY[i] = bullet.GetY();
                            bullet.BulletMove("up");
                            i++;
                        }
                        if(PlayerBullets.size() != 0){
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("type", "action");
                            jsonObject.put("CoordsBulletPlayerX", coordsX);
                            jsonObject.put("CoordsBulletPlayerY", coordsY);
                            oos.writeObject(jsonObject.toString());
                            oos.flush();
                        }

                        coordsX = new int[EnemyBullets.size()];
                        coordsY = new int[EnemyBullets.size()];
                        i=0;
                        for(Bullet bullet : EnemyBullets) {
                            coordsX[i] = bullet.GetX();
                            coordsY[i] = bullet.GetY();
                            bullet.BulletMove("down");
                            i++;
                        }
                        if(EnemyBullets.size() != 0) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("type", "action");
                            jsonObject.put("CoordsBullerEnemyX", coordsX);
                            jsonObject.put("CoordsBullerEnemyY", coordsY);
                            oos.writeObject(jsonObject.toString());
                            oos.flush();
                        }
                        Thread.sleep(SPEED);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            sendThread.start();
        }

        private class Bullet {
            private int coordX = 0;
            private int coordY = 0;
            private int BULLET_WIDTH = PLAYER_SIZE / 5;
            private int BULLET_HEIGHT = BULLET_WIDTH * 2;
            private int damage = 5;

            public Bullet(int x, int y) {
                coordX = x + (PLAYER_SIZE - BULLET_WIDTH) / 2;
                if(y == 0) coordY = PLAYER_SIZE + marginY;
                if(y == HEIGHT) coordY = HEIGHT - BULLET_HEIGHT - PLAYER_SIZE - marginY;
            }

            public int GetX() {
                return coordX;
            }

            public int GetY() {
                return coordY;
            }
            
            public void BulletMove(String direction) throws Exception {
                if(direction.equals("up") && coordY > 0 - BULLET_HEIGHT) {
                    if(coordX >= enemy_coord && coordX <= enemy_coord + PLAYER_SIZE && coordY >= marginY && coordY <= PLAYER_SIZE + marginY) {
                        coordY = 0 - BULLET_HEIGHT;
                        player_health -= damage;
                        hits++;
                        InitHealthBar();
                    }
                    else coordY -= BULLET_HEIGHT;
                }
                if(direction.equals("down") && coordY < HEIGHT) {
                    if(coordX >= player_coord && coordX <= player_coord + PLAYER_SIZE && coordY >= HEIGHT - PLAYER_SIZE - marginY -BULLET_HEIGHT && coordY <= HEIGHT - marginY) {
                        coordY = HEIGHT + BULLET_HEIGHT;
                        enemy_health -= damage;
                        InitHealthBar();
                    }
                    else coordY += BULLET_HEIGHT;
                }
            }
        }
    }
}