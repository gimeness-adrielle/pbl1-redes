package me.gimenez;

import me.gimenez.client.view.ClientApp;
import me.gimenez.exceptions.ServerUnavailableException;
import me.gimenez.server.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class Main {
    public static void main(String[] args) throws IOException {
        switch (args[0]) {
            case "server":
                initServer(args);
                break;
            case "client":
                initClient();
                break;
        }
    }

    private static void initServer(String[] args){
        var context = SpringApplication.run(Main.class, args);
        Server server = context.getBean(Server.class);

        // Start ServerSocket in another thread to run Spring Boot Application and ServerSocket together.
        // ServerSocket is for client-server communication and Spring Boot manages the REST API used for Peer-to-Peer.
        new Thread(server::start).start();
    }

    private static void initClient(){
        try {
            ClientApp clientApp = new ClientApp();
            clientApp.init();
        } catch (ServerUnavailableException e) {
            System.out.println("\n[ERRO] Não foi possível conectar ao servidor.");
            System.out.println("Detalhe: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Ocorreu um erro inesperado no cliente: " + e.getMessage());
        }
    }
}
