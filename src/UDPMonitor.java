import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;
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
    byte[] key;

	public ListenUDPAgents(DatagramSocket ss, StateTable st, Timer timer){
		this.serverSocket = ss;
        this.st = st;
        this.t = timer;
		try{
            this.key="abcdfasdgasefdgsdp".getBytes("UTF-8");
        }catch(Exception e){
            e.printStackTrace();        
        }
    }

    public void run() {
    	try{
			String msg;
			int i;
	        DatagramPacket receivePacket;
	        Mac hmac256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(this.key,"AES");

			byte[] receiveData = new byte[1024];
			byte[] hashReceived = new byte[32];
			byte[] msgReceived = new byte[992];
			byte[] hash, trimmed; 

       		while (true){
                //clean
                Arrays.fill(receiveData,(byte)0);
                Arrays.fill(msgReceived,(byte)0);

                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                endTime = System.currentTimeMillis();
                String srcIp=receivePacket.getAddress().getHostAddress();
               
                receiveData=receivePacket.getData();
                
                System.arraycopy(receiveData,0,hashReceived,0,32);
				System.arraycopy(receiveData,32,msgReceived,0,receiveData.length-32);
               
                trimmed = trim(msgReceived);

                hmac256.reset();
				hmac256.init(secretKey);
                hmac256.update(trimmed);
                hash = hmac256.doFinal();
               
                msg = new String(msgReceived);

				if(Arrays.toString(hash).equals(Arrays.toString(hashReceived))){
					msg = receivePacket.getAddress().getHostAddress() + ";;" + receivePacket.getPort() + ";;" + msg + ";;" + (endTime - t.time);
					this.st.updateLine(msg);
					String[] aux = msg.split(";;");
					System.out.println("Received message: " + st.getLine(aux[0]));
				}else{
					System.out.println("Integrity of packet not verified");
				}
			}
		}catch (Exception e){
            e.printStackTrace();
        }
	}

    byte[] trim(byte[] bytes){
    	int i = bytes.length - 1;
    	
        while (i >= 0 && bytes[i] == 0) i--;
		
        byte[] ret = new byte[i+1];
        System.arraycopy(bytes,0,ret,0,i+1);
        
        return ret;
	}
}
