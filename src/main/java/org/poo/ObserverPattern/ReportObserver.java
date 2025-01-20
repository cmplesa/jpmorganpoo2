package org.poo.ObserverPattern;

import org.poo.Components.Commerciant;

/**
 * The ReportObserver class implements the Observer interface and generates reports.
 */
public final class ReportObserver implements Observer {

    /**
     * Updates the observer with the given commerciant.
     *
     * @param commerciant the commerciant to update
     */
    @Override
    public void update(final Commerciant commerciant) {
        System.out.println("Generare raport: " + commerciant.getName()
                + " are un venit de " + commerciant.getRevenue() + " lei.");
    }
}