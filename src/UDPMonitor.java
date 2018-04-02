import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

class UDPMonitor{

	final static int port = 8888;
	final static String inet_addr = "239.8.8.8";
	final static byte[] msg = "SIR".getBytes();
    static StateTable st = new StateTable();

	public static void main(String[] args) throws UnknownHostException, InterruptedException {

	    InetAddress addr = InetAddress.getByName(inet_addr);

		try (DatagramSocket serverSocket = new DatagramSocket()){

			Thread agentUDPResponse = new Thread(new ListenUDPAgents(serverSocket,st));
			agentUDPResponse.start();

            System.out.println("Sending");
			while (true){
				Thread.sleep(3 * 1000);
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

	DatagramSocket serverSocket;
    StateTable st;

	public ListenUDPAgents(DatagramSocket ss, StateTable st){
		this.serverSocket = ss;
        this.st = st;
	}

    public void run() {
		String msg;
        DatagramPacket receivePacket;
        byte[] receiveData = new byte[1024];

        while (true){
			try{
				receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				msg = new String(receivePacket.getData());
				this.st.updateLine(msg);
                System.out.println("Received message: " + msg);
                String[] aux = msg.split(";;");
			    System.out.println("Received message: " + st.getLine(aux[0]));
            }
			catch (IOException e){
				e.printStackTrace();
			}
		}
	}

}
