package Commons;

import java.io.Serializable;

/**
 * Created by drcon on 21/12/2015.
 */
public class User implements Serializable {
    String userName, name, password;
    public User(String userName, String password, String name) {
        this.userName = userName;
        this.name = name;
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", name='" + name + '\'' +
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
}
