package com.prasanth.training.ems.bmsfrontend.service;


import com.prasanth.training.ems.bmsfrontend.models.Booking;
import com.prasanth.training.ems.bmsfrontend.models.BusInventory;
import com.prasanth.training.ems.bmsfrontend.models.Passenger;
import com.prasanth.training.ems.bmsfrontend.models.Ticket;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class TicketBookingService {

/*    {
        "booking":{
        "busNumber": 7123,
                "source": "Hyderabad",
                "destination": "Kurnool",
                "numberOfSeats": 2
        },
        "passengers":[
        {
            "name": "John Doe",
                "age": 30,
                "gender": "M"
        },
        {
            "name": "Jane Doe",
                "age" : 28,
                "gender": "F"
        }
        ]
    }*/

    private static final Logger log = LoggerFactory.getLogger(TicketBookingService.class);

    private static final String bookingUrl = "http://localhost:9013/api/v1/ticketmanager/add";
    private static final String searchUrl = "http://localhost:9091/api/v1/compositor/history";

    @Autowired
    WebClient.Builder webClientBuilder;

    public Boolean bookTicket(List<String> passengerNames,
                             List<Short> passengerAges,
                             List<Character> passengerGenders,
                             Long busNumber,
                             HttpSession session,
                             Model model) {
        log.info("Booking seats in bus: {}", busNumber);
        List<BusInventory> inventory = (List<BusInventory>) session.getAttribute("buses");
        if (inventory == null || inventory.isEmpty()) {
            log.error("No bus inventory found in model");
            return false; // Redirect to an error page or handle accordingly
        }
        BusInventory busInventory = inventory.stream()
                .filter(bus -> bus.getBusNumber() == busNumber)
                .findFirst()
                .orElse(null);
        if (busInventory == null) {
            log.error("Bus with number {} not found in inventory", busNumber);
            return false; // Redirect to an error page or handle accordingly
        }
        log.info("Booking Bus: {}", busInventory);

        Ticket ticket = new Ticket();
        Booking booking = new Booking();
        List<Passenger> passengers = new ArrayList<>();

        booking.setBusNumber(busNumber);
        booking.setSource(busInventory.getSource());
        booking.setDestination(busInventory.getDestination());
        booking.setNumberOfSeats((short)passengerNames.size());
        booking.setTotalFare(booking.getNumberOfSeats()* busInventory.getPrice());

        for (int i = 0; i < passengerNames.size(); i++) {
            Passenger passenger = new Passenger();
            passenger.setName(passengerNames.get(i));
            passenger.setAge(passengerAges.get(i));
            passenger.setGender(passengerGenders.get(i));
            passengers.add(passenger);
        }

        ticket.setBooking(booking);
        ticket.setPassengers(passengers);

        log.info("Booking ticket: {}", ticket);

        WebClient searchClient = webClientBuilder.build();
        String authToken = (String) session.getAttribute("Authorization-token");
        ResponseEntity<String> bookTicketMono= searchClient.post()
                .uri(bookingUrl)
                .header("Authorization", authToken)
                .bodyValue(ticket)
                .retrieve()
                .toEntity(String.class)
                .block();

        if(!bookTicketMono.getStatusCode().isSameCodeAs(HttpStatus.CREATED)){
            log.error("Booking failed, return code {}", bookTicketMono.getStatusCode());
            return false; // Redirect to an error page or handle accordingly
        }

        log.info("Booking response: {}, {}", bookTicketMono.getStatusCode(), bookTicketMono.getBody());
        return bookTicketMono.getStatusCode().isSameCodeAs(HttpStatus.CREATED);
    }

    public ResponseEntity<?> getBookingHistory(String user, String token) {
        log.info("Searching for booking history for user : {}", user);

        List<Ticket> tickets = new ArrayList<>();

        try {
            log.debug("Making GET request to search bookings at: {}", searchUrl);
            WebClient webClient = webClientBuilder.build();
            ResponseEntity<List<Ticket>> responseMono = webClient.get()
                    .uri(searchUrl)
                    .header("Authorization", token)
                    .retrieve()
                    .toEntityList(Ticket.class)
                    .block();
            if(!responseMono.getStatusCode().is2xxSuccessful()) {
                log.error("Error fetching booking history for user{}, statusCode {}", user, responseMono.getStatusCode());
            }
            tickets = responseMono.getBody();

        } catch (Exception e) {
            log.error("Error Fetching ticketHistory: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }

        log.info("Found {} bookings in history", tickets.size());

        return ResponseEntity.ok(tickets);
    }


}
