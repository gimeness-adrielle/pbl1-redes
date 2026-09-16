package me.gimenez;

import me.gimenez.socket.client.ClientApp;
import me.gimenez.socket.server.Server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        switch (args[0]) {
            case "server":
                Server server = new Server();
                server.start();
                break;

            case "client":
                ClientApp client = new ClientApp();
                client.start();
                break;
        }
    }
}
