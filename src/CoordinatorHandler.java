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

public class CoordinatorHandler extends FileServerHandler implements FileServer.Iface {
    private static ArrayList<String> ServerIP = new ArrayList<String>();
    private static ArrayList<Integer> ServerPort = new ArrayList<Integer>();
    private static int ServerNum = 0;
    private static AtomicBoolean running = new AtomicBoolean(false);
    private static AtomicInteger ReqNum = new AtomicInteger(0);
    private final int Nr, Nw, N;
    private static Map<String, Queue<Request>> RequestQueue = new ConcurrentHashMap<>();

    public CoordinatorHandler(int _nr, int _nw, int _n, String _ip, int _port) {
        this.Nr = _nr;
        this.Nw = _nw;
        this.N = _n;
        join(_ip, _port);
        // join itself, so i == 0 means itself is Coordinator

        try {
            saveDir = new File("../data/"+InetAddress.getLocalHost().getHostAddress()+"_"+_port);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }
            else {//clear folder
                File[] files = saveDir.listFiles();
                for (int i = 0; i < files.length; ++i) {
                    files[i].delete();
                }
            }
        }
        catch (Exception x) {
            x.printStackTrace();
        }
    }

    @Override
    public boolean join(String IP, int port) {
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
    public String read(String filename) {
        int SelfNum = ReqNum.intValue();
        ReqNum.incrementAndGet();
        Request req = new Request(true, SelfNum, filename, null);
        Queue<Request> tmp = RequestQueue.get(filename);
        if (tmp == null) {
            tmp = new ConcurrentLinkedQueue<Request>();
        }
        tmp.add(req);
        RequestQueue.put(filename, tmp);
        while (RequestQueue.get(filename).peek().RequestNo != SelfNum){
            try {
                Thread.sleep(1);
                // active as a lock for this file
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tmp = RequestQueue.get(filename);
        req = tmp.peek();
        String result = ProcessReq(req.RW, req.file, null);
        tmp.poll();
        RequestQueue.put(filename, tmp);

        return result;
    }

    @Override
    public boolean write(String filename, String contents) {
        if (!versionMap.containsKey(filename)) {
            versionMap.put(filename, 0);
        }
        int SelfNum = ReqNum.intValue();
        ReqNum.incrementAndGet();
        Request req = new Request(false, SelfNum, filename, contents);
        Queue<Request> tmp = RequestQueue.get(filename);
        if (tmp == null) {
            tmp = new ConcurrentLinkedQueue<Request>();
        }
        tmp.add(req);
        RequestQueue.put(filename, tmp);
        while (RequestQueue.get(filename).peek().RequestNo != SelfNum) {
            try{
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tmp = RequestQueue.get(filename);
        req = tmp.peek();
        ProcessReq(req.RW, req.file, req.content);
        tmp.poll();
        RequestQueue.put(filename, tmp);

        return true;
    }

    private String ProcessReq(boolean RorW, String filename, String contents) {
    // TRUE for Read, FALSE for Write
        int size = -1;
        String Op = "";
        String result = "";
        if (RorW) {
            size = Nr;
            Op = "read";
        }
        else {
            size = Nw;
            Op = "write";
        }
        int MaxVersion = -1;
        boolean[] used = getTargetServer(size);
        for (int i = 0; i < ServerNum; i++) {
            if (used[i]) {
                String IP = ServerIP.get(i);
                int port = ServerPort.get(i);
                System.out.println("Choose Server IP=" + IP + ", port=" + port +" to " + Op + ".");
                //Coordinator itself
                if (i == 0) {
                    int version = getVersionOf(filename);
                    System.out.println("version of file:" + filename + " is " + version);
                    if (version > MaxVersion) {
                        if (RorW) {
                            result = doRead(filename);
                        }
                        MaxVersion = version;
                        System.out.println("max version is:" + MaxVersion);
                    }
                    continue;
                }
                try {
                    TTransport transport = new TSocket(IP, port);
                    TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                    FileServer.Client client = new FileServer.Client(protocol);
                    transport.open();
                    int version = client.getVersionOf(filename);
                    System.out.println("version of file:" + filename + " is " + version);
                    if (version > MaxVersion) {
                        if (RorW) {
                            result = client.doRead(filename);
                        }
                        MaxVersion = version;
                        System.out.println("max version is:" + MaxVersion);
                    }
                    transport.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (!RorW) {
            // update by write
            for (int i = 0; i < ServerNum; i++) {
                if (used[i]) {
                    String IP = ServerIP.get(i);
                    int port = ServerPort.get(i);
                    if (i == 0) {
                        doWrite(filename, contents, MaxVersion + 1);
                        continue;
                    }
                    try {
                        TTransport transport = new TSocket(IP, port);
                        TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                        FileServer.Client client = new FileServer.Client(protocol);
                        transport.open();
                        client.doWrite(filename, contents, MaxVersion + 1);
                        transport.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return result;
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

    private int findNewestIndex(String filename) {
        int MaxIndex = 0;
        int MaxVersion = getVersionOf(filename);// for i == 0
        for (int i = 1; i < ServerNum; i++) {
            String IP = ServerIP.get(i);
            int port = ServerPort.get(i);
            try {
                TTransport transport = new TSocket(IP, port);
                TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                FileServer.Client client = new FileServer.Client(protocol);
                transport.open();
                int version = client.getVersionOf(filename);
                if (version > MaxVersion) {
                    MaxVersion = version;
                    MaxIndex = i;
                }
                transport.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return MaxIndex;
        // give the machine which has the newest version
    }

    private int findVersion(int machine, String filename) {
        int Version = -1;
        if (machine == 0) {
            Version = getVersionOf(filename);
            return Version;
        }
        String IP = ServerIP.get(machine);
        int port = ServerPort.get(machine);
        try {
            TTransport transport = new TSocket(IP, port);
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
            FileServer.Client client = new FileServer.Client(protocol);
            transport.open();
            Version = client.getVersionOf(filename);
            transport.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return Version;
    }

    private String findContent(int machine, String filename) {
        String Content = "";
        if (machine == 0) {
            Content = doRead(filename);
            return Content;
        }
        String IP = ServerIP.get(machine);
        int port = ServerPort.get(machine);
        try {
            TTransport transport = new TSocket(IP, port);
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
            FileServer.Client client = new FileServer.Client(protocol);
            transport.open();
            Content = client.doRead(filename);
            transport.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return Content;
    }

    public void sync() {
        System.out.println("Running Synch Operation.");
        for (String filename : versionMap.keySet()) {
            int SelfNum = ReqNum.intValue();
            ReqNum.incrementAndGet();
            Request req = new Request(false, SelfNum, null, null);
            Queue<Request> tmp = RequestQueue.get(filename);
            if (tmp == null) {
                tmp = new ConcurrentLinkedQueue<Request>();
            }
            tmp.add(req);
            RequestQueue.put(filename, tmp);
            while (RequestQueue.get(filename).peek().RequestNo != SelfNum) {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            tmp = RequestQueue.get(filename);
            req = tmp.peek();

            int MaxIndex = findNewestIndex(filename);
            int MaxVersion = findVersion(MaxIndex, filename);
            String contents = findContent(MaxIndex, filename);
            // get all max version
            for (int i = 0; i < ServerNum; i++) {
                if (i == 0) {
                    //todo:
                    int version = getVersionOf(filename);
                    if (version < MaxVersion) {
                        doWrite(filename, contents, MaxVersion);
                    }
                    continue;
                }
                String IP = ServerIP.get(i);
                int port = ServerPort.get(i);
                try {
                    TTransport transport = new TSocket(IP, port);
                    TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                    FileServer.Client client = new FileServer.Client(protocol);
                    transport.open();
                    //todo:
                    int version = client.getVersionOf(filename);
                    if (version < MaxVersion) {
                        client.doWrite(filename, contents, MaxVersion);
                    }
                    transport.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            tmp.poll();
            RequestQueue.put(filename, tmp);
        }
        System.out.println("Synch Operation Done.");
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

class Request {
    public boolean RW;
    public int RequestNo;
    public String file;
    public String content;
    public Request(boolean _rw, int _re, String _fi, String _co) {
        this.RW = _rw;
        this.RequestNo = _re;
        this.file = _fi;
        this.content = _co;
    }
    public Request(Request req) {
        this.RW = req.RW;
        this.RequestNo = req.RequestNo;
        this.file = req.file;
        this.content = req.content;
    }
}
