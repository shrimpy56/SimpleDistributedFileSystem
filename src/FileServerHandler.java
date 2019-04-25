import org.apache.thrift.TException;
import org.apache.thrift.server.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import java.net.InetAddress;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.util.concurrent.*;

public class FileServerHandler implements FileServer.Iface {
    private Map<String, Integer> versionMap = new ConcurrentHashMap<>();
    private String coordinatorIP;
    private int coordinatorPort;
    private File saveDir;

    void setData(String coordinatorIP, int coordinatorPort, int port) {
        this.coordinatorIP = coordinatorIP;
        this.coordinatorPort = coordinatorPort;

        try {
            saveDir = new File("../data/"+InetAddress.getLocalHost().getHostAddress()+"_"+port);
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
    public int getVersionOf(String filename) {
        return versionMap.getOrDefault(filename, -1);
    }

    @Override
    public String read(String filename) {
        String result = "";
        try {
            TTransport transport = new TSocket(coordinatorIP, coordinatorPort);
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
            FileServer.Client client = new FileServer.Client(protocol);
            transport.open();
            result = client.read(filename);
            transport.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void write(String filename, String contents) {
        try {
            TTransport transport = new TSocket(coordinatorIP, coordinatorPort);
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
            FileServer.Client client = new FileServer.Client(protocol);
            transport.open();
            client.write(filename, contents);
            transport.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        for (int i = 0; i < files.length; ++i) {
            String fileInfo = "File: "+ files[i].getName() +", version " + getVersionOf(files[i].getName());
            buffer.append(fileInfo+"\n");
            System.out.println(fileInfo);
        }
        System.out.println("===============================================");
        return buffer.toString();
    }

    @Override
    public boolean join(String IP, int port) {
        System.out.println("Normal FileServer cannot process join from others.");
        return false;
    }
}

