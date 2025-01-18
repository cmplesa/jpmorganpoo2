package org.poo.Components;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BusinessComerciantPayment {
    private String commerciant;
    private ArrayList<String> employees;
    private List<String> managers;
    private double totalReceived;

    public BusinessComerciantPayment(final String commerciant) {
        this.commerciant = commerciant;
        this.employees = new ArrayList<>();
        this.managers = new ArrayList<>();
        this.totalReceived = 0;
    }

}
