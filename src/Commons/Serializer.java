package Commons;
import java.io.*;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Diogo on 14/05/2015.
 */
public class Serializer {
    HashMap<String, ReentrantLock> locks;
    ReentrantLock hashLock = new ReentrantLock();

    public Object readObject(String name) throws IOException, ClassNotFoundException {
        Object toRet = null;
        if(locks.containsKey(name)) // Something is writing to the file
        {
            locks.get(name).lock();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(name + ".saved"))) {
                toRet = ois.readObject();
            }
            locks.get(name).unlock();
        }
        else
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(name + ".saved"))) {
                toRet = ois.readObject();
            }

        return toRet;
    }

    public void writeObject(Object o) throws IOException { //Multithreading might pose problems with concurrent writes so lock the file

       
        String name = o.getClass().getName();

        if(!locks.containsKey(name))
        {//First time writing to file, init lock
            hashLock.lock();//Make sure we dont get race conditions creating locks
            locks.put(name, new ReentrantLock());
            hashLock.unlock();
        }
        //Lock is already inited, acquire it
        locks.get(name).lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(name + ".saved"))) {
            oos.writeObject(o);
        }
        locks.get(name).unlock();
    }



}