import edu.mcw.rgd.common.utils.FileList;
import edu.mcw.rgd.nlp.utils.Library;
import edu.mcw.rgd.nlp.utils.LibraryBase;
import edu.mcw.rgd.nlp.utils.ncbi.PubMedDocSet;
import edu.mcw.rgd.nlp.utils.ncbi.PubMedLibrary;
import edu.mcw.rgd.nlp.utils.ncbi.PubMedRetriever;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by wangt on 9/11/2016.
 */
public class NewPubMedLibrary extends LibraryBase implements Library{

    protected static int RETRY_LIMIT = 3;
    protected FileList fileList = new FileList();
    protected FileList failedList = new FileList();
    protected FileList emptyList = new FileList();

    protected static DateFormat FILE_NAME_DF = new SimpleDateFormat(
            "yyyy_MM_dd");
    protected static String DATE_FILE_DIR = "/date_id_maps/";

    protected static String[] DATE_TYPES = { "cdat", "mdat", "edat", "mhda" };

    public static final String HBASE_NAME = "pubmed";

    public String result;
    /*
    public static String solrServer = "http://morgan:45600/solr/";
    public static CommonsHttpSolrServer[] solrServers = null;
    public static Random solrServerIdGenerator = new Random();
    protected List<String> annSets;
    public static Job mrJob;
    public static String MR_ANN_SETS = "mapred.input.annotation.sets";
    protected static AnnieAnnotator mrAnnoator;
    public static ArticleDAO mrArticleDao = new ArticleDAO();

 */
    public NewPubMedLibrary(){
        // did not find setPathDoc usage in the original PubMedLibrary


    }
    /*


    protected static void initSolrServers() {
        solrServers = new CommonsHttpSolrServer[20];

        try {
            byte e = 0;
            solrServers[e] = new CommonsHttpSolrServer("http://morgan:9292/solr/");
            int var2 = e + 1;
            solrServers[var2] = new CommonsHttpSolrServer("http://morgan:9293/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://morgan:9294/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://morgan:9295/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://morgan:9296/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://fox:9292/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://fox:9293/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://fox:9294/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://fox:9295/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://fox:9296/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://morgan:9297/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://fox:9297/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://morgan:9298/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://fox:9298/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://morgan:9299/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://fox:9299/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://morgan:9300/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://fox:9300/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://morgan:9301/solr/");
            ++var2;
            solrServers[var2] = new CommonsHttpSolrServer("http://fox:9301/solr/");
            ++var2;
        } catch (Exception var1) {
            ;
        }
    }

    public void setAnnotationSets(List<String> annSets) {
        this.annSets = annSets;
    }

    public void resetAnnotator(String gateHome, boolean useStemming) {
        mrAnnoator = getAnnotator(gateHome, false, useStemming);
    }
     */
    /**
     * download by ids
     * @param start_id
     * @param end_id
     * @param force_update
     * @return
     */
    public int batchDownload(long start_id, long end_id, boolean force_update) {


        logger.info("Start downloading from PMID " + start_id + " to PMID"
                + end_id);
        NewPubMedRetriever retriever = new NewPubMedRetriever();
        start_id = ((start_id - 1) / NewPubMedRetriever.ID_TRUNK_SIZE)
                * NewPubMedRetriever.ID_TRUNK_SIZE + 1;
        while (start_id <= end_id) {
            try {
                String file_name = getFileNameByID(start_id);
                //System.out.println(file_name);
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
                    logger.info("current path" + file_path);
                    fileList.save();
                    failedList.addFile(file_name);
                    failedList.save();
                    String result = retriever.crawlByIDRange(start_id);
                    if (result != null) {
                        failedList.removeFile(file_name);
                        failedList.save();
                        if (result.length() > 0) {
                            PubMedDocSet docSet = new PubMedDocSet();
                            docSet.setFileName(file_name);
                            docSet.setDocSetXML(result);
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
            start_id += NewPubMedRetriever.ID_TRUNK_SIZE;
        }
        logger.info("Finished downloading from PMID " + start_id + " to PMID "
                + end_id);
        return 0;
    }

    /**
     * download by DAte
     * @param start_date
     * @param end_date
     * @param force_update
     * @return
     */
    public int batchDownload(Date start_date, Date end_date, boolean force_update) {
        String start_date_str = NewPubMedRetriever.dateFormat.format(start_date);
        String end_date_str = NewPubMedRetriever.dateFormat.format(end_date);
        logger.info("Start downloading from " + start_date_str + " to " + end_date_str);
        NewPubMedRetriever retriever = new NewPubMedRetriever();
        retriever.initialize();
        boolean forward_crawling = start_date.before(end_date);

        try {
            for(; forward_crawling && !start_date.after(end_date) || !forward_crawling && !start_date.before(end_date); start_date = new Date(start_date.getTime() + (long)((forward_crawling?1:-1) * 24 * 3600 * 1000))) {
                String e = NewPubMedRetriever.dateFormat.format(start_date);
                if(force_update) {
                    this.fileList.removeFile(e);
                    this.fileList.save();
                    this.failedList.removeFile(e);
                    this.failedList.save();
                    this.emptyList.removeFile(e);
                    this.emptyList.save();
                }

                if(this.fileList.findFile(e, 0) < 0) {
                    logger.info("Crawling " + e);
                    this.fileList.removeFile(e);
                    this.fileList.save();
                    this.failedList.addFile(e);
                    this.failedList.save();
                    String[] ids = new String[0];
                    HashSet idSet = new HashSet();
                    String[] var14 = DATE_TYPES;
                    int var13 = DATE_TYPES.length;

                    for(int var12 = 0; var12 < var13; ++var12) {
                        String dateType = var14[var12];

                        try {
                            ids = retriever.getIdSetByDate(e, dateType);
                        } catch (Exception var19) {
                            logger.info("Error in getting IDs for " + e + "[" + dateType + "]");
                            var19.printStackTrace();
                            ids = (String[])null;
                        }

                        if(ids != null) {
                            String[] var18 = ids;
                            int var17 = ids.length;

                            for(int var16 = 0; var16 < var17; ++var16) {
                                String id_tmp = var18[var16];
                                idSet.add(id_tmp);
                            }
                        }
                    }

                    this.saveDateIdMap(start_date, idSet);
                    this.crawlFilesByDate(start_date, idSet);
                    this.failedList.removeFile(e);
                    this.failedList.save();
                    this.fileList.addFile(e);
                    this.fileList.save();
                } else {
                    logger.info("Date already crawled " + e);
                }
            }
        } catch (Exception var20) {
            logger.error("Error crawling dates", var20);
        }

        logger.info("Finished downloading from " + start_date_str + " to " + end_date_str);
        return 0;
    }
/*

    public int batchImportToDB(long start_id, long end_id) {
        logger.info("Start importing from PMID " + start_id + " to PMID" + end_id);
        long start_set = start_id / 1000L;
        long end_set = end_id / 1000L;
        if(start_set * 1000L != start_id) {
            --start_set;
        }

        ++start_set;
        ++end_set;

        for(long id_set_no = start_set; id_set_no <= end_set; ++id_set_no) {
            String file_name = this.getFileNameByID(id_set_no * 1000L);
            if(this.fileList.findFile(file_name, 0) >= 0) {
                PubMedDocSet pmds = new PubMedDocSet();
                pmds.setFileName(file_name);
                String file_path = this.getFilePathByID(id_set_no * 1000L);

                try {
                    pmds.loadDoc(file_path);
                    pmds.parseDocSet();
                    pmds.importToDB();
                } catch (Exception var15) {
                    var15.printStackTrace();
                }
            }
        }

        return 0;
    }

    public int batchImportToDBByDate(Date start_date, Date end_date) {
        logger.info("Start importing from " + PubMedRetriever.dateFormat.format(start_date) + " to " + PubMedRetriever.dateFormat.format(end_date));
        boolean forward_process = start_date.before(end_date);

        try {
            while(forward_process && !start_date.after(end_date) || !forward_process && !start_date.before(end_date)) {
                String e = this.getFilePathByDate(start_date);
                String file_name = this.getFileNameByDate(start_date);
                String file_name_in_list = PubMedRetriever.dateFormat.format(start_date);
                if(this.fileList.findFile(file_name_in_list, 0) >= 0) {
                    logger.info("Importing " + PubMedRetriever.dateFormat.format(start_date));
                    int chunk_no = 0;
                    String chunk_file_name = file_name + "_" + String.format("%03d", new Object[]{Integer.valueOf(chunk_no)});

                    for(File chunk_file = new File(e + "/" + chunk_file_name + ".xml"); chunk_file.exists(); chunk_file = new File(e + "/" + chunk_file_name + ".xml")) {
                        logger.info("Importing " + chunk_file_name);
                        PubMedDocSet pmds = new PubMedDocSet();
                        pmds.setFileName(chunk_file_name);

                        try {
                            pmds.loadDoc(e);
                            pmds.parseDocSet();
                            pmds.importToDB();
                        } catch (Exception var12) {
                            var12.printStackTrace();
                        }

                        ++chunk_no;
                        chunk_file_name = file_name + "_" + String.format("%03d", new Object[]{Integer.valueOf(chunk_no)});
                    }
                }

                logger.info("Finished importing " + PubMedRetriever.dateFormat.format(start_date));
                start_date = new Date(start_date.getTime() + (long)((forward_process?1:-1) * 24 * 3600 * 1000));
            }
        } catch (Exception var13) {
            var13.printStackTrace();
        }

        return 0;
    }

    public int batchAnnotateToDBByDate(String annotatorPath, List<String> annotationSets, Date startDate, Date endDate) {
        logger.info("Start annotating from " + PubMedRetriever.dateFormat.format(startDate) + " to " + PubMedRetriever.dateFormat.format(endDate) + " for annotator [" + annotatorPath + "] in [" + annotationSets.toString() + "]");
        AnnieAnnotator annotator = getAnnotator(annotatorPath, true);
        if(annotator == null) {
            logger.info("Failed annotating " + PubMedRetriever.dateFormat.format(startDate) + " Can\'t get annotator!");
        }

        ArticleDAO dao = new ArticleDAO();
        boolean forward_process = startDate.before(endDate);

        try {
            while(forward_process && !startDate.after(endDate) || !forward_process && !startDate.before(endDate)) {
                String e = this.getDateFilePath(startDate);
                FileList pmid_list = new FileList();
                pmid_list.setFilePath(e, false, true);
                logger.info("Start annotating " + PubMedRetriever.dateFormat.format(startDate));
                Iterator var11 = pmid_list.fileList.iterator();

                while(var11.hasNext()) {
                    FileEntry pm_article = (FileEntry)var11.next();
                    this.annotateToDB(annotator, annotationSets, pm_article.getFileName(), dao);
                }

                logger.info("Finished annotating " + PubMedRetriever.dateFormat.format(startDate));
                startDate = new Date(startDate.getTime() + (long)((forward_process?1:-1) * 24 * 3600 * 1000));
            }
        } catch (Exception var12) {
            var12.printStackTrace();
        }

        return 0;
    }

    public int batchIndexToSolrByDate(Date startDate, Date endDate) {
        logger.info("Start indexing to Solr from " + PubMedRetriever.dateFormat.format(startDate) + " to " + PubMedRetriever.dateFormat.format(endDate));
        boolean forward_process = startDate.before(endDate);

        try {
            while(forward_process && !startDate.after(endDate) || !forward_process && !startDate.before(endDate)) {
                String e = this.getDateFilePath(startDate);
                FileList pmid_list = new FileList();
                pmid_list.setFilePath(e, false, true);
                logger.info("Start indexing " + PubMedRetriever.dateFormat.format(startDate));
                Iterator var7 = pmid_list.fileList.iterator();

                while(var7.hasNext()) {
                    FileEntry pm_article = (FileEntry)var7.next();

                    try {
                        indexArticle(Long.parseLong(pm_article.getFileName()));
                    } catch (Exception var11) {
                        Exception e1 = var11;

                        try {
                            logger.error("Perform a forced garbage collection and sleep for a while, then retry!");
                            logger.error("Error in updating doc [" + pm_article.getFileName() + "] to Solr: " + BasicUtils.strExceptionStackTrace(e1));
                            indexArticle(Long.parseLong(pm_article.getFileName()));
                        } catch (Exception var10) {
                            logger.error("Can\'t sleep " + BasicUtils.strExceptionStackTrace(var11));
                        }
                    }
                }

                logger.info("Finished indexing " + PubMedRetriever.dateFormat.format(startDate));
                startDate = new Date(startDate.getTime() + (long)((forward_process?1:-1) * 24 * 3600 * 1000));
            }
        } catch (Exception var12) {
            var12.printStackTrace();
        }

        return 0;
    }
 */
    public void saveDateIdMap(Date date, HashSet<String> Ids) throws Exception {
        try {
            BufferedWriter e = new BufferedWriter(new FileWriter(this.getDateFilePath(date)));
            if(Ids != null) {
                e.write(String.format("%d", new Object[]{Integer.valueOf(Ids.size())}));
                e.newLine();
                Iterator var5 = Ids.iterator();

                while(var5.hasNext()) {
                    String id = (String)var5.next();
                    e.write(id.toString());
                    e.newLine();
                }
            } else {
                e.write("0");
                e.newLine();
            }

            e.close();
        } catch (Exception var6) {
            logger.error("Error saving date id map", var6);
            throw var6;
        }
    }

    public String getDateFilePath(Date date) throws Exception {
        File file = new File(this.getDocPath() + DATE_FILE_DIR);

        try {
            if(!file.exists()) {
                file.mkdir();
            }
        } catch (Exception var4) {
            logger.error("Error getting date file directory", var4);
            throw var4;
        }

        return this.getDocPath() + DATE_FILE_DIR + FILE_NAME_DF.format(date) + ".txt";
    }
    /**
     * using retriever to get the file name by id
     * @param id
     * @return
     */
    public String getFileNameByID(long id) {
        long seg_id = (id - 1) / NewPubMedRetriever.ID_TRUNK_SIZE;
        return String.format("%08d", seg_id);
    }

    /**
     *
     * @param id
     * @return
     */
    public String getFilePathByID(long id) {
        long seg_id = (id - 1) / NewPubMedRetriever.ID_TRUNK_SIZE
                / NewPubMedRetriever.ID_TRUNK_SIZE;
        String path = getDocPath() + "/" + String.format("%04d", seg_id);
        File file = new File(path);
        if (!file.exists())
            file.mkdir();
        return path;
    }


    private void crawlDateChunk(String file_name, String file_path, String ids)
            throws Exception {
        NewPubMedRetriever retriever = new NewPubMedRetriever();
        result = retriever.crawlByIdList(ids);
        if (result != null) {
            if (result.length() > 0) {
                PubMedDocSet docSet = new PubMedDocSet();
                docSet.setFileName(file_name);
                docSet.setDocSetXML(result);
                docSet.saveDoc(file_path);
            } else {
                emptyList.addFile(file_name);
                emptyList.save();
            }
        }
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
                if (cur_id_count == NewPubMedRetriever.ID_TRUNK_SIZE) {
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
    public void setPathDoc(String pathDoc) {
        super.setPathDoc(pathDoc);

        try {
            this.fileList.setFilePath(this.getPathDoc() + "/file_list.txt");
            this.failedList.setFilePath(this.getPathDoc() + "/file_list_failed.txt");
            this.emptyList.setFilePath(this.getPathDoc() + "/file_list_empty.txt");
        } catch (Exception var3) {
            logger.error("Error in setting PathDoc", var3);
        }

    }
/*
    public void tryFailed() {
        ArrayList list_copy = this.failedList.cloneList();

        try {
            for(int e = 0; e < list_copy.size(); ++e) {
                long start_id = this.getIDByFileName(((FileEntry)list_copy.get(e)).getFileName());
                this.batchDownload(start_id, start_id, true);
            }
        } catch (Exception var5) {
            logger.error("Error in trying failed list", var5);
        }

    }

    public void tryFailedDates() {
        ArrayList list_copy = this.failedList.cloneList();

        try {
            for(int e = 0; e < list_copy.size(); ++e) {
                Date start_date = PubMedRetriever.dateFormat.parse(((FileEntry)list_copy.get(e)).getFileName());
                Date end_date = new Date(start_date.getTime() + 82800000L + 3540000L + 59000L);
                this.batchDownload(start_date, end_date, true);
            }
        } catch (Exception var5) {
            logger.error("Error in trying failed list", var5);
        }

    }



 */

    public static void crawl(String[] args) {
        String path = args[1];
        String start = args[2];
        String end = args[3];
        PropertyConfigurator.configure(path + "/conf/crawler.cnf");
        if(end == null || end.length() == 0) {
            end = start;
        }

        logger.info("Start crawling: [" + path + "] PMID [" + start + "][" + end + "]");
        PubMedLibrary library = new PubMedLibrary();
        library.setPathDoc(path);

        try {
            library.tryFailed();
            long e = Long.parseLong(start);
            long end_id = Long.parseLong(end);
            library.batchDownload(e, end_id, false);
        } catch (Exception var9) {
            logger.error("Error", var9);
        }

        logger.info("Crawling finished [" + path + "] PMID [" + start + "][" + end + "]");
    }
    public static void crawlByDate(String[] args) {
        String path = args[1];
        Date startDate = new Date();
        Date endDate = new Date();
        getDates(args, 2, startDate, endDate);
        PropertyConfigurator.configure(path + "/conf/crawler.cnf");
        logger.info("Start crawling: [" + path + "] from [" + startDate + "] to [" + endDate + "]");
        PubMedLibrary library = new PubMedLibrary();
        library.setPathDoc(path);

        try {
            library.tryFailedDates();
            library.batchDownload(startDate, endDate, true);
        } catch (Exception var6) {
            logger.error("Error", var6);
        }

        logger.info("Crawling finished [" + path + "] from [" + startDate + "] to [" + endDate + "]");
    }
/*

    public void annotateToDB(String annotator_path, List<String> annotation_sets, long start_id, long end_id) {
        this.annotateToDB(getAnnotator(annotator_path, true), annotation_sets, start_id, end_id);
    }

    public static AnnieAnnotator getAnnotator(String annotator_path, boolean local) {
        return getAnnotator(annotator_path, local, false);
    }

    public static AnnieAnnotator getAnnotator(String annotator_path, boolean local, boolean useStemming) {
        AnnieAnnotator annotator = new AnnieAnnotator();

        try {
            if(local) {
                annotator.init(annotator_path);
            } else {
                annotator.initOnHDFS(annotator_path);
            }

            annotator.setUseStemming(useStemming);
            return annotator;
        } catch (Exception var5) {
            logger.error("Error in getting annotator", var5);
            return null;
        }
    }

    public void annotateToDB(AnnieAnnotator annotator, List<String> annotation_sets, long start_id, long end_id) {
        if(annotator != null && annotation_sets != null) {
            try {
                ArticleDAO e = new ArticleDAO();
                int step = start_id < end_id?1:-1;
                long dest_id = end_id + (long)step;

                for(long cur_id = start_id; cur_id != dest_id; cur_id += (long)step) {
                    this.annotateToDB(annotator, annotation_sets, Long.toString(cur_id), e);
                }
            } catch (Exception var13) {
                logger.error("Error", var13);
            }

        }
    }

    public void annotateToDB(AnnieAnnotator annotator, List<String> annotation_sets, String pmid, ArticleDAO dao) {
        logger.info("Annotating: [" + pmid + "]");

        try {
            if(AnnotationDAO.tableName.equals(Queries.annotationTableName)) {
                Long e = Long.valueOf(Long.parseLong(pmid));
                String queryTerm = Queries.getQueryTerm(e);
                if(queryTerm != null && queryTerm.length() > 0) {
                    this.annotateToDB(annotator, e.longValue(), queryTerm, 0, annotation_sets, annotator.isUseStemming());
                }

                return;
            }

            if(dao.getArticleFromCouch(pmid)) {
                if(dao.articleTitle != null && dao.articleTitle.length() > 0) {
                    this.annotateToDB(annotator, dao.pmid.longValue(), dao.articleTitle, 0, annotation_sets, annotator.isUseStemming());
                }

                if(dao.articleAbstract != null && dao.articleAbstract.length() > 0) {
                    this.annotateToDB(annotator, dao.pmid.longValue(), dao.articleAbstract, 1, annotation_sets, annotator.isUseStemming());
                }

                if(dao.meshTerms != null && dao.meshTerms.length() > 0) {
                    this.annotateToDB(annotator, dao.pmid.longValue(), dao.meshTerms, 2, annotation_sets, annotator.isUseStemming());
                }
            }
        } catch (Exception var7) {
            logger.error("Error", var7);
        }

    }

    public List<String> annotateToFile(AnnieAnnotator annotator, List<String> annotation_sets, String pmid, ArticleDAO dao) {
        logger.info("Annotating: [" + pmid + "]");
        ArrayList annResult = new ArrayList();

        try {
            if(dao.getArticleFromCouch(pmid)) {
                if(dao.articleTitle != null && dao.articleTitle.length() > 0) {
                    annResult.addAll(this.annotateToString(annotator, dao.pmid.longValue(), dao.articleTitle, 0, annotation_sets, annotator.isUseStemming()));
                }

                if(dao.articleAbstract != null && dao.articleAbstract.length() > 0) {
                    annResult.addAll(this.annotateToString(annotator, dao.pmid.longValue(), dao.articleAbstract, 1, annotation_sets, annotator.isUseStemming()));
                }

                if(dao.meshTerms != null && dao.meshTerms.length() > 0) {
                    annResult.addAll(this.annotateToString(annotator, dao.pmid.longValue(), dao.meshTerms, 2, annotation_sets, annotator.isUseStemming()));
                }
            }

            return annResult;
        } catch (Exception var7) {
            logger.error("Error", var7);
            return annResult;
        }
    }

    public List<String> annotateFromHResult(AnnieAnnotator annotator, List<String> annotation_sets, Result result, ArticleDAO dao) {
        ArrayList annResult = new ArrayList();

        try {
            if(dao.getArticleFromHResult(result)) {
                if(dao.articleTitle != null && dao.articleTitle.length() > 0) {
                    annResult.addAll(this.annotateToString(annotator, dao.pmid.longValue(), dao.articleTitle, 0, annotation_sets, annotator.isUseStemming()));
                }

                if(dao.articleAbstract != null && dao.articleAbstract.length() > 0) {
                    annResult.addAll(this.annotateToString(annotator, dao.pmid.longValue(), dao.articleAbstract, 1, annotation_sets, annotator.isUseStemming()));
                }

                if(dao.meshTerms != null && dao.meshTerms.length() > 0) {
                    annResult.addAll(this.annotateToString(annotator, dao.pmid.longValue(), dao.meshTerms, 2, annotation_sets, annotator.isUseStemming()));
                }
            }

            return annResult;
        } catch (Exception var7) {
            logger.error("Error in [" + dao.pmid + "]", var7);
            return annResult;
        }
    }

    public List<String> annotateToOutput(PubMedLibrary.OutputType output, AnnieAnnotator annotator, long pmid, String text_to_annotate, int text_location, List<String> annotation_sets, boolean useStemming) {
        Document doc;
        if(useStemming) {
            doc = annotator.process(Stemmer.stem(text_to_annotate));
        } else {
            doc = annotator.process(text_to_annotate);
        }

        ArrayList outputStr = new ArrayList();
        if(doc != null) {
            try {
                Iterator var12 = annotation_sets.iterator();

                while(var12.hasNext()) {
                    String e = (String)var12.next();
                    AnnotationSet anns = doc.getAnnotations(e);
                    Iterator var15 = anns.iterator();

                    while(var15.hasNext()) {
                        Annotation ann = (Annotation)var15.next();
                        switch($SWITCH_TABLE$edu$mcw$rgd$nlp$utils$ncbi$PubMedLibrary$OutputType()[output.ordinal()]) {
                        case 1:
                            AnnotationDAO.insertRecord(Long.toString(pmid), text_location, e, ann);
                        case 2:
                            outputStr.add(pmid + "\t" + text_location + "\t" + ann.getType() + "\t" + e + "\t" + ann.getStartNode().getOffset() + "\t" + ann.getEndNode().getOffset() + "\t" + ann.getFeatures().toString());
                        case 3:
                            outputStr.add(text_location + "\t" + ann.getType() + "\t" + e + "\t" + ann.getStartNode().getOffset() + "\t" + ann.getEndNode().getOffset() + "\t" + ann.getFeatures().toString());
                        }
                    }
                }
            } catch (Exception var16) {
                logger.error("Error in annotating: [" + text_to_annotate + "]");
                logger.error(var16);
            }
        }

        annotator.clear();
        return outputStr;
    }

    public void annotateToDB(AnnieAnnotator annotator, long pmid, String text_to_annotate, int text_location, List<String> annotation_sets, boolean useStemming) {
        this.annotateToOutput(PubMedLibrary.OutputType.TO_DATABASE, annotator, pmid, text_to_annotate, text_location, annotation_sets, useStemming);
    }

    public List<String> annotateToString(AnnieAnnotator annotator, long pmid, String text_to_annotate, int text_location, List<String> annotation_sets, boolean useStemming) {
        return this.annotateToOutput(PubMedLibrary.OutputType.TO_HBASE, annotator, pmid, text_to_annotate, text_location, annotation_sets, useStemming);
    }

    public static void importToDB(String[] args) {
        String path = args[1];
        String start = args[2];
        String end = args[3];
        PropertyConfigurator.configure(path + "/conf/crawler.cnf");
        if(end == null || end.length() == 0) {
            end = start;
        }

        logger.info("Annotating to DB: [" + path + "] PMID [" + start + "][" + end + "]");
        System.out.println("Importing to DB: [" + path + "] PMID [" + start + "][" + end + "]");
        PubMedLibrary library = new PubMedLibrary();
        library.setPathDoc(path);

        try {
            long e = Long.parseLong(start);
            long end_id = Long.parseLong(end);
            library.batchImportToDB(e, end_id);
        } catch (Exception var9) {
            logger.error("Error", var9);
        }

        logger.info("Importing finished [" + path + "] PMID [" + start + "][" + end + "]");
        System.out.println("Importing finished [" + path + "] PMID [" + start + "][" + end + "]");
    }

 */
private static void getDates(String[] args, int startPos, Date startDateIn, Date endDateIn) {
    Date startDate;
    Date endDate;
    try {
        startDate = PubMedRetriever.dateFormat.parse(args[startPos]);
        if(args[startPos + 1] != null && args[startPos + 1].length() > 0) {
            try {
                endDate = PubMedRetriever.dateFormat.parse(args[startPos + 1]);
            } catch (Exception var7) {
                System.out.println("Can\'t get End date!");
                return;
            }
        } else {
            endDate = startDate;
        }
    } catch (Exception var8) {
        System.out.println("Can\'t get Start date!");
        return;
    }

    if(endDate.after(endDateIn)) {
        endDate.setTime(endDateIn.getTime());
    } else if(!startDate.after(endDate)) {
        endDate = new Date(endDate.getTime() + 82800000L + 3540000L + 59000L);
    }

    startDateIn.setTime(startDate.getTime());
    endDateIn.setTime(endDate.getTime());
}

/*
    public static void importToDBByDate(String[] args) {
        String path = args[1];
        PropertyConfigurator.configure(path + "/conf/crawler.cnf");
        Date startDate = new Date();
        Date endDate = new Date();
        getDates(args, 2, startDate, endDate);
        logger.info("Importing to DB: [" + path + "] Date [" + PubMedRetriever.dateFormat.format(startDate) + "][" + PubMedRetriever.dateFormat.format(endDate) + "]");
        System.out.println("Importing to DB: [" + path + "] Date [" + PubMedRetriever.dateFormat.format(startDate) + "][" + PubMedRetriever.dateFormat.format(endDate) + "]");
        PubMedLibrary library = new PubMedLibrary();
        library.setPathDoc(path);

        try {
            library.batchImportToDBByDate(startDate, endDate);
        } catch (Exception var6) {
            logger.error("Error", var6);
        }

        logger.info("Importing finished [" + path + "] Date [" + PubMedRetriever.dateFormat.format(startDate) + "][" + PubMedRetriever.dateFormat.format(endDate) + "]");
        System.out.println("Importing finished [" + path + "] Date [" + PubMedRetriever.dateFormat.format(startDate) + "][" + PubMedRetriever.dateFormat.format(endDate) + "]");
    }

    public static void batchAnnotateToDB(String[] args) {
        if(args.length < 6) {
            System.out.println("Input: annotateToDB [path to annotator] [DB table name] [start pmid] [end pmid] [annotation set 1] [annotation set 2] ...");
        } else {
            PubMedLibrary library = new PubMedLibrary();
            ArrayList annotation_sets = new ArrayList();

            for(int start_id = 5; start_id < args.length; ++start_id) {
                annotation_sets.add(args[start_id]);
            }

            long var7 = Long.parseLong(args[3]);
            long end_id = Long.parseLong(args[4]);
            AnnotationDAO.tableName = args[2];
            library.annotateToDB((String)args[1], annotation_sets, var7, end_id);
        }
    }

    public static void batchAnnotateToDBByDate(String[] args) {
        if(args.length < 7) {
            System.out.println("Input: annotateToDBByDate [pubmed data path] [path to annotator] [DB table name] [start date] [end date] [annotation set 1] [annotation set 2] ...");
        } else {
            String path = args[1];
            PropertyConfigurator.configure(path + "/conf/annotator.cnf");
            Date startDate = new Date();
            Date endDate = new Date();
            getDates(args, 4, startDate, endDate);
            PubMedLibrary library = new PubMedLibrary();
            library.setPathDoc(path);
            ArrayList annotation_sets = new ArrayList();

            for(int i = 6; i < args.length; ++i) {
                annotation_sets.add(args[i]);
            }

            AnnotationDAO.tableName = args[3];
            library.batchAnnotateToDBByDate(args[2], annotation_sets, startDate, endDate);
        }
    }

    public List<String> mrAnnotateToFile(String pmid) {
        if(mrAnnoator == null) {
            System.setProperty("gate.home", "/shared/users/wliu/Tools/gate-6.1-build3913-ALL/");
            System.setProperty("gate.plugins.home", "/shared/users/wliu/Tools/gate-6.1-build3913-ALL/plugins/");
            System.setProperty("gate.site.config", "/shared/users/wliu/Tools/gate-6.1-build3913-ALL/gate.xml");
            mrAnnoator = getAnnotator("/user/wliu/results/test4 /shared/users/wliu/Work/Gate/Other_ontologies.xgapp", true);
        }

        return this.annotateToFile(mrAnnoator, this.annSets, pmid, new ArticleDAO());
    }

    public List<String> mrAnnotateHResult(Result result, String gateHome, boolean useStemming) {
        if(mrAnnoator == null) {
            mrAnnoator = getAnnotator(gateHome, false, useStemming);
        }

        return this.annotateFromHResult(mrAnnoator, this.annSets, result, mrArticleDao);
    }

    public static void mrAnnotateToFile(Job job, String[] args) {
        if(args.length < 4) {
            System.out.println("Input: annotateToDBByDate [path to annotator] [output folder] [annotation set 1] [annotation set 2] ...");
        } else {
            String annotation_sets = "";

            for(int i = 3; i < args.length; ++i) {
                annotation_sets = annotation_sets + args[i] + "|";
            }

            mrJob = job;
            job.getConfiguration().setStrings(MR_ANN_SETS, new String[]{annotation_sets});
            System.out.println("gateHome: " + System.getProperty("gate.home"));
            mrAnnoator = getAnnotator(args[2], true);
        }
    }

    public void initMrEnv() {
        if(this.annSets == null) {
            String[] annSetEnv = new String[]{"Ontologies"};
            String[] ann_sets = annSetEnv[0].split("|");
            this.annSets = new ArrayList();

            for(int i = 0; i < ann_sets.length; ++i) {
                if(ann_sets[i] != null && ann_sets[i].length() > 0) {
                    this.annSets.add(ann_sets[i]);
                }
            }
        }

    }

    public static void batchIndexToSolrByDate(String[] args) {
        if(args.length < 4) {
            System.out.println("Input: indexToSolrByDate [pubmed data path] [start date] [end date]");
        } else {
            String path = args[1];
            PropertyConfigurator.configure(path + "/conf/indexer.cnf");
            Date startDate = new Date();
            Date endDate = new Date();
            getDates(args, 2, startDate, endDate);
            PubMedLibrary library = new PubMedLibrary();
            library.setPathDoc(path);
            library.batchIndexToSolrByDate(startDate, endDate);
        }
    }

    private static void addMaptoDoc(SolrInputDocument solr_doc, SortedCountMap map, String key_field, String value_field, String count_field, String pos_field) {
        List sorted_counts = map.getSortedCounts();
        List sorted_keys = map.getSortedKeys();
        HashMap unsorted_map = map.getUnsortedMap();
        HashMap unsorted_pos = map.get_unsortedPositions();
        Iterator count_it = sorted_counts.iterator();
        Iterator var12 = sorted_keys.iterator();

        while(var12.hasNext()) {
            Object key = var12.next();
            Long count = (Long)count_it.next();
            if(key_field != null) {
                solr_doc.addField(key_field, (String)key);
            }

            if(value_field != null) {
                solr_doc.addField(value_field, (String)unsorted_map.get(key));
            }

            if(count_field != null) {
                solr_doc.addField(count_field, StringEscapeUtils.escapeXml(count.toString()));
            }

            if(pos_field != null) {
                solr_doc.addField(pos_field, StringEscapeUtils.escapeXml((String)unsorted_pos.get(key)));
            }
        }

    }

    private static void addMaptoDoc(SolrInputDocument solr_doc, SortedCountMap map, String key_field, String value_field, String count_field) {
        addMaptoDoc(solr_doc, map, key_field, value_field, count_field, (String)null);
    }

    public static Boolean indexArticle(Result result) throws Exception {
        ArticleDAO art = new ArticleDAO();
        Boolean indexable = Boolean.valueOf(true);
        if(!art.getArticleFromHResult(result)) {
            return Boolean.valueOf(true);
        } else {
            List annotations = AnnotationDAO.getAnnotationsFromResult(result);
            String pmidStr = Long.toString(art.pmid.longValue());
            PubMedSolrDoc solr_doc = new PubMedSolrDoc();
            solr_doc.addField("pmid", pmidStr);
            solr_doc.addField("title", art.articleTitle);
            solr_doc.addField("abstract", art.articleAbstract);
            solr_doc.addField("p_date", art.articlePubDate);
            solr_doc.addField("j_date_s", art.articleJournalDate);
            solr_doc.addField("authors", art.articleAuthors);
            solr_doc.addField("citation", art.articleCitation);
            solr_doc.addField("mesh_terms", art.meshTerms);
            solr_doc.addField("affiliation", art.affiliation);
            solr_doc.addField("p_year", art.publicationYear);
            solr_doc.addField("issn", art.issn);
            if(art.pmcId != null) {
                solr_doc.addField("pmc_id_s", art.pmcId);
            }

            if(art.doi != null) {
                solr_doc.addField("doi_s", art.doi);
            }

            if(art.publicationTypes != null) {
                String[] onto_maps = art.publicationTypes;
                int organism_map = art.publicationTypes.length;

                for(int rgd_gene_map = 0; rgd_gene_map < organism_map; ++rgd_gene_map) {
                    String gene_map = onto_maps[rgd_gene_map];
                    solr_doc.addField("p_type", gene_map);
                }
            }

            SortedCountMap var20 = new SortedCountMap();
            SortedCountMap var21 = new SortedCountMap();
            SortedCountMap var22 = new SortedCountMap();
            HashMap var23 = new HashMap();
            Iterator server = Ontology.getRgdOntologies().iterator();

            String e;
            while(server.hasNext()) {
                e = (String)server.next();
                var23.put(e, new SortedCountMap());
            }

            server = annotations.iterator();

            while(server.hasNext()) {
                AnnotationRecord var24 = (AnnotationRecord)server.next();
                String req = "";
                switch(var24.text_location) {
                case 0:
                    req = art.articleTitle;
                    break;
                case 1:
                    req = art.articleAbstract;
                    break;
                case 2:
                    req = art.meshTerms;
                }

                String ann_pos = String.format("%d;%d-%d", new Object[]{Integer.valueOf(var24.text_location), Integer.valueOf(var24.text_start), Integer.valueOf(var24.text_end)});
                String rsp;
                if(!var24.annotation_set.equals("OrganismTagger")) {
                    try {
                        rsp = req.substring(var24.text_start, var24.text_end);
                    } catch (Exception var19) {
                        System.err.println("Error getting text: [" + pmidStr + ":" + var24.annotation_set + "] " + var24.text_location + ":" + var24.text_start + ", " + var24.text_end + " from [" + req + "]");
                        rsp = "";
                        indexable = Boolean.valueOf(false);
                        break;
                    }
                } else {
                    rsp = "";
                }

                if(var24.annotation_set.equals("GENES") && !var24.features_table.get("type").equals("CellLine") && !var24.features_table.get("type").equals("CellType") && !var24.features_table.get("type").equals("RNA")) {
                    var20.add(rsp, "", ann_pos);
                } else if(var24.annotation_set.equals("RGDGENE")) {
                    var21.add(var24.features_table.get("RGD_ID"), rsp, ann_pos);
                } else {
                    String snpStr;
                    if(var24.annotation_set.equals("OrganismTagger")) {
                        snpStr = (String)var24.features_table.get("ncbiId");
                        var22.add(snpStr, ArticleOrganismClassifier.getNameByID(Long.parseLong(snpStr)), ann_pos);
                    } else if(var24.annotation_set.equals("Ontologies")) {
                        snpStr = (String)var24.features_table.get("minorType");
                        ((SortedCountMap)var23.get(snpStr)).add(var24.features_table.get("ONTO_ID"), rsp, ann_pos);
                    } else if(var24.annotation_set.equals("Mutations")) {
                        ((SortedCountMap)var23.get("MT")).add((String)var24.features_table.get("wNm"), rsp, ann_pos);
                    } else if(var24.annotation_set.equals("SNP")) {
                        snpStr = (String)var24.features_table.get("string");

                        try {
                            ((SortedCountMap)var23.get("MT")).add(snpStr.replaceAll("\\s", ""), rsp, ann_pos);
                        } catch (Exception var18) {
                            System.err.println("Error in getting SNP annotations of " + pmidStr);
                            var18.printStackTrace();
                            throw var18;
                        }
                    }
                }
            }

            if(!indexable.booleanValue()) {
                return Boolean.valueOf(false);
            } else {
                var20.sort();
                addMaptoDoc(solr_doc, var20, "gene", (String)null, "gene_count", "gene_pos");
                var21.sort();
                addMaptoDoc(solr_doc, var21, "rgd_obj_id", "rgd_obj_term", "rgd_obj_count", "rgd_obj_pos");
                var22.sort();
                addMaptoDoc(solr_doc, var22, "organism_ncbi_id", "organism_term", "organism_count", "organism_pos");
                server = Ontology.getRgdOntologies().iterator();

                while(server.hasNext()) {
                    e = (String)server.next();
                    SortedCountMap var27 = (SortedCountMap)var23.get(e);
                    var27.sort(true);
                    SolrOntologyEntry var29 = (SolrOntologyEntry)Ontology.getSolrOntoFields().get(e);
                    addMaptoDoc(solr_doc, var27, var29.getIdFieldName(), var29.getTermFieldName(), var29.getCountFieldName(), var29.getPosFieldName());
                }

                try {
                    if(solrServers == null) {
                        initSolrServers();
                    }

                    int var25 = solrServerIdGenerator.nextInt(solrServers.length);
                    CommonsHttpSolrServer var26 = solrServers[var25];
                    UpdateRequest var28 = new UpdateRequest();
                    var28.add(solr_doc);
                    var28.process(var26);
                    return Boolean.valueOf(true);
                } catch (Exception var17) {
                    System.err.println("Error when indexing:" + pmidStr);
                    var17.printStackTrace();
                    return Boolean.valueOf(true);
                }
            }
        }
    }

    public static void indexArticle(long pmid) throws Exception {
        logger.info("[pmid:" + Long.toString(pmid) + "] Indexing to Solr...");
        ArticleDAO art = new ArticleDAO();
        if(art.getArticleFromCouch(Long.toString(pmid))) {
            List annotations = AnnotationDAO.getAllAnnotations(pmid);
            SolrInputDocument solr_doc = new SolrInputDocument();
            solr_doc.addField("pmid", Long.toString(pmid));
            solr_doc.addField("title", art.articleTitle);
            solr_doc.addField("abstract", art.articleAbstract);
            solr_doc.addField("p_date", art.articlePubDate);
            solr_doc.addField("j_date_s", art.articleJournalDate);
            solr_doc.addField("authors", art.articleAuthors);
            solr_doc.addField("citation", art.articleCitation);
            solr_doc.addField("mesh_terms", art.meshTerms);
            SortedCountMap gene_map = new SortedCountMap();
            SortedCountMap rgd_gene_map = new SortedCountMap();
            new SortedCountMap();
            new SortedCountMap();
            SortedCountMap organism_map = new SortedCountMap();
            HashMap onto_maps = new HashMap();
            Iterator req = Ontology.getRgdOntologies().iterator();

            String e;
            while(req.hasNext()) {
                e = (String)req.next();
                onto_maps.put(e, new SortedCountMap());
            }

            req = annotations.iterator();

            while(req.hasNext()) {
                AnnotationRecord e1 = (AnnotationRecord)req.next();
                String rsp = "";
                switch(e1.text_location) {
                case 0:
                    rsp = art.articleTitle;
                    break;
                case 1:
                    rsp = art.articleAbstract;
                    break;
                case 2:
                    rsp = art.meshTerms;
                }

                String ann_pos = String.format("%d;%d-%d", new Object[]{Integer.valueOf(e1.text_location), Integer.valueOf(e1.text_start), Integer.valueOf(e1.text_end)});
                String solr_entry;
                if(!e1.annotation_set.equals("OrganismTagger")) {
                    solr_entry = rsp.substring(e1.text_start, e1.text_end);
                } else {
                    solr_entry = "";
                }

                if(e1.annotation_set.equals("GENES")) {
                    gene_map.add(solr_entry, "", ann_pos);
                } else if(e1.annotation_set.equals("RGDGENE")) {
                    rgd_gene_map.add(e1.features_table.get("RGD_ID"), solr_entry, ann_pos);
                } else {
                    String onto_name;
                    if(e1.annotation_set.equals("OrganismTagger")) {
                        onto_name = (String)e1.features_table.get("ncbiId");
                        organism_map.add(onto_name, ArticleOrganismClassifier.getNameByID(Long.parseLong(onto_name)), ann_pos);
                    } else if(e1.annotation_set.equals("Ontologies")) {
                        onto_name = (String)e1.features_table.get("minorType");
                        ((SortedCountMap)onto_maps.get(onto_name)).add(e1.features_table.get("ONTO_ID"), solr_entry, ann_pos);
                    }
                }
            }

            gene_map.sort();
            addMaptoDoc(solr_doc, gene_map, "gene", (String)null, "gene_count", "gene_pos");
            rgd_gene_map.sort();
            addMaptoDoc(solr_doc, rgd_gene_map, "rgd_obj_id", "rgd_obj_term", "rgd_obj_count", "rgd_obj_pos");
            organism_map.sort();
            addMaptoDoc(solr_doc, organism_map, "organism_ncbi_id", "organism_term", "organism_count", "organism_pos");
            req = Ontology.getRgdOntologies().iterator();

            while(req.hasNext()) {
                e = (String)req.next();
                SortedCountMap rsp1 = (SortedCountMap)onto_maps.get(e);
                rsp1.sort();
                SolrOntologyEntry solr_entry1 = (SolrOntologyEntry)Ontology.getSolrOntoFields().get(e);
                addMaptoDoc(solr_doc, rsp1, solr_entry1.getIdFieldName(), solr_entry1.getTermFieldName(), solr_entry1.getCountFieldName(), solr_entry1.getPosFieldName());
            }

            try {
                HttpSolrServer e2 = new HttpSolrServer(solrServer);
                logger.info("[pmid:" + Long.toString(art.pmid.longValue()) + "] Solr doc string: " + solr_doc.toString());
                UpdateRequest req1 = new UpdateRequest();
                req1.setAction(ACTION.COMMIT, false, false);
                req1.add(solr_doc);
                req1.process(e2);
                req1.clear();
                solr_doc.clear();
                logger.info("[pmid:" + Long.toString(art.pmid.longValue()) + "] finished indexing to Solr...");
            } catch (Exception var17) {
                var17.printStackTrace();
                throw var17;
            }
        }

    }

    public static String pmidToHbaseKey(String pmid) {
        return (new StringBuilder(pmid)).reverse().toString();
    }

    public static String hbaseKeyToPmid(String key) {
        return (new StringBuilder(key)).reverse().toString();
    }


 */
    public static void main(String[] args) {
        /*BasicConfigurator.configure();
        System.out.print("Type the path to store the XML: ");
        Scanner read = new Scanner(System.in);
        String path = read.next();
        NewPubMedLibrary newPubMedLibrary = new NewPubMedLibrary();
        newPubMedLibrary.setPathDoc(path);
        System.out.print("Type 1 for crawling by index, 2 for crawling by data: ");
        String flag = read.next();
        System.out.print("Force update? Y/N: ");
        String forceUpdate = read.next();
        //System.out.print(flag);
        if (flag.equals("1")){
            System.out.print("start index: ");
            String startIndex = read.next();
            System.out.print("end index: ");
            String endIndex = read.next();
            newPubMedLibrary.batchDownload(Long.parseLong(startIndex), Long.parseLong(endIndex), forceUpdate.equals("Y"));
        }
        else{
            System.out.print("start date as yyyy/MM/dd: ");
            String startDate = read.next();
            System.out.print("end date as yyyy/MM/dd: ");
            String endDate = read.next();
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            Date start = null;
            try {
                start = dateFormat.parse(startDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Date end = null;
            try {
                end = dateFormat.parse(endDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            newPubMedLibrary.batchDownload(start, end, forceUpdate.equals("Y"));

        }*/
        BasicConfigurator.configure();
        String[] arguments;
        System.out.print("arguments should be split by empty blank ");
        System.out.print("(full path of the file to store the XML) ");
        System.out.print("(1 for crawling by index, 2 for crawling by date) ");
        System.out.print("(Force update(Y/N)) ");
        System.out.println("(if 1: start index end index if 2: start date end date (yyyy/MM/dd)) ");
        //Scanner read = new Scanner(System.in);
        //String argument = read.nextLine();
        //arguments = argument.split(" ");
        arguments = args;
        NewPubMedLibrary newPubMedLibrary = new NewPubMedLibrary();
        newPubMedLibrary.setPathDoc(arguments[0]);
        String forceUpdate = arguments[2];
        if (arguments[1].equals("1")){
            String startIndex = arguments[3];
            String endIndex = arguments[4];
            newPubMedLibrary.batchDownload(Long.parseLong(startIndex), Long.parseLong(endIndex), forceUpdate.equals("Y"));
        }
        else{
            String startDate = arguments[3];
            String endDate = arguments[4];
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            Date start = null;
            try {
                start = dateFormat.parse(startDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Date end = null;
            try {
                end = dateFormat.parse(endDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            newPubMedLibrary.batchDownload(start, end, forceUpdate.equals("Y"));

        }







        //Date date = new Date(116,8,1);

        //newPubMedLibrary.batchDownload(date, date, false);
    }
/*
    public static enum OutputType {
        TO_DATABASE,
        TO_FILE,
        TO_HBASE;

        private OutputType() {
        }
    }
 */


}
