import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

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

    /**
     * Send information of server to UDPMonitor and uses hashing to
     * to ensure authenticity and integrity.
     * @param args main method arguments
     */
    public static void main(String[] args){
        try{
            byte[] buf=new byte[10];
            int port = 8888;
            InetAddress mcGroupIP=InetAddress.getByName("239.8.8.8");
            MulticastSocket receiveSkt=new MulticastSocket(port); //create multicast socket that listens on port 8888
            DatagramPacket probeRequest=new DatagramPacket(buf, 10);
            
            Sigar sigar = new Sigar();
            Mem mem;
            Cpu cpu;
            String msg, resp;
            long memFree;
            float cpuTotalTime, cpuPerc;
            byte[] receiveData=new byte[300];
            byte[] response, fullResponse, hash;
            Mac hmac256 = Mac.getInstance("HmacSHA256");
            byte[] key="abcdfasdgasefdgsdp".getBytes("UTF-8");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            DatagramPacket msgR;
            
            DatagramSocket sendSkt = new DatagramSocket();

            System.out.println("Recieving at: "+mcGroupIP);
            receiveSkt.joinGroup(mcGroupIP);
            
            do{
                System.out.println("Waiting for data...");
                receiveSkt.receive(probeRequest);
                receiveData=probeRequest.getData();
                msg=new String(receiveData,0,probeRequest.getLength());
                
                System.out.println("Recieved: "+msg);

                String rcvd = new String(probeRequest.getData(), 0, probeRequest.getLength()) + ", from address: "+ probeRequest.getAddress() + ", port: " + probeRequest.getPort();
                System.out.println(rcvd);
                
                //get state from server
                mem = sigar.getMem();
                cpu = sigar.getCpu();
                memFree = mem.getFree();
                cpuTotalTime = cpu.getTotal(); 
                cpuPerc = (cpuTotalTime-cpu.getIdle())/cpuTotalTime;
                
                //create message and hash 
                resp = "SIRR\n" + cpuPerc + ";;" + memFree;
                response = resp.getBytes("UTF-8");
                hmac256.reset();
                hmac256.init(secretKey);
                hmac256.update(response);
                hash = hmac256.doFinal();
                
                fullResponse=new byte[response.length+hash.length];
                System.arraycopy(hash, 0, fullResponse, 0, hash.length);
                System.arraycopy(response, 0, fullResponse, hash.length, response.length);

                msgR = new DatagramPacket(fullResponse, fullResponse.length, probeRequest.getAddress(), probeRequest.getPort());
				sendSkt.send(msgR);

            }while(!msg.equals("CT"));

            receiveSkt.leaveGroup(mcGroupIP);
            receiveSkt.close();
        }catch(Exception e){
            e.printStackTrace();
        }   
    }
    
}
