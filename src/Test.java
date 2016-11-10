import edu.mcw.rgd.common.utils.FileEntry;
import edu.mcw.rgd.common.utils.FileList;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by wangt on 8/25/2016.
 */
public class Test {
    private String filePath = "";
    public ArrayList<FileEntry> fileList = new ArrayList();

    public int save() throws Exception {
        try {
            BufferedWriter e = new BufferedWriter(new FileWriter(this.filePath));

            for(int i = 0; i < this.fileList.size(); ++i) {
                FileEntry fe = this.fileList.get(i);
                e.write(fe.toString());
                e.newLine();
            }

            e.close();
            return this.fileList.size();
        } catch (Exception var4) {
            throw var4;
        }
    }

    public static void main(String[] args) throws Exception{
        for (String line : Files.readAllLines(Paths.get("/Users/Tianyang/CS545/CrawlerPipline/tags.txt"))){
            System.out.println(line);
        }
    }



}
