package uk.ac.leeds.sc11mp.cloudautoscaling.monitoring.event;

import java.util.Observable;

public class ResponseTimeEvent extends Observable {
    
    public void emitEvent() {
        setChanged();
        this.notifyObservers();
    }
    
}
