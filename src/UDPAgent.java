import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetAddress;

import java.net.URLConnection;
import java.net.URL;
import java.io.*;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Cpu;

import java.security.MessageDigest;
import java.util.Arrays;

class UDPAgent{

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
            String aux=";;";
            byte[] response, responseHash, fullResponse;
            DatagramPacket msgR;
            MessageDigest hasher = MessageDigest.getInstance("MD5");

            DatagramSocket sendSkt = new DatagramSocket();

            System.out.println("Recieving at: "+mcGroupIP);
            receiveSkt.joinGroup(mcGroupIP);
            
            do{
                System.out.println("Waiting for data...");
                receiveSkt.receive(probeRequest);
                msg=new String(probeRequest.getData(),probeRequest.getOffset(),probeRequest.getLength());
                System.out.println("Recieved: "+msg);

                String rcvd = new String(probeRequest.getData(), 0, probeRequest.getLength()) + ", from address: "+ probeRequest.getAddress() + ", port: " + probeRequest.getPort();
                System.out.println(rcvd);
                
                mem = sigar.getMem();
                cpu = sigar.getCpu();
                memFree = mem.getFree();
                cpuTotalTime = cpu.getTotal(); 
                cpuPerc = (cpuTotalTime-cpu.getIdle())/cpuTotalTime;

                resp = ua.curIp + ";;" + port + ";;" + cpuPerc + ";;" + memFree;
                response = resp.getBytes("UTF-8");
                hasher.reset();
                hasher.update(response);
                responseHash = hasher.digest(); //calculate hash value of message

                //Debugging
                System.out.println(responseHash);

                //FIXME
                fullResponse=new byte[response.length+responseHash.length];
                System.arraycopy(responseHash, 0, fullResponse, 0, responseHash.length);
                System.arraycopy(response, 0, fullResponse, responseHash.length, response.length);
                
                //Debugging
                System.out.println(responseHash);
                System.out.println(Arrays.copyOfRange(fullResponse,0,responseHash.length));

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
