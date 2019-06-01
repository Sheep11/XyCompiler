package bit.minisys.minicc.parser;

import javax.sound.midi.Sequence;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class LRMaster {
    // 成员变量,产生式集，终结符集，非终结符集
    ArrayList<Production> productions=new ArrayList<>();

    ArrayList<String> terminals=new ArrayList<>();
    ArrayList<String> nonterminals=new ArrayList<>();
    HashMap<String, ArrayList<String>> firsts=new HashMap<>();

    ArrayList<ArrayList<Item>> Collection;
    ArrayList<HashMap<String, Action>> actionTable;
    ArrayList<HashMap<String, Integer>> gotoTable;

    public SyntaxTree tree=new SyntaxTree();
    public ArrayList<Token> errors=new ArrayList<>();

    public LRMaster(String grammer) throws Exception {
        setProductions(grammer);
        setNonTerminals();
        setTerminals();
        setFirst();
        setItemCollection();
        initTable();
    }

    //LR(1) control program
    public void parse(Token[] inputs){
        Stack<Integer> stateStack=new Stack<>();
        stateStack.push(0);
        for(int i=0;i<inputs.length;i++){
            int state=stateStack.peek();
            Action action=actionTable.get(state).get(inputs[i].lex);

            //error recovery
            if(action==null) {
                errors.add(inputs[i]);
                while (i<inputs.length && !inputs[i].value.equals(";") && !inputs[i].value.equals("}"))
                    i++;
                i-=1;
                continue;
            }

            switch (action.type) {
                case "acc":
                    System.out.println("accepted\n");
                    break;
                case "move":
                    stateStack.push(action.intMove);
                    tree.move(action.left+"@@"+inputs[i].value);
                    System.out.println("move " +action.left);
                    break;
                case "reduce":
                    int reduceLen = action.right.length;
                    for (int j = 0; j < reduceLen; j++)
                        stateStack.pop();
                    state=stateStack.peek();
                    int gotoState = gotoTable.get(state).get(action.left);
                    stateStack.push(gotoState);

                    i--;

                    tree.reduce(action.left,action.right.length);
                    String s=action.right.toString();
                    System.out.printf("reduce to [%s], move state [%d] to stack\n", action.left, gotoState);
                    break;
                default:
            }
        }
    }

    public int getProductionIndex(String left){
        for(int i=0;i<productions.size();i++) {
            if (productions.get(i).left.equals(left))
                return i;
        }
        return -1;
    }

    // read grammer from file
    private void setProductions(String grammerPath){
        try {
            File file = new File(grammerPath);
            RandomAccessFile randomfile = new RandomAccessFile(file, "r");
            String line;
            Production production;
            while ((line=randomfile.readLine())!=null) {
                if(line.equals("")) continue;
                production = new Production(line);
                productions.add(production);
            }
            randomfile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //set nonterminals, every left of a production is a nonterminal
    private void setNonTerminals(){
        for(Production p:productions){
            if(!nonterminals.contains(p.left))
                nonterminals.add(p.left);
        }
    }

    //set terminals, if it's not a nonterminal, it's a terminal
    private void setTerminals() {
        terminals.add("$");

        for (Production p : productions) {
            for (String[] right : p.rights) {
                for (String symbol : right) {
                    if (!nonterminals.contains(symbol) && !terminals.contains(symbol))
                        terminals.add(symbol);
                }
            }
        }
    }

    //get firsts of terminals and nonterminals, for getFirst func
    private void setFirst(){
        // set every terminal
        ArrayList<String> first;
        for (String terminal:terminals) {
            first = new ArrayList<>();
            first.add(terminal);
            firsts.put(terminal, first);
        }
        // init every nonterminals
        for (String nonterminal:nonterminals) {
            first = new ArrayList<>();
            firsts.put(nonterminal, first);
        }

        boolean flag;
        String left;
        // set nonterminals
        do {
            flag = true;
            for (Production p : productions) {
                left = p.left;
                for (String[] right : p.rights)
                    //if epsilon in right
                    for (String symbol : right) {
                        ArrayList<String> new_firsts = firsts.get(symbol);
                        for (String first_iter : new_firsts) {
                            if (!firsts.get(left).contains(first_iter)) {
                                firsts.get(left).add(first_iter);
                                flag = false;
                            }
                        }
                        if (!new_firsts.contains("#"))
                            break;
                    }
            }
        } while (!flag);
    }

    //get firsts of a phase, based on ArrayList firsts
    private ArrayList<String> getFirst(ArrayList<String> words){
        ArrayList<String> result=new ArrayList<>();
        for(String word:words){
            for(String first:firsts.get(word)) {
                if(!result.contains(first))
                    result.add(first);
            }
            if(!firsts.get(word).contains("#"))
                break;
        }

        return result;
    }

    private ArrayList<Item> getClosure(ArrayList<Item> I){
        boolean changed;
        do{
            changed=false;
            //every Item in I
            int size=I.size();
            for(int i=0;i<size;i++){
                Item item=I.get(i);
                int index=getProductionIndex(item.dotNext());

                if(item.dotNext().equals("") || index<0) continue;
                Production p=productions.get(index);

                //every production in grammer
                for(String[] right:p.rights) {
                    ArrayList<String> suffix = item.getSuffix();
                    //every terminal word in first
                    for (String nt : getFirst(suffix)) {
                        Item newItem=new Item(item.dotNext(),right,0,nt);
                        if(!Item.Icontains(I,newItem)) {
                            I.add(newItem);
                            changed = true;
                        }
                    }
                }
            }
        } while(changed);
        return I;
    }

    private ArrayList<Item> getGOTO(ArrayList<Item> I, String symbol){
        ArrayList<Item> J=new ArrayList<>();
        for(Item item:I) {
            if(item.dotIndex==item.right.length) continue;
            if (!item.right[item.dotIndex].equals(symbol))
                continue;

            Item newItem=new Item(item);
            newItem.dotIndex++;
            J.add(newItem);
        }
        return getClosure(J);
    }

    //get LR(1) collection
    private void setItemCollection(){
        Collection= new ArrayList<>();
        ArrayList<String> allSymbol=new ArrayList<>();
        //get all symbols
        allSymbol.addAll(nonterminals);
        allSymbol.addAll(terminals);

        //init collection, add start I
        Item startItem=new Item("S'", new String[]{"S"},0,"$");
        ArrayList<Item> I0=new ArrayList<>();
        I0.add(startItem);

        Collection.add(getClosure(I0));
        boolean changed;
        do{
            changed=false;
            int size=Collection.size();
            //for every I in collection
            for(int i=0;i<size;i++){
                ArrayList<Item> I=Collection.get(i);
                //for every symbol in all symbols
                for(String symbol:allSymbol){
                    //get Ij and see whether it has existed
                    ArrayList<Item> newI=getGOTO(I,symbol);
                    //if not, add it to Collection
                    if(newI.size()!=0 && !Ccontains(Collection,newI)) {
                        Collection.add(newI);
                        changed=true;
                    }
                }
            }
        }while(changed);
    }

    //get analysis table
    private void initTable(){
        actionTable=new ArrayList<>();
        gotoTable=new ArrayList<>();
        for(int i=0;i<Collection.size();i++){
            actionTable.add(new HashMap<>());
            gotoTable.add(new HashMap<>());
        }

        for(int i=0;i<Collection.size();i++){
            for(Item item:Collection.get(i)){
                //S'->S·,$
                if(item.left.equals("S'")&&item.dotIndex==item.right.length&&item.forwardSymbol.equals("$")) {
                    actionTable.get(i).put("$", new Action("acc", 0, null, null));
                    continue;
                }

                //A->alpha·,a
                if(item.dotIndex==item.right.length) {
                    actionTable.get(i).put(item.forwardSymbol, new Action("reduce", 0, item.left, item.right));
                    continue;
                }

                //A->alpha·a beta, b
                int move = getGOTOIndex(Collection.get(i), item.right[item.dotIndex]);
                if(terminals.contains(item.right[item.dotIndex]))
                    actionTable.get(i).put(item.right[item.dotIndex], new Action("move",move,item.right[item.dotIndex],null));
                else
                    gotoTable.get(i).put(item.right[item.dotIndex],move);
            }
        }

        System.out.println(" ");
    }

    private int getGOTOIndex(ArrayList<Item> I, String symbol){
        ArrayList<Item> J=getGOTO(I,symbol);
        for(int i=0;i<Collection.size();i++){
            if(Item.Iequals(J,Collection.get(i)))
                return i;
        }
        return -1;
    }

    private boolean Ccontains(ArrayList<ArrayList<Item>> C, ArrayList<Item> Ia){
        for(ArrayList<Item> Ib:C){
            if(Item.Iequals(Ia,Ib))
                return true;
        }
        return false;
    }
}
