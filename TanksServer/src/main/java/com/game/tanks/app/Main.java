package com.game.tanks.app;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


import com.game.tanks.server.Server;
import com.game.tanks.config.SocketsApplicationConfig;

public class Main {
    private static int port = 33;
    public static void main( String[] args ) {
        try {
            new Server().run(port);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
