package me.gimenez.socket.client;

import java.io.IOException;
import java.util.Scanner;

public class ClientApp {

    private static final Scanner sc =  new Scanner(System.in);
    private final DriverApp driverApp;

    public ClientApp() {
        Client client = null;
        try {
            client = new Client("localhost", 5000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.driverApp = new DriverApp(client);
    }

    static void main(String[] args) {
        ClientApp app = new ClientApp();
        app.start();
    }

    public void start() {
        System.out.println("Olá, seja bem-vindo ao GoTogether!");
        System.out.println("Selecione uma opção:");

        System.out.println("1- Cadastrar carona");
        System.out.println("q- Sair");

        String choice = sc.nextLine();

        switch(choice) {
            case "q": System.exit(0);
            case "1": driverApp.showPublishRide();
        }
    }

}
