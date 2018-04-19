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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
            byte[] receiveData=new byte[300];
            byte[] response, fullResponse, hash;
            Mac hmac256 = Mac.getInstance("HmacSHA256");
            byte[] key="abcdfasdgasefdgsdp".getBytes();
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
                
                mem = sigar.getMem();
                cpu = sigar.getCpu();
                memFree = mem.getFree();
                cpuTotalTime = cpu.getTotal(); 
                cpuPerc = (cpuTotalTime-cpu.getIdle())/cpuTotalTime;

                resp = cpuPerc + ";;" + memFree;
                response = resp.getBytes("UTF-8");
                hmac256.reset();
                hmac256.init(new SecretKeySpec(key, 0, key.length, "AES"));
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
