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
                try {
                    ClientApp client = new ClientApp();
                    client.start();
                } catch (IOException e) {
                    System.out.println("Não foi possível conectar ao servidor.");
                    System.out.println("Verifique se o servidor está disponível.");
                }
                break;
        }
    }
}
