package me.gimenez.client.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.client.AuthClient;
import me.gimenez.client.Client;
import me.gimenez.client.DriverClient;
import me.gimenez.client.PassengerClient;
import me.gimenez.domain.models.UserType;
import me.gimenez.domain.dto.responses.Response;

import java.util.Scanner;
import java.util.function.Predicate;

import static me.gimenez.util.InputValidation.isQuitting;
import static me.gimenez.util.InputValidation.readInput;

public class ClientApp {
    private static final Scanner sc =  new Scanner(System.in);

    private final DriverApp driverApp;
    private final PassengerApp passengerApp;

    private final Client client;
    private final AuthClient authClient;

    public ClientApp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        client = new Client(mapper);

        authClient = new AuthClient(client, mapper);
        DriverClient driverClient = new DriverClient(client, mapper);
        PassengerClient passengerClient = new PassengerClient(client, mapper);

        this.driverApp = new DriverApp(driverClient);
        this.passengerApp = new PassengerApp(passengerClient);
    }

    public void init()  {
        client.connect();
        System.out.println("Olá, seja bem-vindo ao GoTogether!");
        showAuth();
    }

    public void showAuth() {
        while (true){
            System.out.println("\nEntre na sua conta ou registre-se!");

            System.out.println("Selecione uma opção: ");
            System.out.println("1- Entrar na conta");
            System.out.println("2- Registrar uma conta");
            System.out.println("q- Sair");

            String choice = sc.nextLine();

            switch(choice) {
                case "1": showLogin(); break;
                case "2": showRegister(); break;
                case "Q":
                case "q": {
                    System.out.println("Encerrando...");
                    client.close();
                    return;
                }
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    public void showLogin (){
        while(true) {
            System.out.println("\nDigite 'q' para sair à qualquer momento.");

            System.out.print("Digite seu email: ");
            String email = sc.nextLine();
            if (isQuitting(email)){ return; }

            System.out.print("Digite sua senha: ");
            String password = sc.nextLine();
            if (isQuitting(password)) { return; }

            Response response = authClient.login(email, password);

            if (response.status().equals("ERROR")) {
                System.out.println("\nUsuário ou senha incorretos.");
                continue;
            }
            UserType currentUserType = (UserType)response.data();

            if (currentUserType == UserType.DRIVER){
                driverApp.start();
            } else {
                passengerApp.start();
            }
            break;
        }
    }

    public void showRegister() {
        System.out.println("\nDigite 'q' para sair à qualquer momento.\n");

        System.out.print("Digite seu email: ");
        String email = sc.nextLine();
        if (isQuitting(email)) { return; }

        System.out.print("Digite seu nome: ");
        String name = sc.nextLine();
        if (isQuitting(name)) { return; }

        Predicate<String> validPassword =password -> password.length() >= 8;
        String password = readInput("Digite sua senha: ", "A senha deve ter mais de 8 caracteres.", validPassword);

        System.out.println("Você é motorista ou passageiro?");
        System.out.println("1- Motorista");
        System.out.println("2- Passageiro");
        String choice = sc.nextLine();

        if (isQuitting(choice)) { return; }

        UserType userType = null;

        switch(choice) {
            case "1": userType = UserType.DRIVER; break;
            case "2": userType = UserType.PASSENGER; break;
            default:
                System.out.println("Opção inválida.");
                return;
        }

        Response response = authClient.register(email, name, password, userType);

        if ("OK".equals(response.status())) {
            System.out.println("Sua conta foi registrada com sucesso!");
        }
    }

}
