package me.gimenez.socket.client;

import me.gimenez.dto.ride.CreateRideRequest;
import me.gimenez.dto.Response;
import me.gimenez.dto.ride.DeleteRideRequest;
import me.gimenez.model.Ride;
import me.gimenez.model.Segment;

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
            System.out.println("\nBem-vindo ao painel de motorista.");

            System.out.println("Selecione uma opção:\n");

            System.out.println("1- Cadastrar carona");
            System.out.println("2- Listar caronas cadastradas");
            System.out.println("3- Cancelar uma carona");
            System.out.println("q- Sair");

            String choice = sc.nextLine();

            switch (choice) {
                case "1": showPublishRide(); break;
                case "2": showAllRides(); break;
                case "3": showDeleteRide(); break;
                case "q": System.exit(0); break;
                default:
                    System.out.println("Opção inválida");
                    break;
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

        CreateRideRequest request = new CreateRideRequest(routes, date, time, prices, seats);

        Response response = client.publishRide(request);
        System.out.println(response.message());
    }

    public List<Ride> showAllRides (){
        List<Ride> rides = client.listRides();

        if (rides.isEmpty()){
            System.out.println("Você não possui caronas cadastradas.");
        }

        System.out.println("Veja abaixo todas as suas caronas cadastradas: ");
        for (int i = 0; i<rides.size(); i++) {
            Ride ride = rides.get(i);

            System.out.println("\nCarona " + (i+1) + ":");
            System.out.println(ride.getDate() + " saída às " + ride.getDepartureTime());

            List<Segment> segments =  ride.getSegments();

            System.out.println("Trechos da viagem:");

            for (Segment segment : segments) {
                System.out.println("\n    " + segment.getOrigin() + " → " + segment.getDestination());
                System.out.println("    Preço: R$" + segment.getPrice());

                if (segment.getAvailableSeats() == 0){
                    System.out.println("    Status: LOTADO");
                } else {
                    System.out.println("    Status: " + segment.getAvailableSeats() + " assentos disponíveis.");
                }
            }
        }

        return rides;
    }

    public void showDeleteRide(){
        List<Ride> rides = showAllRides();

        System.out.println("Escolha o número da carona para deletar: ");
        System.out.println("Ou digite 'q' para cancelar a operação.");
        String choice = sc.nextLine();

        if ("q".equalsIgnoreCase(choice)){
            return;
        }

        Ride ride = rides.get(Integer.parseInt(choice)-1);

        DeleteRideRequest request = new DeleteRideRequest(ride.getId());
        Response response = client.deleteRide(request);

        System.out.println(response.message());

    }
}
