import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.SecretKey;
import java.security.KeyPairGenerator;
import java.security.KeyPair;


class UDPMonitor{

	final static int port = 8888;
	final static String inet_addr = "239.8.8.8";
	final static byte[] msg = "SIR;;".getBytes();
    static StateTable st = new StateTable();

	public static void main(String[] args) throws UnknownHostException, InterruptedException {

	    InetAddress addr = InetAddress.getByName(inet_addr);
	    Timer t = new Timer();

		try (DatagramSocket serverSocket = new DatagramSocket()){
			
			KeyPairGenerator keyPairGen=KeyPairGenerator.getInstance("RSA");
			KeyPair keyPair=keyPairGen.generateKeyPair();
			PublicKey pkey=keyPair.getPublic();
			byte[] pKeyBytes=pkey.getEncoded();
			byte[] fullMessage=new byte[msg.length+pKeyBytes.length];
			System.arraycopy(msg, 0, fullMessage, 0, msg.length);
			System.arraycopy(pKeyBytes, 0, fullMessage, msg.length, pkeyBytes.length);

			Thread agentUDPResponse = new Thread(new ListenUDPAgents(serverSocket,st,t));
			agentUDPResponse.start();

            System.out.println("Sending");
			while (true){
				Thread.sleep(3 * 1000);
				DatagramPacket msgPacket = new DatagramPacket(fullMessage, fullMessage.length, addr, port);
				serverSocket.send(msgPacket);
				t.time = System.currentTimeMillis();
			}
		}
		catch (IOException|NoSuchAlgorithmException e){
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
		byte[] msgHash,msgInfo,calcHash;
		
		try{
			MessageDigest hasher = MessageDigest.getInstance("SHA-256");
			while (true){
				try{
					receivePacket = new DatagramPacket(receiveData, receiveData.length);
					serverSocket.receive(receivePacket);
					endTime = System.currentTimeMillis();
					String srcIp=receivePacket.getAddress().getHostAddress();
					
					receiveData=receivePacket.getData();
					msgInfo=Arrays.copyOfRange(receiveData,32,receivePacket.getLength());
					msgHash=Arrays.copyOfRange(receiveData,0,32);
					hasher.reset();
					hasher.update(msgInfo);
					calcHash=hasher.digest();
					msg=new String(msgInfo);

					if(Arrays.toString(calcHash).equals(Arrays.toString(msgHash))){
						msg = receivePacket.getAddress().getHostAddress() + ";;" + receivePacket.getPort() + ";;" + msg + ";;" + (endTime - t.time);
						this.st.updateLine(msg);
						String[] aux = msg.split(";;");
						System.out.println("Received message: " + st.getLine(aux[0]));
					}else{
						System.out.println("Corrupted packet: hashes do not match\n");
					}
				}
				catch (IOException e){
					e.printStackTrace();
				}
			}
		}catch(NoSuchAlgorithmException e){
			e.printStackTrace();
		}
	}

}
