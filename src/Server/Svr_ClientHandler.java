package Server;

import Commons.ClientServerCodes;
import Commons.Serializer;
import Commons.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
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

    private Login loginDB;
    public Svr_ClientHandler(Socket s, Login loginDB)
    {
        this.loginDB = loginDB;

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
            System.out.println("Awaiting command.");

            try {
                String packet = input.readLine();
                System.out.println("Got packet : " + packet);

                parsePacket(packet);

                if(lastCommand.get("command").equals("login"))
                    loginHandler();
                else if(lastCommand.get("command").equals("register"))
                    registerHandler();
            } catch (IOException e) {
                cleanup();
                e.printStackTrace();
            }
        }
    }
    private void registerHandler()
    {
        String username = lastCommand.get("username");
        String password = lastCommand.get("password");

        if (password == null || username == null) { //packet malformed
            output.println("Malformed packet");
            output.flush();
        }
        else
        {

            try {
                loginDB.registerUser(username, password, "");
                loginHandler();//Login the user as well;
            }  catch (IOException e) {
                output.println("failure:IOException");
                output.flush();
            } catch (NoSuchAlgorithmException e) {
                output.println("failure:Failed encrypting data");
                output.flush();
            } catch (UserRegisteredException e ) {
                output.println("failure:Register failed");//Precise error obscured to prevent user enumeration
                output.flush();
            }
        }
    }
    private void loginHandler()
    {
        String username = lastCommand.get("username");
        String password = lastCommand.get("password");

        if (password == null || username == null) { //packet malformed
            output.println("Malformed packet");
            output.flush();
        }
        else
        {
            System.out.println("Got request to login : " + username);
            User user = null;

            try {
                user = loginDB.authenticateUser(username, password);
                output.println("success:" + Serializer.serializeToString(user));
                output.flush();
            } catch (IOException e) {
                output.println("failure:IOException");
                output.flush();
            } catch (NoSuchAlgorithmException e) {
                output.println("failure:Failed decrypting data");
                output.flush();
            } catch (UserNotFoundException | LoginFailedException e) {
                output.println("failure:Login failed");//Precise error obscured to prevent// user enumeration
                output.flush();
            }

        }
    }

    private void parsePacket(String packet)
    {
        String[] split = packet.split("[;:]");
        lastCommand = new HashMap<>();
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
