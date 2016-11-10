import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Created by Tianyang on 10/23/16.
 */
public class PubParser extends RawParser {
    public PubParser(String xml, String tagPath, String startTag) throws ParserConfigurationException, IOException, SAXException {
        super(xml, tagPath, startTag);
    }
    public PubParser(File xmlFile, String tagPath, String startTag) throws ParserConfigurationException, SAXException, IOException {
        super(xmlFile, tagPath, startTag);
    }
}
