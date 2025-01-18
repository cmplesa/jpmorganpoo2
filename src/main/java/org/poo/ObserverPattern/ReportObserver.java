package org.poo.ObserverPattern;

import org.poo.Components.Commerciant;

public class ReportObserver implements Observer {
    @Override
    public void update(Commerciant commerciant) {
        System.out.println("Generare raport: " + commerciant.getName() +
                " are un venit de " + commerciant.getRevenue() + " lei.");
    }
}
