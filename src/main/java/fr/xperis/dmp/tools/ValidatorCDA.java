package fr.xperis.dmp.tools;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Map;

public class ValidatorCDA {

    private static final Logger logger = Logger.getLogger(ValidatorCDA.class);
    private static final String[] MANDATORY_PROPERTIES = {"ENGINE_PATH", "ENGINE_NAME", "VALID_FILE_PATH", "CHECK_FILE_PATH"};
    private static final String VALIDATION_FILE_SUFFIX = "validCDA.xml";
    private static final String VERIFICATION_FILE_SUFFIX = "verif.xml";
    private Map<String, Object> properties = null;
    private boolean isValid = false;
    private boolean isChecked = false;
    private String schematronPath = "";
    private String checkingFile = "";
    private String validationFile = "";

    public ValidatorCDA(String schematronPath, Map<String, Object> prop) {
        this.schematronPath = schematronPath;
        this.properties = prop;
    }

    public void launchCheckingAndValidation(String cdaFileName) {
        try {
            this.checkConfig();
        } catch (Exception var2) {
            logger.error(var2);
        }

        this.createValidationReport(cdaFileName);
        if (this.isValid) {
            this.validAndCheckedData(cdaFileName);
        }

    }

    private void checkConfig() {
        for (String property : MANDATORY_PROPERTIES) {
            if (!this.properties.containsKey(property) || this.properties.get(property) == null) {
                try {
                    throw new MissingPropertiesException("Please complete " + property + " in schematron.properties file");
                } catch (MissingPropertiesException e) {
                    logger.error(e);
                }
            }
        }
    }

    public void createValidationReport(String cdaFileName) {
        String enginePath = this.schematronPath + this.properties.get("ENGINE_PATH");
        logger.debug("Report creation start");

        this.isValid = true;
        try {
            String[] execCommand = null;
            if (SystemUtils.IS_OS_LINUX) {
                logger.debug("Linux System");
                execCommand = new String[]{"/bin/bash", "-c", enginePath + this.properties.get("ENGINE_NAME") + " " + cdaFileName};
            } else if (SystemUtils.IS_OS_WINDOWS) {
                logger.debug("Windows System");
                execCommand = new String[]{"cmd.exe", "/C", enginePath + this.properties.get("ENGINE_NAME") + " " + cdaFileName};
            } else {
                logger.error("No Operating System found");
                try {
                    throw new Exception("No compatible OS found!! Only Linux and Windows are supported");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            logger.debug("Command line engine:");
            logger.debug(enginePath + this.properties.get("ENGINE_NAME") + " " + cdaFileName);
            Process runTimeProc = Runtime.getRuntime().exec(execCommand, null, new File(enginePath));
            BufferedReader output = this.getOutput(runTimeProc);
            BufferedReader error = this.getError(runTimeProc);

            String line;
            while ((line = output.readLine()) != null) {
                logger.debug(line);
            }

            while ((line = error.readLine()) != null) {
                logger.debug(line);
                this.isValid = false;
            }

            runTimeProc.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error(e);
            this.isValid = false;
        }

        logger.debug("Report has been created");
    }

    public void validAndCheckedData(String fileName) {
        try (BufferedReader buffValidationFileXML = new BufferedReader(new FileReader(this.schematronPath + this.properties.get("VALID_FILE_PATH") + fileName + "_" + VALIDATION_FILE_SUFFIX))) {
            try (BufferedReader buffVerifFileXML = new BufferedReader(new FileReader(this.schematronPath + this.properties.get("CHECK_FILE_PATH") + fileName + "_" + VERIFICATION_FILE_SUFFIX))) {

                logger.debug("Valid path: " + this.schematronPath + this.properties.get("VALID_FILE_PATH") + fileName + "_" + VALIDATION_FILE_SUFFIX);
                logger.debug("Verif path: " + this.schematronPath + this.properties.get("CHECK_FILE_PATH") + fileName + "_" + VERIFICATION_FILE_SUFFIX);
                this.validateFile(buffValidationFileXML, buffVerifFileXML);
                this.isValid = this.validationFile.contains("<xsd-validation result=\"OK\">");
                logger.info("Check file: " + String.valueOf(this.checkingFile.contains("failed-assert")));
                this.isChecked = !this.checkingFile.contains("failed-assert");
            }
        } catch (Exception e) {
            logger.error("Unable to generate reports.");
            logger.error(e);
        }
    }

    private void validateFile(BufferedReader buffValidationFileXML, BufferedReader buffVerifFileXML) {
        try {
            StringBuilder strBdValid = new StringBuilder();
            while (buffValidationFileXML.ready()) {
                strBdValid.append(buffValidationFileXML.readLine());
            }
            this.validationFile = strBdValid.toString();
            logger.debug("File to validate :" + this.validationFile);
            StringBuilder strBdVerif = new StringBuilder();
            while (buffVerifFileXML.ready()) {
                strBdVerif.append(buffVerifFileXML.readLine());
            }
            this.checkingFile = strBdVerif.toString();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    /**
     * Purge report files
     *
     * @param fileName
     */
    public void purgeReportFiles(String fileName) {
        try {
            File checkFileToDel = new File(this.schematronPath + this.properties.get("CHECK_FILE_PATH") + fileName + "_" + VERIFICATION_FILE_SUFFIX);
            File validateFileToDel = new File(this.schematronPath + this.properties.get("VALID_FILE_PATH") + fileName + "_" + VALIDATION_FILE_SUFFIX);

            if (checkFileToDel.delete()) {
                logger.debug("File: " + checkFileToDel.getPath() + " has been deleted");
            } else {
                logger.error("Deletion of file: " + checkFileToDel.getPath() + " has failed");
            }

            if (validateFileToDel.delete()) {
                logger.debug("File: " + validateFileToDel.getPath() + " has been deleted");
            } else {
                logger.error("Deletion of file: " + validateFileToDel.getPath() + " has failed");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    public void purgeFiles(File fileToDel) {
        try {
            if (fileToDel.exists() && fileToDel.delete()) {
                logger.debug("File: " + fileToDel.getPath() + " has been deleted");
            } else {
                logger.error("Deletion of file: " + fileToDel.getPath() + " has failed");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private BufferedReader getOutput(Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    private BufferedReader getError(Process process) {
        return new BufferedReader(new InputStreamReader(process.getErrorStream()));
    }

    public boolean isValid() {
        return this.isValid;
    }

    public String getCheckingFile() {
        return this.checkingFile;
    }

    public String getCDATACheckingFile() {
        return this.checkingFile;
    }

    public boolean isChecked() {
        return this.isChecked;
    }

    @Override
    public String toString() {
        return "Validation file: " + this.validationFile + "\n" + "----------------------------------------\n" + "Checking file: " + this.checkingFile + "\n" + "----------------------------------------\n" + "Data is valid: " + this.isValid + "\n" + "Data is checked: " + this.isChecked + "\n";
    }

    private class MissingPropertiesException extends Exception {
        public MissingPropertiesException() {
            super();
        }

        public MissingPropertiesException(String message) {
            super(message);
        }

        public MissingPropertiesException(String message, Throwable cause) {
            super(message, cause);
        }

        public MissingPropertiesException(Throwable cause) {
            super(cause);
        }
    }
}


