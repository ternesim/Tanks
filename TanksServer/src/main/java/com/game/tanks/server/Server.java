package com.game.tanks.server;

import org.springframework.stereotype.Component;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

@Component("Server")
public class Server {
    public void run(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("\033[33mServer started: " + serverSocket + "\033[0m");
            while (true) {
                Socket clientSocket = serverSocket.accept(); 
                System.out.println("\033[32mClient connected: " + clientSocket + "\033[0m");
                
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
