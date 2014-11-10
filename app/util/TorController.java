package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 30/10/14
 * Time: 14:13
 */
public class TorController {

    private static TorController ourInstance = new TorController();

    public static TorController getInstance() {
        return ourInstance;
    }

    private PrintWriter out;
    private BufferedReader in;

    private TorController() {
        Socket socket;
        try{
            socket = new Socket("localhost", 9051);
            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.write("AUTHENTICATE \"\"");
            System.out.println(in.readLine());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void renewCircuit(){
        out.write("SIGNAL NEWNYM");
        try {
            System.out.println("Tor said: "+in.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
