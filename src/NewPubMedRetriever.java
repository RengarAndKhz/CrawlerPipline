import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wangt on 8/21/2016.
 */
public final class NewPubMedRetriever {
    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    public static final String DB_NAME = "pubmed";
    public static final String SEARCHHEAD = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?";
    public static final String DI_CHUNKSIZE = "1000";
    protected final String retmax = "999999";


    protected String today;
    protected String usehistory;
    protected String reldate;


    public NewPubMedRetriever(){
        Date date = new Date();
        today = dateFormat.format(date);
    }

    /**
     * url exampel:
     * https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&datetype=edat&reldate=0&retmax=300&usehistory=y
     * https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&datetype=edat&mindate=2016/08/21&maxdate=2016/08/21&retmax=300&usehistory=y
     * @param dateToRequest
     * @param dateType
     * @return list of article ID
     * @throws Exception
     */
    public String[] getIdSetByDate(String dateToRequest, String dateType) throws Exception{
        String inputurl = SEARCHHEAD + "dp=" + DB_NAME + "&datetype=" +dateType + "&mindate=" + dateToRequest + "&maxdate="+ dateToRequest + "&retmax="+ retmax + "&usehistory=" + usehistory;
        StringBuilder result = new StringBuilder();
        URL url = new URL(inputurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        /*BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while((line = rd.readLine()) != null){
            result.append(line);
        }*/
        Document doc = parseXML(conn.getInputStream());
        NodeList nodeList = doc.getElementsByTagName("Id");

        String[] res = new String[nodeList.getLength()];
        for (int i = 0; i < nodeList.getLength(); i++){
            res[i] = nodeList.item(i).getTextContent();
        }

        return res;

    }
    private Document parseXML(InputStream stream) throws Exception{
        DocumentBuilderFactory documentBuilderFactory = null;
        DocumentBuilder documentBuilder = null;
        Document document = null;

        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        document = documentBuilder.parse(stream);

        return document;


    }

    public static void main(String[] args){

        NewPubMedRetriever newPubMedRetriever = new NewPubMedRetriever();




    }


}
