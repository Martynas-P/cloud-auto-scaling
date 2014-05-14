package uk.ac.leeds.sc11mp.cloudautoscaling.monitoring.event;

import java.util.Observable;

/**
 *
 * @author martynas
 */
public class RequestCountEvent extends Observable {
    
    public void emitEvent() {
        setChanged();
        notifyObservers();
    }
    
}
