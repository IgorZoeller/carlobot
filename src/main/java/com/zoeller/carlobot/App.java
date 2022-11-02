package com.zoeller.carlobot;

import com.zoeller.carlobot.TwitterApi;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;

public final class App {

    public static TwitterApi apiInstance = new TwitterApi();

    private App() {
    }

    public static void consumeHttpResponse(HttpResponse response) {
        try {
            InputStream httpStream = response.getEntity().getContent();
            httpStream.close();
        }
        catch (IOException error) {
            System.out.println("Could not consume HttpResponse");
            System.out.println(error);
        }
    }

    /**
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        String tweetUserId = "2389522849"; // @IgorZoeller
        HttpResponse response = apiInstance.getLikedTweetsFromUserId(tweetUserId);
        consumeHttpResponse(response);
    }
}
