package org.poo.ObserverPattern;

import org.poo.Components.Commerciant;

public interface Observer {
    /**
     * Notifică observer-ul despre o modificare.
     *
     * @param commerciant Commerciantul care a generat notificarea.
     */
    void update(Commerciant commerciant);
}
