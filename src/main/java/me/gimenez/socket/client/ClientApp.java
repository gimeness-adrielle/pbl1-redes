package me.gimenez.socket.client;

import me.gimenez.model.users.User;
import me.gimenez.model.users.UserType;
import me.gimenez.requests.Response;
import me.gimenez.requests.auth.LoginRequest;
import me.gimenez.requests.auth.RegisterRequest;

import java.io.IOException;
import java.util.Scanner;

public class ClientApp {

    private static final Scanner sc =  new Scanner(System.in);
    private final DriverApp driverApp;
    private final PassengerApp passengerApp;
    private final Client client;

    public ClientApp() {
        try {
            client = new Client("localhost", 5000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.driverApp = new DriverApp(client);
        this.passengerApp = new PassengerApp(client);
    }

    static void main(String[] args) {
        ClientApp app = new ClientApp();
        app.start();
    }

    public void start() {
        System.out.println("Olá, seja bem-vindo ao GoTogether!");
        showAuth();
    }

    public void showAuth(){
        System.out.println("Entre na sua conta ou registre-se!");

        System.out.println("Selecione uma opção: ");
        System.out.println("1- Entrar na conta");
        System.out.println("2- Registrar uma conta");
        System.out.println("q- Sair");

        String choice = sc.nextLine();

        switch(choice) {
            case "1": showLogin(); break;
            case "2": showRegister(); break;
            case "q": System.exit(0); break;
        }

    }

    public void showLogin (){
        while(true) {
            System.out.println("Digite seu username: ");
            String username = sc.nextLine();

            System.out.println("Digite sua senha: ");
            String password = sc.nextLine();

            LoginRequest request = new LoginRequest(username,password);

            try {
                Response response = client.login(request);

                System.out.println("CLIENT APP: " + response); // TÁ RECEBENDO USER NULO

                if (response.status().equals("ERROR")) {
                    System.out.println("\nUsuário ou senha incorretos.");
                    System.out.println("Tente novamente.\n");
                    continue;
                }
                User user = (User) response.data();

                if (user.userType() == UserType.DRIVER){
                    driverApp.start();
                } else {
                    passengerApp.start();
                }

                System.exit(0);
            } catch (IOException e) {
                System.out.println("Erro de comunicação com o servidor.");
            }
        }
    }

    public void showRegister(){
        System.out.println("Digite seu nome: ");
        String name = sc.nextLine();

        System.out.println("Digite seu username: ");
        String username = sc.nextLine();

        System.out.println("Digite sua senha: ");
        String password = sc.nextLine();

        System.out.println("Você é motorista ou passageiro?");
        System.out.println("1- Motorista");
        System.out.println("2- Passageiro");
        String choice = sc.nextLine();

        UserType userType = null;

        switch(choice) {
            case "1": userType = UserType.DRIVER; break;
            case "2": userType = UserType.PASSENGER; break;
            default:
                System.out.println("Opção inválida.");
                return;
        }

        RegisterRequest request = new RegisterRequest(name, username, password, userType);

        try{
            client.sendRequest("REGISTER", request);

            System.out.println("Registrado com sucesso.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        showAuth();
    }

}
