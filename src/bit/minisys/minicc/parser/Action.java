package bit.minisys.minicc.parser;

public class Action {
    String type;
    int intMove;
    String[] right;
    String left;

    public Action(String type, int move, String left, String[] right){
        this.type=type;
        this.intMove=move;
        this.left=left;
        this.right=right;
    }
}
