package me.gimenez.server.socket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;

@Component
public class Server {
    private final HandlerFactory handlerFactory;

    private final int serverPort;

    public Server(HandlerFactory handlerFactory,
                  @Value("${socket.port:7071}") int serverPort
    ) {
        this.handlerFactory = handlerFactory;
        this.serverPort = serverPort;
    }

    public void start () {
        try (ServerSocket serverSocket = new ServerSocket(serverPort)){
            System.out.println("Server started on port " +  serverPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();


                ClientHandler handler = handlerFactory.createHandler(clientSocket);
                new Thread(handler).start();

                System.out.println("Client connected.");
            }

        }catch (SocketTimeoutException e){
            System.out.println("Server timed out.");
        }catch (IOException e){
            System.out.println("Cannot start server." + e);
        }

    }
}
