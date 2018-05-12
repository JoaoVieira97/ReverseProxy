import java.util.ArrayList;
import java.util.ListIterator;

/**
* Saves the server HTTP status 
* Table design (each line):
* IP;;Port;;CPU(%);;RAM(free Memory, bytes);;RTT(ms);;BW(Kbps)
* 
* @author Grupo 49
* @version 1.0
*/
class StateTable{
    private ArrayList<String> table;
    private double maxRAM;
    private double maxBW;
    
    StateTable(){
        this.table=new ArrayList<String>();
        this.maxRAM = 0;
        this.maxBW = 0;
    }
    
    /**
    * Update line if already exists but without changing BW, if not exists add new line
    * @param s String with all values except BW
    * @return Nothing
    */
    void updateLine(String s){
        ListIterator<String> it = this.table.listIterator();
        boolean notFound=true;
        String[] aux = s.split(";;");
        String ip = aux[0];
        double ram = Double.parseDouble(aux[3]);

        while(it.hasNext() && notFound){
            String l = it.next();
            String[] auxL = l.split(";;");
            if(auxL[0].equals(ip)) {
                it.set(s + ";;" + auxL[5]);
                notFound=false;
            }   
        }
        if(notFound) this.table.add(s + ";;0");
        if (ram > maxRAM) maxRAM = ram;
    }
    
    /**
    * Return status line of an server with IP
    * @param ip ip of the server wanted
    * @return status of a server with ip
    */
    String getLine(String ip){
        for(String s: this.table){
            String[] aux = s.split(";;");
            if(aux[0].equals(ip)) return s;
        }
        return null;
    }
    
    /**
    * Check if sever with IP is already on state table
    * @param ip ip of the server that we want to check
    * @return false if don't exist and true if exist
    */
    boolean containsIp(String ip){
        for(String s: this.table){
            String[] aux = s.split(";;");
            if(aux[0].equals(ip)) return true;
        }
        return false;
    }
    
    /**
    * Update bandwdith of an server with IP
    * @param ip ip of the server that we want to change the BW
    * @param bw the value of the new bandwidth
    * @return Nothing
    */
    void updateBW(String ip, double bw){
       ListIterator<String> it = this.table.listIterator();
       boolean notFound=true;
       double auxBW; 

       while(it.hasNext() && notFound){
            String l = it.next();
            String[] auxL = l.split(";;");
            if(auxL[0].equals(ip)) {
                auxBW = bw + Double.parseDouble(auxL[5]); 
                it.set(auxL[0] + ";;" + auxL[1] + ";;" + auxL[2] + ";;" + auxL[3] + ";;" + auxL[4] + ";;" + Double.toString(auxBW));
                if (bw > maxBW) maxBW = bw;
                notFound=false;
            }   
        }
    }

    /**
    * Choose an server from state table acording to an algorithm
    * @return ip of the choosen server
    */
    String getServerAlgorithm(){
        int size = this.table.size();
        double[] values = new double[size];
        String line;
        int minP=0;
        double min=-Double.MAX_VALUE;

        for(int i=0; i<size; i++){
            line = this.table.get(i);
            String[] aux = line.split(";;");
            values[i] = 0.25*Double.parseDouble(aux[2]) - 0.25*Double.parseDouble(aux[3])/maxRAM + 0.25*Double.parseDouble(aux[4])/3000 + 0.25*Double.parseDouble(aux[5])/maxBW;
        }
        
        for(int i=0; i<size; i++){
            if(min>values[i]){
                min = values[i];
                minP = i;
            }
        }
        
        String[] aux = this.table.get(minP).split(";;");
        return aux[0]; 
    }
}
