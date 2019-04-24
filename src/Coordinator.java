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
    public static FileServer.Processor<CoordinatorServerHandler> processor;

    public static void main(String [] args) {
        try {
            // pass coordinator port, Nr, Nw
            int port = Integer.parseInt(args[0]);
            int Nr = Integer.parseInt(args[1]);
            int Nw = Integer.parseInt(args[2]);
            int N = Integer.parseInt(args[3]);
            if (Nr + Nw <= N)
            {
                System.out.println("Constraint Nr+Nw>N not satisfied.");
                return;
            }
            if (Nw <= N / 2)
            {
                System.out.println("Constraint Nw>N/2 not satisfied.");
                return;
            }

            //Create Thrift server socket
            TServerTransport serverTransport = new TServerSocket(port);
            TTransportFactory factory = new TFramedTransport.Factory();
            //Create service request handler
            handler = new CoordinatorHandler(Nr, Nw, N);
            processor = new FileServer.Processor(handler);
            //Set server arguments
            TThreadPoolServer.Args arguments = new TThreadPoolServer.Args(serverTransport);
            arguments.processor(processor);  //Set handler
            arguments.transportFactory(factory);  //Set FramedTransport (for performance)

            System.out.println("Coordinator running on: " + InetAddress.getLocalHost().getHostAddress() + ":" + port);
            //Run server
            TServer server = new TThreadPoolServer(arguments);
            server.serve();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}

