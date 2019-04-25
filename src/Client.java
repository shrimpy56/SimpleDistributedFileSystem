import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import java.io.*;
import java.util.*;
import java.lang.*;

public class Client {
    private static ArrayList<String> ServerIP = new ArrayList<String>();
    private static ArrayList<Integer> ServerPort = new ArrayList<Integer>();
    private static int ServerNum = 0;

    public static void main(String [] args) {
        // passing parameters
        // Client ServerFile ServerNum mode
        int argc = args.length;
        if (argc < 3) {
            System.out.println("Error. Too less parameters.");
            return;
        }
        String ServerFile = args[0];
        ServerNum = Integer.parseInt(args[1]);
        String mode = args[2];
        File file = new File(ServerFile);
        if (!file.exists()) {
            System.out.println("Error. ServerFile does not exist.");
            return;
        }
        if (ServerNum < 1) {
            System.out.println("Error. ServerNum is less than 1.");
            return;
        }
        input_server(ServerFile, ServerNum);
        if (mode.equals("UI")) {
            testUI();
        }
        else if (mode.equals("auto")) {
            if (argc < 6) {
                System.out.println("Too less parameters. Can't do auto test.");
                return;
            }
            int writeTimes = Integer.parseInt(args[3]);
            int readTimes = Integer.parseInt(args[4]);
            int fileNumber = Integer.parseInt(args[5]);
            auto_test(writeTimes, readTimes, fileNumber);
        }
    }

    private static void testUI() {
        try {
            System.out.println("Usage: read:<filename>");
            System.out.println("Usage: write:<filename>:contents");
            System.out.println("Usage: list");
            System.out.println("Usage: exit");

            Scanner input = new Scanner(System.in);
            Random r = new Random();
            while (true) {
                //Create client connect.
                int random_server = (int)(Math.random() * ServerNum);
                String IP = ServerIP.get(random_server);
                int Port = ServerPort.get(random_server);
                TTransport transport = new TSocket(IP, Port);
                TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                FileServer.Client server = new FileServer.Client(protocol);

                String line = input.nextLine();
                String[] params = line.split(":");
                if (params.length < 1) continue;
                if (params[0].equals("read")) {
                    transport.open();
                    String result = server.read(params[1]);
                    transport.close();
                    System.out.println("================Read Result====================");
                    System.out.println(result.isEmpty() ? "File not found" : result);
                    System.out.println("===============================================");
                }
                else if (params[0].equals("write")) {
                    transport.open();
                    server.write(params[1], params[2]);
                    transport.close();
                    System.out.println("write finish!");
                }
                else if (params[0].equals("list")) {
                    showAllVersions();
                }
                else if (params[0].equals("exit")) {
                    break;
                }
                else {
                    System.out.println("Wrong Command. Try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void auto_test(int writeTimes, int readTimes, int fileNumber) {
        //start timer
        long startTime = System.currentTimeMillis();
        try {
            int random_server = (int)(Math.random() * ServerNum);
            String IP = ServerIP.get(random_server);
            int Port = ServerPort.get(random_server);
            TTransport transport = new TSocket(IP, Port);
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
            FileServer.Client server = new FileServer.Client(protocol);
            transport.open();
            for (int j = 0; j < writeTimes; ++j) {
                int random = (int)(Math.random() * fileNumber);
                String filename = "filename"+random;
                server.write(filename, filename);
            }
            for (int j = 0; j < readTimes; ++j) {
                int random = (int)(Math.random() * fileNumber);
                String filename = "filename"+random;
                String result = server.read(filename);
            }
            transport.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //end timer
        long timeUsed = System.currentTimeMillis() - startTime;
        //timer end
        System.out.println("=================Benchmark=====================");
        System.out.println("writeTimes: " + writeTimes);
        System.out.println("readTimes: " + readTimes);
        System.out.println("fileNumber: " + fileNumber);
        System.out.println("time used: " + timeUsed + " ms");
        System.out.println("===============================================");
    }

    private static void input_server(String file, int num) {
        try {
            BufferedReader input = new BufferedReader(new FileReader(file));
            for(int i = 0; i < num; i++) {
                String line = input.readLine();
                String[] params = line.split(":");
                ServerIP.add(params[0]);
                int port = Integer.parseInt(params[1]);
                ServerPort.add(port);
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showAllVersions() {
        try {
            for (int i = 0; i < ServerNum; i++) {
                String IP = ServerIP.get(i);
                int Port = ServerPort.get(i);
                TTransport transport = new TSocket(IP, Port);
                TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
                FileServer.Client server = new FileServer.Client(protocol);
                transport.open();
                String result = server.getFileList();
                System.out.println(result);
                transport.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
