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

	    InetAddress addr = InetAddress.getByName(inet_addr);
	    Timer t = new Timer();

		try (DatagramSocket serverSocket = new DatagramSocket()){

			Thread agentUDPResponse = new Thread(new ListenUDPAgents(serverSocket,t));
			agentUDPResponse.start();

            System.out.println("Sending");
			while (true){
				Thread.sleep(3 * 1000);
				DatagramPacket msgPacket = new DatagramPacket(msg, msg.length, addr, port);
				serverSocket.send(msgPacket);
				t.time = System.currentTimeMillis();
			}
		}
		catch (IOException e){
			e.printStackTrace();
		}
  	}

}

class Timer{
	long time;
}

class ListenUDPAgents implements Runnable {

	byte[] receiveData;
	DatagramSocket serverSocket;
	DatagramPacket receivePacket;
	String msg;
	Timer t;
	long endTime;


	public ListenUDPAgents(DatagramSocket ss, Timer timer){
		receiveData = new byte[1024];
		serverSocket = ss;
		t = timer;
	}

    public void run() {
		while (true){
			try{
				receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				endTime = System.currentTimeMillis();
				msg = new String(receivePacket.getData());
				System.out.println("Received message: " + msg + ", RTT = " + (endTime - t.time) + " ms");
			}
			catch (IOException e){
				e.printStackTrace();
			}
		}
	}

}