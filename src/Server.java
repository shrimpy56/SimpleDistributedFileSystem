import org.apache.thrift.TException;
import org.apache.thrift.server.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import java.net.InetAddress;

import java.io.*;
import java.util.*;
import java.lang.*;
import java.math.*;

public class Server {

    public static void main(String [] args) {
        try {
            // pass coordinator ip, coordinator port, server port.
            String serverIP = args[0];
            int serverPort = Integer.parseInt(args[1]);
            int port = Integer.parseInt(args[2]);

            // @todo: register node
            TTransport transport = new TSocket(serverIP, serverPort);
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
            Coordinator.Client serverClient = new Coordinator.Client(protocol);
            transport.open();
            serverClient.join(InetAddress.getLocalHost().getHostAddress(), port);
            transport.close();

            //Create Thrift server socket
            TServerTransport serverTransport = new TServerSocket(port);
            TTransportFactory factory = new TFramedTransport.Factory();
            //Create service request handler
            FileServerHandler handler = new FileServerHandler();
            handler.setData(serverIP, serverPort, port);
            FileServer.Processor processor = new FileServer.Processor(handler);
            //Set server arguments
            TThreadPoolServer.Args arguments = new TThreadPoolServer.Args(serverTransport);
            arguments.processor(processor);  //Set handler
            arguments.transportFactory(factory);  //Set FramedTransport (for performance)

            System.out.println("File server running on: " + InetAddress.getLocalHost().getHostAddress() + ":" + port);

            //Run server
            TServer server = new TThreadPoolServer(arguments);
            server.serve();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}

