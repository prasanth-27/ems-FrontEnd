package com.prasanth.training.ems.bmsfrontend.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
public class Booking {

    private Long bookingId;

    private Long busNumber;
    private LocalDate bookingDate;
    private String source;
    private String destination;
    private short numberOfSeats;
    private String status;
    private Long seatReservationId;
    private Double totalFare;
    private String userId;

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId='" + bookingId + '\'' +
                ", busNumber=" + busNumber +
                ", bookingDate=" + bookingDate +
                ", source='" + source + '\'' +
                ", destination='" + destination + '\'' +
                ", numberOfSeats=" + numberOfSeats +
                ", status='" + status + '\'' +
                '}';
    }
}
