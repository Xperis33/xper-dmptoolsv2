package fr.xperis.dmp.tools;

import com.quovadx.cloverleaf.upoc.CloverEnv;
import com.quovadx.cloverleaf.upoc.CloverleafException;
import com.quovadx.cloverleaf.upoc.XLTStrings;
import com.quovadx.cloverleaf.upoc.Xpm;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.Vector;


public class XltpPdfToB64 extends XLTStrings {

    private static final Logger logger = Logger.getLogger(XltpPdfToB64.class);


    @Override
    public Vector xlateStrings(CloverEnv cloverEnv, Xpm xpm, Vector vector) throws CloverleafException {
        String file = null;
        try {
            file = (String) vector.get(0);
        } catch (Exception e) {
            logger.error(XltpPdfToB64.class.getName()+"; L'argument sourcePDF n'est pas definit");
        }

        vector = new Vector<String>();
        vector.add(this.encode64(file, true));

        return vector;
    }

    private String encode64(String path, boolean delete){
        String retour = null;
        try {
            File file = new File(path);


            byte[] buffer;
            byte[] encode;
            try (BufferedInputStream bis = new BufferedInputStream(new java.io.FileInputStream(file))) {

                int bytes = (int) file.length();
                buffer = new byte[bytes];
                bis.read(buffer);
                bis.close();
            }
            encode = Base64.encodeBase64(buffer);
            if(delete)
                file.delete();
            retour = new String(encode);

        } catch (Exception e) {
            logger.error(e);
        }
        return retour;

    }
}
