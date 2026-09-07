package me.gimenez.socket.client;

import java.io.*;
import java.net.Socket;

public class Client {
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) throws IOException {
        Socket clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String sendMessage(String message) throws IOException {
        out.println(message);
        return in.readLine();
    }

}
