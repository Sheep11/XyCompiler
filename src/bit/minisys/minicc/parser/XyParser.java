package bit.minisys.minicc.parser;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class XyParser implements IMiniCCParser{
    public String run(String iFile) throws Exception {
        String oFile=iFile.substring(iFile.lastIndexOf('/')+1,iFile.lastIndexOf('.'));
        oFile+="_XyTree.xml";

        //parse .xml file to token stream
        ArrayList<Token> tokens=this.getTokens(iFile);
        //construct LR(1) analysis table
        LRMaster master=new LRMaster("grammer_full");
        //analysis token stream
        master.parse(tokens.toArray(new Token[tokens.size()]));
        //print syntax tree
        if(master.errors.size()==0) {
            master.tree.printTree(oFile);
            System.out.println("\nno error! .xml file is: " + oFile);
        }
        else {
            //error information
            System.out.println("\nSyntax errors:");
            for (Token t : master.errors) {
                System.out.printf("In line %s, col %s : %s\n", t.row, t.col, t.lex);
            }
        }

        return oFile;
    }

    public ArrayList<Token> getTokens(String iFile) throws Exception {
        ArrayList<Token> tokens=new ArrayList<>();
        Set<String> lexSet=new HashSet<>();
        lexSet.add("Identifier");
        lexSet.add("String");
        lexSet.add("Integer");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(iFile);
        NodeList tokenList = document.getElementsByTagName("token");
        for (int i = 0; i < tokenList.getLength(); i++) {
            Element token = (Element)tokenList.item(i);

            String lex, value, num, row, col;
            lex=token.getElementsByTagName("type").item(0).getFirstChild().getNodeValue();
            value=token.getElementsByTagName("value").item(0).getFirstChild().getNodeValue();
            row=token.getElementsByTagName("row").item(0).getFirstChild().getNodeValue();
            col=token.getElementsByTagName("col").item(0).getFirstChild().getNodeValue();
            num=token.getElementsByTagName("number").item(0).getFirstChild().getNodeValue();

            if(lexSet.contains(lex)){
                tokens.add(new Token(lex, value, row, col, num));
            }
            else{
                tokens.add(new Token(value, lex, row, col, num));
            }

        }

        tokens.add(new Token("$","$","","",""));
        return tokens;
    }
}
