package com.prasanth.training.ems.bmsfrontend.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Ticket {

    Booking booking;
    List<Passenger> passengers;

    @Override
    public String toString() {
        return "Ticket{" +
                "booking=" + booking +
                ", passengers=" + passengers +
                '}';
    }
}
