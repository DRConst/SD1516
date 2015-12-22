package Client;

/**
 * Created by drcon on 22/12/2015.
 */
public class ServerUnreachableException extends Throwable {
    public ServerUnreachableException() {
    }

    public ServerUnreachableException(String s) {
        super(s);
    }
}
