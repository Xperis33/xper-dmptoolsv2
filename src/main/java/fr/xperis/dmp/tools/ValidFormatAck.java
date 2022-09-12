package fr.xperis.dmp.tools;

import com.quovadx.cloverleaf.upoc.*;
import com.sun.xml.bind.marshaller.CharacterEscapeHandler;
import fr.xperis.dmp.data.ack.DmpResponseToCis;
import fr.xperis.dmp.ws.utils.ResponseInfo;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.util.*;

public class ValidFormatAck extends TPS {
    private static final Logger logger = Logger.getLogger(ValidFormatAck.class);
    private static final String CDA_EXTENSION = "xml";
    private static final String DEFAULT_CDA_NAME = "CDA_TEST";
    private static final String VALUE_FOR_DELETION = "YES";
    private Map<String, Map<String,Object>> properties = null;
    private String confPath;
    private File cdaFile = null;
    private String cdaFileName = null;

    public ValidFormatAck(CloverEnv cloverEnv, PropertyTree xArgs) {
        super(cloverEnv, xArgs);

        try {
            this.extractAndValidateUserArguments(cloverEnv, xArgs);
            this.confPath = xArgs.getString("CONF_PATH");
            logger.debug("Path: " + this.confPath);
            logger.debug("System path: " + System.getProperty("user.dir"));
            this.initProperties();
        } catch (CloverleafException var5) {
            logger.error(var5);
        }

    }

//    public ValidFormatAck(String confPath) {
//        this.confPath = confPath;
//        try {
//            logger.debug("ConfPath: " + this.confPath);
//            Yaml dmpPropertiesFile = new Yaml();
//            this.properties =(Map<String, Map<String,Object>>) dmpPropertiesFile.load(new FileInputStream(new File(confPath)));
//        } catch (IOException var3) {
//            logger.error("Unable to access to config file", var3);
//        }
//    }

//    public ValidFormatAck(File fileToParse) {
//        try {
//            Yaml dmpPropertiesFile = new Yaml();
//            Map<String, Map<String,Object>> dmpProp = (Map<String, Map<String,Object>>) dmpPropertiesFile.load(new FileInputStream(fileToParse));
//            ArrayList<String> test = (ArrayList<String>) dmpProp.get("SCHEMATRON").get("DEST_REPLY_KO");
//            String booya = (String) dmpProp.get("SCHEMATRON").get("ENGNE_NAME");
//            logger.info("engine_name: "+ booya);
//            logger.info("first dest: "+ test.get(0));
//        } catch (IOException var3) {
//            logger.error("Unable to access to config file", var3);
//        }
//    }

    public void initProperties() {
        try {
            logger.debug("ConfPath: " + this.confPath);
            Yaml dmpPropertiesFile = new Yaml();
            this.properties =(Map<String, Map<String,Object>>) dmpPropertiesFile.load(new FileInputStream(new File(confPath)));
        } catch (Exception var4) {
            logger.error("Unable to access to config file", var4);
        }
    }

