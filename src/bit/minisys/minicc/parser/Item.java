package bit.minisys.minicc.parser;

import org.python.antlr.ast.Str;

import java.util.*;

public class Item {
    String left;
    String [] right;
    int dotIndex;
    String forwardSymbol;

    public Item(String left, String[] right, int dotIndex, String forwardSymbol){
        this.left=left;
        this.right=right;
        this.dotIndex=dotIndex;
        this.forwardSymbol=forwardSymbol;
    }

    public Item(Item item){
        this.left=new String(item.left);
        this.right=item.right.clone();
        this.dotIndex=item.dotIndex;
        this.forwardSymbol=new String(item.forwardSymbol);
    }

    public String dotNext(){
        if(dotIndex<right.length)
            return right[dotIndex];
        else return new String("");
    }

    public ArrayList<String> getSuffix(){
        ArrayList<String> suffix=new ArrayList<>();
        for(int i=dotIndex+1;i<right.length;i++)
            suffix.add(right[i]);
        suffix.add(forwardSymbol);

        return suffix;
    }

    public boolean equals(Item item) {
        if (this.left.equals(item.left))
            if (Arrays.equals(this.right, item.right))
                if (this.dotIndex == item.dotIndex)
                    if (this.forwardSymbol.equals(item.forwardSymbol))
                        return true;
        return false;

    }

    static boolean Icontains(ArrayList<Item> I, Item item){
        for(Item iter:I){
            if(iter.equals(item))
                return true;
        }
        return false;
    }

    static boolean Iequals(ArrayList<Item> a, ArrayList<Item> b){
        if(a.size()!=b.size())
            return false;

        for(Item item:b){
            if(!Item.Icontains(a,item))
                return false;
        }
        return true;
    }
}
