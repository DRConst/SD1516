package Commons;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Diogo on 14/05/2015.
 */
public class Serializer {
    HashMap<String, ReentrantLock> locks;
    ReentrantLock hashLock;

    public Serializer()
    {
        locks = new HashMap<>();
        hashLock  = new ReentrantLock();
    }
    public Object readObject(String name) throws IOException, ClassNotFoundException {
        Object toRet = null;
        if(!locks.containsKey(name))
        {
            //First time writing to file, init lock
            hashLock.lock();//Make sure we don't get race conditions creating locks
            locks.put(name, new ReentrantLock());
            hashLock.unlock();
        }
        locks.get(name).lock();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(name + ".saved"))) {
            toRet = ois.readObject();
        }catch (IOException e)
        {
            return null;
        }
        locks.get(name).unlock();

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

    public static String serializeToString(Object o)
    {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bao);
            oos.writeObject(o);
            //return new BASE64Encoder().encode(bao.toByteArray()).toString();
            return Base64.getEncoder().encodeToString(bao.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static Object unserializeFromString(String s)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Object o = null;
        try {
            baos.write(s.getBytes("UTF-8"));
            ObjectInputStream oos = new ObjectInputStream(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(s)));
            o = oos.readObject();
            String classname = o.getClass().toString();
            classname.charAt(1);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return o;
    }





}