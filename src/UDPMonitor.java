import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;

/** 
 * <h1>UDP Monitor</h1>
 * The UDPMonitor class implements the component responsible for sending 
 * multicast information request messages to UDPAgent's in the group.
 * It receives the answers on unicast and then updates the State Table 
 * with information of the back-end servers.
 *
 * @author Grupo 49
 * @version 1.0
 *
 */
class UDPMonitor{
	final static int port = 8888;
	final static String inet_addr = "239.8.8.8";
	final static byte[] msg = "SIR".getBytes();
    static StateTable st = new StateTable();

    private DatagramSocket sSocket;
    private DatagramPacket sirPacket;
    private InetAddress addr;

    public UDPMonitor(InetAddress ipAddr) throws SocketException{
        this.addr=ipAddr;
        this.sSocket=new DatagramSocket();
        this.sirPacket=new DatagramPacket(UDPMonitor.msg, UDPMonitor.msg.length, this.addr, UDPMonitor.port);
    }

    /**
     * Send <b>SIR</b> messages to the multicast address every 3 seconds and start
     * thread's Reverse Proxy and to listen UDPAgent's reponses.
     */
	public static void main(String[] args) throws UnknownHostException, InterruptedException {
	    InetAddress addr=InetAddress.getByName(inet_addr);
	    Timer t = new Timer();

		try{
            UDPMonitor monitor=new UDPMonitor(addr);
            monitor.init(t);
			            
            System.out.println("Sending");
			while(true){
				Thread.sleep(3 * 1000); //send datagram every 3 seconds
                monitor.sendMessage(); 
				synchronized(t){
                    t.update(System.currentTimeMillis());
			    }
            }
		}
		catch (IOException e){
			e.printStackTrace();
		}
  	}

    /**
     * Initializes the reverse proxy and the thread
     * responsible for handling UDPAgents messages
     * @param t timer used for RTT calculation
     */
    public void init(Timer t){
        Thread agentUDPResponse = new Thread(new ListenUDPAgents(this.sSocket,UDPMonitor.st,t));
		agentUDPResponse.start();
        Thread reverseProxy = new Thread(new ReverseProxy(UDPMonitor.st));
        reverseProxy.start();
    }

    /**
     * Sends a datagram containing a <b>SIR</b> about the
     * servers that are members of the multicast group.
     */
    public void sendMessage() throws IOException{
        this.sSocket.send(this.sirPacket);
    }

}

/**
 * <h2>Timer</h2>
 * The Timer class is used to calculate RTT (Round Trip Time) between messages
 * sended by UDPMonitor and the answers received by UDPAgent's.
 */
class Timer{
	long time;
    
    /**
     * Constructor for Timer class.
     */
    Timer(){
        this.time=System.currentTimeMillis();
    }

    /**
     * Update the Timer time.
     * @param t time used to make the update
     */
    void update(long t){
        this.time = t;
    }

    /**
     * Method to get Timer time.
     * @return value of time
     */
    long get(){
        return this.time;
    }
}

/**
 * <h2>ListenUDPAgents</h2>
 * The ListenUDPAgents class is responsible for receiving the messages
 * from each UDPAgent, calculate the RTT using Timer class and updating State Table.
 */
class ListenUDPAgents implements Runnable {
	private DatagramSocket serverSocket;
    private StateTable st;
    private Timer t;
    private byte[] key;

    /**
     * Constructor for class ListenUDPAgents.
     * @param ss     server socket
     * @param st  	 state table
     * @param timer  object of Timer class used to calculate RTT 
     */
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

    /**
     * Method required by all classes implementing the Runnable interface.
     * Receives and process messages from UDPAgent's
     */
    public void run() {
    	try{
			String msg;
			int i;
	        long RTT;
            DatagramPacket receivePacket;
	        Mac hmac256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(this.key,"AES");

			byte[] receiveData = new byte[1024];
			byte[] hashReceived = new byte[32];
			byte[] msgReceived = new byte[992];
			byte[] hash, trimmed; 

       		while (true){
                //clean vars
                Arrays.fill(receiveData,(byte)0);
                Arrays.fill(msgReceived,(byte)0);
                
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                //calculate RTT
                synchronized(this.t){
                    RTT = System.currentTimeMillis() - this.t.get();
                }
               
                receiveData=receivePacket.getData();
                
                //separate hash from message
                System.arraycopy(receiveData,0,hashReceived,0,32);
				System.arraycopy(receiveData,32,msgReceived,0,receiveData.length-32);
               
                //remove padding
                trimmed = trim(msgReceived);
                
                //calculate hash from message
                hmac256.reset();
				hmac256.init(secretKey);
                hmac256.update(trimmed);
                hash = hmac256.doFinal();
               
                msg = new String(msgReceived);
                msg = msg.substring(5); //remove SIRR\n
                
                //if hash received equals to calculated hash save data on state table
				if(Arrays.toString(hash).equals(Arrays.toString(hashReceived))){
					msg = receivePacket.getAddress().getHostAddress() + ";;" + receivePacket.getPort() + ";;" + msg + ";;" + RTT;
					synchronized(this.st){
                        this.st.updateLine(msg);
                    }
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

	/**
	 * Method used to remove padding from messages.
	 * @param bytes array of bytes to process
	 * @return array of bytes processed with no padding
	 */
    byte[] trim(byte[] bytes){
    	int i = bytes.length - 1;
    	
        while (i >= 0 && bytes[i] == 0) i--;
		
        byte[] ret = new byte[i+1];
        System.arraycopy(bytes,0,ret,0,i+1);
        
        return ret;
	}
}
