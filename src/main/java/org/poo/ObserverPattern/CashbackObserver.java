package org.poo.ObserverPattern;

import org.poo.Components.Commerciant;

public class CashbackObserver implements Observer {
    @Override
    public void update(Commerciant commerciant) {
        System.out.println("Notificare Cashback: Commerciantul " + commerciant.getName() +
                " are acum venitul actualizat la " + commerciant.getRevenue());
    }
}
