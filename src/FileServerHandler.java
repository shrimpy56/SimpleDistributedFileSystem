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

public class FileServerHandler implements FileServer.Iface
{
    private Map<String, Integer> versionMap = new ConcurrentHashMap<>();
    private String coordinatorIP;
    private int coordinatorPort;
    private File saveDir;

    void setData(String coordinatorIP, int coordinatorPort, int port)
    {
        this.coordinatorIP = coordinatorIP;
        this.coordinatorPort = coordinatorPort;

        try{
            saveDir = new File("./data/"+InetAddress.getLocalHost().getHostAddress()+"_"+port);
            if (!saveDir.exists())
            {
                saveDir.mkdirs();
            }
            else //clear folder
            {
                File[] files = saveDir.listFiles();
                for (int i = 0; i < files.length; ++i)
                {
                    files[i].delete();
                }
            }
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }

    @Override
    public int getVersionOf(String filename) throws org.apache.thrift.TException
    {
        return versionMap.getOrDefault(filename, -1);
    }

    @Override
    public String read(String filename) throws org.apache.thrift.TException
    {
        return null;
        //todo:
//        TTransport transport = new TSocket(coordinatorIP, coordinatorPort);
//        TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
//        Coordinator.Client client = new Coordinator.Client(protocol);
//        transport.open();
//        String result = client.read(filename);
//        transport.close();
//        return result;
    }

    @Override
    public void write(String filename, String contents) throws org.apache.thrift.TException
    {
        //todo:
//        TTransport transport = new TSocket(coordinatorIP, coordinatorPort);
//        TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
//        Coordinator.Client client = new Coordinator.Client(protocol);
//        transport.open();
//        client.write(filename, contents);
//        transport.close();
    }

    @Override
    public String doRead(String filename) throws org.apache.thrift.TException
    {
        String result = null;
        try
        {
            File file = new File(saveDir, filename);
            if (file != null && file.isFile() && file.exists())
            {
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
                result = "File " + filename + " does not exist!";
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();

            result = "Read file " + filename + " error!";
        }

        return result;
    }

    @Override
    public void doWrite(String filename, String contents) throws org.apache.thrift.TException {
        try
        {
            File file = new File(saveDir, filename);
            File parent = file.getParentFile();
            if (parent != null && parent.isDirectory() && !parent.exists())
            {
                parent.mkdirs();
            }

            PrintWriter output = new PrintWriter(file);
            output.print(contents);
            output.close();

            //update version
            Integer version = versionMap.get(filename);
            if (version == null)
            {
                versionMap.put(filename, 0);
            }
            else
            {
                versionMap.put(filename, version+1);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void clearFiles() throws org.apache.thrift.TException
    {
        //@todo delete this function
    }

    @Override
    public String getFileList() throws org.apache.thrift.TException
    {
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

