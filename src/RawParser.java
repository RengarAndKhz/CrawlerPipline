/**
 * Created by Tianyang on 10/17/16.
 */

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RawParser {
    private String STARTTAG;
    private File xmlFile;
    private String xml;
    private Document doc;
    private List<String> tags;
    private Set<String> tagDict;
    private Map<String, List<String>> parseMap;
    public RawParser(String xml, String tagPath, String startTag) throws ParserConfigurationException, IOException, SAXException {
        this.tags = new ArrayList<>();
        this.STARTTAG = startTag;
        this.xml = xml;
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(this.xml));
        this.doc = db.parse(is);
        //get tags from input
        for (String line : Files.readAllLines(Paths.get(tagPath))){
            tags.add(line);
            //System.out.println(line);
        }
        this.tagDict = new HashSet<>(tags);
    }
    public RawParser(File xmlFile, String tagPath, String startTag) throws IOException, SAXException, ParserConfigurationException {
        this.tags = new ArrayList<>();
        this.STARTTAG = startTag;
        this.xmlFile = xmlFile;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbFactory.newDocumentBuilder();
        this.doc = db.parse(xmlFile);
        //get tags from input
        for (String line : Files.readAllLines(Paths.get(tagPath))){
            tags.add(line);
        }
        this.tagDict = new HashSet<>(tags);
    }

    public List<Map<String, List<String>>> parser(){
        doc.getDocumentElement().normalize();
        List<Map<String, List<String>>> res = new ArrayList<>();
        NodeList articles = doc.getElementsByTagName(STARTTAG);
        for(int i = 0; i < articles.getLength(); i++){
            Node article = articles.item(i);
            res.add(singleArticleParser(article));
        }
        return res;
    }

    /**
     * use to parse single article
     * @param article
     * @return
     */
    public Map<String, List<String>> singleArticleParser(Node article){
        parseMap = new HashMap<>();
        //Set<String> dict = new HashSet<>(this.tags);
        search(article);
        return parseMap;

    }

    /**
     * use to search the target tags in a article
     * @param root
     */
    public void search(Node root){
        if(tagDict.contains(root.getNodeName())){
            insertValue(root);
            return;
        }
        if(root.hasChildNodes()){
            for(int i = 0; i < root.getChildNodes().getLength(); i++){
                search(root.getChildNodes().item(i));
            }
        }
    }

    /**
     * insert the value fo target tags into the map
     * @param root
     */
    public void insertValue(Node root){
        StringBuilder sb = new StringBuilder();
        if (!root.hasChildNodes()){
            sb.append(root.getTextContent());
        }
        else{
            NodeList tempList = root.getChildNodes();
            for(int i = 0; i < tempList.getLength(); i++){
                sb.append(tempList.item(i).getTextContent());
                sb.append("$");
            }
            sb.delete(sb.length()-1, sb.length());
        }
        if(parseMap.containsKey(root.getNodeName())){
            parseMap.get(root.getNodeName()).add(sb.toString());
        }
        else{
            List<String> temp = new ArrayList<>();
            temp.add(sb.toString());
            parseMap.put(root.getNodeName(), temp);
        }

    }
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        RawParser parser = new RawParser(new File("/Users/Tianyang/CS545/CrawlerPipline/efetch.fcgi.xml"), "/Users/Tianyang/CS545/CrawlerPipline/tags.txt", "PubmedArticle");
        List<Map<String, List<String>>> res = parser.parser();
        for (Map<String, List<String>> curr : res){
            for (String key : curr.keySet()){
                System.out.println(curr.get(key));
            }
        }
    }

}
