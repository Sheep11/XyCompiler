package bit.minisys.minicc.scanner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Vector;

public class XyScanner implements IMiniCCScanner{
    private String[] KeyWrod={"break","case","char","const","continue","default","do","double","else","float",
                "for","goto","if","int","return","sizeof","struct","switch","void","while"};
    private int row=1,col=1,number=1;
    private Vector<Token> tokens;
    private InputStream f=null;
    private char ch,next_ch;
    private int ch_value;
    int EOF=0xFFFF;

    public XyScanner(){
        tokens=new Vector<Token>();
        ch=next_ch=0;
        ch_value=0;
    }

    public String run (String iFile) throws Exception{
        f=new FileInputStream(iFile);

        String oFile=iFile.substring(iFile.lastIndexOf('/')+1,iFile.lastIndexOf('.'));
        oFile+="_XyTokens.xml";
        FileOutputStream out=new FileOutputStream(oFile);
        System.out.println("XyScanner out:"+oFile);

        readChar();
        col=0;
        scanAll();

        DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
        DocumentBuilder db=dbf.newDocumentBuilder();
        Document doc=db.newDocument();

        Element root=doc.createElement("project");
        root.setAttribute("name",iFile);
        doc.appendChild(root);

        Element tokens_elem=doc.createElement("tokens");
        root.appendChild(tokens_elem);

        for(Token t:tokens){
            Element token_elem=doc.createElement("token");

            Element[] attrs=new Element[5];
            attrs[0]=doc.createElement("type");
            attrs[1]=doc.createElement("value");
            attrs[2]=doc.createElement("number");
            attrs[3]=doc.createElement("row");
            attrs[4]=doc.createElement("col");

            attrs[0].setTextContent(t.type);
            attrs[1].setTextContent(t.value);
            attrs[2].setTextContent(String.valueOf(t.number));
            attrs[3].setTextContent(String.valueOf(t.row));
            attrs[4].setTextContent(String.valueOf(t.col));

            for(Element e:attrs)
                token_elem.appendChild(e);

            tokens_elem.appendChild(token_elem);
    }

        TransformerFactory tsff=TransformerFactory.newInstance();
        Transformer tsf=tsff.newTransformer();
        tsf.setOutputProperty(OutputKeys.INDENT,"yes");
        tsf.transform(new DOMSource(doc),new StreamResult(new File(oFile)));

        return oFile;
    }

    private void scanAll() throws Exception{
        while(readChar()!=EOF){
            if(isAlpha(ch)){
                String word= readWord();
            }
            else if(isNumber(ch)){
                String number=readNumber();
            }
            else if(ch=='\"') {
                String s = readString();
            }
            else if(ch=='\''){
                String char_constant=readCharConstant();
            }

            switch (ch) {
                case ' ':
                case '\t': case '\r':
                    break;
                case '\n':
                    row += 1;
                    col = 0;
                    break;
                case '+':
                    if (next_ch == '+')
                        makeToken("OP_INC", "++", true);
                    else if (next_ch == '=')
                        makeToken("OP_ADD_EQUAL", "+=", true);
                    else
                        makeToken("OP_ADD", "+", true);

                    if (next_ch == '+' || next_ch == '=')
                        readChar();
                    break;
                case '-':
                    if (next_ch == '-')
                        makeToken("OP_DEC", "--", true);
                    else if (next_ch == '=')
                        makeToken("OP_MINUS_EQUAL", "-=", true);
                    else
                        makeToken("OP_MINUS", "-", true);

                    if (next_ch == '-' || next_ch == '=')
                        readChar();
                    break;
                case '*':
                    if (next_ch == '=')
                        makeToken("OP_MUL_EQUAL", "*=", true);
                    else
                        makeToken("OP_MUL", "*", true);

                    if (next_ch == '=')
                        readChar();
                    break;
                case '/':
                    if (next_ch == '=')
                        makeToken("OP_DIV_EQUAL", "/=", true);
                    else
                        makeToken("OP_DIV", "/", true);

                    if (next_ch == '=')
                        readChar();
                    break;
                case '=':
                    if (next_ch == '=')
                        makeToken("OP_EQUAL", "==", true);
                    else
                        makeToken("OP_ASSIGN", "=", true);

                    if (next_ch == '=')
                        readChar();
                    break;
                case '%':
                    if (next_ch == '=')
                        makeToken("OP_MOD_EQUAL", "%=", true);
                    else
                        makeToken("OP_MOD", "%", true);

                    if (next_ch == '=')
                        readChar();
                    break;
                case '^':
                    if (next_ch == '=')
                        makeToken("OP_BIT_XOR_EQUAL", "^=", true);
                    else
                        makeToken("OP_BIT_XOR", "^", true);

                    if (next_ch == '=')
                        readChar();
                    break;
                case '&':
                    if (next_ch == '=')
                        makeToken("OP_BIT_AND_EQUAL", "&=", true);
                    else if (next_ch == '&')
                        makeToken("OP_AND", "&&", true);
                    else
                        makeToken("OP_BIT_AND", "&", true);

                    if (next_ch == '=' || next_ch == '&')
                        readChar();
                    break;
                case '|':
                    if (next_ch == '=')
                        makeToken("OP_BIT_OR_EQUAL", "|=", true);
                    else if (next_ch == '|')
                        makeToken("OP_OR", "||", true);
                    else
                        makeToken("OP_BIT_OR", "|", true);

                    if (next_ch == '=' || next_ch == '|')
                        readChar();
                    break;
                case '~':
                    if (next_ch == '=')
                        makeToken("OP_BIT_NOT_EQUAL", "|=", true);
                    else
                        makeToken("OP_BIT_OR", "|", true);

                    if (next_ch == '=' || next_ch == '|')
                        readChar();
                    break;
                case '!':
                    if (next_ch == '=')
                        makeToken("OP_NEQUAL", "!=", true);
                    else
                        makeToken("OP_NOT", "!", true);

                    if (next_ch == '=')
                        readChar();
                    break;
                case '<':
                    if (next_ch == '=')
                        makeToken("OP_LESS_EQUAL", "<=", true);
                    else if (next_ch == '<')
                        makeToken("OP_SHIFTL", "<<", true);
                    else
                        makeToken("OP_LESS", "<", true);

                    if (next_ch == '=' || next_ch == '<')
                        readChar();
                    break;
                case '>':
                    if (next_ch == '=')
                        makeToken("OP_GREATER_EQUAL", ">=", true);
                    else if (next_ch == '>')
                        makeToken("OP_SHIFTR", ">>", true);
                    else
                        makeToken("OP_GREATER", "<", true);

                    if (next_ch == '=' || next_ch == '>')
                        readChar();
                    break;
                case '(': case ')': case '[': case']': case'{': case'}': case',': case';':
                    makeToken("SEP",""+ch,true);
                    break;
            }
        }
    }

