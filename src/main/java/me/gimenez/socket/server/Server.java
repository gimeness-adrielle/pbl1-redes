package me.gimenez.socket.server;

import java.io.*;
import java.net.*;

public class Server {

    public void start () throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);

        System.out.println("Server started on port 5000");

        while (true) {
            Socket clientSocket = serverSocket.accept();

            new Thread(new ClientHandler(clientSocket)).start();
        }

    }
}
