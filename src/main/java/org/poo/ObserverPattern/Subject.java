package org.poo.ObserverPattern;

public interface Subject {
    void registerObserver(Observer observer);  // Înregistrează un observer
    void removeObserver(Observer observer);    // Elimină un observer
    void notifyObservers();                    // Notifică toți observerii
}
