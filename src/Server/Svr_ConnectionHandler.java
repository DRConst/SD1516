package Server;

import Commons.Serializer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import static java.lang.Thread.sleep;
/**
 * Created by drcon on 21/12/2015.
 */
public class Svr_ConnectionHandler {

    ServerSocket mainSocket;
    Login loginDB;
    DriverPool driverPool;
    public Svr_ConnectionHandler(ServerSocket mainSocket, Login loginDB, DriverPool driverPool) throws IOException {
        this.mainSocket = mainSocket;
        this.loginDB = loginDB;
        this.driverPool = driverPool;
        Thread loopThread = new Thread(() -> {
            try {
                mainLoop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        loopThread.start();
    }

    public Svr_ConnectionHandler(Login loginDB, DriverPool driverPool) throws IOException {
        this.loginDB = loginDB;
        this.mainSocket = new ServerSocket(28960);
        this.driverPool = driverPool;
        Thread loopThread = new Thread(() -> {
            try {
                mainLoop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        loopThread.start();
    }

    public void mainLoop() throws IOException {
        while(true)
        {
            System.out.println("Waiting on client");
            Socket client = mainSocket.accept();
            System.out.println("Got a client");
            Thread cThread = new Thread(() -> {
                new Svr_ClientHandler(client, loginDB, driverPool);
            });
            cThread.start();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Login login = null;
        Serializer serializer = new Serializer();
        try {
            login = (Login) serializer.readObject("Server.Login");
        } catch (ClassNotFoundException e) {

        }
        if (login == null) {
            login = new Login();
        }

        DriverPool driverPool = null;
        /*try {
            driverPool = (DriverPool) serializer.readObject("Server.DriverPool");
        } catch (ClassNotFoundException e) {

        }*/
        if (driverPool == null) {
            driverPool = new DriverPool();
        }



        new Svr_ConnectionHandler(login, driverPool);

        while (true)
        {
            sleep(10000);
            //serializer.writeObject(driverPool);
            serializer.writeObject(login);
            System.out.println("State saved");
        }

    }
}


