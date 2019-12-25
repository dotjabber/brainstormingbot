package pl.wilddogsoftware.slack.utils;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class BotConfig {
    private static final URL URL_CONFIG_FILE = BotConfig.class.getClassLoader().getResource("conf.properties");
    private static Properties properties;

    static {
        properties = new Properties();

        try {
            properties.load(URL_CONFIG_FILE.openStream());

            File confFile = new File("conf.properties");
            if(confFile.exists()) {
                properties.load(new FileInputStream(confFile));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getString(String propertyName) {
        return properties.getProperty(propertyName);
    }

    public static String[] getStrings(String propertyName) {
        return getString(propertyName).replace(" ", "").split(",");
    }

    public static int getInt(String propertyName) {
        return Integer.parseInt(getString(propertyName));
    }
}
