package me.gimenez.socket.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import me.gimenez.requests.PublishRideRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DriverApp {

    private static final Scanner sc =  new Scanner(System.in);
    private final Client client;

    public DriverApp(Client client) {
        this.client = client;
    }

    public void start(){
        while(true){
            System.out.println("Bem-vindo ao painel de motorista.");

            System.out.println("Selecione uma opção: ");

            System.out.println("1- Cadastrar carona");
            System.out.println("2- Listar caronas cadastradas");
            System.out.println("3- Cancelar uma carona");
            System.out.println("q- Sair");

            String choice = sc.nextLine();

            switch (choice) {
                case "1": showPublishRide(); break;
                case "2": break;
                case "3": break;
                case "q": System.exit(0); break;
                default:
                    System.out.println("Opção inválida");;
            }
        }
    }

    public void showPublishRide() {
        System.out.println("Você está cadastrando uma carona");

        System.out.println(
                "Informe a quantidade de cidades da rota.\n" +
                        "Cada par consecutivo de cidades formará um trecho.\n" +
                        "Você deverá informar o preço de cada trecho.\n" +
                        "Exemplo:\n" +
                        "Salvador → Feira de Santana → Vitória da Conquista\n" +
                        "Trecho 1: Salvador → Feira de Santana\n" +
                        "Trecho 2: Feira de Santana → Vitória da Conquista\n"
        );

        System.out.println("Número de cidades: ");
        int numOfCities = Integer.parseInt(sc.nextLine());

        List<String> routes = new ArrayList<>();
        List<Double> prices = new ArrayList<>();

        for (int i = 0; i < numOfCities; i++) {
            System.out.println("Informe o nome da cidade " + (i+1) + ":");
            String city = sc.nextLine();
            routes.add(city);
        }

        for (int i = 0; i < routes.size() - 1; i++) {
            System.out.println(
                    "Preço do trecho " + routes.get(i) +
                            " → " + routes.get(i + 1) + ":"
            );

            double price = Double.parseDouble(sc.nextLine());
            prices.add(price);
        }

        System.out.println("Data da viagem (dd/MM/yyyy): ");
        String dateInput =  sc.nextLine();

        System.out.println("Horário de partida (HH:mm): ");
        String timeInput =  sc.nextLine();

        System.out.println("Quantidade de assentos: ");
        int seats =  Integer.parseInt(sc.nextLine());

        // Converter data e hora
        LocalDate date = LocalDate.parse(dateInput, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        LocalTime time = LocalTime.parse(timeInput, DateTimeFormatter.ofPattern("HH:mm"));

        PublishRideRequest request = new  PublishRideRequest(routes, date, time, prices, seats);

        try {
            client.sendRequest("PUBLISH_RIDE", request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void showAllRides (){
        System.out.println("Veja abaixo todas as suas caronas cadastradas: ");
    }

    public void deleteRide(){
        showAllRides();

        System.out.println("Escolha o número da carona para deletar: ");
        System.out.println("Digite 'q' para cancelar a operação.");
        String choice = sc.nextLine();

        if (choice.equals("q")) {
            return;
        }

        // aqui tem que mandar deletar lá pro negócio.
    }
}
