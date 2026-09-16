package me.gimenez.socket.client;

import me.gimenez.dto.reservation.DeleteReservationRequest;
import me.gimenez.model.Itinerary;
import me.gimenez.model.Reservation;
import me.gimenez.model.Segment;
import me.gimenez.dto.reservation.CreateReservationRequest;
import me.gimenez.dto.Response;
import me.gimenez.dto.ride.SearchRideRequest;

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
            System.out.println("2- Consultar minhas reservas");
            System.out.println("3- Cancelar reservas");
            System.out.println("q- Sair");

            String choice = sc.nextLine();

            switch (choice) {
                case "1": showSearchRides(); break;
                case "2": showListReservations(); break;
                case "3": showDeleteReservation(); break;
                case "q": System.exit(0); break;
                default: break;
            }
        }
    }

    void showSearchRides(){
        System.out.println("Digite a origem que deseja procurar: ");
        String origin = sc.nextLine();

        System.out.println("Digite o destino: ");
        String destination = sc.nextLine();

        System.out.println("Digite a data que deseja viajar (dd/MM/yyyy): ");
        String dateInput =  sc.nextLine();
        LocalDate date = LocalDate.parse(dateInput, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        SearchRideRequest searchRequest = new SearchRideRequest(origin, destination, date);

        List<Itinerary> itineraries;

        itineraries = client.searchRides(searchRequest);

        if (itineraries == null){
            System.out.println("Não foram encontrados itinerários.");
            return;
        }

        for (int i=0; i<itineraries.size(); i++) {
            Itinerary itinerary = itineraries.get(i);
            printItinerary(itinerary, i);
        }

        System.out.println("Digite o número do itinerário desejado");
        System.out.println("Ou digite 'q' para cancelar");
        String choice =  sc.nextLine();

        if ("q".equalsIgnoreCase(choice)){
            return;
        }

        Itinerary itinerary = itineraries.get(Integer.parseInt(choice)-1);

        if (itinerary == null){
            System.out.println("Número do itinerário não encontrado.");
            return;
        }

        CreateReservationRequest reserveRequest = new CreateReservationRequest(itinerary, itinerary.segments().stream().map(Segment::getId).toList());

        Response response = client.reserveItinerary(reserveRequest);

        System.out.println(response.message());
    }

    List<Reservation> showListReservations(){
        List<Reservation> reservations = client.listReservations();
        if (reservations.isEmpty()){
            System.out.println("Você não tem reservas.");
            return null;
        }

        System.out.println("\nSuas reservas: ");

        for (int i=0; i<reservations.size(); i++) {
            Itinerary itinerary = reservations.get(i).itinerary();
            printItinerary(itinerary, i);
        }

        return reservations;
    }

    void showDeleteReservation(){
        List<Reservation> reservations = showListReservations();

        if (reservations == null){
            return;
        }

        System.out.println("Digite o número do itinerário para cancelar sua reserva: ");
        System.out.println("Ou digite 'q' para cancelar");
        String choice = sc.nextLine();

        if ("q".equalsIgnoreCase(choice)){
            return;
        }

        Reservation reservation = reservations.get(Integer.parseInt(choice)-1);

        Response response = client.deleteReservation(new DeleteReservationRequest(reservation.id()));

        System.out.println(response.message());
    }

    void printItinerary(Itinerary itinerary, int i){
        System.out.println("\nItinerário " + (i+1) + ":");
        System.out.println("    Trechos: ");

        for (int j=0; j<itinerary.segments().size(); j++) {
            Segment segment = itinerary.segments().get(j);
            System.out.println("        " + segment.getOrigin() + " → " + segment.getDestination() + " (R$ " + segment.getPrice() + ")");
        }

        System.out.println("Preço total: R$ " + itinerary.totalPrice() + "\n");
    }

}
