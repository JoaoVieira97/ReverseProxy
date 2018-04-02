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
                it.set(s);
                notFound=false;
            }   
        }
        if(notFound) this.table.add(s);
    }

    String getLine(String ip){
        String ret=null;
        for(String s: this.table){
            String[] aux = s.split(";;");
            if(aux[0].equals(ip)) ret=s;
        }
        return ret;
    }
}
