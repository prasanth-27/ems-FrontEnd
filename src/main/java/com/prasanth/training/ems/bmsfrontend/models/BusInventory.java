package com.prasanth.training.ems.bmsfrontend.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BusInventory {
    private long busNumber;

    private String source;
    private String destination;
    private double price;
    private short totalSeats;
    private short availableSeats;
}
