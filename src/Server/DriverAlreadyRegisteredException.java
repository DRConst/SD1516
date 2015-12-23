package Server;

/**
 * Created by drcon on 23/12/2015.
 */
public class DriverAlreadyRegisteredException extends Throwable {
    public DriverAlreadyRegisteredException() {
    }

    public DriverAlreadyRegisteredException(String s) {
        super(s);
    }
}
