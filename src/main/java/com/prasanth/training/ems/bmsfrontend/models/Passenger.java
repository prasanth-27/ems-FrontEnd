package com.prasanth.training.ems.bmsfrontend.models;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Passenger {

    private Long passengerId;
    private Long bookingId;

    private String name;
    private short age;
    private char gender;

    @Override
    public String toString() {
        return "Passenger{" +
                "passengerId=" + passengerId +
                ", bookingId=" + bookingId +
                '}';
    }
}