    @Override
    public DispositionList process(CloverEnv cloverEnv, String contextCIS, String mode, Message message) throws CloverleafException {
        DispositionList dispList = new DispositionList();
        if ("start".equalsIgnoreCase(mode)) {
            logger.debug("Nothing to do at start.");
        } else if ("run".equalsIgnoreCase(mode)) {
            logger.debug("Nothing to do at run.");
            String cdaContent = null;

            try {
                cdaContent = message.getContent();
            } catch (CloverleafException var15) {
                logger.error(var15);
            }
            String filePath= message.metadata.getDriverctl().getString("FILENAME");
            logger.info("Path: "+filePath);
            File ibFile = new File(filePath);
            String fileName = ibFile.getName();
            logger.info("Current File: "+fileName);
            this.cdaFileName = FilenameUtils.removeExtension(fileName);
            if(this.cdaFileName == null || this.cdaFileName.equalsIgnoreCase("")){
                this.cdaFileName = DEFAULT_CDA_NAME;
            }
            logger.info("File name: "+this.cdaFileName);
            this.createCDAForEngine(cdaContent);
            ValidatorCDA cdaValidator = new ValidatorCDA((String)this.properties.get("COMMON").get("CIS_PATH"), this.properties.get("SCHEMATRON"));
            cdaValidator.launchCheckingAndValidation(this.cdaFileName);
            if (cdaValidator.isValid() && cdaValidator.isChecked()) {
                logger.info("Successful cda file validation and checking");
                dispList.add(DispositionList.CONTINUE, this.addUserdatatoMsg(message));
            } else {
                ResponseInfo responseInfo = new ResponseInfo(false, "ErrFormat", "Message format does not correspond to CDA R2", cdaValidator.getCDATACheckingFile());
                responseInfo.createCisResponse(null);
                StringWriter msgToCis = new StringWriter();
                try {
                    ClassLoader classLoader = DmpResponseToCis.class.getClassLoader();
                    JAXBContext context = JAXBContext.newInstance(DmpResponseToCis.class.getPackage().getName(), classLoader);
                    Marshaller m = context.createMarshaller();
                    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                    m.setProperty(CharacterEscapeHandler.class.getName(),
                            new CharacterEscapeHandler() {
                                @Override
                                public void escape(char[] ac, int i, int j, boolean flag,
                                                   Writer writer) throws IOException {
                                    writer.write(ac, i, j);
                                }
                            });
                    m.marshal(responseInfo.getDmpResponseToCis(), msgToCis);
                } catch (JAXBException e) {
                    logger.error(e);
                }

                logger.debug("File Name for response : " + cdaFileName);
                PropertyTree pt = new PropertyTree();
                if (cdaFileName != null) {
                    pt.put("FILENAME", cdaFileName + "." + ValidFormatAck.CDA_EXTENSION + ".ack");
                } else {
                    pt.put("FILENAME", "reply" + System.currentTimeMillis() + ".ack");
                }
                logger.debug("data: "+msgToCis.toString());

                Message replyMsg = null;
                ArrayList<String> destList = (ArrayList<String>)this.properties.get("SCHEMATRON").get("DEST_REPLY_KO");
                //Send messages to all destinations
                for(int i = 0; i < destList.size(); i++){
                    replyMsg = cloverEnv.makeMessage(msgToCis.toString(), Message.DATA_TYPE, Message.ENGINE_CLASS, true);
                    replyMsg.metadata.setUserdata(message.metadata.getUserdata());
                    replyMsg.metadata.set("DESTCONN", destList.get(i));
                    replyMsg.metadata.setDriverctl(pt);
                    dispList.add(DispositionList.CONTINUE, replyMsg);
                    logger.debug("Response sent");
                }
                //suppress message from engine
                dispList.add(DispositionList.KILL,message);

                //suppress temporary cda file create by schematron
                try {
                    String tmpCdaPath = (String)this.properties.get("COMMON").get("CIS_PATH") + this.properties.get("SCHEMATRON").get("CDA_PATH") + "" + this.cdaFileName+"."+ValidFormatAck.CDA_EXTENSION;
                    File fileToDelete =  new File(tmpCdaPath);
                    if(fileToDelete.exists() && fileToDelete.delete()){
                        logger.debug("Temporary file 's name: "+tmpCdaPath+" has been deleted");
                    } else {
                        logger.debug("Temporary file 's name: "+tmpCdaPath+" cannot be deleted");
                    }

                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }

            //delete cda file for schematron engine
            try{
                if(this.cdaFile.exists() && this.cdaFile.delete()){
                    logger.debug("The file, "+this.cdaFile.getName()+" for engine has been deleted");
                } else {
                    logger.debug("The file, "+this.cdaFile.getName()+" for engine cannot be delete");
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }


            if(this.properties.get("SCHEMATRON").containsKey("DELETE_REPORT") && (Boolean)this.properties.get("SCHEMATRON").get("DELETE_REPORT")) {
                logger.debug("Report on file "+cdaFileName+" to delete.");
                cdaValidator.purgeFiles(this.cdaFileName);
            }
        } else {
            logger.debug("Nothing to do at start or at run.");
        }

        return dispList;
    }


    private void createCDAForEngine(String cdaContent) {
        String fullEnginePath = (String)this.properties.get("COMMON").get("CIS_PATH") + this.properties.get("SCHEMATRON").get("ENGINE_PATH") + "" + this.cdaFileName+"."+ValidFormatAck.CDA_EXTENSION;
        logger.debug("CDA file: " + fullEnginePath);
        this.cdaFile = new File(fullEnginePath);
        OutputStream outputStream;
        Writer outputStreamWriter = null;
        try {
            outputStream = new FileOutputStream(fullEnginePath);
            outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
            outputStreamWriter.write(cdaContent);
        } catch (FileNotFoundException e) {
            logger.error(e);
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } finally {
            try {
                outputStreamWriter.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    public Map<String, Map<String,Object>> getProperties() {
        return this.properties;
    }

    /**
     * Add USer data to message
     * @param message
     * @return
     */
    private Message addUserdatatoMsg(Message message){
        //add userdata
        PropertyTree userDataPropTree = null;
        try {
            userDataPropTree = message.getUserdata();
        } catch (CloverleafException e) {
            userDataPropTree = new PropertyTree();
        }
        for(Map.Entry<String,Object> entry: this.properties.get("USERDATA").entrySet()){
            try {
                String valueForUserdata = "";
                if(entry.getValue().getClass() == Integer.class){
                    valueForUserdata = String.valueOf(entry.getValue());
                } else if(entry.getValue().getClass() == LinkedHashMap.class) {
                    LinkedHashMap tempHashMap = (LinkedHashMap) entry.getValue();

                    // Get a set of the entries
                    Set set = tempHashMap.entrySet();

                    // Get an iterator
                    Iterator i = set.iterator();
                    while(i.hasNext()) {
                        Map.Entry data = (Map.Entry)i.next();
                        valueForUserdata += data.getValue();
                        /*System.out.print(me.getKey() + ": ");
                        System.out.println(me.getValue());*/
                    }
                } else {
                    valueForUserdata = (String)entry.getValue();
                }
                userDataPropTree.put(entry.getKey(), valueForUserdata);
            } catch (PropertyTree.PropertyTreeException e) {
                logger.error(e.getMessage());
            }
        }
        //Add CDA PATH
        String cdaPath = this.properties.get("COMMON").get("CIS_PATH") +""+ this.properties.get("SCHEMATRON").get("CDA_PATH");
        try {
            userDataPropTree.put("CDA_PATH", cdaPath );
            message.setUserdata(userDataPropTree);
        } catch (CloverleafException e) {
            logger.error(e);
        }

        return message;
    }
}
