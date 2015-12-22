package Server;

/**
 * Created by drcon on 21/12/2015.
 */
public class Driver extends User {

    String plate, model;

    public Driver(String userName, String name, String plate, String model) {
        super(userName, name);
        this.plate = plate;
        this.model = model;
    }
}
