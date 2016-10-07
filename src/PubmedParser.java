/**
 * Created by Tianyang on 10/6/16.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PubmedParser {
    private String xml;
    private File xmlFile;
    private Document doc;
    private final String ARTICLE_TITLE = "ArticleTitle";
    private final String AUTHOR_NAME = "Authors";
    private final String ABSTRACT = "Abstract";
    private final String PMID = "PMID";
    private final String NLM_UNIQUE_ID = "NlmUniqueID";
    public PubmedParser(String xml) throws ParserConfigurationException, IOException, SAXException {
        this.xml = xml;
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(this.xml));
        this.doc = db.parse(is);
    }
    public PubmedParser(File xmlFile) throws ParserConfigurationException, IOException, SAXException {
        this.xmlFile = xmlFile;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbFactory.newDocumentBuilder();
        this.doc = db.parse(xmlFile);
    }

    /**
     * articleMap contains tags and item under the specific tags
     * articles contains different NodeList of articles split by tag PubmedArticle
     * @return a list of articleMap
     */
    public List<Map<String, List<String>>> parser(){
        List<Map<String, List<String>>> res = new ArrayList<>();
        doc.getDocumentElement().normalize();
        //System.out.println(doc.getDocumentElement().getNodeName());
        NodeList articles = doc.getElementsByTagName("PubmedArticle");
        Map<String, List<String>> articleMap = new HashMap<>();

        for (int i = 0; i < articles.getLength(); i++){
            Node article = articles.item(i);
            NodeList MC_PD = article.getChildNodes();
            Node medilineCitation = MC_PD.item(0);
            // parse medlineCitatoin
            NodeList mcChildren = medilineCitation.getChildNodes();
            for (int x = 0; x < mcChildren.getLength(); x++){
                if ("PMID".equals(mcChildren.item(x).getNodeName())) {
                    Element ePMID = (Element) mcChildren.item(x);
                    List<String> pmidList = new ArrayList<>();
                    pmidList.add(ePMID.getTextContent());
                    articleMap.put(PMID, pmidList);
                    //System.out.println(ePMID.getTextContent());
                }
                if ("Article".equals(mcChildren.item(x).getNodeName())){
                    //parse article title
                    Element eArticle = (Element) mcChildren.item(x);
                    List<String> articleTitleList = new ArrayList<>();
                    articleTitleList.add(eArticle.getElementsByTagName("ArticleTitle").item(0).getTextContent());
                    articleMap.put(ARTICLE_TITLE, articleTitleList);
                    //System.out.println(eArticle.getElementsByTagName("ArticleTitle").item(0).getTextContent());
                    //parse authors
                    NodeList authors = eArticle.getElementsByTagName("Author");
                    List<String> authorList = new ArrayList<>();
                    for (int authorID = 0; authorID < authors.getLength(); authorID++){
                        Element eAuthor = (Element) authors.item(authorID);
                        authorList.add(eAuthor.getElementsByTagName("LastName").item(0).getTextContent() + " " +
                                eAuthor.getElementsByTagName("ForeName").item(0).getTextContent() + " " +
                                eAuthor.getElementsByTagName("Initials").item(0).getTextContent());
                        //System.out.println(eAuthor.getElementsByTagName("LastName").item(0).getTextContent() + " " +
                        //eAuthor.getElementsByTagName("ForeName").item(0).getTextContent() + " " +
                        //eAuthor.getElementsByTagName("Initials").item(0).getTextContent());
                    }
                    articleMap.put(AUTHOR_NAME, authorList);
                    //parse abstract if have
                    if (eArticle.getElementsByTagName("Abstract").item(0) != null){
                        List<String> abstractList = new ArrayList<>();
                        abstractList.add(eArticle.getElementsByTagName("Abstract").item(0).getTextContent());
                        articleMap.put(ABSTRACT, abstractList);
                        //System.out.println(eArticle.getElementsByTagName("Abstract").item(0).getTextContent());
                    }
                    //parse journal title
                    //System.out.println(eArticle.getElementsByTagName("Title").item(0).getTextContent());
                }
                if ("MedlineJournalInfo".equals(mcChildren.item(x).getNodeName())){
                    Element eInfo = (Element) mcChildren.item(x);
                    List<String> uniqueIDList = new ArrayList<>();
                    uniqueIDList.add(eInfo.getElementsByTagName("NlmUniqueID").item(0).getTextContent());
                    articleMap.put(NLM_UNIQUE_ID, uniqueIDList);
                    //System.out.println(eInfo.getElementsByTagName("NlmUniqueID").item(0).getTextContent());
                }
            }
            res.add(articleMap);
        }
        return res;
    }
    /*public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("/Users/Tianyang/CS545/CrawlerPipline/00000011.xml")));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line=bufferedReader.readLine()) != null){
            sb.append(line.trim());
        }
        PubmedParser pubmedParser = new PubmedParser(sb.toString());
        pubmedParser.parser();
    }*/
}
