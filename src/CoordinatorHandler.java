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
    private static ArrayList<String> ServerIP = new ArrayList<String>();
    private static ArrayList<Integer> ServerPort = new ArrayList<Integer>();
    private static int ServerNum = 0;
    private static AtomicBoolean running = new AtomicBoolean(false);
    private static AtomicInteger ReqNum = new AtomicInteger(0);
    private final int Nr, Nw, N;
    private static Map<String, Queue<Request>> RequestQueue = new HashMap<String, Queue<Request>>();
    private static Map<String, Integer> versionMap = new HashMap<String, Integer>();
    private static boolean NeedSync = false;
    private static File saveDir;

    public CoordinatorHandler(int _nr, int _nw, int _n, String _ip, int _port) {
        this.Nr = _nr;
        this.Nw = _nw;
        this.N = _n;
        join(_ip, _port);
        // join itself, so i == 0 means itself is Coordinator
        saveDir = new File("../data/"+_ip+"_"+_port);
    }

    public void SetSync() {
        NeedSync = true;
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
        tmp.add(req);
        RequestQueue.put(filename, tmp);
        while (RequestQueue.get(filename).peek().RequestNo != SelfNum){
            if (NeedSync) {
                sync();
                NeedSync = false;
            }
            // active as a lock for this file
        }
        tmp = RequestQueue.get(filename);
        req = tmp.remove();
        RequestQueue.put(filename, tmp);
        String result = ProcessReq(req.RW, req.file, null);
        return result;
    }

    @Override
    public void write(String filename, String contents) {
        if (!versionMap.containsKey(filename)) {
            versionMap.put(filename, 0);
        }
        int SelfNum = ReqNum.intValue();
        ReqNum.incrementAndGet();
        Request req = new Request(false, SelfNum, filename, contents);
        Queue<Request> tmp = RequestQueue.get(filename);
        tmp.add(req);
        while (RequestQueue.get(filename).peek().RequestNo != SelfNum) {
            if (NeedSync) {
                sync();
                NeedSync = false;
            }
        }
        tmp = RequestQueue.get(filename);
        req = tmp.remove();
        RequestQueue.put(filename, tmp);
        ProcessReq(req.RW, req.file, req.content);
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
                if (i == 0) {
                    int version = getVersionOf(filename);
                    if (version > MaxVersion) {
                        if (RorW) {
                            result = doRead(filename);
                        }
                        MaxVersion = version;
                    }
                    continue;
                }
                try {
                    TTransport transport = new TSocket(IP, port);
                    TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                    FileServer.Client client = new FileServer.Client(protocol);
                    transport.open();
                    int version = getVersionOf(filename);
                    if (version > MaxVersion) {
                        if (RorW) {
                            result = client.doRead(filename);
                        }
                        MaxVersion = version;
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

    private void sync() {
        for (String filename : versionMap.keySet()) {
            int MaxIndex = findNewestIndex(filename);
            int MaxVersion = findVersion(MaxIndex, filename);
            String contents = findContent(MaxIndex, filename);
            versionMap.put(filename, MaxVersion);
            // get all max version
            for (int i = 0; i < ServerNum; i++) {
                if (i == 0) {
                    int version = getVersionOf(filename);
                    if (version < MaxVersion) {
                        doWrite(filename, contents, version);
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
                    int version = client.getVersionOf(filename);
                    if (version < MaxVersion) {
                        client.doWrite(filename, contents, version);
                    }
                    transport.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
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

    @Override
    public int getVersionOf(String filename) {
        return versionMap.getOrDefault(filename, -1);
    }

    @Override
    public String doRead(String filename) {
        String result = "";
        try {
            File file = new File(saveDir, filename);
            if (file != null && file.isFile() && file.exists()) {
                Scanner scanner = new Scanner(file);
                StringBuffer buffer = new StringBuffer();
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    buffer.append(line);
                }
                scanner.close();
                result = buffer.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
        // return empty string "" means not found
    }

    @Override
    public void doWrite(String filename, String contents, int version) {
        try {
            File file = new File(saveDir, filename);
            File parent = file.getParentFile();
            if (parent != null && parent.isDirectory() && !parent.exists()) {
                parent.mkdirs();
            }

            PrintWriter output = new PrintWriter(file);
            output.print(contents);
            output.close();

            //update version
            versionMap.put(filename, version);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getFileList() {
        StringBuffer buffer = new StringBuffer();
        File[] files = saveDir.listFiles();
        System.out.println("List of folder " + saveDir.getPath());
        System.out.println("===============================================");
        for (int i = 0; i < files.length; ++i)
        {
            String fileInfo = "File: "+ files[i].getName() +", version " + getVersionOf(files[i].getName());
            buffer.append(fileInfo+"\n");
            System.out.println(fileInfo);
        }
        System.out.println("===============================================");
        return buffer.toString();
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
