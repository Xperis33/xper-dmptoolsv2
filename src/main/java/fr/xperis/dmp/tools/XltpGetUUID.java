package fr.xperis.dmp.tools;

import com.quovadx.cloverleaf.upoc.*;
import fr.xperis.dmp.ConfLoader;
import fr.xperis.dmp.ws.DocumentRegistryService;
import fr.xperis.dmp.ws.utils.ResponseInfo;
import fr.xperis.dmp.ws.utils.UserData;
import org.apache.log4j.Logger;
import java.util.Vector;

public class XltpGetUUID extends XLTStrings {

    private static final Logger logger = Logger.getLogger(XltpGetUUID.class);

    protected ResponseInfo responseInfo;

    public XltpGetUUID(CloverEnv cloverEnv, PropertyTree xArgs) {
        super(cloverEnv, xArgs);

        try {
            extractAndValidateUserArguments(cloverEnv, xArgs);
            String wsConfPath = xArgs.getString("CONF_PATH");
            logger.debug("Path: " + wsConfPath);
            //Load webservices config file
            ConfLoader.getInstance(wsConfPath);
        } catch (CloverleafException e) {
            logger.error(e);
        }
    }

    @Override
    public Vector<String> xlateStrings(CloverEnv _cloverEnv, Xpm _xpm, Vector _vector)
            throws CloverleafException {

        //initialisation of response
        this.responseInfo = new ResponseInfo();

        Vector<String> vector_ = new Vector<String>();
        String oidRPLC = (String) _vector.get(0);
        MessageMetadata msgMD = _xpm.metadata;
        UserData userData = MetadataService.getInstance().extractUserData(msgMD.getUserdata());
        //Change context classLoader to solve chain delegation issues
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            String UUID_ = this.searchDocumentFromDMP(userData,oidRPLC);
            logger.debug("UUID found : " + UUID_);
            //INSC Extraction
            vector_.add(UUID_);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return vector_;
    }

    /**
     * Get document from DMP
     *
     * @param userData UserData
     * @return ResponseInfo
     */
    private String searchDocumentFromDMP(UserData userData,String oidRPLC) {
        String UUID = null;

        DocumentRegistryService docRegistryService = null;
        try {

            docRegistryService = new DocumentRegistryService(userData);
            if (docRegistryService != null) {
                docRegistryService.adhocQueryRequest(oidRPLC, this.responseInfo);
                UUID = docRegistryService.getUUID();
            } else {
                try {
                    throw new Exception("No UUID Found!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return UUID;
    }

}