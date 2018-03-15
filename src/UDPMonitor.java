import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

class UDPMonitor{

	final static int port = 8888;
	final static String inet_addr = "239.8.8.8";
	final static byte[] msg = "SIR".getBytes();
	
	public static void main(String[] args) throws UnknownHostException, InterruptedException {
		
		Thread agentUDPResponse = new Thread(new ListenUDPAgents());
		agentUDPResponse.start();

	    InetAddress addr = InetAddress.getByName(inet_addr);

		try (DatagramSocket serverSocket = new DatagramSocket()){
			while (true){
				DatagramPacket msgPacket = new DatagramPacket(msg, msg.length, addr, port);
				serverSocket.send(msgPacket);
			}
		}
		catch (IOException e){
			e.printStackTrace();
		}
  	}

}

class ListenUDPAgents implements Runnable {

    public void run() {
        System.out.println("TO DO");
    }

}