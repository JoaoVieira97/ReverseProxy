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
        long timeS, timeE;
        double BW=0, time, prevBW=0;
        int calcCicle=0;

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
            
            timeS = System.currentTimeMillis();
            //Server to Client
            while((nR=in.read(current,0,1024))!=-1){
                out.write(current,0,nR);
                calcCicle++;
                if(calcCicle==20){
                    timeE = System.currentTimeMillis();
                    time = (double)(timeE-timeS)/1000;
                    if(time!=0.f){
                        prevBW=BW;
                        BW = ((double)20*nR/1024) /time;
                    } 
                    else{
                        prevBW=BW;
                        BW=0;
                    }
                    calcCicle=0;
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
                if(time!=0.f){
                    prevBW=BW;
                    BW = ((double)calcCicle*nR/1024) /time;
                } 
                else{
                    prevBW=BW;
                    BW=0;
                }
                synchronized(this.st){
                    this.st.updateBW(serverIp,BW-prevBW);
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
        long timeE, timeS;
        double time, BW=0, prevBW=0;
        int calcCicle=0;

        //Client to Server
        try{
            timeS = System.currentTimeMillis();
            while((nR=this.in.read(current,0,1024)) != -1){
                this.out.write(current,0,nR);
                calcCicle++;
                if(calcCicle==20){
                    timeE = System.currentTimeMillis();
                    time = (double)(timeE-timeS)/1000;
                    if(time!=0.f){
                        prevBW=BW;
                        BW = ((double)20*nR/1024) /time;
                    } 
                    else{
                        prevBW=BW;
                        BW=0;
                    }
                    calcCicle=0;
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
                if(time!=0.f){
                    prevBW=BW;
                    BW = ((double)calcCicle*nR/1024) /time;
                } 
                else{
                    prevBW=BW;
                    BW=0;
                }
                synchronized(this.st){
                    this.st.updateBW(serverIp,BW-prevBW);
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
