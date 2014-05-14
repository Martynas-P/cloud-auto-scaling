package uk.ac.leeds.sc11mp.cloudautoscaling.exception;

/**
 * 
 * @author sc11mp
 */
public class OpennebulaException extends Exception {

    /**
     * {@inheritDoc}
     */
    public OpennebulaException() {
    }

    /**
     * {@inheritDoc}
     */
    public OpennebulaException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     */
    public OpennebulaException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     */
    public OpennebulaException(Throwable cause) {
        super(cause);
    }
    
}