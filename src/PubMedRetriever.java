/**
 * 
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractSet;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;

import edu.mcw.rgd.nlp.utils.Crawler;
import edu.mcw.rgd.nlp.utils.CrawlerBase;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub.PubmedArticleSetChoiceE;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceStub;

/**
 * @author wliu
 *
 */
public final class PubMedRetriever extends CrawlerBase implements Crawler {

	public static final int CRAWL_DELAY = 5000;
	public static final int ID_TRUNK_SIZE = 1000;
	
    public static final String DB_Name = "pubmed"; 
	public static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

	protected EUtilsServiceStub searchService;
    protected EFetchPubmedServiceStub retrieveService;
	protected EUtilsServiceStub.ESearchRequest searchRequest;
	protected EUtilsServiceStub.EPostRequest postRequest;
    protected String webEnv = "";
    protected String queryKey = "";
    protected EUtilsServiceStub.ESearchResult searchResult;
    protected EUtilsServiceStub.EPostResult postResult;
    protected int return_count = 0;
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PubMedRetriever pmr = new PubMedRetriever();
		pmr.initialize();
		try {
//			pmr.crawlByDate(dateFormat.parse("2001/02/03"));
			//HashSet<String> ids = pmr.getFileNameSetByIdList( pmr.getIdSetByDate("2013/06/12","edat") );
			String dateStr = "2013/06/13";
			String[] ids;
//			ids = pmr.getIdSetByDate(dateStr,"mdat") ;
//			System.out.println("Total ids: " + ids.length);
//			ids = pmr.getIdSetByDate(dateStr,"edat") ;
//			System.out.println("Total ids: " + ids.length);
//			ids = pmr.getIdSetByDate(dateStr,"cdat") ;
//			System.out.println("Total ids: " + ids.length);
			ids = pmr.getIdSetByDate(dateStr,"mhda") ;
			System.out.println("Total ids: " + ids.length);
			for (int i = 0; i < ids.length; i++)
			System.out.print(ids[i] + ", ");
			if (ids == null) return;
		} catch (Exception e) {
			logger.error("Error ", e);
		}
	}

	@Override
	public void initialize() {
		try {
            // call NCBI ESearch utility
			searchService = new EUtilsServiceStub();
			searchRequest = new EUtilsServiceStub.ESearchRequest();
			searchRequest.setDb(DB_Name);
			searchRequest.setUsehistory("y");
			// Search by Publication Date	
//			searchRequest.setDatetype("edat");
			
			// Search by Create Date

//			searchRequest.setSort("CreateDate");
//			searchRequest.setSort("PublicationDate");
			
//          searchRequest.searchRequest("rat");
//			searchRequest.setMindate("2001/12/01");
//			searchRequest.setMaxdate("2001/12/01");

			postRequest = new EUtilsServiceStub.EPostRequest();
			postRequest.setDb(DB_Name);
			return_count = 0;
		} catch (Exception e) {
			logger.error("Error in initializing PubMed crawler", e);
		}
		
	}

	@Override
	public void prepare() {
		initialize();
	}

	public void execute() throws Exception {
		crawlResult = null;
		searchResult = searchService.run_eSearch(searchRequest);
		webEnv = searchResult.getWebEnv();
		queryKey = searchResult.getQueryKey();
//        logger.info("Docs fectched: " + searchResult.getIdList(). .getId().length);
		if (webEnv == null || queryKey == null) {
			logger.error("[Empty search result returned]");
			try {
				throw new Exception("empty search result");
			} catch (Exception e) {
				logger.error("Error in calling ESearch", e);
			}
		}
		return_count = Integer.parseInt(searchResult.getCount());
		crawlResult = "";

		int ret_start = 0;
		int ret_step = ID_TRUNK_SIZE;
		logger.info("Search returns: " + return_count);

		if (return_count > 0) {
			EFetchPubmedServiceStub.EFetchResult res_final = null;
//			while (ret_start < return_count)
	        try
	        {
	      	  
	            EFetchPubmedServiceStub service = new EFetchPubmedServiceStub();

	            // call NCBI EFetch utility
	            EFetchPubmedServiceStub.EFetchRequest req = new EFetchPubmedServiceStub.EFetchRequest();
	            req.setWebEnv(webEnv);
	            req.setQuery_key(queryKey);
	            req.setRetstart(String.format("%d", ret_start));
	            req.setRetmax(String.format("%d", ret_step));
				logger.info("Retrieving for " + 
						String.format("%d", ret_step) + " starting from " + 
						String.format("%d", ret_start));
//	            req.setRetmax("5");
				Thread.sleep(CRAWL_DELAY);

	            EFetchPubmedServiceStub.EFetchResult res = service.run_eFetch(req);

	            if (res_final == null) {
	            	res_final = res;
//	            	ret_start += res.getPubmedArticleSet().getPubmedArticleSetChoice().length;
	            } else {
	            	PubmedArticleSetChoiceE[] docs = res.getPubmedArticleSet().getPubmedArticleSetChoice();
	            	for (int i = 0; i < docs.length; i++) {
	            		res_final.getPubmedArticleSet().addPubmedArticleSetChoice(docs[i]);
	            	}
	            	ret_start += docs.length;
	            }
	        }
	        catch (Exception e) { 
				if (e.getMessage() != null && e.getMessage().endsWith("Required attribute Status is missing")) {
					if (ret_step == 1) {
						ret_start ++;
						ret_step = return_count;
					} else {
						ret_step = ret_step / 4 + 1;
					}
					logger.info("Retrying with smaller retrieve size: " + 
							String.format("%d", ret_step));
				} else throw e;
	        }
            OMElement res_oe = res_final.getOMElement(EFetchPubmedServiceStub.EFetchResult.MY_QNAME, OMAbstractFactory.getOMFactory());
            crawlResult = res_oe.toString();
		}
	}

	@Override
	public void process() {
//		try {
//	        File file = new File("/tmp/t1.xml");
//	        FileOutputStream fo = new FileOutputStream(file);
//	        fo.write(crawlResult.getBytes());
//	        fo.close();
////	        System.out.println("XML: " + res_oe);
//	        XMLInputFactory xml_factory = XMLInputFactory.newInstance();
//	        EFetchPubmedServiceStub.EFetchResult res2 = EFetchPubmedServiceStub.EFetchResult.Factory.parse(xml_factory.createXMLStreamReader(new StringInputStream(crawlResult)));
//
//	        System.out.println("XML string parsed, totla documents: "+ res2.getPubmedArticleSet().getPubmedArticleSetChoice().length);
//		} catch (Exception e) {
//		}
	}

	public String crawlByDate(Date date) throws Exception {
		prepare();
		searchRequest.setDatetype("crdt");
		searchRequest.setMindate(dateFormat.format(date));
		searchRequest.setMaxdate(dateFormat.format(date));
		try {
			execute();
		} catch (Exception e) {
			throw e;
		}
		process();
		return crawlResult;
	}
	
	public String crawByIdList(String Ids) throws Exception {
		prepare();
		postRequest.setId(Ids);
		try {
			postResult = searchService.run_ePost(postRequest);
			webEnv = postResult.getWebEnv();
			queryKey = postResult.getQueryKey();
			searchRequest.setQueryKey(queryKey);
			searchRequest.setWebEnv(webEnv);
			searchRequest.setDatetype("edat");
			searchRequest.setMindate("1865/01/01");
//			searchRequest.setSort("PMID");
			execute();
		} catch (Exception e) {
			throw e;
		}
		process();
		return crawlResult;
	}
	
	public String crawlByIDRange(long start_id) throws Exception {
		prepare();
		postRequest.setId(getIDStr(start_id));
		try {
			postResult = searchService.run_ePost(postRequest);
			webEnv = postResult.getWebEnv();
			queryKey = postResult.getQueryKey();
			searchRequest.setQueryKey(queryKey);
			searchRequest.setWebEnv(webEnv);
			searchRequest.setDatetype("edat");
			searchRequest.setMindate("1865/01/01");
//			searchRequest.setSort("PMID");
			execute();
		} catch (Exception e) {
			throw e;
		}
		process();
		return crawlResult;
	}
	
	private String getIDStr(long start_id) {
		String return_str = "";
		for (int i = 0; i < ID_TRUNK_SIZE; i++) {
			return_str += String.format("%d,", start_id + i);
		}
		return return_str;
	}
	
	public PubMedRetriever() {
	}
	
	public String[] getIDRangeByDate(String dateToRequest) throws Exception {

		try {
	        searchRequest.setDb("pubmed");
	        searchRequest.setUsehistory("y");
	        searchRequest.setDatetype("edat");
	        searchRequest.setMindate(dateToRequest);
	        searchRequest.setMaxdate(dateToRequest);
	        searchRequest.setRetMax("999999");
	        
	        searchResult = searchService.run_eSearch(searchRequest);
	        
            String[] id_list =  searchResult.getIdList().getId();
            if (id_list == null) return null;
            Long min_id=(long)-1, max_id=(long)-1;
            for (String cur_id : id_list) {
          	  Long cur_id_long = Long.parseLong(cur_id);
          	  if (min_id == (long)-1) {
          		  min_id = max_id = cur_id_long;
          	  } else {
          		  if (min_id > cur_id_long) min_id = cur_id_long;
          		  if (max_id < cur_id_long) max_id = cur_id_long;
          	  }
            }
            String[] return_result = new String[2];
            return_result[0] = min_id.toString();
            return_result[1] = max_id.toString();
            return return_result;
	        
		} catch (Exception e) {
			throw e;
		}
	}

	public HashSet<String> getFileNameSetByIdList(String[] Ids) throws Exception {
        if (Ids == null) return null;
        HashSet<String> return_set = new HashSet<String>();
        for (String cur_id : Ids) {
        	Long set_id = Long.parseLong(cur_id)/1000*1000;
        	return_set.add(set_id.toString());
        }
        return return_set;
	}
	
	public String[] getIdSetByDate(String dateToRequest, String dateType) throws Exception {

		try {
	        searchRequest.setDb("pubmed");
	        searchRequest.setUsehistory("y");
	        searchRequest.setDatetype(dateType);
	        searchRequest.setMindate(dateToRequest);
	        searchRequest.setMaxdate(dateToRequest);
	        searchRequest.setRetMax("999999");
	        
	        searchResult = searchService.run_eSearch(searchRequest);
	        
            return searchResult.getIdList().getId();
	        
		} catch (Exception e) {
			throw e;
		}
	}
}
