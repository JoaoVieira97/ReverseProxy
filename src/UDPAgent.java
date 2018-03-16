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

class UDPAgent{
    private String curIp; 
   
    UDPAgent(){
        this.curIp = this.getIp();
    }
    
    //dont know if is safe (verify): TODO
    private String getIp() throws IOException {
        URL url = new URL("http://checkip.amazonaws.com/");
        URLConnection urlCon = url.openConnection();
        String ret = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
        ret = in.readLine();
        return ret;
    }

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
            UDPAgent ua = new UDPAgent();

            System.out.println("Recieving at: "+mcGroupIP);
            receiveSkt.joinGroup(mcGroupIP);

            System.out.println("Waiting for data...");
            receiveSkt.receive(probeRequest);

            String msg=new String(probeRequest.getData(),probeRequest.getOffset(),probeRequest.getLength());
            System.out.println("Recieved: "+msg);
            
            if(!msg.equals("")){
                mem = sigar.getMem();
                cpu = sigar.getCpu();
                long memFree = mem.getFree();
                long memTotal = mem.getTotal();
                
                String resp = ua.curIp + ";;" + port + ";;" + memFree;

            }

            receiveSkt.leaveGroup(mcGroupIP);
            receiveSkt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
         
    }
}

class UDPListener implements Runnable{

    UDPListener(){

    }

    public void run(){


    }
}
