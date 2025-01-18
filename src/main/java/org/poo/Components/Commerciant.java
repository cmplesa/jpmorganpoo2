package org.poo.Components;

import lombok.Data;
import org.poo.ObserverPattern.Observer;
import org.poo.ObserverPattern.Subject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a commerciant.
 */
@Data
public final class Commerciant implements Subject {
    private String name;
    private int id;
    private String account;
    private String type;
    private String cashbackStrategy;
    private Double revenue;

    private final List<Observer> observers;  // Lista cu observeri

    public Commerciant(final String name, final int id, final String account,
                       final String type, final String cashbackStrategy) {
        this.name = name;
        this.id = id;
        this.account = account;
        this.type = type;
        this.cashbackStrategy = cashbackStrategy;
        this.revenue = 0.0;
        this.observers = new ArrayList<>();
    }

    @Override
    public void registerObserver(final Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(final Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(this);
        }
    }

    /**
     * Updates the revenue of the commerciant and notifies observers.
     *
     * @param amount the amount to add to the revenue
     */
    public void addRevenue(final double amount) {
        this.revenue += amount;
        notifyObservers();  // Notifică modificarea venitului
    }
}
