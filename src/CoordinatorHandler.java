import org.apache.thrift.TException;
import org.apache.thrift.server.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import java.net.InetAddress;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CoordinatorHandler implements FileServer.Iface {
    private ArrayList<String> ServerIP = new ArrayList<String>();
    private ArrayList<Integer> ServerPort = new ArrayList<Integer>();
    private static int ServerNum = 0;
    private static AtomicBoolean running = new AtomicBoolean(false);
    private static AtomicInteger ReqNum = new AtomicInteger(0);
    private final int Nr, Nw, N;
    private Map<String, Queue<Request>> RequestQueue;
    private Map<String, Integer> versionMap = new HashMap<String, Integer>();

    public CoordinatorHandler(int _nr, int _nw, int _n, String _ip, int _port) {
        this.Nr = _nr;
        this.Nw = _nw;
        this.N = _n;
        join(_ip, _port);
    }

    @Override
    boolean join(String IP, int port) throws org.apache.thrift.TException {
        System.out.println("Join request from IP:" + IP + ", port:" + port);
        if (ServerNum >= this.N) {
            System.out.println("Sorry, now the pool is full, can't join.");
            return false;
        }
        if (running.compareAndSet(false, true)) {
            // deal with multiple joins
            ServerIP.add(IP);
            ServerPort.add(port);
            ServerNum++;
            printServers();
            running.set(false);
            return true;
        }
        else {
            System.out.println("Sorry, the Coordinator is busy, can't join.");
            return false;
        }
    }

    @Override
    public String read(String filename) throws org.apache.thrift.TException {
        int SelfNum = ReqNum;
        ReqNum.incrementAndGet();
        Request req = new Request(true, SelfNum, filename, null);
        RequestQueue.get(filename).add(req);
        while ()
        return result;
    }

    @Override
    public void write(String filename, String contents) throws org.apache.thrift.TException {
        if (!VersionMap.containsKey(filename)) {
            VersionMap.put(filename, 0);
        }
        int SelfNum = ReqNum;
        ReqNum.incrementAndGet();
        Request req = new Request(false, SelfNum, filename, contents);
    }

    private ProcessReq(boolean RorW, String filename, String contents)
    // TRUE for Read, FALSE for Write
    {
        int size = -1;
        String Op = "";
        if (RorW) {
            size = Nr;
            Op = "read";
        }
        else {
            size = Nw;
            Op = "write";
        }
        boolean[] used = getTargetServer(size);
        for (int i = 0; i < ServerNum; i++) {
            if (used[i]) {
                String IP = ServerIP.get(i);
                int port = ServerPort.get(i);
                System.out.println("Choose Server IP=" + IP + ", port=" + port +" to " + Op + ".");
                try {
                    TTransport transport = new TSocket(IP, port);
                    TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                    FileServer.Client client = new FileServer.Client(protocol);
                    transport.open();
                    if (RorW) {
                        client.doRead(filename);
                    }
                    else {
                        client.doWrite(filename, contents);
                    }
                    transport.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean[] getTargetServer(int size) {
        boolean[] used = new boolean[ServerNum];
        Random r = new Random();
        for (int i = 0; i < size; i++) {
            while (true) {
                int random_server = r.nextInt(ServerNum);
                if (!used[random_server]) {
                    used[random_server] = true;
                    break;
                }
            }
        }
        return used;
    }

    private int findNewestVersion() {
        int maxIndex = -1;
        int maxVersion = -1;
        for (int i = 0; i < ServerNum; i++) {
            String IP = ServerIP.get(i);
            int port = ServerPort.get(i);
            try {
                TTransport transport = new TSocket(IP, port);
                TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                FileServer.Client client = new FileServer.Client(protocol);
                transport.open();
                int version = client.getVersionOf(filename);
                if (version > maxVersion) {
                    maxVersion = version;
                    maxIndex = i;
                }
                transport.close();
            }
            catch (Exception e) {
                e.printStackTrace();NewestVerNum
            }
        }
        return maxIndex;
        // give the machine which has the newest version
    }

    private void sync() {
        // TODO: get all max version
        for(int i = 0; i < ServerNum; i++) {
            String IP = ServerIP.get(i);
            int port = ServerPort.get(i);
            try {
                TTransport transport = new TSocket(IP, port);
                TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                FileServer.Client client = new FileServer.Client(protocol);
                transport.open();
                int version = client.getVersionOf(filename);
                if (version < maxVersion) {
                    client.doWrite();
                }
                transport.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void printServers() {
        System.out.println("Now we have following servers:");
        for(int i = 0; i < ServerNum; i++) {
            System.out.println("Server No." + i
                    + ", Server IP = " + ServerIP.get(i)
                    + ", Server port = " + ServerPort.get(i));
        }
        System.out.println("-----------------------------");
    }
}

public class Request {
    public boolean RW;
    public int RequestNo;
    public String file;
    public String content;
    public Request(boolean _rw, int _re, String _fi, String _co){
        this.RW = _rw;
        this.RequestNo = _re;
        this.file = _fi;
        this.content = _co;
    }
}
