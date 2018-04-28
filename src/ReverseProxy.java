import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.BufferedReader;

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

            PrintWriter out = new PrintWriter(this.s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sToServer.getInputStream()));
            String current;
            
            //Server to Client
            while((current = in.readLine()) != null){
                out.println(current); 
            }      

        }catch(Exception e){
            e.printStackTrace();
        }   
    }
}

class listenFromClient implements Runnable{
    
    private PrintWriter out;
    private BufferedReader in;

    public listenFromClient(Socket inS, Socket outS){
        try{
            this.out = new PrintWriter(outS.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(inS.getInputStream()));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        
        String current;
        
        //Client to Server
        try{
            while((current = this.in.readLine()) != null){
                this.out.println(current); 
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
