import org.apache.thrift.TException;
import org.apache.thrift.server.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import java.net.InetAddress;

import java.io.*;
import java.util.*;

public class Coordinator {
    public static CoordinatorHandler handler;
    public static FileServer.Processor<CoordinatorHandler> processor;

    public static void main(String [] args) {
        try {
            // pass coordinator port, Nr, Nw
            int port = Integer.parseInt(args[0]);
            int Nr = Integer.parseInt(args[1]);
            int Nw = Integer.parseInt(args[2]);
            int N = Integer.parseInt(args[3]);
            if (Nr + Nw <= N) {
                System.out.println("Constraint Nr+Nw>N not satisfied.");
                return;
            }
            if (Nw <= N / 2) {
                System.out.println("Constraint Nw>N/2 not satisfied.");
                return;
            }

            String IP = InetAddress.getLocalHost().getHostAddress();
            //Create Thrift server socket
            TServerTransport serverTransport = new TServerSocket(port);
            TTransportFactory factory = new TFramedTransport.Factory();
            //Create service request handler
            handler = new CoordinatorHandler(Nr, Nw, N, IP, port);
            processor = new FileServer.Processor<CoordinatorHandler>(handler);
            //Set server arguments
            TThreadPoolServer.Args arguments = new TThreadPoolServer.Args(serverTransport);
            arguments.processor(processor);  //Set handler
            arguments.transportFactory(factory);  //Set FramedTransport (for performance)

            System.out.println("Coordinator running on: " + IP + ":" + port);

            Runnable ReqSync = new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1500);
                            // require sync every 1s
                            handler.sync();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread thread = new Thread(ReqSync);
            thread.start();

            //Run server
            TServer server = new TThreadPoolServer(arguments);
            server.serve();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}

