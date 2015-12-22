package Client;

import Commons.ClientServerCodes;

import java.io.*;
import java.net.Socket;

/**
 * Created by drcon on 22/12/2015.
 */
public class Cli_ConnectionHandler {

    private Socket socket, hbSocket;
    boolean timeout = false;
    String host = "localhost";
    private BufferedReader input, hbIn;
    private PrintWriter output, hbOut;

    public Cli_ConnectionHandler()
    {
        try {

            socket = new Socket(host, 28960);
            output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) { //Host unreachable
            e.printStackTrace();
        }

        Thread hbThread = new Thread(() -> {
            try {
                heartbeat();
            } catch (ServerUnreachableException e) {
                e.printStackTrace();
            }
        });

        initHeartbeat();

        hbThread.start();

        mainLoop();


    }


    public void mainLoop()
    {
        while(!timeout)
        {

        }
    }

    private void heartbeat() throws ServerUnreachableException {
        String response;
        while(!timeout)
        {

            try
            {
                response = hbIn.readLine();
                if(!response.equals(ClientServerCodes.svr2cli_heartbeat))
                {
                    //Something went wrong, just drop the connection;
                    throw new ServerUnreachableException();
                }else{
                    hbOut.println(ClientServerCodes.cli2svr_heartbeat);
                    hbOut.flush();
                    System.out.println("Server HB successful");
                }
            } catch (IOException e) {
                throw new ServerUnreachableException();
            }




        }
    }

    private void initHeartbeat()
    {
        int port = -1;
        try {
            String portnum = input.readLine();
            System.out.println("Got port num : " + portnum);
            port = new Integer(portnum);
            hbSocket = new Socket(host, port);
            hbOut = new PrintWriter(new OutputStreamWriter(hbSocket.getOutputStream()));
            hbIn = new BufferedReader(new InputStreamReader(hbSocket.getInputStream()));
            hbSocket.setSoTimeout(5000);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        Cli_ConnectionHandler cli_connectionHandler = new Cli_ConnectionHandler();
    }
}
