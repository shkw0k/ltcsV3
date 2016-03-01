package utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpService extends Thread {
    ServerSocket thisSocket;    
    boolean mustStop = false;
    HandlerFactory hFactory;
 
    public HttpService(int portNr, HandlerFactory hfact) throws IOException {
        ServerSocket ss = new ServerSocket(portNr);
        thisSocket = ss;
        hFactory = hfact;
    } // constructor
  
    public void run() {
        while (!mustStop) {
            try {
                Socket ss = thisSocket.accept(); 
            	HttpHandler handler = hFactory.createHandler();
                handler.setSocket (ss);
                handler.processRequest();
                handler = null;
                System.gc();
            } catch (IOException e) {
            	Logger.error(e.getMessage());
                return;
            }        
        }
    } // run
}

