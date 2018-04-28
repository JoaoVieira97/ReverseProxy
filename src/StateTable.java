import java.util.ArrayList;
import java.util.ListIterator;

/*
Table design (each line):
IP;;Port;;CPU(%);;RAM(free Memory, bytes);;RTT(ms);;BW
*/
class StateTable{
    private ArrayList<String> table;
    
    StateTable(){
        this.table=new ArrayList<String>();
    }

    void insertLine(String s){
        this.table.add(s);
    }

    void updateLine(String s){
        ListIterator<String> it = this.table.listIterator();
        boolean notFound=true;
        String[] aux = s.split(";;");
        String ip = aux[0];

        while(it.hasNext() && notFound){
            String l = it.next();
            String[] auxL = l.split(";;");
            if(auxL[0].equals(ip)) {
                it.set(s + ";;" + auxL[5]);
                notFound=false;
            }   
        }
        if(notFound) this.table.add(s + ";;0");
    }

    String getLine(String ip){
        for(String s: this.table){
            String[] aux = s.split(";;");
            if(aux[0].equals(ip)) return s;
        }
        return null;
    }

    boolean containsIp(String ip){
        for(String s: this.table){
            String[] aux = s.split(";;");
            if(aux[0].equals(ip)) return true;
        }
        return false;
    }

    void updateBW(String ip, String bw){
       ListIterator<String> it = this.table.listIterator();
       boolean notFound=true;

       while(it.hasNext() && notFound){
            String l = it.next();
            String[] auxL = l.split(";;");
            if(auxL[0].equals(ip)) {
                it.set(auxL[0] + ";;" + auxL[1] + ";;" + auxL[2] + ";;" + auxL[3] + auxL[4] + ";;" + bw);
                notFound=false;
            }   
        }
    }

    String getServerAlgorithm(){
        int size = this.table.size();
        double[] values = new double[size];
        String line;
        int minP=0;
        double min=-Double.MAX_VALUE;

        for(int i=0; i<size; i++){
            line = this.table.get(i);
            String[] aux = line.split(";;");
            values[i] = 0.25*Double.parseDouble(aux[2]) - 0.25*Double.parseDouble(aux[3]) + 0.25*Double.parseDouble(aux[4]) + 0.25*Double.parseDouble(aux[5]);
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
