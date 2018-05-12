import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;

/** 
 * <h1> Reverse Proxy </h1>
 * The ReverseProxy class implements the component responsible for
 * establishing the connection between a client and an HTTP server
 * and also delegating the servers to each client
 *
 * @author Grupo 49
 * @version 1.0
 *
 */
class ReverseProxy implements Runnable{
    private StateTable st;
    
    public ReverseProxy(StateTable st){
        this.st = st;
    }

    /**
     * The run method is required by all Runnable implementing classes.
     * In this case it accepts connections from clients via a ServerSocket.
     */
    public void run(){
        try{
            ServerSocket ss = new ServerSocket(80);

            while(true){
                //wait for client connection and delegate thread
                Socket clientSocket = ss.accept();
                
                Thread conect = new Thread(new Connection(clientSocket,st));
                conect.start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

/**
 * The Connection class is the communication channel between the client and the HTTP
 * server. 
 */
class Connection implements Runnable{

    private Socket s;
    private StateTable st;

    public Connection(Socket s, StateTable st){
        this.s = s;
        this.st = st;
    }

    public void run(){
        String serverIp;
        long timeS, timeE;
        double BW=0, time, prevBW=0;
        int calcCicle=0;
        
        synchronized(this.st){
            serverIp = this.st.getServerAlgorithm();
        }

        try{
            //create socket to server HTTP port
            Socket sToServer = new Socket(serverIp,80);
        
            Thread listenClient = new Thread(new ListenFromClient(s,sToServer,this.st,serverIp));
            listenClient.start();

            OutputStream out = this.s.getOutputStream();
            InputStream in = sToServer.getInputStream();
            byte[] current=new byte[1024];
            int nR, nRT=0;
            
            timeS = System.currentTimeMillis();
            
            //Server to Client
            while((nR=in.read(current,0,1024))!=-1){
                out.write(current,0,nR); //send
                calcCicle++; 
                nRT+=nR;
                //calculate BW
                if(calcCicle==20){
                    timeE = System.currentTimeMillis();
                    time = (double)(timeE-timeS)/1000;
                    prevBW=BW;
                    if(time!=0.f) BW = ((double)20*nR/1024) /time;
                    else BW=0;
                    calcCicle=0;
                    nRT=0;
                    //update BW on state table
                    synchronized(this.st){
                        this.st.updateBW(serverIp,BW-prevBW);
                    }
                    timeS = System.currentTimeMillis();
                }   
            }
            
            //case end with 1 a 19 cicles
            if(calcCicle!=0){
                timeE = System.currentTimeMillis();
                time = (double)(timeE-timeS)/1000;
                prevBW=BW;
                if(time!=0.f) BW = ((double)calcCicle*nR/1024) /time;
                else BW=0;
                //update BW on state table
                synchronized(this.st){
                    this.st.updateBW(serverIp,BW-prevBW);
                }
            }
            
            //update BW on state table
            synchronized(this.st){
                this.st.updateBW(serverIp,-BW);
            }
        }catch(Exception e){
            e.printStackTrace();
        }   
    }
}



/**
 * <h2>ListenFromClient</h2>
 * The ListFromClient class is responsible for taking input from the client and forwarding it to 
 * the HTTP server as is, <b>i.e.</b> it is transmited as raw data.
 */
class ListenFromClient implements Runnable{
    
    private OutputStream out;
    private InputStream in;
    private StateTable st;
    private String serverIp;

    /**
     * Constructor for class ListFromClient
     * @param inS   socket to client
     * @param outS  socket to HTTP server
     * @param st    table holding all info about all HTTP servers 
     * @param sIp   IP of the HTTP server
     */
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
        int nR, nRT=0;
        long timeE, timeS;
        double time, BW=0, prevBW=0;
        int calcCicle=0;

        try{
            timeS = System.currentTimeMillis();
            while((nR=this.in.read(current,0,1024)) != -1){
                this.out.write(current,0,nR);
                calcCicle++;
                nRT+=nR;
                //caculate/update bandwith every 20 cycles to avoid round to 0
                if(calcCicle==20){
                    timeE = System.currentTimeMillis();
                    time = (double)(timeE-timeS)/1000;
                    if(time!=0.f){
                        prevBW=BW;
                        BW = ((double)nRT/1024) /time;
                    } 
                    else{
                        prevBW=BW;
                        BW=0;
                    }
                    //reset counter for further measures
                    calcCicle=0;
                    nRT=0;
                    synchronized(this.st){
                        this.st.updateBW(serverIp,BW-prevBW);
                    }
                    timeS = System.currentTimeMillis();
                }   
            }
           
            //update even if no. of measures doesn't reach 20
            if(calcCicle!=0){
                timeE = System.currentTimeMillis();
                time = (double)(timeE-timeS)/1000;
                if(time!=0.f){
                    prevBW=BW;
                    BW = ((double)nRT/1024) /time;
                } 
                else{
                    prevBW=BW;
                    BW=0;
                }
                synchronized(this.st){
                    this.st.updateBW(serverIp,BW-prevBW); //prevent ever increasing BW value
                }
            }
            
            synchronized(this.st){
                this.st.updateBW(serverIp,-BW);
            } 
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
