import java.util.ArrayList;
import java.util.Iterator;

/*
Table design (each line):
IP;;Port;;CPU;;RAM;;RTT;;BW
*/
class StateTable{
    private ArrayList<String> table;
    
    StateTable(){
        this.table=new ArrayList<String>();
    }

    void insertLine(String s){
        this.table.add(s);
    }

    void updateLine(String ip, String s){
        Iterator<String> it = this.table.iterator();
        boolean notFound=true;
        while(it.hasNext() && notFound){
            String l = it.next();
            String[] aux = s.split(";;");
            if(aux[0].equals(ip)) {
                l=s;
                notFound=false;
            }   
        }
        if(notFound) this.table.add(s);
    }

    String getLine(String ip){
        String ret=null;
        for(String s: this.table){
            String[] aux = s.split(";;");
            if(aux[0].equals(ip)) ret=aux[0];
        }
        return ret;
    }
}
