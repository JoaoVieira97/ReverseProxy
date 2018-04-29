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

        synchronized(this.st){
            serverIp = this.st.getServerAlgorithm();
        }

        try{
            Socket sToServer = new Socket(serverIp,80);
        
            Thread listenClient = new Thread(new listenFromClient(s,sToServer));
            listenClient.start();

            OutputStream out = this.s.getOutputStream();
            InputStream in = sToServer.getInputStream();
            byte[] current=new byte[1024];
            int nR;
            
            //Server to Client
            while((nR=in.read(current,0,1024))!=-1){
                out.write(current,0,nR); 
            }      

        }catch(Exception e){
            e.printStackTrace();
        }   
    }
}

class listenFromClient implements Runnable{
    
    private OutputStream out;
    private InputStream in;

    public listenFromClient(Socket inS, Socket outS){
        try{
            this.out = outS.getOutputStream();
            this.in = inS.getInputStream();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        
        byte[] current=new byte[1024];
        int nR;
        
        //Client to Server
        try{
            while((nR=this.in.read(current,0,1024)) != -1){
                this.out.write(current,0,nR); 
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
