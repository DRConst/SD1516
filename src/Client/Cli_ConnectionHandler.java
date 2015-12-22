package Client;

import Commons.ClientServerCodes;
import Commons.Serializer;
import Commons.User;

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
    private BufferedReader bufferedReader = null;
    User user = null;
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
        String option = "";
        bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        while(!timeout)
        {
            System.out.println("Please choose an option :");

            System.out.println("a - Login");
            System.out.println("b - Register");


            try {
               option = bufferedReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(option.equals("a"))
            {
                loginOption();
            }else if(option.equals("b"))
            {
                registerOption();
            }

        }
    }
    private void registerOption()
    {
        String name, password;


        try {
            System.out.println("Please choose a username :");
            name = bufferedReader.readLine();
            System.out.println("Please choose a password :");
            password = bufferedReader.readLine();
            user = new User(name, password);
            String packet = user.getPacketHeader("register");
            output.println(packet);
            output.flush();

            String response = input.readLine();
            String[] chunks = response.split(":");
            if(chunks[0].equals("success"))
            {
                System.out.println("Register successful, you are now logged in");
                user = (User) Serializer.unserializeFromString(chunks[1]);
            }else
            {
                System.out.println("Failed to register, the reason :  " + chunks[1]);
                user = null;
            }
        } catch (IOException e) {
            System.out.println("Failed to register");
            e.printStackTrace();
        }


    }


    private void loginOption()
    {
        String name, password;


        try {
            System.out.println("Please choose a username :");
            name = bufferedReader.readLine();
            System.out.println("Please choose a password :");
            password = bufferedReader.readLine();
            user = new User(name, password);
            String packet = user.getPacketHeader("login");
            output.println(packet);
            output.flush();

            String response = input.readLine();
            String[] chunks = response.split(":");
            if(chunks[0].equals("success"))
            {
                user = (User) Serializer.unserializeFromString(chunks[1]);
                System.out.println("Logged in " + user.toString());
            }else
            {
                System.out.println("Failed to log in, the reason :  " + chunks[1]);
                user = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
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
