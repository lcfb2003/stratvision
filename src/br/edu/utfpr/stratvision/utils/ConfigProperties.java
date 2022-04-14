package br.edu.utfpr.stratvision.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lcfb2
 */
public class ConfigProperties {
    private static final String PROPERTIESPATH = "/res/strings/config.properties"; 
    Properties configProp = new Properties();
    private String filesPath;

    public void LoadProperties() {

        InputStream in = this.getClass().getResourceAsStream(PROPERTIESPATH);
        try {
            configProp.load(in);
            filesPath = configProp.getProperty("PatternFilesPath");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void SaveProperties() {
        try {
            FileOutputStream out = new FileOutputStream(PROPERTIESPATH);
            configProp.store(out,"Configuration properties");
        } catch (IOException ex) {
            Logger.getLogger(ConfigProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the filesPath
     */
    public String getFilesPath() {
        return filesPath;
    }

    /**
     * @param filesPath the filesPath to set
     */
    public void setFilesPath(String filesPath) {
        this.filesPath = filesPath;
    }
}
