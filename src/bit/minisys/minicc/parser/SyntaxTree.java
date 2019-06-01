package bit.minisys.minicc.parser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

public class SyntaxTree {
    Stack<SyntaxNode> nodeStack;

    static DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
    static DocumentBuilder db;
    static {
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
    static Document doc=db.newDocument();

    public SyntaxTree() throws Exception{
        nodeStack=new Stack<>();
    }

    public void move(String value){
        nodeStack.push(new SyntaxNode(value));
    }

    public void reduce(String value, int num){
        SyntaxNode child=new SyntaxNode(value);
        for(int i=0;i<num;i++){
            child.addChild(nodeStack.pop());
        }
        nodeStack.push(child);
    }

    public void printTree(String oFile) throws Exception{
        Element root=doc.createElement("project");
        root.setAttribute("name","Syntax tree");
        doc.appendChild(root);

        printNode(root, nodeStack.peek());

        TransformerFactory tsff=TransformerFactory.newInstance();
        Transformer tsf=tsff.newTransformer();
        tsf.setOutputProperty(OutputKeys.INDENT,"yes");
        tsf.transform(new DOMSource(doc),new StreamResult(new File(oFile)));

    }

    static void printNode(Element root, SyntaxNode node){
        if(node.childs.size()==0) {
            String[] value_and_type=node.value.split("@@");
            String value,type;
            if(value_and_type[0].equals("Identifier")||value_and_type[0].equals("String")||value_and_type[0].equals("Integer")) {
                value = value_and_type[1];
                type =value_and_type[0];
            }
            else{
                value = value_and_type[0];
                type =value_and_type[1];
            }

            Element e=doc.createElement(type);
            e.setTextContent(value);
            root.appendChild(e);
            return;
        }
        Element e=doc.createElement(node.value);
        root.appendChild(e);

        for(int i=node.childs.size() - 1;i>=0;i--){
            printNode(e, node.childs.get(i));
        }
    }

    static void printSimpleNode(Element root, SyntaxNode node){
        if(node.childs.size()==0) {
            String[] value_and_type=node.value.split("@@");
            String value,type;
            if(value_and_type[0].equals("Identifier")||value_and_type[0].equals("String")||value_and_type[0].equals("Integer")) {
                value = value_and_type[1];
                type =value_and_type[0];
            }
            else{
                value = value_and_type[0];
                type =value_and_type[1];
            }

            Element e=doc.createElement(type);
            e.setTextContent(value);
            root.appendChild(e);
            return;
        }

        for(int i=node.childs.size() - 1;i>=0;i--){
            printNode(root, node.childs.get(i));
        }
    }
}

class SyntaxNode{
    ArrayList<SyntaxNode> childs;
    String value;

    public SyntaxNode(String value){
        this.value=value;
        childs=new ArrayList<>();
    }

    public boolean addChild(SyntaxNode child){
        if(this.childs.add(child))
            return true;
        else
            return false;
    }
}
