package bit.minisys.minicc.parser;

public class Token {
    String lex;
    String value;
    String row, col, num;

    public Token(String lex, String value, String row, String col, String num){
        this.lex=lex;
        this.value=value;
        this.row=row;
        this.col=col;
        this.num=num;
    }
}
