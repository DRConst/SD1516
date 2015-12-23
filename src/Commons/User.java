package Commons;

import java.io.Serializable;

/**
 * Created by drcon on 21/12/2015.
 */
public class User implements Serializable {
    String userName, password;

    String plate, model;

    public User(String userName, String password, String plate, String model) {
        this.userName = userName;
        this.password = password;
        this.plate = plate;
        this.model = model;
    }

    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    public User(String userName, String passwordHash) {
        this.userName = userName;
        this.password = passwordHash;
    }

    public String getPacketHeader(String command)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("username:");
        sb.append(userName);
        sb.append(";");

        sb.append("password:");
        sb.append(password);
        sb.append(";");

        sb.append("command:");
        sb.append(command);
        sb.append(";");

        return sb.toString();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
