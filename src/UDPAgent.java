import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;


import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Cpu;

/** 
 * <h1>UDP Agent</h1>
 * The UDPAgent class implements the component responsible for receiving 
 * multicast information request messages from UDPMonitor and answers it
 * on unicast with the information of the back-end server he's running in.
 *
 * @author Grupo 49
 * @version 1.0
 *
 */
class UDPAgent{
    private static String key="abcdfasdgasefdgsdp";

    private InetAddress mcGroupIP;
    private MulticastSocket receiveSkt;
    private DatagramSocket sendSkt;
    private int port;
    private SecretKeySpec secretKey;
    private DatagramPacket probeRequest;
    

    public UDPAgent(InetAddress ip, int prt, byte[] key) throws IOException{
        this.mcGroupIP=ip;
        this.receiveSkt=new MulticastSocket(prt);
        this.port=prt;
        this.secretKey=new SecretKeySpec(key, "AES");
        this.probeRequest=new DatagramPacket(new byte[10], 10);
        this.sendSkt=new DatagramSocket();
    }

    /**
     * Send server related information to UDPMonitor and uses HMAC to
     * to ensure authenticity and integrity.
     */
    public static void main(String[] args){
        try{
            UDPAgent agent=new UDPAgent(InetAddress.getByName("239.8.8.8"),8888, UDPAgent.key.getBytes("UTF-8"));
            Mac hmac256 = Mac.getInstance("HmacSHA256");
            DatagramPacket msgR;
            
            Sigar sigar = new Sigar();
            Cpu cpu;
            String rcvd;

            long memFree;
            float cpuTotalTime, cpuPerc;
                        
            agent.init();

            do{
                rcvd = agent.waitRequest();
                System.out.println(rcvd); //debugging purposes
                
                //Retreive server state
                memFree = sigar.getMem().getFree();
                cpu=sigar.getCpu();
                cpuTotalTime = cpu.getTotal(); 
                cpuPerc = (cpuTotalTime-cpu.getIdle())/cpuTotalTime;
                
                msgR = agent.createMessage(hmac256,memFree,cpuPerc);;
                agent.sendMessage(msgR);
            }while(!rcvd.equals("CT"));

            agent.cleanUp();
        }catch(Exception e){
            e.printStackTrace();
        }   
    }

    /**
     * Joins the multicast group
     */
    public void init() throws IOException{
        this.receiveSkt.joinGroup(this.mcGroupIP);
        System.out.println("Recieving at: "+this.mcGroupIP);
    }

    /**
     * Waits for a <b>SIR</b>(Server Information Request) packet
     * @return string with the contents of the packet recieved and the IP and port that it came from
     */
    public String waitRequest() throws IOException{
        String msg;
        byte[] receiveData=new byte[300];

        System.out.println("Waiting for data...");
        this.receiveSkt.receive(this.probeRequest);
        receiveData=this.probeRequest.getData();

        msg=new String(receiveData,0,probeRequest.getLength());

        if(!msg.equals("CT")) return msg;
        else return (msg+", from address: "+ probeRequest.getAddress() + ", port: " + probeRequest.getPort());
    }

    /**
     * Method responsible for creating the message to be sent with info related to the HTTP server.
     * @param hmac256   hmac digest used for authentication and integrity of the message being sent
     * @param freeMem   amount of free memory(RAM) in the HTTP server
     * @param cpuUsage  amount of cpu being used at a given instant by the HTTP server
     * @return packet containing server info aswell as the hash associated with it
     */
    public DatagramPacket createMessage(Mac hmac256, long freeMem, float cpuUsage) throws UnsupportedEncodingException,InvalidKeyException{
        String resp;
        byte[] response, fullResponse, hash;

        resp = "SIRR\n" + cpuUsage + ";;" + freeMem;
        response = resp.getBytes("UTF-8");
        hmac256.reset();
        hmac256.init(this.secretKey);
        hmac256.update(response);
        hash = hmac256.doFinal();

        fullResponse=new byte[response.length+hash.length];
        System.arraycopy(hash, 0, fullResponse, 0, hash.length);
        System.arraycopy(response, 0, fullResponse, hash.length, response.length);

        return new DatagramPacket(response, response.length, probeRequest.getAddress(), probeRequest.getPort());
    }

    /**
     * Sends a single datagram via it's datagram socket.
     */
    public void sendMessage(DatagramPacket msg) throws IOException{
        this.sendSkt.send(msg);
    }

    /**
     * Method is responsible for closing the sockekt and unsubscribing the multicast group
     */
    void cleanUp() throws IOException{
        receiveSkt.leaveGroup(this.mcGroupIP);
        receiveSkt.close();
    }
    
}
