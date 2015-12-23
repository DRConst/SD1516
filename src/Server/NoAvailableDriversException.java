package Server;

/**
 * Created by drcon on 23/12/2015.
 */
public class NoAvailableDriversException extends Throwable {
    public NoAvailableDriversException() {
    }

    public NoAvailableDriversException(String s) {
        super(s);
    }
}
