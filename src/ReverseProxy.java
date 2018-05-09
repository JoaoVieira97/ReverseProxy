import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;

class ReverseProxy implements Runnable{
    private StateTable st;
    
    public ReverseProxy(StateTable st){
        this.st = st;
    }

    public void run(){
        try{
            ServerSocket ss = new ServerSocket(80);

            while(true){
                Socket clientSocket = ss.accept();

                Thread conect = new Thread(new Connection(clientSocket,st));
                conect.start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

class Connection implements Runnable{

    private Socket s;
    private StateTable st;

    public Connection(Socket s, StateTable st){
        this.s = s;
        this.st = st;
    }

    public void run(){
        String serverIp;
        long timeS, timeE, time; 
        double BW;

        synchronized(this.st){
            serverIp = this.st.getServerAlgorithm();
        }

        try{
            Socket sToServer = new Socket(serverIp,80);
        
            Thread listenClient = new Thread(new ListenFromClient(s,sToServer,this.st,serverIp));
            listenClient.start();

            OutputStream out = this.s.getOutputStream();
            InputStream in = sToServer.getInputStream();
            byte[] current=new byte[1024];
            int nR;
            
            //Server to Client
            while((nR=in.read(current,0,1024))!=-1){
                timeS = System.currentTimeMillis();
                out.write(current,0,nR);
                timeE = System.currentTimeMillis();
                time = (timeE-timeS)/1000;
                if(time!=0) BW = (nR/1024) / time;
                else BW=0;
                synchronized(this.st){
                    this.st.updateBW(serverIp,Double.toString(BW));
                }
            }      

        }catch(Exception e){
            e.printStackTrace();
        }   
    }
}

class ListenFromClient implements Runnable{
    
    private OutputStream out;
    private InputStream in;
    private StateTable st;
    private String serverIp;

    public ListenFromClient(Socket inS, Socket outS,StateTable st, String sIp){
        try{
            this.out = outS.getOutputStream();
            this.in = inS.getInputStream();
            this.st = st;
            this.serverIp = sIp;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        
        byte[] current=new byte[1024];
        int nR;
        long timeE, timeS, time, BW;

        //Client to Server
        try{
            while((nR=this.in.read(current,0,1024)) != -1){
                timeS = System.currentTimeMillis();
                this.out.write(current,0,nR);
                timeE = System.currentTimeMillis();
                time = (timeE-timeS)/1000;
                if(time!=0) BW = (nR/1024) / time;
                else BW=0;
                synchronized(this.st){
                    this.st.updateBW(serverIp,Long.toString(BW));
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
