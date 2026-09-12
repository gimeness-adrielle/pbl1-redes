package me.gimenez.socket.client;

import me.gimenez.model.Itinerary;
import me.gimenez.model.Segment;
import me.gimenez.requests.ReservationRequest;
import me.gimenez.requests.Response;
import me.gimenez.requests.SearchRideRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class PassengerApp {

    private static final Scanner sc =  new Scanner(System.in);
    private final Client client;

    public PassengerApp(Client client) {
        this.client = client;
    }

    public void start() {
        while(true) {
            System.out.println("Bem vindo ao painel de passageiro!");

            System.out.println("Selecione uma opção: ");
            System.out.println("1- Buscar itinerários");
            System.out.println("2- Consultar reservas");
            System.out.println("3- Cancelar reservas");
            System.out.println("q- Sair");

            String choice = sc.nextLine();

            switch (choice) {
                case "1": showSearchRides(); break;
                case "2": break;
                case "3": break;
                case "q": System.exit(0); break;
                default: break;
            }
        }
    }

    public void showSearchRides(){
        System.out.println("Digite a origem que deseja procurar: ");
        String origin = sc.nextLine();

        System.out.println("Digite o destino: ");
        String destination = sc.nextLine();

        System.out.println("Digite a data que deseja viajar (dd/MM/yyyy): ");
        String dateInput =  sc.nextLine();
        LocalDate date = LocalDate.parse(dateInput, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        SearchRideRequest searchRequest = new SearchRideRequest(origin, destination, date);

        List<Itinerary> itineraries;

        try {
            itineraries = client.searchRides(searchRequest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (itineraries == null){
            System.out.println("Não foram encontrados itinerários.");
            return;
        }

        for (int i=0; i<itineraries.size(); i++) {
            System.out.println("\nItinerário " + (i+1) + ":");
            Itinerary itinerary = itineraries.get(i);
            System.out.println("    Trechos: ");

            for (int j=0; j<itinerary.segments().size(); j++) {
                Segment segment = itinerary.segments().get(j);
                System.out.println("        " + segment.getOrigin() + " → " + segment.getDestination() + " (R$ " + segment.getPrice() + ")");
            }

            System.out.println("Preço total: R$ " + itinerary.totalPrice() + "\n");
        }

        System.out.println("Digite o número do itinerário desejado");
        System.out.println("Ou digite 'q' para cancelar");
        String choice =  sc.nextLine();

        if (choice.equals("q")){
            return;
        }

        Itinerary itinerary = itineraries.get(Integer.parseInt(choice)-1);

        if (itinerary == null){
            System.out.println("Número do itinerário não encontrado.");
            return;
        }

        ReservationRequest reserveRequest = new ReservationRequest(itinerary, itinerary.segments().stream().map(Segment::getId).toList());

        try {
            Response response = client.reserveItinerary(reserveRequest);

            if (response.status().equals("OK")) {
                System.out.println("Reserva realizada com sucesso!");
            } else {
                System.out.println("Erro ao realizar reserva: " + response.message());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
