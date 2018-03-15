import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetAddress;

class UDPAgent{
    private InetAddress curIp; 
   
    UDPAgent(){

    }
    
    public static void main(String[] args){
        try{
            byte[] buf=new byte[10];
            InetAddress mcGroupIP=InetAddress.getByName("239.8.8.8");
            MulticastSocket receiveSkt=new MulticastSocket(8888); //create multicast socket that listens on port 8888
            DatagramPacket probeRequest=new DatagramPacket(buf, 10);

            System.out.println("Recieving at: "+mcGroupIP);
            receiveSkt.joinGroup(mcGroupIP);

            System.out.println("Waiting for data...");
            receiveSkt.receive(probeRequest);

            String msg=new String(probeRequest.getData(),probeRequest.getOffset(),probeRequest.getLength());
            System.out.println("Recieved: "+msg);

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
