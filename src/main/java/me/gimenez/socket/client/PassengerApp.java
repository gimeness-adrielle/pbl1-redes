package me.gimenez.socket.client;

import java.util.Scanner;

public class PassengerApp {

    private static final Scanner sc =  new Scanner(System.in);
    private final Client client;

    public PassengerApp(Client client) {
        this.client = client;
    }

    public void start() {
        System.out.println("Bem vindo ao painel de passageiro!");

        System.out.println("Selecione uma opção: ");
        System.out.println("1- Buscar itinerários");
        System.out.println("2- Consultar reservas");
        System.out.println("3- Cancelar reservas");
        System.out.println("q- Sair");

        String choice = sc.nextLine();

        switch (choice) {
            case "1": break;
            case "2": break;
            case "3": break;
            case "q": System.exit(0); break;
            default: break;
        }
    }


}
