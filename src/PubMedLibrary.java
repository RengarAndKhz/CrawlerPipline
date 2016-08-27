/**
 * 
 */

import java.io.BufferedWriter;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.mapreduce.Job;
import org.apache.log4j.PropertyConfigurator;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.tartarus.snowball.Stemmer;

import edu.mcw.rgd.common.utils.BasicUtils;
import edu.mcw.rgd.common.utils.FileEntry;
import edu.mcw.rgd.common.utils.FileList;
import edu.mcw.rgd.common.utils.SortedCountMap;
import edu.mcw.rgd.database.ncbi.log.Queries;
import edu.mcw.rgd.database.ncbi.pubmed.AnnotationDAO;
import edu.mcw.rgd.database.ncbi.pubmed.AnnotationRecord;
import edu.mcw.rgd.database.ncbi.pubmed.ArticleDAO;
import edu.mcw.rgd.database.ncbi.pubmed.PubmedCouchDAO;
import edu.mcw.rgd.nlp.classifier.ArticleOrganismClassifier;
import edu.mcw.rgd.nlp.datamodel.NcbiQueryLogEntry;
import edu.mcw.rgd.nlp.datamodel.Ontology;
import edu.mcw.rgd.nlp.datamodel.SolrOntologyEntry;
import edu.mcw.rgd.nlp.utils.Library;
import edu.mcw.rgd.nlp.utils.LibraryBase;
import edu.mcw.rgd.nlp.utils.gate.AnnieAnnotator;
import edu.mcw.rgd.nlp.utils.solr.PubMedSolrDoc;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub.PublicationType;

/**
 * @author wliu
 * 
 */
public class PubMedLibrary extends LibraryBase implements Library {

//	public static String solrServer = "http://localhost:8080/solr/";
	public static String solrServer = "http://morgan:45600/solr/";
	public static CommonsHttpSolrServer[] solrServers = null;
	public static Random solrServerIdGenerator = new Random();

	protected static int RETRY_LIMIT = 3;
	protected FileList fileList = new FileList();
	protected FileList failedList = new FileList();
	protected FileList emptyList = new FileList();

	protected static DateFormat FILE_NAME_DF = new SimpleDateFormat(
			"yyyy_MM_dd");
	protected static String DATE_FILE_DIR = "/date_id_maps/";

	protected static String[] DATE_TYPES = { "cdat", "mdat", "edat", "mhda" };
	
	protected List<String> annSets;
	public static Job mrJob;
	
	public static String MR_ANN_SETS = "mapred.input.annotation.sets";
//	protected static ArticleDAO mrDao;
	protected static AnnieAnnotator mrAnnoator;
	public static ArticleDAO mrArticleDao = new ArticleDAO();
	
	public static final String HBASE_NAME = "pubmed";

	/**
	 * 
	 */
	public PubMedLibrary() {
		// TODO Auto-generated constructor stub
	}

	protected static void initSolrServers() {
		solrServers = new CommonsHttpSolrServer[20];
		try {
			int serverId = 0;
			solrServers[serverId] = new CommonsHttpSolrServer("http://morgan:9292/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://morgan:9293/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://morgan:9294/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://morgan:9295/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://morgan:9296/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://fox:9292/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://fox:9293/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://fox:9294/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://fox:9295/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://fox:9296/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://morgan:9297/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://fox:9297/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://morgan:9298/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://fox:9298/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://morgan:9299/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://fox:9299/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://morgan:9300/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://fox:9300/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://morgan:9301/solr/"); serverId++;
			solrServers[serverId] = new CommonsHttpSolrServer("http://fox:9301/solr/"); serverId++;
		} catch (Exception e) {
			return;
		}
	}
	
	public void setAnnotationSets(List<String> annSets) {
		this.annSets = annSets;
	}
	
	public void resetAnnotator(String gateHome, boolean useStemming) {
		mrAnnoator = getAnnotator(gateHome, false, useStemming);
	}
	
	public int batchDownload(long start_id, long end_id, boolean force_update) {
		logger.info("Start downloading from PMID " + start_id + " to PMID"
				+ end_id);
		PubMedRetriever retriever = new PubMedRetriever();
		start_id = ((start_id - 1) / PubMedRetriever.ID_TRUNK_SIZE)
				* PubMedRetriever.ID_TRUNK_SIZE + 1;
		while (start_id <= end_id) {
			try {
				String file_name = getFileNameByID(start_id);
				logger.info("Downloading " + file_name);
				if (force_update) {
					fileList.removeFile(file_name);
					fileList.save();
					failedList.removeFile(file_name);
					failedList.save();
					emptyList.removeFile(file_name);
					emptyList.save();
				}
				if (fileList.findFile(file_name, 0) < 0
						&& emptyList.findFile(file_name, 0) < 0) {
					String file_path = getFilePathByID(start_id);
					fileList.removeFile(file_name);
					fileList.save();
					failedList.addFile(file_name);
					failedList.save();
					retriever.crawlByIDRange(start_id);
					if (retriever.getCrawlResult() != null) {
						failedList.removeFile(file_name);
						failedList.save();
						if (retriever.getCrawlResult().length() > 0) {
							PubMedDocSet docSet = new PubMedDocSet();
							docSet.setFileName(file_name);
							docSet.setDocSetXML(retriever.getCrawlResult());
							docSet.saveDoc(file_path);
							fileList.addFile(file_name);
							fileList.save();
						} else {
							emptyList.addFile(file_name);
							emptyList.save();
						}
					}
				} else {
					logger.info("PMID set already crawled " + file_name);
				}
			} catch (Exception e) {
				logger.error("Error in crawling a PMID set ", e);
			}
			start_id += PubMedRetriever.ID_TRUNK_SIZE;
		}
		logger.info("Finished downloading from PMID " + start_id + " to PMID "
				+ end_id);
		return 0;
	}

