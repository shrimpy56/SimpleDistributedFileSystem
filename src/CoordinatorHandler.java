import org.apache.thrift.TException;
import org.apache.thrift.server.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import java.net.InetAddress;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CoordinatorHandler implements FileServer.Iface
{
    private ArrayList<String> ServerIP = Collections.synchronizedList(new ArrayList<String>());
    private ArrayList<Integer> ServerPort = Collections.synchronizedList(new ArrayList<Integer>());
    private static AtomicInteger ServerNum = new AtomicInteger(0);
    private final int Nr, Nw, N;
    private Map<String, Integer> versionMap = new ConcurrentHashMap<String, Integer>();

    public CoordinatorHandler(int _nr, int _nw, int _n)
    {
        this.Nr = _nr;
        this.Nw = _nw;
        this.N = _n;
    }

    @Override
    bool join(String IP, int port) throws org.apache.thrift.TException
    {
        System.out.println("Join request from IP:" + IP + ", port:" + port);
        if (ServerNum >= this.N)
        {
            System.out.println("Sorry, now the pool is full, can't join.");
            return false;
        }
        ServerIP.add(IP);
        ServerPort.add(port);
        ServerNum.incrementAndGet();
        printServers();
        return true;
    }

    @Override
    public String read(String filename) throws org.apache.thrift.TException
    {
        if (!versionHashMap.containsKey(filename)) return "";
        taskQueue.get(filename);
        for(int i = 0; i < Nr; i++)
        {
            Random r = new Random(ServerNum);
            int random_server = r.nextInt();
            String IP = ServerIP.get(random_server);
            int port = ServerPort.get(random_server);
            TTransport transport = new TSocket(IP, port);
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
            FileServer.Client client = new FileServer.Client(protocol);
            transport.open();
            String result = client.doread(filename);
            transport.close();
        }
        return result;
    }

    @Override
    public void write(String filename, String contents) throws org.apache.thrift.TException
    {
        for(int i = 0; i < Nw; i++)
        {
            Random r = new Random(ServerNum);
            int random_server = r.nextInt();
            String IP = ServerIP.get(random_server);
            int port = ServerPort.get(random_server);
            TTransport transport = new TSocket(IP, port);
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
            FileServer.Client client = new FileServer.Client(protocol);
            transport.open();
            String result = client.dowrite(filename, contents);
            transport.close();
        }
        return;
    }

    private void printServers()
    {
        System.out.println("Now we have following servers:");
        for(int i = 0; i < this.N; i++)
        {
            System.out.println("Server No." + i
                    + ", Server IP = " + ServerIP.get(i)
                    + ", Server port = " + ServerPort.get(i));
        }
        System.out.println("-----------------------------");
    }
}

