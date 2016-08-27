import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.apache.http.HttpHeaders.USER_AGENT;

/**
 * Created by wangt on 8/21/2016.
 */
public final class NewPubMedRetriever {
    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    public static final String DB_NAME = "pubmed";
    public static final String BASE = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    public static final int ID_CHUNKSIZE = 1000;
    protected final String retmax = "999999";


    protected String today;
    protected String usehistory;
    protected String reldate;

    /**
     * did nothing, just uesd to fit with PubMedLibrary
     */
    public void initialize(){
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
        String inputurl = BASE + "esearch.fcgi?" + "dp=" + DB_NAME + "&datetype=" +dateType + "&mindate=" + dateToRequest + "&maxdate="+ dateToRequest + "&retmax="+ retmax + "&usehistory=" + usehistory;
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

    /**
     * not sure what Ids look like
     * default 1,2,3,4,5
     * @param Ids
     * @return result.toString()
     */
    public String crawlByIdList(String Ids) throws Exception{
        String url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);

        post.setHeader("User-Agent", USER_AGENT);
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("db", DB_NAME));
        urlParameters.add(new BasicNameValuePair("Id", Ids));

        urlParameters.add(new BasicNameValuePair("retmode", "xml"));
        //urlParameters.add(new BasicNameValuePair("rettype", "abstract"));
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        HttpResponse response = httpClient.execute(post);
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
            //System.out.println(line);
        }
        return result.toString();
    }

    public String crawByDate(Date date) throws Exception{
        String theDate = dateFormat.format(date);
        String[] idList = getIdSetByDate(theDate, "edat");
        StringBuilder ids = new StringBuilder();
        for(String currId : idList){
            ids.append(currId + ",");
        }
        // delete the last ","
        ids.delete(ids.length()-1, ids.length());
        return crawlByIdList(ids.toString());
    }

    /**
     *
     * @param start_id
     * @return
     * @throws Exception
     */
    public String crawlByIDRange(long start_id) throws Exception{
        return crawlByIdList(getIDStr(start_id));
    }

    private String getIDStr(long start_id){
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < ID_CHUNKSIZE; i++){
            ids.append(String.format("%d,", start_id+i));
        }
        ids.delete(ids.length()-1, ids.length());
        return ids.toString();
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

    /**
     * get max Id and min Id
     * @param dateToRequest
     * @return
     * @throws Exception
     */
    public String[] getIDRangeByDate(String dateToRequest) throws Exception{
        String[] ids = getIdSetByDate(dateToRequest, "edat");
        Long currMax = Long.MIN_VALUE;
        Long currMin = Long.MAX_VALUE;
        if (ids == null) return null;
        for (String currId : ids){
            Long tempId = Long.parseLong(currId);
            currMax = Long.max(currMax, tempId);
            currMin = Long.min(currMin, tempId);
        }

        return new String[]{currMin.toString(), currMax.toString()};
    }

    /**
     *
     * @param Ids
     * @return
     * @throws Exception
     */
    public HashSet<String> getFileNameSetByIdList(String[] Ids) throws Exception {
        if (Ids == null) return null;
        HashSet<String> return_set = new HashSet<String>();
        for (String cur_id : Ids) {
            Long set_id = Long.parseLong(cur_id)/1000*1000;
            return_set.add(set_id.toString());
        }
        return return_set;
    }

    public static void main(String[] args){

        NewPubMedRetriever newPubMedRetriever = new NewPubMedRetriever();

    }


}
