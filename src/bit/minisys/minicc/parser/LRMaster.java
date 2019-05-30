package bit.minisys.minicc.parser;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class LRMaster {
    // 成员变量,产生式集，终结符集，非终结符集
    ArrayList<Production> productions=new ArrayList<>();

    ArrayList<String> terminals=new ArrayList<>();
    ArrayList<String> nonterminals=new ArrayList<>();
    HashMap<String, ArrayList<String>> firsts=new HashMap<>();

    ArrayList<ArrayList<Item>> Cluster;
    ArrayList<HashMap<String, Action>> actionTable;
    ArrayList<HashMap<String, Integer>> gotoTable;

    public SyntaxTree tree=new SyntaxTree();

    public LRMaster() throws Exception {
        setProductions("grammer.txt");
        setNonTerminals();
        setTerminals();
        setFirst();
        setItemCluster();
        initTable();
    }

    public void parse(String[] inputs){
        Stack<Integer> stateStack=new Stack<>();
        stateStack.push(0);
        for(int i=0;i<inputs.length;i++){
            int state=stateStack.peek();
            Action action=actionTable.get(state).get(inputs[i]);
            switch (action.type) {
                case "acc":
                    System.out.println("accepted\n");
                    break;
                case "move":
                    stateStack.push(action.intMove);
                    tree.move(action.left);
                    System.out.println("move state " + action.intMove + " to stack\n");
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
                    System.out.printf("reduce [%s] to [%s], move state [%d] to stack\n", action.right.toString(), action.left, gotoState);
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

    // 从文件中读取产生式
    public void setProductions(String grammerPath){
        try {
            File file = new File(grammerPath);
            RandomAccessFile randomfile = new RandomAccessFile(file, "r");
            String line;
            Production production;
            while ((line=randomfile.readLine())!=null) {
                production = new Production(line);
                productions.add(production);
            }
            randomfile.close();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    // 获得非终结符集
    public void setNonTerminals(){
        for(Production p:productions){
            if(!nonterminals.contains(p.left))
                nonterminals.add(p.left);
        }
    }

    // 获得终结符集,依赖于获得产生式函数
    public void setTerminals() {
        terminals.add("$");

        // 遍历所有的产生式
        for (Production p : productions) {
            for (String[] right : p.rights) {
                for (String symbol : right)
                    if (!nonterminals.contains(symbol))
                        terminals.add(symbol);
            }
        }
    }

    // 获取First集
    public void setFirst(){
        // 终结符全部求出first集
        ArrayList<String> first;
        for (String terminal:terminals) {
            first = new ArrayList<String>();
            first.add(terminal);
            firsts.put(terminal, first);
        }
        // 给所有非终结符注册一下
        for (String nonterminal:nonterminals) {
            first = new ArrayList<String>();
            firsts.put(nonterminal, first);
        }

        boolean flag;
        String left;
        // 非终结符的first集
        while (true) {
            flag = true;
            for (Production p:productions) {
                left=p.left;
                for(String[] right:p.rights)
                    for(String symbol:right){
                        ArrayList<String> new_firsts=firsts.get(symbol);
                        for(String first_iter:new_firsts) {
                            if (!firsts.get(left).contains(first_iter)) {
                                firsts.get(left).add(first_iter);
                                flag=false;
                            }
                        }
                        if(!new_firsts.contains("#"))
                            break;
                    }
            }
            if (flag==true)
                break;
        }
    }

    public ArrayList<String> getFirst(ArrayList<String> words){
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

    public ArrayList<Item> getClosure(ArrayList<Item> I){
        boolean changed=false;
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

    public ArrayList<Item> getGOTO(ArrayList<Item> I, String symbol){
        ArrayList<Item> J=new ArrayList<>();
        for(Item item:I) {
            if(item.dotIndex==item.right.length) continue;
            if (!item.right[item.dotIndex].equals(symbol))
                continue;

            if (item.dotIndex < item.right.length) {
                Item newItem=new Item(item);
                newItem.dotIndex++;
                J.add(newItem);
            }
        }
        return getClosure(J);
    }

    public int getGOTOIndex(ArrayList<Item> I, String symbol){
        ArrayList<Item> J=getGOTO(I,symbol);
        for(int i=0;i<Cluster.size();i++){
            if(Item.Iequals(J,Cluster.get(i)))
                return i;
        }
        return -1;
    }

    public boolean Ccontains(ArrayList<ArrayList<Item>> C, ArrayList<Item>Ia){
        for(ArrayList<Item> Ib:C){
            if(Item.Iequals(Ia,Ib))
                return true;
        }
        return false;
    }

    void setItemCluster(){
        Cluster=new ArrayList<ArrayList<Item>>();
        ArrayList<String> allSymbol=new ArrayList<>();
        allSymbol.addAll(nonterminals);
        allSymbol.addAll(terminals);

        Item startItem=new Item("S'", new String[]{"S"},0,"$");
        ArrayList<Item> I0=new ArrayList<>();
        I0.add(startItem);

        Cluster.add(getClosure(I0));
        boolean changed=false;
        do{
            changed=false;
            int size=Cluster.size();
            for(int i=0;i<size;i++){
                ArrayList<Item> I=Cluster.get(i);
                for(String symbol:allSymbol){
                    ArrayList<Item> newI=getGOTO(I,symbol);
                    if(newI.size()!=0 && !Ccontains(Cluster,newI)) {
                        Cluster.add(newI);
                        changed=true;
                    }
                }
            }
        }while(changed);
    }

    void initTable(){
        actionTable=new ArrayList<>();
        gotoTable=new ArrayList<>();
        for(int i=0;i<Cluster.size();i++){
            actionTable.add(new HashMap<String,Action>());
            gotoTable.add(new HashMap<String,Integer>());
        }

        for(int i=0;i<Cluster.size();i++){
            for(Item item:Cluster.get(i)){
                if(item.left.equals("S'")&&item.dotIndex==item.right.length&&item.forwardSymbol.equals("$")) {
                    actionTable.get(i).put("$", new Action("acc", 0, null, null));
                    continue;
                }

                if(item.dotIndex==item.right.length) {
                    actionTable.get(i).put(item.forwardSymbol, new Action("reduce", 0, item.left, item.right));
                    continue;
                }

                if(item.dotIndex!=item.right.length) {
                    int move = getGOTOIndex(Cluster.get(i), item.right[item.dotIndex]);
                    if(terminals.contains(item.right[item.dotIndex]))
                        actionTable.get(i).put(item.right[item.dotIndex], new Action("move",move,item.right[item.dotIndex],null));
                    else
                        gotoTable.get(i).put(item.right[item.dotIndex],move);
                }
            }
        }

        System.out.println(" ");
    }
}