    private int readChar() throws Exception{
        ch_value=(int)next_ch;
        ch=next_ch;
        next_ch=(char)f.read();

        col+=1;

        return ch_value=='\uFFFF'?EOF:1;
    }

    private void readChar(int count) throws Exception{
        for(int i=0;i<count;i++) {
            ch_value = (int) next_ch;
            ch = next_ch;
            next_ch = (char) f.read();

            col += 1;
        }
    }

    private boolean isAlpha(char c){
        return ((c <= 'z') && (c >= 'a')) || ((c <= 'Z') && (c >= 'A')) || (c == '_');
    }

    private boolean isNumber(char c){
        return (c >= '0') && (c <= '9');
    }

    private boolean isKeyWord(String word){
        for (String key:KeyWrod) {
            if(key.equals(word)) return true;
        }
        return false;
    }

    private void makeToken(String type, String value, boolean valid){
        Token token=new Token(number,row,col-value.length()+1,type,value,valid);
        tokens.add(token);
        number+=1;
    }

    private String readWord() throws Exception{
       StringBuilder word= new StringBuilder();

       word.append(ch);
       while(((int)next_ch != EOF) && (isNumber(next_ch) || isAlpha(next_ch))) {
           readChar();
           word.append(ch);
       }

       if(word.toString().matches("[a-z|_][a-z|0-9|_]*"))
           if(isKeyWord(word.toString()))
               makeToken("Keyword", word.toString(),true);
           else
               makeToken("Identifier", word.toString(),true);
       else
               makeToken("Identifier", word.toString(),false);

        return word.toString();
    }

    private String readNumber() throws Exception {
        StringBuilder number= new StringBuilder();

        number.append(ch);
        while (((int)next_ch != EOF) && isNumber(next_ch)) {
            readChar();
            number.append(ch);
        }
        if(next_ch=='.') {
            readChar();
            number.append(ch);
            while ((int)next_ch != EOF &&isNumber(next_ch)){
                readChar();
                number.append(ch);
            }
        }
        if(next_ch=='e'||next_ch=='E'){
            readChar();
            if(next_ch=='+'||next_ch=='-'){
                number.append(ch + next_ch);
                readChar();
            }
            else
                number.append(ch);

             while ((int)next_ch != EOF &&isNumber(next_ch)){
                 readChar();
                number.append(ch);
            }
        }

        String Integer_constant="0|[1-9]\\d*";
        String fractional_constant="\\d+\\.\\d+";
        String exponent_part="[e|E][+|-]\\d+";

        if(number.toString().matches(Integer_constant))
            makeToken("Integer", number.toString(),true);
        else if(number.toString().matches(fractional_constant+exponent_part))
            makeToken("Float", number.toString(),true);
        else
            makeToken("Number", number.toString(),false);

        return number.toString();
    }

    private char readEscapeChar() throws Exception{
        switch (next_ch) {
            case '\'': case '"': case '?': case '\\':
                return next_ch;
            case 'b': return '\b';
            case 'f': return '\f';
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
        }
        System.out.println("unknown escape char");
        return '\0';
    }

    private String readString() throws Exception{
        StringBuilder s= new StringBuilder("" + "\"");
        while((int)next_ch!=EOF&& next_ch!='\r'&&next_ch!='\t'&&next_ch!='\n'){
            readChar();
            if(ch=='\"'){
                s.append(ch);
                break;
            }

           if(ch=='\\') {
               s.append(readEscapeChar());
               readChar();
           }
           else{
               s.append(ch);
           }
        }

        if(s.toString().matches("\".*\""))
            makeToken("String", s.toString(),true);
        else
            makeToken("String", s.toString(),false);
        return s.toString();
    }

        private String readCharConstant() throws Exception{
        StringBuilder s= new StringBuilder("" + "\'");
        while((int)next_ch!=EOF&& next_ch!='\r'&&next_ch!='\t'&&next_ch!='\n'){
            readChar();
            if(ch=='\''){
                s.append(ch);
                break;
            }

           if(ch=='\\') {
               s.append(readEscapeChar());
               readChar();
           }
           else{
               s.append(ch);
           }
        }

        if(s.toString().matches("\'[!\'\\\\]\'"))
            makeToken("Char", s.toString(),true);
        else
            makeToken("Char", s.toString(),false);
        return s.toString();
    }
}

class Token{
        int number;
        int row,col;
        String type;
        String value;
        boolean valid;

        Token(int number,int row,int col,String type,String value,boolean valid){
            this.number=number;
            this.row=row;
            this.col=col;
            this.type=type;
            this.value=value;
            this.valid=valid;
        }
}
