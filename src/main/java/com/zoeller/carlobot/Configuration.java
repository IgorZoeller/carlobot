package com.zoeller.carlobot;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public final class Configuration {

    private final String PROPERTIES_FILE_NAME = "properties.file";
    private final String DATA_FILE_NAME = "data.file";
    private final String consumerKey = "twitter.consumer.key";
    private final String consumerSecret = "twitter.consumer.secret";
    private final String bearerToken = "twitter.auth.bearer.token";
    private final String accessToken = "twitter.auth.access.token";
    private final String accessSecret = "twitter.auth.access.secret";
    private final String userID = "twitter.user.ID";
    private final String lastTweetId = "tweet.last_id";
    private String propertiesFile;
    private String dataFile;

    public Configuration() {
        System.out.println("[INFO] Loading config file.");
        readProperties();
        readData();
    }

    private void readFile(String fileName) {
        BufferedReader buffer = null;
        try {
            buffer = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = buffer.readLine()) != null) {
                int index = line.indexOf("=");
                if (index <= 0) {
                    continue;
                }
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();
                System.setProperty(key, value);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                buffer.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    private void readProperties() {
        this.propertiesFile = System.getProperty(PROPERTIES_FILE_NAME, "src/main/resources/.properties");
        readFile(this.propertiesFile);
    }

    private void readData(){
        System.out.println("[INFO] Reading last session data.");
        this.dataFile = System.getProperty(DATA_FILE_NAME, "src/main/resources/.data");
        readFile(this.dataFile);
    }

    public String getConsumerKey() {
        return System.getProperty(this.consumerKey);
    }

    public String getConsumerSecret() {
        return System.getProperty(this.consumerSecret);
    }

    public String getBearerToken() {
        return System.getProperty(this.bearerToken);
    }

    public String getAccessToken() {
        return System.getProperty(this.accessToken);
    }

    public String getAccessSecret() {
        return System.getProperty(this.accessSecret);
    }

    public String getUserId() {
        return System.getProperty(this.userID);
    }

    public String getLastTweetId() {
        return System.getProperty(lastTweetId);
    }
}
