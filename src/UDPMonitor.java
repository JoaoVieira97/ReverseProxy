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
	    Timer t = new Timer();

		try (DatagramSocket serverSocket = new DatagramSocket()){

			Thread agentUDPResponse = new Thread(new ListenUDPAgents(serverSocket,st,t));
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

	DatagramSocket serverSocket;
    StateTable st;
    Timer t;
    long endTime;

	public ListenUDPAgents(DatagramSocket ss, StateTable st, Timer timer){
		this.serverSocket = ss;
        this.st = st;
        this.t = timer;
	}

    public void run() {
		String msg;
        DatagramPacket receivePacket;
        byte[] receiveData = new byte[1024];

        while (true){
			try{
				receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				endTime = System.currentTimeMillis();
				msg = new String(receivePacket.getData());
				
                msg = msg + ";;" + (endTime - t.time);
                this.st.updateLine(msg);
                String[] aux = msg.split(";;");
			    System.out.println("Received message: " + st.getLine(aux[0]));
            }
			catch (IOException e){
				e.printStackTrace();
			}
		}
	}

}
