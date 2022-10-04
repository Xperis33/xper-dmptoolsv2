package fr.xperis.dmp.tools;

import com.quovadx.cloverleaf.upoc.PropertyTree;
import fr.xperis.dmp.ws.utils.UserData;
import org.apache.log4j.Logger;

/**
 * Created by ffonvillia on 27/03/2017.
 */
public class MetadataService {

    private static final Logger logger = Logger.getLogger(MetadataService.class);

    private static MetadataService instance;

    private MetadataService(){
    }

    /**
     * Instance to get ConfLoader
     *
     * @return
     */
    public static MetadataService getInstance() {
        if(instance == null) {
            instance = new MetadataService();
        }
        return instance;
    }

    /**
     * Extract User Data
     *
     * @param rawUserData String
     * @return UserData
     */
    public static UserData extractUserData(PropertyTree rawUserData) {
        UserData userData = null;
        try {
            logger.debug("INS : "+rawUserData.get("INS"));
            logger.debug("OID_DOCUMENT : "+rawUserData.get("OID_DOCUMENT"));
        }  catch (PropertyTree.PropertyTreeException e) {
            logger.error(e);
        }
        try {
            userData = new UserData.UserDataBuilder((String)rawUserData.get("INS"), (String)rawUserData.get("IPP"))
                    .accordAcces((String)rawUserData.get("ACCORD_ACCES"))
                    .transactionType((String)rawUserData.get("TRANSACTION_TYPE"))
                    .addCanalType((String)rawUserData.get("ADD_TYPE_ACCES"))
                    .clientID((String)rawUserData.get("ID_CLIENT"))
                    .addCanalValue((String)rawUserData.get("ADD_VALEUR_ACCES"))
                    .canalAction((String)rawUserData.get("ACTION_ACCES"))
                    .birthDate((String)rawUserData.get("DATE_NAISS"))
                    .deletion((String)rawUserData.get("SUPRESS"))
                    .doc1InternalID((String)rawUserData.get("DOCUMENT_ID"))
                    .doc1UniqueId((String)rawUserData.get("OID_DOCUMENT"))
                    .docSignInternalID((String)rawUserData.get("SIGNATURE_ID"))
                    .docSignUniqueId((String)rawUserData.get("OID_SIGNATURE"))
                    .fileName((String)rawUserData.get("FILENAME"))
                    .firstNamePS((String)rawUserData.get("PRENOM_MED"))
                    .lastNamePS((String)rawUserData.get("NOM_MED"))
                    .idPS((String)rawUserData.get("CODE_MED"))
                    .jobCode((String)rawUserData.get("PROFESSION"))
                    .jobOID((String)rawUserData.get("PROFESSION_OID"))
                    .jobDesc((String)rawUserData.get("PROFESSION_LIBELLE"))
                    .lotUniqueId((String)rawUserData.get("OID_LOT"))
                    .lpsOID((String)rawUserData.get("OID_RACINE"))
                    .origSourceConn((String)rawUserData.get("ORIGSOURCECONN"))
                    .patientFirstName((String)rawUserData.get("PRENOMP"))
                    .patientNIROD((String) rawUserData.get("NIROD"))
                    .nirOID((String) rawUserData.get("NIROID"))
                    .patientRangNAISS((String) rawUserData.get("RANGNAISS"))
                    .specialtyCode((String)rawUserData.get("SPECIALITE"))
                    .specialtyDesc((String)rawUserData.get("SPECIALITE_LIBELLE"))
                    .specialtyOID((String)rawUserData.get("SPECIALITE_OID"))
                    .structureId((String)rawUserData.get("IDENTIFIANT_STRUCTURE"))
                    .structureName((String)rawUserData.get("STRUCTURE_NOM"))
                    .activityField((String)rawUserData.get("CODE_SECTEUR_STRUCTURE"))
                    .canalType((String)rawUserData.get("TYPE_ACCES"))
                    .canalValue((String)rawUserData.get("VALEUR_ACCES"))
                    .build();
        } catch (PropertyTree.PropertyTreeException e) {
            logger.error(e);
        }

        return userData;
    }
}
