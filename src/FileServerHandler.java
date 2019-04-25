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
    protected Map<String, Integer> versionMap = new ConcurrentHashMap<>();
    protected String coordinatorIP;
    protected int coordinatorPort;
    protected File saveDir;

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
    public boolean write(String filename, String contents) {
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
        return true;
    }

    @Override
    public String doRead(String filename) {
        String result = "";
        try {
            File file = new File(saveDir, filename);
            if (file != null && file.isFile() && file.exists()) {
                System.out.println("reading file:"+file.getName());

                Scanner scanner = new Scanner(file);
                StringBuffer buffer = new StringBuffer();
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    buffer.append(line);
                }
                scanner.close();
                result = buffer.toString();
            }
            else
            {
                System.out.println("reading file error "+(file == null? "(null)" : file.getName()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
        // return empty string "" means not found
    }

    @Override
    public boolean doWrite(String filename, String contents, int version) {
        try {
            File file = new File(saveDir, filename);
            File parent = file.getParentFile();
            if (parent != null && parent.isDirectory() && !parent.exists()) {
                parent.mkdirs();
            }

            PrintWriter output = new PrintWriter(file);
            output.print(contents);
            output.close();

            System.out.println("writing. filename:"+filename+", contents:"+contents+", version:"+version);

            //update version
            versionMap.put(filename, version);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //log
        getFileList();

        return true;
    }

    @Override
    public String getFileList() {
        StringBuffer buffer = new StringBuffer();
        File[] files = saveDir.listFiles();

        String fileInfo = "List of folder " + saveDir.getPath();
        buffer.append(fileInfo + "\n");
        System.out.println(fileInfo);
        fileInfo = "===============================================";
        buffer.append(fileInfo + "\n");
        System.out.println(fileInfo);

        for (int i = 0; files != null && i < files.length; ++i)
        {
            fileInfo = "File: "+ files[i].getName() +", Version " + getVersionOf(files[i].getName());
            buffer.append(fileInfo + "\n");
            System.out.println(fileInfo);
        }
        fileInfo = "===============================================";
        buffer.append(fileInfo + "\n");
        System.out.println(fileInfo);

        return buffer.toString();
    }

    @Override
    public boolean join(String IP, int port) {
        System.out.println("Normal FileServer cannot process join from others.");
        return false;
    }
}

