import edu.mcw.rgd.common.utils.FileList;
import edu.mcw.rgd.nlp.utils.Library;
import edu.mcw.rgd.nlp.utils.LibraryBase;
import edu.mcw.rgd.nlp.utils.ncbi.PubMedDocSet;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

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

    public int batchDownload(long start_id, long end_id, boolean force_update) {
        logger.info("Start downloading from PMID " + start_id + " to PMID"
                + end_id);
        NewPubMedRetriever retriever = new NewPubMedRetriever();
        start_id = ((start_id - 1) / NewPubMedRetriever.ID_TRUNK_SIZE)
                * NewPubMedRetriever.ID_TRUNK_SIZE + 1;
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
        String result = retriever.crawlByIdList(ids);
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

    public static void main(String[] args){
        NewPubMedLibrary newPubMedLibrary = new NewPubMedLibrary();
        newPubMedLibrary.batchDownload(27614362, 27614324, true);
    }

}
