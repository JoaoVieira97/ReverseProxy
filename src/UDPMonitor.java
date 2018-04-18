import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.SecretKey;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;

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
			keyPairGen.initialize(2048);
			KeyPair keyPair=keyPairGen.generateKeyPair();
			PublicKey pkey=keyPair.getPublic();
			byte[] pKeyBytes=pkey.getEncoded();
			byte[] fullMessage=new byte[msg.length+pKeyBytes.length];
			System.arraycopy(msg, 0, fullMessage, 0, msg.length);
			System.arraycopy(pKeyBytes, 0, fullMessage, msg.length, pKeyBytes.length);

			Thread agentUDPResponse = new Thread(new ListenUDPAgents(serverSocket,st,t,keyPair.getPrivate()));
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
    PrivateKey pk;

	public ListenUDPAgents(DatagramSocket ss, StateTable st, Timer timer, PrivateKey pk){
		this.serverSocket = ss;
        this.st = st;
        this.t = timer;
        this.pk = pk;
	}

    public void run() {
    	try{
			String msg;
	        DatagramPacket receivePacket;
	        Mac hmac256 = Mac.getInstance("HmacSHA256");

			byte[] receiveData = new byte[1024];
			byte[] hashReceived = new byte[32];
			byte[] msgReceived = new byte[992];
			byte[] hash;

       		while (true){
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                endTime = System.currentTimeMillis();
                String srcIp=receivePacket.getAddress().getHostAddress();
                
                receiveData=receivePacket.getData();

				System.arraycopy(receiveData,0,hashReceived,0,32);
				System.arraycopy(receiveData,32,msgReceived,0,receiveData.length-32);

				msg = Arrays.toString(msgReceived);
                hmac256.reset();
                hmac256.update(msgReceived);
                hash = hmac256.doFinal();

				if(hash.equals(hashReceived)){
					msg = receivePacket.getAddress().getHostAddress() + ";;" + receivePacket.getPort() + ";;" + msg + ";;" + (endTime - t.time);
					this.st.updateLine(msg);
					String[] aux = msg.split(";;");
					System.out.println("Received message: " + st.getLine(aux[0]));
				}else{
					System.out.println("Integrity of packet not verified");
				}
			}
		}catch (IOException | NoSuchAlgorithmException e){
            e.printStackTrace();
        }
	}
}
