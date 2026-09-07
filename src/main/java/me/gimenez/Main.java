package me.gimenez;

import me.gimenez.socket.server.Server;

import java.io.IOException;

public class Main {
    static void main() throws IOException {
        Server server = new Server();
        server.start();
    }
}
