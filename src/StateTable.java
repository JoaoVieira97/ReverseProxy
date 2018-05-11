import java.util.ArrayList;
import java.util.ListIterator;

/*
Table design (each line):
IP;;Port;;CPU(%);;RAM(free Memory, bytes);;RTT(ms);;BW(Kbps)
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
    
    //update line if already exists but without changing BW, if not exists add new line
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
    
    //return line of an server with IP
    String getLine(String ip){
        for(String s: this.table){
            String[] aux = s.split(";;");
            if(aux[0].equals(ip)) return s;
        }
        return null;
    }
    
    //check if sever with IP is already on state table
    boolean containsIp(String ip){
        for(String s: this.table){
            String[] aux = s.split(";;");
            if(aux[0].equals(ip)) return true;
        }
        return false;
    }
    
    //update bandwdith of an server with IP
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

    //choose an server from state table
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
