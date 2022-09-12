package fr.xperis.dmp.tools;

import fr.xperis.toolbox.file.FileManager;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;


public class ValidFormatAckTest {

    private static final Logger logger = Logger.getLogger(ValidFormatAckTest.class);
    private static final String CDA_PATH = "\\resources-test\\template_CDA.xml";
    private static final String CONF_FILE = "\\resources-test\\schematron.properties";
    private String cdaContent;
    private File cdaFile;
    private String currentPath;

    public ValidFormatAckTest(){
        this.currentPath = System.getProperty("user.dir");
    }

    public void setUp() {
        logger.debug("Path: "+this.currentPath);
        try {
            FileManager fileManager = new FileManager();
            this.cdaContent = fileManager.readFile(this.currentPath+ValidFormatAckTest.CDA_PATH);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Test
    public void testCreateCDAFile() {
        ValidFormatAck validFormatAck = new ValidFormatAck(this.currentPath+ValidFormatAckTest.CONF_FILE);
        //validFormatAck.createCDAFile(this.cdaContent);
        String filePath = this.currentPath+""+validFormatAck.getProperties().get("CDA_PATH");
        logger.debug("File path: "+filePath);
        this.cdaFile =  new File(filePath);
        assertEquals(true, this.cdaFile.exists());
    }

    @Test
    public void testPropertiesFile(){
        String fileName = "dmp_properties.yml";
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        ValidFormatAck validFormatAck = new ValidFormatAck(file);
    }

    public void tearDown() {
        this.cdaFile.delete();
    }
}