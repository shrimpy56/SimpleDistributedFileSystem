import org.apache.thrift.TException;
import org.apache.thrift.server.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import java.net.InetAddress;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CoordinatorHandler implements FileServer.Iface
{
    private ArrayList<String> ServerIP = new ArrayList<String>();
    private ArrayList<Integer> ServerPort = new ArrayList<Integer>();
    private static int ServerNum = 0;
    private static AtomicBoolean running = new AtomicBoolean(false);
    private final int Nr, Nw, N;
    private Map<String, Integer> versionMap = new ConcurrentHashMap<String, Integer>();

    public CoordinatorHandler(int _nr, int _nw, int _n)
    {
        this.Nr = _nr;
        this.Nw = _nw;
        this.N = _n;
    }

    @Override
    boolean join(String IP, int port) throws org.apache.thrift.TException
    {
        System.out.println("Join request from IP:" + IP + ", port:" + port);
        if (ServerNum >= this.N)
        {
            System.out.println("Sorry, now the pool is full, can't join.");
            return false;
        }
        if (running.compareAndSet(false, true))
        // deal with multiple joins
        {
            ServerIP.add(IP);
            ServerPort.add(port);
            ServerNum.incrementAndGet();
            printServers();
            running.set(false);
            return true;
        }
        else
        {
            System.out.println("Sorry, the Coordinator is busy, can't join.");
            return false;
        }
    }

    @Override
    public String read(String filename) throws org.apache.thrift.TException
    {
        boolean[] used = getTargetServer(this.Nr);
        for(int i = 0; i < this.ServerNum; i++)
        {
            if (used[i])
            {
                String IP = ServerIP.get(i);
                int port = ServerPort.get(i);
                System.out.println("Choose Server IP=" + IP + ", port=" + port +" to read.");
                try
                {
                    TTransport transport = new TSocket(IP, port);
                    TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                    FileServer.Client client = new FileServer.Client(protocol);
                    transport.open();
                    String result = client.doRead(filename);
                    int version = client.getVersionOf(filename);
                    transport.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    @Override
    public void write(String filename, String contents) throws org.apache.thrift.TException
    {
        boolean[] used = getTargetServer(this.Nw);
        for(int i = 0; i < this.ServerNum; i++)
        {
            if (used[i])
            {
                String IP = ServerIP.get(i);
                int port = ServerPort.get(i);
                System.out.println("Choose Server IP=" + IP + ", port=" + port +" to write.");
                try
                {
                    TTransport transport = new TSocket(IP, port);
                    TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                    FileServer.Client client = new FileServer.Client(protocol);
                    transport.open();
                    client.doWrite(filename, contents);
                    int version = client.getVersionOf(filename);
                    transport.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        return;
    }

    private boolean[] getTargetServer(int size)
    {
        boolean[] used = new boolean[this.ServerNum];
        Random r = new Random();
        for(int i = 0; i < size; i++)
        {
            while (true)
            {
                int random_server = r.nextInt(this.ServerNum);
                if (!used[random_server])
                {
                    used[random_server] = true;
                    break;
                }
            }
        }
        return used;
    }

    private int findNewestVersion()
    {
        int maxIndex = -1;
        int maxVersion = -1;
        for(int i = 0; i < this.ServerNum; i++)
        {
            String IP = ServerIP.get(i);
            int port = ServerPort.get(i);
            try
            {
                TTransport transport = new TSocket(IP, port);
                TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                FileServer.Client client = new FileServer.Client(protocol);
                transport.open();
                int version = client.getVersionOf(filename);
                if (version > maxVersion)
                {
                    maxVersion = version;
                    maxIndex = i;
                }
                transport.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return maxIndex;
        // give the machine which has the newest version
    }

    private void sync()
    {
        for(int i = 0; i < this.ServerNum; i++)
        {
            String IP = ServerIP.get(i);
            int port = ServerPort.get(i);
            try
            {
                TTransport transport = new TSocket(IP, port);
                TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                FileServer.Client client = new FileServer.Client(protocol);
                transport.open();
                int version = client.getVersionOf(filename);
                if (version < maxVersion)
                {
                    client.doWrite();
                }
                transport.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void printServers()
    {
        System.out.println("Now we have following servers:");
        for(int i = 0; i < this.ServerNum; i++)
        {
            System.out.println("Server No." + i
                    + ", Server IP = " + ServerIP.get(i)
                    + ", Server port = " + ServerPort.get(i));
        }
        System.out.println("-----------------------------");
    }
}

