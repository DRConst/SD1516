package Server;

import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by drcon on 22/12/2015.
 */
public class DriverPool implements Serializable{

    private HashMap<String , Position> waitingDrivers;
    private HashMap<String , Socket> waitingDriversSocket; //Where to communicate with the driver when they have been requested
    private HashMap<String , Socket> assignedDriversSocket; //Where to communicate with the driver when they have been requested
    private HashMap<String , Socket> assignedClientsSocket; //Where to communicate with the client when they have been assigned a driver
                                                            //Key is the drivers ID
    private ReentrantLock wD, wDS, assignedLock;

    public DriverPool()
    {
        waitingDrivers = new HashMap<>();
        waitingDriversSocket = new HashMap<>();
        assignedDriversSocket = new HashMap<>();
        assignedClientsSocket = new HashMap<>();
        wD = new ReentrantLock();
        wDS  = new ReentrantLock();
        assignedLock = new ReentrantLock();
    }
    public void registerDriver(String userName, int x, int y, Socket driverSocket) throws DriverAlreadyRegisteredException{
        wD.lock();
        wDS.lock();

        if(waitingDrivers.containsKey(userName))
            throw new DriverAlreadyRegisteredException();
        waitingDrivers.put(userName, new Position(x, y));

        waitingDriversSocket.put(userName, driverSocket);

        wDS.unlock();
        wD.unlock();
    }

    public Socket getClientSocket(String driver)
    {
        return assignedClientsSocket.get(driver);
    }
    public String getClosestDriver(int x, int y) throws NoAvailableDriversException {
        int mapSize = waitingDrivers.size();
        Position clientPos = new Position(x, y);
        String currentMaxID = null;
        int currentMaxDist = 0;
        if(mapSize > 0)
        {



            Position driverPos;
            String driverID;
            Set<Map.Entry<String, Position>> entries = waitingDrivers.entrySet();
            Iterator<Map.Entry<String, Position>> it = entries.iterator();

            while(it.hasNext())
            {
                Map.Entry<String, Position> e = it.next();
                if(e.getValue().getManhattanDistance(clientPos) > currentMaxDist)
                {
                    currentMaxDist = e.getValue().getManhattanDistance(clientPos);
                    currentMaxID = e.getKey();
                }
            }
        }else
        {
            throw new NoAvailableDriversException();
        }

        return currentMaxID;
    }

    public int getDistanceToDriver(String driver, int x, int y)
    {
        Position clientPos = new Position(x, y);
        return waitingDrivers.get(driver).getManhattanDistance(clientPos);
    }

    public Socket getAssignedDriverSocket(String driver)
    {
        return assignedDriversSocket.get(driver);
    }

    public synchronized void assignDriver(String driver, Socket clientSocket) //This is the clients socket
    {
        wD.lock();
        wDS.lock();
        assignedLock.lock();

        assignedClientsSocket.put(driver, clientSocket); //Register where to communicate with the client that has been assigned to that driver.
        assignedDriversSocket.put(driver, waitingDriversSocket.get(driver)); //Change the driver to assigned.
        waitingDriversSocket.remove(driver);//Cleanup
        waitingDrivers.remove(driver);

        assignedLock.unlock();
        wDS.unlock();
        wD.unlock();

    }

    public void completedTrip(String driver)
    {
        assignedLock.lock();
        assignedClientsSocket.remove(driver);
        assignedDriversSocket.remove(driver);
        assignedLock.unlock();
    }
    private class Position
    {
        int x, y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getManhattanDistance(Position p)
        {
            return Math.abs(p.x - this.x) + Math.abs(p.y - this.y);
        }
    }
}
