package bit.minisys.minicc.parser;
import java.util.ArrayList;
import java.util.Arrays;

public class Production{
    String left;
    ArrayList<String []> rights= new ArrayList<>();

    public Production(String production){
        parseProduction(production);
    }

    public int parseProduction(String s){
        left=s.split(":")[0].trim();

        String[] rights_list=s.split(":")[1].split("\\|");
        for(String right:rights_list) {
            ArrayList<String> symbols=new ArrayList<>(Arrays.asList(right.split(" ")));
            symbols.removeIf(
                    symbol->symbol.isEmpty()
            );
            rights.add(symbols.toArray(new String[symbols.size()]));
        }

        return rights.size();
    }
}