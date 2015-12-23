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
    Socket socket, hbCliSocket, pushCliSocket;
    ServerSocket hbSvrSocket, pushSvrSocket;
    boolean timeout = false;
    HashMap<String,String> lastCommand;

    private BufferedReader input, hbIn;
    private PrintWriter output, hbOut;

    private Login loginDB;

    private DriverPool driverPool;
    public Svr_ClientHandler(Socket s, Login loginDB, DriverPool driverPool)
    {
        this.loginDB = loginDB;

        this.driverPool = driverPool;

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
                    registrationHandler();
                else if(lastCommand.get("command").equals("request"))
                    requestHandler();
                else if(lastCommand.get("command").equals("driver"))
                    driverHandler();
                else if(lastCommand.get("command").equals("arrival"))
                    arrivalHandler();
                else if(lastCommand.get("command").equals("destination"))
                    destinationHandler();
            } catch (IOException e) {
                cleanup();
                e.printStackTrace();
            }
        }
    }

    private void destinationHandler()
    {
        String username = lastCommand.get("username");
        String password = lastCommand.get("password");
        User driver;
        if (password == null || username == null) { //packet malformed
            output.println("Malformed packet");
            output.flush();
        }else {
            try {
                driver = loginDB.authenticateUser(username, password);

                Socket clientPushSocket = driverPool.getClientSocket(username);
                if(clientPushSocket != null)
                {
                    new PrintWriter(new OutputStreamWriter(clientPushSocket.getOutputStream())).println("price:" + lastCommand.get("price")); //Write price to client
                    output.println("success: Please register again if you wish to apply for another job.");
                    output.flush();
                }else
                {
                    output.println("failure:");
                    output.flush();
                }





            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (UserNotFoundException | LoginFailedException e) {
                output.println("failure: ");
                output.flush();
            }
        }
    }

    private void arrivalHandler() {
        String username = lastCommand.get("username");
        String password = lastCommand.get("password");
        User driver;
        if (password == null || username == null) { //packet malformed
            output.println("Malformed packet");
            output.flush();
        } else {
            try {
                driver = loginDB.authenticateUser(username, password);

                Socket clientSocket = driverPool.getClientSocket(username);
                if (clientSocket != null) {
                    PrintWriter cliWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    cliWriter.println("arrival");
                    output.println("success: ");
                    output.flush();
                } else {
                    output.println("failure: No client has been assigned, please wait.");
                    output.flush();
                }


            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (UserNotFoundException | LoginFailedException e) {
                output.println("failure: ");
                output.flush();
            }
        }
    }

    private void driverHandler()
    {
        String username = lastCommand.get("username");
        String password = lastCommand.get("password");
        User driver;
        if (password == null || username == null) { //packet malformed
            output.println("Malformed packet");
            output.flush();
        }
        else
        {
            try {
                driver = loginDB.authenticateUser(username, password);
                int x = Integer.parseInt(lastCommand.get("x"));
                int y = Integer.parseInt(lastCommand.get("y"));





                ServerSocket driverPushSvr = new ServerSocket(0);
                driverPushSvr.setSoTimeout(0);
                int svrPort = driverPushSvr.getLocalPort();

                output.println("success: ;port:" + svrPort + ";");
                output.flush();

                Socket driverSocket = driverPushSvr.accept();

                driverPool.registerDriver(driver.getUserName(), x, y, driverSocket);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UserNotFoundException e) {
                e.printStackTrace();
            } catch (LoginFailedException e) {
                e.printStackTrace();
            } catch (DriverAlreadyRegisteredException e) {
                e.printStackTrace();
            }

        }
    }

    private void requestHandler()
    {
        String username = lastCommand.get("username");
        String password = lastCommand.get("password");

        if (password == null || username == null) { //packet malformed
            output.println("Malformed packet");
            output.flush();
        }
        else
        {
            int startx = Integer.parseInt(lastCommand.get("startx"));
            int starty = Integer.parseInt(lastCommand.get("starty"));
            int endx = Integer.parseInt(lastCommand.get("endx"));
            int endy = Integer.parseInt(lastCommand.get("endy"));

            try {
                String driverID = driverPool.getClosestDriver(startx, starty);
                User driver = loginDB.getRegisteredUser(driverID);
                StringBuilder sb = new StringBuilder("success: ;plate:");
                sb.append(driver.getPlate());
                sb.append(";make:");
                sb.append(driver.getModel());
                sb.append(";eta:");
                int eta = driverPool.getDistanceToDriver(driverID, startx, starty)/2 ; //Cabs go at 2 m/s
                sb.append(eta);


                pushSvrSocket = new ServerSocket(0);
                int pushPort = pushSvrSocket.getLocalPort();
                pushSvrSocket.setSoTimeout(5000);

                sb.append(";port:");
                sb.append(pushPort + ";");
                output.println(sb.toString());
                output.flush();

                pushCliSocket = pushSvrSocket.accept();
                pushCliSocket.setSoTimeout(5000);

                driverPool.assignDriver(driverID, pushCliSocket);


                //Now communicate with the driver to inform of the client to pickup;
                Socket driverSocket = driverPool.getAssignedDriverSocket(driverID);

                PrintWriter driverWriter = new PrintWriter(new OutputStreamWriter(driverSocket.getOutputStream()));

                sb.setLength(0); //Reset the SB

                sb.append("client: ;startx:");
                sb.append(startx);
                sb.append(";starty:");
                sb.append(starty);
                sb.append(";endx:");
                sb.append(endx);
                sb.append(";endy:");
                sb.append(endy);


                driverWriter.println(sb.toString());

                driverWriter.close();
            } catch (NoAvailableDriversException e) {
                output.println("failure:No drivers available at this time, please try again later;");
                output.flush();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void registrationHandler()
    {
        String username = lastCommand.get("username");
        String password = lastCommand.get("password");
        String make = lastCommand.get("make");
        String plate = lastCommand.get("plate");
        if (password == null || username == null) { //packet malformed
            output.println("Malformed packet");
            output.flush();
        }
        else
        {

            try {

                loginDB.registerUser(username, password, plate, make);
                loginHandler();//Login the user as well;
            }  catch (IOException e) {
                output.println("failure:IOException");
                output.flush();
            } catch (NoSuchAlgorithmException e) {
                output.println("failure:Failed encrypting data");
                output.flush();
            } catch (UserRegisteredException e ) {
                output.println("failure:Registration failed");//Precise error obscured to prevent user enumeration
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
        for(int i = 0; i < split.length - 1;)
        {
            lastCommand.put(split[i++], split[i++]);
        }
    }

    private void initHeartbeat() throws IOException {
        hbSvrSocket = new ServerSocket(0);
        int hbPort = hbSvrSocket.getLocalPort();
        hbSvrSocket.setSoTimeout(10000);
        //output.print(ClientServerCodes.svr2cli_heartbeatPort);
        output.println(hbPort);
        output.flush();
        hbCliSocket = hbSvrSocket.accept();
        hbCliSocket.setSoTimeout(10000);

        hbOut = new PrintWriter(new OutputStreamWriter(hbCliSocket.getOutputStream()));
        hbIn = new BufferedReader(new InputStreamReader(hbCliSocket.getInputStream()));

        hbSvrSocket.close();
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
