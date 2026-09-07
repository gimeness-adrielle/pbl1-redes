package me.gimenez.socket.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {this.clientSocket = clientSocket;}

    @Override
    public void run(){
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String message = in.readLine();
            System.out.println("Client received: " + message);

            out.println("hello client!");

            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