	public void crawlFilesByDate(Date date, HashSet<String> ids)
			throws Exception {
		int cur_file_no = 0;
		int cur_id_count = 0;
		String ids_buf = "";
		String file_name_base = getFileNameByDate(date);
		String file_path = getFilePathByDate(date);
		String cur_file_no_str = "", file_name = "";
		cur_file_no_str = String.format("%03d", cur_file_no);

		if (ids == null)
			return;

		try {
			for (String cur_id : ids) {
				ids_buf += (cur_id + ",");
				cur_id_count++;
				if (cur_id_count == PubMedRetriever.ID_TRUNK_SIZE) {
					file_name = file_name_base + "_" + cur_file_no_str;
					int retryCounter = RETRY_LIMIT + 1;
					while (retryCounter > 0) {
						try {
							crawlDateChunk(file_name, file_path, ids_buf);
							retryCounter = 0;
						} catch (Exception e) {
							logger.error("Error in crawling a date chunk ["
									+ cur_file_no_str + "]", e);
							retryCounter --;
							if (retryCounter > 0) {
								logger.info("Retrying crawling a date chunk ["
										+ cur_file_no_str + "]: " + (RETRY_LIMIT - retryCounter + 1));
							}
						}
					}
					cur_id_count = 0;
					ids_buf = "";
					cur_file_no++;
					cur_file_no_str = String.format("%03d", cur_file_no);
				}
			}
			if (cur_id_count > 0) {
				try {
					file_name = file_name_base + "_" + cur_file_no_str;
					crawlDateChunk(file_name, file_path, ids_buf);
				} catch (Exception e) {
					logger.error("Error in crawling a date chunk ["
							+ cur_file_no_str + "]", e);
					throw e;
				}
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private void crawlDateChunk(String file_name, String file_path, String ids)
			throws Exception {
		PubMedRetriever retriever = new PubMedRetriever();
		retriever.crawByIdList(ids);
		if (retriever.getCrawlResult() != null) {
			if (retriever.getCrawlResult().length() > 0) {
				PubMedDocSet docSet = new PubMedDocSet();
				docSet.setFileName(file_name);
				docSet.setDocSetXML(retriever.getCrawlResult());
				docSet.saveDoc(file_path);
			} else {
				emptyList.addFile(file_name);
				emptyList.save();
			}
		}
	}

	/**
	 * @param start_date
	 * @param end_date
	 * @return
	 */
	public int batchDownload(Date start_date, Date end_date, boolean force_update) {
		String start_date_str = PubMedRetriever.dateFormat.format(start_date);
		String end_date_str = PubMedRetriever.dateFormat.format(end_date);
		logger.info("Start downloading from " + start_date_str + " to "
				+ end_date_str);

		PubMedRetriever retriever = new PubMedRetriever();
		retriever.initialize();
		boolean forward_crawling = start_date.before(end_date);

		try {

			while ((forward_crawling && !start_date.after(end_date))
					|| (!forward_crawling && !start_date.before(end_date))) {
				String startdateStr = PubMedRetriever.dateFormat
						.format(start_date);
				if (force_update) {
					fileList.removeFile(startdateStr);
					fileList.save();
					failedList.removeFile(startdateStr);
					failedList.save();
					emptyList.removeFile(startdateStr);
					emptyList.save();
				}

				if (fileList.findFile(startdateStr, 0) < 0) {
					logger.info("Crawling " + startdateStr);
					fileList.removeFile(startdateStr);
					fileList.save();
					failedList.addFile(startdateStr);
					failedList.save();
					String[] ids = {};
					HashSet<String> idSet = new HashSet<String>();
					for (String dateType : DATE_TYPES) {
						try {
							ids = retriever.getIdSetByDate(startdateStr,
									dateType);
						} catch (Exception e) {
							logger.info("Error in getting IDs for "
									+ startdateStr + "[" + dateType + "]");
							e.printStackTrace();
							ids = null;
						}
						if (ids != null) {
							for (String id_tmp : ids)
								idSet.add(id_tmp);
						}
					}
					saveDateIdMap(start_date, idSet);
					crawlFilesByDate(start_date, idSet);
					failedList.removeFile(startdateStr);
					failedList.save();
					fileList.addFile(startdateStr);
					fileList.save();

				} else {
					logger.info("Date already crawled " + startdateStr);
				}

				start_date = new Date(start_date.getTime()
						+ (forward_crawling ? 1 : -1) * 24 * 3600 * 1000);
			}

			// for (String id : idSet) {
			// batchDownload(Long.parseLong(id), Long.parseLong(id), true);
			// };
		} catch (Exception e) {
			logger.error("Error crawling dates", e);
		}

		logger.info("Finished downloading from " + start_date_str + " to "
				+ end_date_str);
		return 0;
	}

	/**
	 * @param start_id
	 * @param end_id
	 * @return
	 */
	public int batchImportToDB(long start_id, long end_id) {
		logger.info("Start importing from PMID " + start_id + " to PMID"
				+ end_id);
		long start_set = start_id / PubMedRetriever.ID_TRUNK_SIZE;
		long end_set = end_id / PubMedRetriever.ID_TRUNK_SIZE;
		if (start_set * PubMedRetriever.ID_TRUNK_SIZE != start_id)
			start_set--;
		start_set++;
		end_set++;
		for (long id_set_no = start_set; id_set_no <= end_set; id_set_no++) {
			String file_name = getFileNameByID(id_set_no
					* PubMedRetriever.ID_TRUNK_SIZE);
			if (fileList.findFile(file_name, 0) >= 0) {
				PubMedDocSet pmds = new PubMedDocSet();
				pmds.setFileName(file_name);
				String file_path = getFilePathByID(id_set_no
						* PubMedRetriever.ID_TRUNK_SIZE);
				try {
					pmds.loadDoc(file_path);
					pmds.parseDocSet();
					// System.out.println("length: " + n);
					pmds.importToDB();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return 0;
	}

	/**
	 * @param start_date
	 * @param end_date
	 * @return
	 */
	public int batchImportToDBByDate(Date start_date, Date end_date) {
		logger.info("Start importing from "
				+ PubMedRetriever.dateFormat.format(start_date) + " to "
				+ PubMedRetriever.dateFormat.format(end_date));

		boolean forward_process = start_date.before(end_date);

		try {

			while ((forward_process && !start_date.after(end_date))
					|| (!forward_process && !start_date.before(end_date))) {
				String file_path = getFilePathByDate(start_date);
				String file_name = getFileNameByDate(start_date);
				String file_name_in_list = PubMedRetriever.dateFormat
						.format(start_date);

				if (fileList.findFile(file_name_in_list, 0) >= 0) {
					logger.info("Importing "
							+ PubMedRetriever.dateFormat.format(start_date));

					int chunk_no = 0;
					String chunk_file_name;
					chunk_file_name = file_name + "_"
							+ String.format("%03d", chunk_no);
					File chunk_file = new File(file_path + "/"
							+ chunk_file_name + ".xml");
					while (chunk_file.exists()) {
						logger.info("Importing " + chunk_file_name);
						PubMedDocSet pmds = new PubMedDocSet();
						pmds.setFileName(chunk_file_name);
						try {
							pmds.loadDoc(file_path);
							pmds.parseDocSet();
							pmds.importToDB();
						} catch (Exception e) {
							e.printStackTrace();
						}
						chunk_no++;
						chunk_file_name = file_name + "_"
								+ String.format("%03d", chunk_no);
						chunk_file = new File(file_path + "/" + chunk_file_name
								+ ".xml");
					}
				}
				logger.info("Finished importing "
						+ PubMedRetriever.dateFormat.format(start_date));
				start_date = new Date(start_date.getTime()
						+ (forward_process ? 1 : -1) * 24 * 3600 * 1000);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		;
		return 0;
	}

	/**
	 * @param start_date
	 * @param end_date
	 * @return
	 */
	public int batchAnnotateToDBByDate(String annotatorPath,
			List<String> annotationSets, Date startDate, Date endDate) {
		logger.info("Start annotating from "
				+ PubMedRetriever.dateFormat.format(startDate) + " to "
				+ PubMedRetriever.dateFormat.format(endDate)
				+ " for annotator [" + annotatorPath + "] in ["
				+ annotationSets.toString() + "]");

		AnnieAnnotator annotator = getAnnotator(annotatorPath, true);
		if (annotator == null) {
			logger.info("Failed annotating "
					+ PubMedRetriever.dateFormat.format(startDate)
					+ " Can't get annotator!");
		}
		ArticleDAO dao = new ArticleDAO();
		boolean forward_process = startDate.before(endDate);

		try {

			while ((forward_process && !startDate.after(endDate))
					|| (!forward_process && !startDate.before(endDate))) {
				String file_path = getDateFilePath(startDate);
				FileList pmid_list = new FileList();
				pmid_list.setFilePath(file_path, false, true);

				logger.info("Start annotating "
						+ PubMedRetriever.dateFormat.format(startDate));

				for (FileEntry pm_article : pmid_list.fileList) {
					annotateToDB(annotator, annotationSets,
							pm_article.getFileName(), dao);
				}

				logger.info("Finished annotating "
						+ PubMedRetriever.dateFormat.format(startDate));
				startDate = new Date(startDate.getTime()
						+ (forward_process ? 1 : -1) * 24 * 3600 * 1000);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		;
		return 0;
	}

	/**
	 * @param start_date
	 * @param end_date
	 * @return
	 */
	public int batchIndexToSolrByDate(Date startDate, Date endDate) {
		logger.info("Start indexing to Solr from "
				+ PubMedRetriever.dateFormat.format(startDate) + " to "
				+ PubMedRetriever.dateFormat.format(endDate));

		boolean forward_process = startDate.before(endDate);

		try {

			while ((forward_process && !startDate.after(endDate))
					|| (!forward_process && !startDate.before(endDate))) {
				String file_path = getDateFilePath(startDate);
				FileList pmid_list = new FileList();
				pmid_list.setFilePath(file_path, false, true);

				logger.info("Start indexing "
						+ PubMedRetriever.dateFormat.format(startDate));

				for (FileEntry pm_article : pmid_list.fileList) {
					try {
						indexArticle(Long.parseLong(pm_article.getFileName()));
					} catch (Exception e) {
						try {
							logger.error("Perform a forced garbage collection and sleep for a while, then retry!");
//							System.gc();
//							Thread.sleep(10000);
							logger.error("Error in updating doc ["
									+ pm_article.getFileName() + "] to Solr: "
									+ BasicUtils.strExceptionStackTrace(e));
							indexArticle(Long.parseLong(pm_article
									.getFileName()));
						} catch (Exception e1) {
							logger.error("Can't sleep " + BasicUtils.strExceptionStackTrace(e));
						}

					}
				}

				logger.info("Finished indexing "
						+ PubMedRetriever.dateFormat.format(startDate));
				startDate = new Date(startDate.getTime()
						+ (forward_process ? 1 : -1) * 24 * 3600 * 1000);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		;
		return 0;
	}

	public String getFileNameByDate(Date date) {
		return FILE_NAME_DF.format(date);
	}

	public String getFilePathByDate(Date date) {
		DateFormat df = new SimpleDateFormat("yyyy");
		String path = getDocPath() + "/" + df.format(date);
		File file = new File(path);
		if (!file.exists())
			file.mkdir();
		return path;
	}

	public Date getDateByFileName(String file_name) throws ParseException {
		return FILE_NAME_DF.parse(file_name);
	}

	public long getIDByFileName(String file_name) throws ParseException {
		return Long.parseLong(file_name) * 1000 + 1;
	}

	@Override
	public String getDocPath() {
		return getPathDoc();
	}

	@Override
	public void setPathDoc(String pathDoc) {
		super.setPathDoc(pathDoc);
		try {
			fileList.setFilePath(getPathDoc() + "/file_list.txt");
			failedList.setFilePath(getPathDoc() + "/file_list_failed.txt");
			emptyList.setFilePath(getPathDoc() + "/file_list_empty.txt");
		} catch (Exception e) {
			logger.error("Error in setting PathDoc", e);
		}
	}

	public void tryFailed() {
		ArrayList<FileEntry> list_copy = failedList.cloneList();
		try {
			for (int i = 0; i < list_copy.size(); i++) {
				long start_id = getIDByFileName(list_copy.get(i).getFileName());
				batchDownload(start_id, start_id, true);
			}
			;
		} catch (Exception e) {
			logger.error("Error in trying failed list", e);
		}
	}

	public void tryFailedDates() {
		ArrayList<FileEntry> list_copy = failedList.cloneList();
		try {
			for (int i = 0; i < list_copy.size(); i++) {
				Date start_date = PubMedRetriever.dateFormat.parse(list_copy
						.get(i).getFileName());
				Date end_date = new Date(start_date.getTime() + 23 * 3600
						* 1000 + 59 * 60 * 1000 + 59 * 1000);

				batchDownload(start_date, end_date, true);
			}
			;
		} catch (Exception e) {
			logger.error("Error in trying failed list", e);
		}
	}

	public String getFileNameByID(long id) {
		long seg_id = (id - 1) / PubMedRetriever.ID_TRUNK_SIZE;
		return String.format("%08d", seg_id);
	}

	public String getFilePathByID(long id) {
		long seg_id = (id - 1) / PubMedRetriever.ID_TRUNK_SIZE
				/ PubMedRetriever.ID_TRUNK_SIZE;
		String path = getDocPath() + "/" + String.format("%04d", seg_id);
		File file = new File(path);
		if (!file.exists())
			file.mkdir();
		return path;
	}

	public String getDateFilePath(Date date) throws Exception {
		File file = new File(getDocPath() + DATE_FILE_DIR);
		try {
			if (!file.exists())
				file.mkdir();
		} catch (Exception e) {
			logger.error("Error getting date file directory", e);
			throw e;
		}
		return getDocPath() + DATE_FILE_DIR + FILE_NAME_DF.format(date)
				+ ".txt";
	}

	public void saveDateIdMap(Date date, HashSet<String> Ids) throws Exception {
		try {
			BufferedWriter bw = new BufferedWriter(new java.io.FileWriter(
					getDateFilePath(date)));
			if (Ids != null) {
				bw.write(String.format("%d", Ids.size()));
				bw.newLine();
				for (String id : Ids) {
					bw.write(id.toString());
					bw.newLine();
				}
			} else {
				bw.write("0");
				bw.newLine();
			}
			bw.close();

		} catch (Exception e) {
			logger.error("Error saving date id map", e);
			throw e;
		}
	}

	/**
	 * @param args
	 */
	public static void crawl(String[] args) {

		String path = args[1];
		String start = args[2];
		String end = args[3];
		PropertyConfigurator.configure(path + "/conf/crawler.cnf");

		// Crawl by id
		if (end == null || end.length() == 0)
			end = start;
		logger.info("Start crawling: [" + path + "] PMID [" + start + "]["
				+ end + "]");

		PubMedLibrary library = new PubMedLibrary();
		// library.setPathDoc("/shared/users/wliu/tmp/pubmed");
		// library.setPathDoc("/data/pubmed");
		library.setPathDoc(path);
		try {
			library.tryFailed();
			// Date start_date = PubMedRetriever.dateFormat.parse("2001/01/01");
			long start_id = Long.parseLong(start);
			long end_id = Long.parseLong(end);
			library.batchDownload(start_id, end_id, false);
		} catch (Exception e) {
			logger.error("Error", e);
		}
		;
		logger.info("Crawling finished [" + path + "] PMID [" + start + "]["
				+ end + "]");
	}

	public static void crawlByDate(String[] args) {

		String path = args[1];
		Date startDate = new Date(), endDate = new Date();
		PubMedLibrary.getDates(args, 2, startDate, endDate);
		PropertyConfigurator.configure(path + "/conf/crawler.cnf");

		// Crawl by dates
		logger.info("Start crawling: [" + path + "] from [" + startDate
				+ "] to [" + endDate + "]");

		PubMedLibrary library = new PubMedLibrary();
		library.setPathDoc(path);
		try {
			library.tryFailedDates();
			library.batchDownload(startDate, endDate, true);
		} catch (Exception e) {
			logger.error("Error", e);
		}
		;
		logger.info("Crawling finished [" + path + "] from [" + startDate
				+ "] to [" + endDate + "]");
	}

	public void annotateToDB(String annotator_path,
			List<String> annotation_sets, long start_id, long end_id) {
		annotateToDB(getAnnotator(annotator_path, true), annotation_sets, start_id,
				end_id);
	}

	public static AnnieAnnotator getAnnotator(String annotator_path, boolean local) {
		return getAnnotator(annotator_path, local, false);
	}
	
	public static AnnieAnnotator getAnnotator(String annotator_path, boolean local, boolean useStemming) {
		AnnieAnnotator annotator = new AnnieAnnotator();
		try {
			 if (local) {
				 annotator.init(annotator_path);
			 } else {
				 annotator.initOnHDFS(annotator_path);
			 }

			 annotator.setUseStemming(useStemming);
			 return annotator;
		} catch (Exception e) {
			logger.error("Error in getting annotator", e);
			return null;
		}
	}

	public void annotateToDB(AnnieAnnotator annotator,
			List<String> annotation_sets, long start_id, long end_id) {
		if (annotator == null || annotation_sets == null)
			return;
		try {
			ArticleDAO dao = new ArticleDAO();
			int step = start_id < end_id ? 1 : -1;
			long dest_id = end_id + step;
			for (long cur_id = start_id; cur_id != dest_id; cur_id += step) {
				annotateToDB(annotator, annotation_sets, Long.toString(cur_id),
						dao);
			}

		} catch (Exception e) {
			logger.error("Error", e);
		}
	}

	public void annotateToDB(AnnieAnnotator annotator,
			List<String> annotation_sets, String pmid, ArticleDAO dao) {
		logger.info("Annotating: [" + pmid + "]");
		try {
			
			// Check if it's for NCBI log annotation
			if (AnnotationDAO.tableName.equals(Queries.annotationTableName)) {
				Long queryId = Long.parseLong(pmid);
				String queryTerm = Queries.getQueryTerm(queryId);
				if (queryTerm != null && queryTerm.length() > 0) {
					annotateToDB(annotator, queryId, queryTerm, 0,
							annotation_sets, annotator.isUseStemming());
				}
				return;
			}
			
			if (dao.getArticleFromCouch(pmid)) {
				// Annotate title
				if (dao.articleTitle != null && dao.articleTitle.length() > 0) {
					annotateToDB(annotator, dao.pmid, dao.articleTitle, 0,
							annotation_sets, annotator.isUseStemming());
				}
				// Annotate abstract
				if (dao.articleAbstract != null
						&& dao.articleAbstract.length() > 0) {
					annotateToDB(annotator, dao.pmid, dao.articleAbstract, 1,
							annotation_sets, annotator.isUseStemming());
				}
				// Annotate mesh terms
				if (dao.meshTerms != null && dao.meshTerms.length() > 0) {
					annotateToDB(annotator, dao.pmid, dao.meshTerms, 2,
							annotation_sets, annotator.isUseStemming());
				}
				

			}
		} catch (Exception e) {
			logger.error("Error", e);
		}
	}

	public List<String> annotateToFile(AnnieAnnotator annotator,
			List<String> annotation_sets, String pmid, ArticleDAO dao) {
		logger.info("Annotating: [" + pmid + "]");
		List<String> annResult = new ArrayList<String>(); 
		try {
			if (dao.getArticleFromCouch(pmid)) {
				// Must have a title
				if (dao.articleTitle != null && dao.articleTitle.length() > 0) {
					annResult.addAll(annotateToString(annotator, dao.pmid, dao.articleTitle, 0,
							annotation_sets, annotator.isUseStemming()));
				}
				// Must have an abstract
				if (dao.articleAbstract != null
						&& dao.articleAbstract.length() > 0) {
					annResult.addAll(annotateToString(annotator, dao.pmid, dao.articleAbstract, 1,
							annotation_sets, annotator.isUseStemming()));
				}
				// Must have an abstract
				if (dao.meshTerms != null && dao.meshTerms.length() > 0) {
					annResult.addAll(annotateToString(annotator, dao.pmid, dao.meshTerms, 2,
							annotation_sets, annotator.isUseStemming()));
				}
			}
		} catch (Exception e) {
			logger.error("Error", e);
			return annResult;
		}
		return annResult;
	}

	public List<String> annotateFromHResult(AnnieAnnotator annotator,
			List<String> annotation_sets, Result result, ArticleDAO dao) {
		List<String> annResult = new ArrayList<String>(); 
		try {
			if (dao.getArticleFromHResult(result)) {
//				logger.info("Annotating: [" + dao.pmid + "]");
				// Annotate title
				if (dao.articleTitle != null && dao.articleTitle.length() > 0) {
					annResult.addAll(annotateToString(annotator, dao.pmid, dao.articleTitle, 0,
							annotation_sets, annotator.isUseStemming()));
				}
				// Annotate abstract
				if (dao.articleAbstract != null
						&& dao.articleAbstract.length() > 0) {
					annResult.addAll(annotateToString(annotator, dao.pmid, dao.articleAbstract, 1,
							annotation_sets, annotator.isUseStemming()));
				}
				// Annotate MeSH terms
				if (dao.meshTerms != null && dao.meshTerms.length() > 0) {
					annResult.addAll(annotateToString(annotator, dao.pmid, dao.meshTerms, 2,
							annotation_sets, annotator.isUseStemming()));
				}
				
				// Annotate Keywords
				if (dao.keywords != null && dao.keywords.length() > 0) {
					annResult.addAll(annotateToString(annotator, dao.pmid, dao.keywords, 3,
							annotation_sets, annotator.isUseStemming()));
				}
				
				// Annotate Keywords
				if (dao.chemicals != null && dao.chemicals.length() > 0) {
					annResult.addAll(annotateToString(annotator, dao.pmid, dao.chemicals, 4,
							annotation_sets, annotator.isUseStemming()));
				}
			}
		} catch (Exception e) {
			logger.error("Error in [" + dao.pmid + "]", e);
			return annResult;
		}
		return annResult;
	}

	public enum OutputType {
		TO_DATABASE, TO_FILE, TO_HBASE
	}
	
	public List<String> annotateToOutput(OutputType output, AnnieAnnotator annotator, long pmid,
			String text_to_annotate, int text_location,
			List<String> annotation_sets, boolean useStemming) {
//		AnnotationDAO.deleteRecord(Long.toString(pmid), annotation_sets, text_location);
		Document doc;
		if (useStemming) {
			doc= annotator.process(Stemmer.stem(text_to_annotate));
		} else {
			doc= annotator.process(text_to_annotate);
		}
		List<String> outputStr = new ArrayList<String>();
		if (doc != null) {
			try { 
				for (String anns_set_name : annotation_sets) {
					AnnotationSet anns = doc.getAnnotations(anns_set_name);
					for (Annotation ann : anns) {
						switch (output) {
						case TO_DATABASE:
							AnnotationDAO.insertRecord(Long.toString(pmid), text_location,
									anns_set_name, ann);
						case TO_FILE:
							outputStr.add(pmid + "\t" + text_location + "\t" + ann.getType() + "\t"
									+ anns_set_name + "\t" + ann.getStartNode().getOffset() + "\t"
									+ ann.getEndNode().getOffset() + "\t" + ann.getFeatures().toString());
						case TO_HBASE:
							outputStr.add(text_location + "\t"  + ann.getType() + "\t"
									+ anns_set_name + "\t" + ann.getStartNode().getOffset() + "\t"
									+ ann.getEndNode().getOffset() + "\t" + ann.getFeatures().toString());
						default:
						}
					}
				}
				} catch (Exception e) {
					logger.error("Error in annotating: [" + text_to_annotate + "]");
					logger.error(e);
				}
		}
		annotator.clear();
		return outputStr;
			
	}

	public void annotateToDB(AnnieAnnotator annotator, long pmid,
			String text_to_annotate, int text_location,
			List<String> annotation_sets, boolean useStemming) {
		annotateToOutput(OutputType.TO_DATABASE, annotator, pmid, text_to_annotate, text_location, annotation_sets, useStemming);
	}

	public List<String> annotateToString(AnnieAnnotator annotator, long pmid,
			String text_to_annotate, int text_location,
			List<String> annotation_sets, boolean useStemming) {
		return annotateToOutput(OutputType.TO_HBASE, annotator, pmid, text_to_annotate, text_location, annotation_sets, useStemming);
	}
	
	public static void importToDB(String[] args) {

		String path = args[1];
		String start = args[2];
		String end = args[3];
		PropertyConfigurator.configure(path + "/conf/crawler.cnf");

		// Crawl by id
		if (end == null || end.length() == 0)
			end = start;
		logger.info("Annotating to DB: [" + path + "] PMID [" + start + "]["
				+ end + "]");
		System.out.println("Importing to DB: [" + path + "] PMID [" + start
				+ "][" + end + "]");

		PubMedLibrary library = new PubMedLibrary();
		// library.setPathDoc("/shared/users/wliu/tmp/pubmed");
		// library.setPathDoc("/data/pubmed");
		library.setPathDoc(path);
		try {
			// Date start_date = PubMedRetriever.dateFormat.parse("2001/01/01");
			long start_id = Long.parseLong(start);
			long end_id = Long.parseLong(end);
			library.batchImportToDB(start_id, end_id);
		} catch (Exception e) {
			logger.error("Error", e);
		}
		;
		logger.info("Importing finished [" + path + "] PMID [" + start + "]["
				+ end + "]");
		System.out.println("Importing finished [" + path + "] PMID [" + start
				+ "][" + end + "]");
	}

	private static void getDates(String[] args, int startPos, Date startDateIn,
			Date endDateIn) {

		Date startDate, endDate;
		try {
			startDate = PubMedRetriever.dateFormat.parse(args[startPos]);
			if (args[startPos + 1] != null && args[startPos + 1].length() > 0) {
				try {
					endDate = PubMedRetriever.dateFormat
							.parse(args[startPos + 1]);
				} catch (Exception e) {
					System.out.println("Can't get End date!");
					return;
				}
			} else {
				endDate = startDate;
			}
			;
		} catch (Exception e) {
			System.out.println("Can't get Start date!");
			return;
		}

		if (endDate.after(endDateIn)) {
			endDate.setTime(endDateIn.getTime());
		} else if (!startDate.after(endDate))
			endDate = new Date(endDate.getTime() + 23 * 3600 * 1000 + 59 * 60
					* 1000 + 59 * 1000);
		startDateIn.setTime(startDate.getTime());
		endDateIn.setTime(endDate.getTime());
	}

	public static void importToDBByDate(String[] args) {

		String path = args[1];
		PropertyConfigurator.configure(path + "/conf/crawler.cnf");

		Date startDate = new Date(), endDate = new Date();
		PubMedLibrary.getDates(args, 2, startDate, endDate);
		logger.info("Importing to DB: [" + path + "] Date ["
				+ PubMedRetriever.dateFormat.format(startDate) + "]["
				+ PubMedRetriever.dateFormat.format(endDate) + "]");
		System.out.println("Importing to DB: [" + path + "] Date ["
				+ PubMedRetriever.dateFormat.format(startDate) + "]["
				+ PubMedRetriever.dateFormat.format(endDate) + "]");

		PubMedLibrary library = new PubMedLibrary();
		library.setPathDoc(path);
		try {
			library.batchImportToDBByDate(startDate, endDate);
		} catch (Exception e) {
			logger.error("Error", e);
		}
		;
		logger.info("Importing finished [" + path + "] Date ["
				+ PubMedRetriever.dateFormat.format(startDate) + "]["
				+ PubMedRetriever.dateFormat.format(endDate) + "]");
		System.out.println("Importing finished [" + path + "] Date ["
				+ PubMedRetriever.dateFormat.format(startDate) + "]["
				+ PubMedRetriever.dateFormat.format(endDate) + "]");
	}

	public static void batchAnnotateToDB(String[] args) {
		if (args.length < 6) {
			System.out
					.println("Input: annotateToDB [path to annotator] [DB table name] [start pmid] [end pmid] [annotation set 1] [annotation set 2] ...");
			return;
		}
		PubMedLibrary library = new PubMedLibrary();
		List<String> annotation_sets = new ArrayList<String>();
		for (int i = 5; i < args.length; i++) {
			annotation_sets.add(args[i]);
		}
		long start_id = Long.parseLong(args[3]);
		long end_id = Long.parseLong(args[4]);
		AnnotationDAO.tableName = args[2];
		library.annotateToDB(args[1], annotation_sets, start_id, end_id);
	}

	public static void batchAnnotateToDBByDate(String[] args) {
		if (args.length < 7) {
			System.out
					.println("Input: annotateToDBByDate [pubmed data path] [path to annotator] [DB table name] [start date] [end date] [annotation set 1] [annotation set 2] ...");
			return;
		}

		String path = args[1];
		PropertyConfigurator.configure(path + "/conf/annotator.cnf");

		Date startDate = new Date(), endDate = new Date();
		PubMedLibrary.getDates(args, 4, startDate, endDate);

		PubMedLibrary library = new PubMedLibrary();
		library.setPathDoc(path);

		List<String> annotation_sets = new ArrayList<String>();
		for (int i = 6; i < args.length; i++) {
			annotation_sets.add(args[i]);
		}
		AnnotationDAO.tableName = args[3];
		library.batchAnnotateToDBByDate(args[2], annotation_sets, startDate,
				endDate);
	}
	
	public List<String> mrAnnotateToFile(String pmid) {
		if (PubMedLibrary.mrAnnoator == null) {
		    System.setProperty("gate.home","/shared/users/wliu/Tools/gate-6.1-build3913-ALL/");
		    
		    System.setProperty("gate.plugins.home", "/shared/users/wliu/Tools/gate-6.1-build3913-ALL/plugins/");
		    System.setProperty("gate.site.config", "/shared/users/wliu/Tools/gate-6.1-build3913-ALL/gate.xml");

			PubMedLibrary.mrAnnoator = getAnnotator("/user/wliu/results/test4 /shared/users/wliu/Work/Gate/Other_ontologies.xgapp", true);
		}
		return annotateToFile(PubMedLibrary.mrAnnoator, annSets, pmid, new ArticleDAO());
	}

	public List<String> mrAnnotateHResult(Result result, String gateHome, boolean useStemming) {
		if (PubMedLibrary.mrAnnoator == null) {
			PubMedLibrary.mrAnnoator = getAnnotator(gateHome, false, useStemming);
		}
		return annotateFromHResult(PubMedLibrary.mrAnnoator, annSets, result, mrArticleDao);
	}

	public static void mrAnnotateToFile(Job job, String[] args) {
		if (args.length < 4) {
			System.out
					.println("Input: annotateToDBByDate [path to annotator] [output folder] [annotation set 1] [annotation set 2] ...");
			return;
		}

		String annotation_sets = "";
		for (int i = 3; i < args.length; i++) {
			annotation_sets += args[i] + "|";
		}
		PubMedLibrary.mrJob = job;
		job.getConfiguration().setStrings(MR_ANN_SETS, annotation_sets);

		System.out.println("gateHome: " + System.getProperty("gate.home"));
		PubMedLibrary.mrAnnoator = getAnnotator(args[2], true);
	}

	public void initMrEnv() {
		if (annSets == null) {
			String[] annSetEnv = {"Ontologies"}; 
					//PubMedLibrary.mrJob.getConfiguration().getStrings(MR_ANN_SETS);
			String[] ann_sets = annSetEnv[0].split("|");
			annSets = new ArrayList<String>();
			for (int i = 0; i < ann_sets.length; i++) {
				if (ann_sets[i] != null && ann_sets[i].length() > 0) annSets.add(ann_sets[i]);
			}
		}
	}
	
	public static void batchIndexToSolrByDate(String[] args) {
		if (args.length < 4) {
			System.out
					.println("Input: indexToSolrByDate [pubmed data path] [start date] [end date]");
			return;
		}

		String path = args[1];
		PropertyConfigurator.configure(path + "/conf/indexer.cnf");

		Date startDate = new Date(), endDate = new Date();
		PubMedLibrary.getDates(args, 2, startDate, endDate);

		PubMedLibrary library = new PubMedLibrary();
		library.setPathDoc(path);

		library.batchIndexToSolrByDate(startDate, endDate);
	}

	// private static void addMaptoDoc(XmlDoc solr_doc, SortedCountMap map,
	// String key_field, String value_field, String count_field) {
	private static void addMaptoDoc(SolrInputDocument solr_doc,
			SortedCountMap map, String key_field, String value_field, 
			String count_field, String pos_field) {

		List<Long> sorted_counts = map.getSortedCounts();
		List<Object> sorted_keys = map.getSortedKeys();
		HashMap<Object, Object> unsorted_map = map.getUnsortedMap();
		HashMap<Object, String> unsorted_pos = map.get_unsortedPositions();
		Iterator<Long> count_it = sorted_counts.iterator();
		for (Object key : sorted_keys) {
			Long count = count_it.next();
			if (key_field != null)
				solr_doc.addField(key_field, (String) key);
			if (value_field != null)
				solr_doc.addField(value_field, (String) unsorted_map.get(key));
			if (count_field != null) {
				solr_doc.addField(count_field,
						StringEscapeUtils.escapeXml(count.toString()));
			}
			if (pos_field != null) {
				solr_doc.addField(pos_field,
						StringEscapeUtils.escapeXml(unsorted_pos.get(key)));
			}
		}

	}

	// private static void addMaptoDoc(XmlDoc solr_doc, SortedCountMap map,
	// String key_field, String value_field, String count_field) {
	private static void addMaptoDoc(SolrInputDocument solr_doc,
			SortedCountMap map, String key_field, String value_field,
			String count_field) {

		addMaptoDoc(solr_doc, map, key_field, value_field, count_field, null);
	}
	
	public static Boolean indexArticle(Result result) throws Exception {
		ArticleDAO art = new ArticleDAO();
		Boolean indexable = true;
 
		if (art.getArticleFromHResult(result)) {
		List<AnnotationRecord> annotations = AnnotationDAO
			.getAnnotationsFromResult(result);
		List<String> links = AnnotationDAO.getLinksFromResult(result);
		String pmidStr = Long.toString(art.pmid);
		PubMedSolrDoc solr_doc = new PubMedSolrDoc();
		solr_doc.addField("pmid", pmidStr);
		solr_doc.addField("title", art.articleTitle);
		solr_doc.addField("abstract", art.articleAbstract);
		// solr_doc.addField("epub_date", art.articlePubDate);
		solr_doc.addField("p_date", art.articlePubDate);
		solr_doc.addField("j_date_s", art.articleJournalDate);
		solr_doc.addField("authors", art.articleAuthors);
		solr_doc.addField("citation", art.articleCitation);
		solr_doc.addField("mesh_terms", art.meshTerms);
		solr_doc.addField("keywords", art.keywords);
		solr_doc.addField("chemicals", art.chemicals);
		solr_doc.addField("affiliation", art.affiliation);
		solr_doc.addField("p_year", art.publicationYear);
		solr_doc.addField("issn", art.issn);
		if (art.pmcId != null) solr_doc.addField("pmc_id_s", art.pmcId);
		if (art.doi != null) solr_doc.addField("doi_s", art.doi);
 
		
		if (art.publicationTypes != null) {
			for (String pt : art.publicationTypes) {
				solr_doc.addField("p_type", pt);
			}
		}

		SortedCountMap gene_map = new SortedCountMap();
		SortedCountMap rgd_gene_map = new SortedCountMap();
		SortedCountMap organism_map = new SortedCountMap();
		HashMap<String, SortedCountMap> onto_maps = new HashMap<String, SortedCountMap>();
		for (String onto_name : Ontology.getRgdOntologies()) {
			onto_maps.put(onto_name, new SortedCountMap());
		}

		for (AnnotationRecord annotation : annotations) {
			String ann_section = "";
			switch (annotation.text_location) {
			case 0:
				ann_section = art.articleTitle;
				break;
			case 1:
				ann_section = art.articleAbstract;
				break;
			case 2:
				ann_section = art.meshTerms;
				break;
			case 3:
				ann_section = art.keywords;
				break;
			case 4:
				ann_section = art.chemicals;
				break;
			}
			String ann_text, ann_pos;
			ann_pos = String.format("%d;%d-%d", annotation.text_location,
					annotation.text_start,
					annotation.text_end);

			if (!annotation.annotation_set.equals("OrganismTagger")) {
				try {
				ann_text = ann_section.substring(annotation.text_start,
						annotation.text_end);
				} catch (Exception e) {
					System.err.println("Error getting text: [" + pmidStr + ":" + annotation.annotation_set 
							+ "] " + annotation.text_location + ":" + annotation.text_start + ", " + annotation.text_end + " from ["
							+ ann_section + "]");
					ann_text = "";
					indexable = false;
					break;
				}
			} else {
				ann_text = "";
			}

			// Simply put annotations together and index them.
			// Assuming they are independent to each other
			// Complex algorithms can be used later.
			if (annotation.annotation_set.equals("GENES") && !annotation.features_table.get("type").equals("CellLine")
					&& !annotation.features_table.get("type").equals("CellType") 
					&& !annotation.features_table.get("type").equals("RNA")) {
				gene_map.add(ann_text, "", ann_pos);
				// solr_doc.add("gene", ann_text);
			} else if (annotation.annotation_set.equals("RGDGENE")) {
				rgd_gene_map.add(annotation.features_table.get("RGD_ID"),
						ann_text, ann_pos);
			} else if (annotation.annotation_set.equals("OrganismTagger")) {
				String organism_id = (String) annotation.features_table
						.get("ncbiId");
				organism_map.add(organism_id, ArticleOrganismClassifier
						.getNameByID(Long.parseLong(organism_id)), ann_pos);
			} else if (annotation.annotation_set.equals("Ontologies")) {
				String onto_name = (String) annotation.features_table
						.get("minorType");
				onto_maps.get(onto_name).add(
						annotation.features_table.get("ONTO_ID"), ann_text, ann_pos);
			} else if (annotation.annotation_set.equals("Mutations")) {
				onto_maps.get("MT").add((String) annotation.features_table
						.get("wNm"), ann_text, ann_pos);
			} else if (annotation.annotation_set.equals("SNP")) {
				String snpStr = (String) annotation.features_table
						.get("string");
				try {
					onto_maps.get("MT").add(snpStr.replaceAll("\\s",""), ann_text, ann_pos);
				} catch (Exception e) {
					System.err.println("Error in getting SNP annotations of " + pmidStr);
					e.printStackTrace();
					throw e;
				}
			}
		}

		if (!indexable) return false;
		
		gene_map.sort();
		addMaptoDoc(solr_doc, gene_map, "gene", null, "gene_count", "gene_pos");

		rgd_gene_map.sort();
		addMaptoDoc(solr_doc, rgd_gene_map, "rgd_obj_id", "rgd_obj_term",
				"rgd_obj_count", "rgd_obj_pos");

		organism_map.sort();
		addMaptoDoc(solr_doc, organism_map, "organism_ncbi_id",
				"organism_term", "organism_count", "organism_pos");

		for (String onto_name : Ontology.getRgdOntologies()) {
			SortedCountMap onto_map = onto_maps.get(onto_name);
			onto_map.sort(true);
			if (onto_name.equals("MT") && links != null) {
				for (String linkId : links) {
					onto_map.appendVirtualEntry("rs"+linkId);
				}
			}
			SolrOntologyEntry solr_entry = Ontology.getSolrOntoFields()
					.get(onto_name);
			addMaptoDoc(solr_doc, onto_map, solr_entry.getIdFieldName(),
					solr_entry.getTermFieldName(),
					solr_entry.getCountFieldName(),
					solr_entry.getPosFieldName());
		}

		try {
			// Using simple post
			// Server server = new Server(solrServer);

			if (solrServers == null) initSolrServers();
			
			// Used to use the last digit of PMID to decide which Solr server to process the request.
			// After HBase changed to use reversed PMID as keys for balancing reasons, one MR task usually
			// only process articles that have the same last digit of PMID. The load on the Solr
			// servers are not balanced since one MR task will only use one Solr core.  
			//			int serverId = Integer.parseInt(pmidStr.substring(pmidStr.length()-1));
			
			// Change to pick Solr server with a random number. This also allows to use an arbitrary
			// number of Solr cores.
			int serverId = solrServerIdGenerator.nextInt(solrServers.length);
			
			SolrServer server = solrServers[serverId];

			UpdateRequest req = new UpdateRequest();
			req.add(solr_doc);
			UpdateResponse rsp = req.process(server);
			return true;
			
		} catch (Exception e) {
			System.err.println("Error when indexing:" + pmidStr);
			e.printStackTrace();
			return true;
		}
		}
		return true;
	}
	
	public static void indexArticle(long pmid) throws Exception {
		logger.info("[pmid:" + Long.toString(pmid) + "] Indexing to Solr...");
		ArticleDAO art = new ArticleDAO();
		// ArticleOrganismClassifier aoc = new ArticleOrganismClassifier();
		if (art.getArticleFromCouch(Long.toString(pmid))) {
			List<AnnotationRecord> annotations = AnnotationDAO
					.getAllAnnotations(pmid);

			SolrInputDocument solr_doc = new SolrInputDocument();
			solr_doc.addField("pmid", Long.toString(pmid));
			solr_doc.addField("title", art.articleTitle);
			solr_doc.addField("abstract", art.articleAbstract);
			// solr_doc.addField("epub_date", art.articlePubDate);
			solr_doc.addField("p_date", art.articlePubDate);
			solr_doc.addField("j_date_s", art.articleJournalDate);
			solr_doc.addField("authors", art.articleAuthors);
			solr_doc.addField("citation", art.articleCitation);
			solr_doc.addField("mesh_terms", art.meshTerms);

			SortedCountMap gene_map = new SortedCountMap();
			SortedCountMap rgd_gene_map = new SortedCountMap();
			SortedCountMap ctd_map = new SortedCountMap();
			SortedCountMap rs_map = new SortedCountMap();
			SortedCountMap organism_map = new SortedCountMap();
			HashMap<String, SortedCountMap> onto_maps = new HashMap<String, SortedCountMap>();
			for (String onto_name : Ontology.getRgdOntologies()) {
				onto_maps.put(onto_name, new SortedCountMap());
			}

			for (AnnotationRecord annotation : annotations) {
				String ann_section = "";
				switch (annotation.text_location) {
				case 0:
					ann_section = art.articleTitle;
					break;
				case 1:
					ann_section = art.articleAbstract;
					break;
				case 2:
					ann_section = art.meshTerms;
					break;
				}
				String ann_text, ann_pos;
				ann_pos = String.format("%d;%d-%d", annotation.text_location,
						annotation.text_start,
						annotation.text_end);

				if (!annotation.annotation_set.equals("OrganismTagger")) {
					ann_text = ann_section.substring(annotation.text_start,
							annotation.text_end);
				} else {
					ann_text = "";
				}

				// Simply put annotations together and index them.
				// Assuming they are independent to each other
				// Complex algorithms can be used later.
				if (annotation.annotation_set.equals("GENES")) {
					gene_map.add(ann_text, "", ann_pos);
					// solr_doc.add("gene", ann_text);
				} else if (annotation.annotation_set.equals("RGDGENE")) {
					rgd_gene_map.add(annotation.features_table.get("RGD_ID"),
							ann_text, ann_pos);
					// solr_doc.add("rgd_obj_term", ann_text);
					// solr_doc.add("rgd_obj_id", "RGD:" +
					// annotation.features_table.get("RGD_ID"));
				} else if (annotation.annotation_set.equals("OrganismTagger")) {
					String organism_id = (String) annotation.features_table
							.get("ncbiId");
					organism_map.add(organism_id, ArticleOrganismClassifier
							.getNameByID(Long.parseLong(organism_id)), ann_pos);
					// long ncbiID = Long.parseLong((String)
					// annotation.features_table.get("ncbiId"));
					// aoc.AddID(ncbiID);
				} else if (annotation.annotation_set.equals("Ontologies")) {
					String onto_name = (String) annotation.features_table
							.get("minorType");
					onto_maps.get(onto_name).add(
							annotation.features_table.get("ONTO_ID"), ann_text, ann_pos);
				}
			}

			gene_map.sort();
			addMaptoDoc(solr_doc, gene_map, "gene", null, "gene_count", "gene_pos");

			rgd_gene_map.sort();
			addMaptoDoc(solr_doc, rgd_gene_map, "rgd_obj_id", "rgd_obj_term",
					"rgd_obj_count", "rgd_obj_pos");

			organism_map.sort();
			addMaptoDoc(solr_doc, organism_map, "organism_ncbi_id",
					"organism_term", "organism_count", "organism_pos");

			for (String onto_name : Ontology.getRgdOntologies()) {
				SortedCountMap onto_map = onto_maps.get(onto_name);
				onto_map.sort();
				SolrOntologyEntry solr_entry = Ontology.getSolrOntoFields()
						.get(onto_name);
				addMaptoDoc(solr_doc, onto_map, solr_entry.getIdFieldName(),
						solr_entry.getTermFieldName(),
						solr_entry.getCountFieldName(),
						solr_entry.getPosFieldName());
			}

			try {
				// Using simple post
				// Server server = new Server(solrServer);

				// Using SolrJ
				SolrServer server = new HttpSolrServer(solrServer);

				logger.info("[pmid:" + Long.toString(art.pmid)
						+ "] Solr doc string: " + solr_doc.toString());
				// Using simple post
				// server.updateDoc(solr_doc);
				// server.commitChanges();

				// Using SolrJ
				UpdateRequest req = new UpdateRequest();
				req.setAction(ACTION.COMMIT, false, false);
				req.add(solr_doc);
				UpdateResponse rsp = req.process(server);
				req.clear();
				solr_doc.clear();
				logger.info("[pmid:" + Long.toString(art.pmid)
						+ "] finished indexing to Solr...");
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
				// logger.error("Error in update doc [" + Long.toString(pmid)
				// + " to Solr: " + BasicUtils.strExceptionStackTrace(e));
			}
		}
	}
	
	public static String pmidToHbaseKey(String pmid) {
		return new StringBuilder(pmid).reverse().toString();
	}
	
	public static String hbaseKeyToPmid(String key) {
		return new StringBuilder(key).reverse().toString();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.setProperty("java.io.tmpdir", "/data/tmp");
		String command_str = args[0];
		if (command_str.equals("importToDB")) {
			importToDB(args);
			return;
		} else if (command_str.equals("importToDBByDate")) {
			importToDBByDate(args);
			return;
		} else if (command_str.equals("annotateToDB")) {
			batchAnnotateToDB(args);
			return;
		} else if (command_str.equals("annotateToDBByDate")) {
			batchAnnotateToDBByDate(args);
			return;
		} else if (command_str.equals("crawl")) {
			crawl(args);
			return;
		} else if (command_str.equals("crawlByDate")) {
			crawlByDate(args);
			return;
		} else if (command_str.equals("indexToSolr")) {

			long start = Long.parseLong(args[1]);
			long end = Long.parseLong(args[2]);

			int step = (start <= end ? 1 : -1);
			end += step;
			for (long i = start; i != end; i += step) {
				try {
					indexArticle(i);
				} catch (Exception e) {
					try {
						logger.error("Perform a forced garbage collection and sleep for a while, then retry!");
						logger.error("Error in updating doc [" + Long.toString(i)
								+ "] to Solr: "
								+ BasicUtils.strExceptionStackTrace(e));
						indexArticle(i);
					} catch (Exception e1) {
						logger.error("Cant' sleep: " + BasicUtils.strExceptionStackTrace(e));
					}

				}
			}

			return;
		} else if (command_str.equals("indexToSolrByDate")) {
			batchIndexToSolrByDate(args);
			return;
		}

		System.out
				.println("Command string not found. Please use one of these: crawl, crawlByDate, importToDB, importToDBByDate, annotateToDB, annotateToDBByDate, indexToSolr, indexToSolrByDate.");
	}
}
