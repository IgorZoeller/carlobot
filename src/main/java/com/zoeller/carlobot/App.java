package com.zoeller.carlobot;

import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;

public final class App {

    public static TwitterApi apiInstance = new TwitterApi();

    private App() {
        
    }

    public void checkLatestLikedTweetsFromId(String id) {
        HttpResponse response = apiInstance.getLikedTweetsFromUserId(id);
        List<HashMap<String, Object>> likedTweets = HttpHandler.consumeHttpResponse(response);
    }

    /**
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        App bot = new App();
        String tweetUserId = "2389522849"; // @IgorZoeller
        bot.checkLatestLikedTweetsFromId(tweetUserId);
    }
}
