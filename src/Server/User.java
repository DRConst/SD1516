package Server;

/**
 * Created by drcon on 21/12/2015.
 */
public class User {
    String userName, name, passwordHash;
    public User(String userName, String passwordHash, String name) {
        this.userName = userName;
        this.name = name;
    }

    public User(String userName, String passwordHash) {
        this.userName = userName;
        this.passwordHash = passwordHash;
    }

    public String getPacketHeader(String command)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("username:");
        sb.append(userName);
        sb.append(";");

        sb.append("passwordHash:");
        sb.append(passwordHash);
        sb.append(";");

        sb.append("command:");
        sb.append(command);
        sb.append(";");

        return sb.toString();
    }
}
