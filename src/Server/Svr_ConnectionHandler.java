package Server;

import Commons.Serializer;
import Commons.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by drcon on 21/12/2015.
 */
public class Svr_ConnectionHandler {

    ServerSocket mainSocket;
    Login loginDB;

    public Svr_ConnectionHandler(ServerSocket mainSocket, Login loginDB) throws IOException {
        this.mainSocket = mainSocket;
        this.loginDB = loginDB;
        mainLoop();
    }

    public Svr_ConnectionHandler(Login loginDB) throws IOException {
        this.loginDB = loginDB;
        this.mainSocket = new ServerSocket(28960);
        mainLoop();
    }

    public void mainLoop() throws IOException {
        while(true)
        {
            System.out.println("Waiting on client");
            Socket client = mainSocket.accept();
            System.out.println("Got a client");
            Thread cThread = new Thread(() -> {
                new Svr_ClientHandler(client, loginDB);
            });
            cThread.start();
        }
    }

    public static void main(String[] args) throws IOException {
        Login login = null;
        Serializer serializer = new Serializer();
        try {
            login = (Login) serializer.readObject("Login");
        } catch (ClassNotFoundException e) {

        }
        if (login == null) {
            login = new Login();
        }

        new Svr_ConnectionHandler(login);

    }
}


