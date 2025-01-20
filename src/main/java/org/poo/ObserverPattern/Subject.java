package org.poo.ObserverPattern;

/**
 * The Subject interface for the Observer pattern.
 */
public interface Subject {

    /**
     * Registers an observer.
     *
     * @param observer the observer to register
     */
    void registerObserver(Observer observer);

    /**
     * Removes an observer.
     *
     * @param observer the observer to remove
     */
    void removeObserver(Observer observer);

    /**
     * Notifies all registered observers.
     */
    void notifyObservers();
}
