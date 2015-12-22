package Server;

import Commons.ClientServerCodes;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

import static java.lang.Thread.sleep;

/**
 * Created by drcon on 21/12/2015.
 */
public class Svr_ClientHandler {
    Socket socket, hbCliSocket;
    ServerSocket hbSvrSocket;
    boolean timeout = false;
    HashMap<String,String> lastCommand;

    private BufferedReader input, hbIn;
    private PrintWriter output, hbOut;

    public Svr_ClientHandler(Socket s)
    {
        this.socket = s;
        try {
            socket.setSoTimeout(0);
        } catch (SocketException e) {
            e.printStackTrace();
            cleanup();
        }



        try {
            output = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
            input = new BufferedReader(new InputStreamReader(s.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread heartbeatThread = new Thread(() -> {

            try
            {

                heartbeat();
            } catch (ClientTimedOutException e) {
                System.out.println("Client on port " + socket.getLocalPort() + " timed out.");
                cleanup();
            }
        });
        try {
            initHeartbeat();
        } catch (IOException e) {//Failed to open ports
            e.printStackTrace();
        }
        heartbeatThread.start();

        clientLoop();


    }


    public void clientLoop()
    {
        while(!timeout)
        {
            output.println("Awaiting command.");
            output.flush();

            try {
                String packet = input.readLine();
                output.println("Got packet : " + packet);
                output.flush();
            } catch (IOException e) {
                cleanup();
                e.printStackTrace();
            }
        }
    }


    private void parsePacket(String packet)
    {
        String[] split = packet.split("\\s;");

        for(int i = 0; i < split.length;)
        {
            lastCommand.put(split[i++], split[i++]);
        }
    }

    private void initHeartbeat() throws IOException {
        hbSvrSocket = new ServerSocket(0);
        int hbPort = hbSvrSocket.getLocalPort();
        hbSvrSocket.setSoTimeout(5000);
        //output.print(ClientServerCodes.svr2cli_heartbeatPort);
        output.println(hbPort);
        output.flush();
        hbCliSocket = hbSvrSocket.accept();
        hbCliSocket.setSoTimeout(5000);

        hbOut = new PrintWriter(new OutputStreamWriter(hbCliSocket.getOutputStream()));
        hbIn = new BufferedReader(new InputStreamReader(hbCliSocket.getInputStream()));
    }
    public void heartbeat() throws ClientTimedOutException {

        String response;
        while(!timeout)
        {
            hbOut.println(ClientServerCodes.svr2cli_heartbeat);
            hbOut.flush();


            try
            {
                response = hbIn.readLine();
                if(!response.equals(ClientServerCodes.cli2svr_heartbeat))
                {
                    //Something went wrong, just drop the connection;
                    throw new ClientTimedOutException();
                }
            } catch (IOException e) {
                throw new ClientTimedOutException();
            }


            try {
                sleep(500); //Only ping every half second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private void cleanup() {
        timeout = true;
        try{
        this.socket.close();
        this.hbCliSocket.close();
        this.hbSvrSocket.close();
        }
        catch(IOException e)
        {
            e.printStackTrace(); //Sockets already closed, redundant call, should never happen
        }
    }
}
