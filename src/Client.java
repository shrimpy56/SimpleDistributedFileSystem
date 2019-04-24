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
import java.util.concurrent.*;

public class Client {
    public static void main(String [] args) {
        //Create client connect.
        try {
            // pass params in
            String serverIP = args[0];
            int serverPort = Integer.parseInt(args[1]);

            TTransport transport = new TSocket(serverIP, serverPort);
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
            FileServer.Client server = new FileServer.Client(protocol);

            System.out.println("Usage: read:<filename>");
            System.out.println("Usage: write:<filename>:contents");
            System.out.println("Usage: test:<numOfClients>:<writeTimes>:<readTimes>:<fileNumber>");

            Scanner input = new Scanner(System.in);
            while (true)
            {
                String line = input.nextLine();
                String[] params = line.split(":");
                if (params.length < 2) continue;

                if (params[0].equals("read"))
                {
                    transport.open();
                    String result = server.read(params[1]);
                    transport.close();
                    System.out.println("================Read Result====================");
                    System.out.println(result);
                    System.out.println("===============================================");
                }
                else if (params[0].equals("write"))
                {
                    transport.open();
                    server.write(params[1], params[2]);
                    transport.close();
                }
                else if (params[0].equals("test"))
                {
                    int numOfClients = Integer.parseInt(params[1]);
                    int writeTimes = Integer.parseInt(params[2]);
                    int readTimes = Integer.parseInt(params[3]);
                    int fileNumber = Integer.parseInt(params[4]);

                    //start timer
                    long startTime = System.currentTimeMillis();

                    countDown = new CountDownLatch(numOfClients);
                    for (int i = 0; i < numOfClients; ++i)
                    {
                        Thread thread = new Thread(new Runnable(){
                            @Override
                            public void run() {
                                for (int j = 0; j < writeTimes; ++j)
                                {
                                    int random = (int)(Math.random() * fileNumber);
                                    String filename = "filename"+random;

                                    transport.open();
                                    server.write(filename, filename);
                                    transport.close();
                                }
                                for (int j = 0; j < readTimes; ++j)
                                {
                                    int random = (int)(Math.random() * fileNumber);
                                    String filename = "filename"+random;

                                    transport.open();
                                    String result = server.read(filename);
                                    transport.close();
                                }
                                countDown.countDown();
                            }
                        });
                        thread.start();
                    }

                    try
                    {
                        countDown.await();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    //end timer
                    long timeUsed = System.currentTimeMillis() - startTime;

                    //timer end
                    System.out.println("=================Benchmark=====================");
                    System.out.println("numOfClients: " + numOfClients);
                    System.out.println("writeTimes: " + writeTimes);
                    System.out.println("readTimes: " + readTimes);
                    System.out.println("fileNumber: " + fileNumber);
                    System.out.println("time used: " + timeUsed + " ms");
                    System.out.println("===============================================");
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
