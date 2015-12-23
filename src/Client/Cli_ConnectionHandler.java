package Client;

import Commons.ClientServerCodes;
import Commons.Serializer;
import Commons.User;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by drcon on 22/12/2015.
 */
public class Cli_ConnectionHandler {

    private Socket socket, hbSocket, pushSocket, driverPushSocket;
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
            if(user != null)
            {
                System.out.println("c - Request ride");
                if(user.getPlate() != null)
                {
                    System.out.println("d - Register as driver");
                    System.out.println("e - Report arrival at pickup location");
                    System.out.println("f - Report arrival at final location");
                }

            }




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
            if(user != null)
            {
                if(option.equals("c"))
                {
                    rideOption();
                }
                if(option.equals("d") && user.getPlate() != null)
                {
                    driverOption();
                }
                if(option.equals("e") && user.getPlate() != null)
                {
                    arrivalOption();
                }
                if(option.equals("f") && user.getPlate() != null)
                {
                    destinationOption();
                }
            }

        }
    }

    private void destinationOption()
    {
        String packetHeader = user.getPacketHeader("destination");
        int price = 0;
        try {
            price = Integer.parseInt(new BufferedReader(new InputStreamReader(System.in)).readLine());
            String packet = packetHeader + "price:" + price + ";";
            output.println(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void arrivalOption()
    {
        String packetHeader = user.getPacketHeader("arrival");
        output.println(packetHeader);

        try {
            String response = input.readLine();
            String[] responseChunks = response.split("[:;]");
            if(responseChunks[0].equals("success"))
            {
                System.out.println("The client has been informed of your arrival");
            }else{
                System.out.println("You have no assigned client, please wait until one is assigned to you");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void driverOption()
    {
        int x, y;
        try
        {
            System.out.println("Please specify starting location X:");
            x = new Integer(bufferedReader.readLine());
            System.out.println("Please specify starting location Y:");
            y = new Integer(bufferedReader.readLine());

            String packetHeader = user.getPacketHeader("driver");
            StringBuilder sb = new StringBuilder(packetHeader);
            sb.append("x:");
            sb.append(x);
            sb.append(";y:");
            sb.append(y);
            sb.append(";");

            output.println(sb.toString());

            int port = Integer.parseInt(input.readLine());

            driverPushSocket = new Socket(host,port);

            Thread pushThread = new Thread(() ->
            {

                try {
                    driverPushSocket.setSoTimeout(0);//Disable timeout, it might take a while to get a client
                    BufferedReader pushReader = new BufferedReader(new InputStreamReader(driverPushSocket.getInputStream()));

                    String pushResponse = pushReader.readLine();
                    String[] pushChunks = pushResponse.split("[:;]");

                    if(pushChunks[0].equals("client"))
                    {
                        System.out.println("A client has been found.");
                        System.out.println("Please head to (" + pushChunks[2] + ", " + pushChunks[4]
                                + "), and deliver the client to (" + pushChunks[6] + ", " + pushChunks[8] + ").");
                    }

                    driverPushSocket.close();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            pushThread.start();
        }catch (IOException e)
        {

        }

    }



    private void rideOption()
    {
        int startX, startY, endX, endY;
        try {
            System.out.println("Please specify starting location X:");
            startX = new Integer(bufferedReader.readLine());
            System.out.println("Please specify starting location Y:");
            startY = new Integer(bufferedReader.readLine());

            System.out.println("Please specify ending location X:");
            endX = new Integer(bufferedReader.readLine());
            System.out.println("Please specify ending location Y:");
            endY = new Integer(bufferedReader.readLine());

            System.out.println("Please wait, searching for available drivers");


            String packetHeader = user.getPacketHeader("request");
            StringBuilder sb = new StringBuilder(packetHeader);
            sb.append("startx:");
            sb.append(startX);
            sb.append(";starty:");
            sb.append(startY);
            sb.append(";endx:");
            sb.append(endX);
            sb.append(";endy:");
            sb.append(endY);

            String packet = sb.toString();
            output.println(packet);
            output.flush();

            String response = input.readLine();
            String[] chunks = response.split("[:;]");
            if(chunks[0].equals("success"))
            {
                System.out.println("A driver is on his way, please hold:");
                System.out.println("Expect a " + chunks[5] + " with the plate " + chunks[3] + " in about " + chunks[7] + " minutes.");
                pushSocket = new Socket(host, Integer.parseInt(chunks[9]));

                Thread pushThread = new Thread(() ->
                {
                    boolean finished = false;
                    try {
                        pushSocket.setSoTimeout(0);//Disable timeout, the driver might take long
                        BufferedReader pushReader = new BufferedReader(new InputStreamReader(pushSocket.getInputStream()));
                        while(!finished)
                        {
                            String pushResponse = pushReader.readLine();
                            String[] pushChunks = pushResponse.split("[:;]");
                            if(pushChunks[0].equals("arrival"))
                            {
                                System.out.println("Your driver has arrived.");
                            }else if (pushChunks[0].equals("price"))
                            {
                                System.out.println("You have arrived.");
                                System.out.println("The price to pay is " + pushChunks[1]);
                            }

                        }
                        pushSocket.close();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                pushThread.start();
            }else
            {
                System.out.println("Failed to register, the reason :  " + chunks[1]);
            }
        } catch (IOException e) {
            System.out.println("Failed to register");
            e.printStackTrace();
        }
    }

    private void registerOption()
    {
        String name, password;
        String plate, make;

        try {
            System.out.println("Please choose a username :");
            name = bufferedReader.readLine();
            System.out.println("Please choose a password :");
            password = bufferedReader.readLine();
            System.out.println("Please specify your car's plate (leave blank if N/A) :");
            plate = bufferedReader.readLine();
            System.out.println("Please specify your car's make (leave blank if N/A) :");
            make = bufferedReader.readLine();

            user = new User(name, password);

            String packetHeader = user.getPacketHeader("register");
            StringBuilder sb = new StringBuilder(packetHeader);
            if(!make.equals("") && !plate.equals(""))
            {
                sb.append("plate:");
                sb.append(plate);
                sb.append("make:");
                sb.append(make);
                sb.append(";");
            }

            output.println(sb.toString());
            output.flush();

            String response = input.readLine();
            String[] chunks = response.split(":");
            if(chunks[0].equals("success"))
            {
                System.out.println("Registration successful, you are now logged in");
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
                    //System.out.println("Server HB successful");
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
