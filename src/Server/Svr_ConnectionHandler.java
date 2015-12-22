package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by drcon on 21/12/2015.
 */
public class Svr_ConnectionHandler {

    ServerSocket mainSocket;


    public Svr_ConnectionHandler(ServerSocket mainSocket) {
        this.mainSocket = mainSocket;
    }

    public Svr_ConnectionHandler() throws IOException {
        this.mainSocket = new ServerSocket(28960);
    }

    public void mainLoop() throws IOException {
        while(true)
        {
            System.out.println("Waiting on client");
            Socket client = mainSocket.accept();
            System.out.println("Got a client");
            Thread cThread = new Thread(() -> {
                new Svr_ClientHandler(client);
            });
            cThread.start();
        }
    }

    public static void main(String[] args) throws IOException {
        Svr_ConnectionHandler ch = new Svr_ConnectionHandler();
        ch.mainLoop();
    }
}


