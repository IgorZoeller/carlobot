package com.zoeller.carlobot;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;

public final class App {

    public static Configuration cfg = new Configuration();
    public static TwitterApi apiInstance = new TwitterApi(cfg);

    private App() {
        
    }

    public List<HashMap<String, Object>> checkLatestLikedTweetsFromUserId(String id) {
        HttpResponse response = apiInstance.getLikedTweetsFromUserId(id);
        List<HashMap<String, Object>> likedTweets = HttpHandler.consumeHttpResponse(response);
        String latestSessionTweetId = cfg.getLastTweetId();
        System.out.println(
            String.format("[INFO] The last tweet ID from latest session is: %s", latestSessionTweetId)
        );
        int latestSessionIndex = 0;
        for (HashMap<String, Object> like : likedTweets) {
            if (String.valueOf(like.get("id")).equals(latestSessionTweetId)) { break; }
            latestSessionIndex++;
        }
        likedTweets.subList(latestSessionIndex, likedTweets.size()).clear();
        // Debug
        // likedTweets.forEach(like -> {
        //     System.out.println("\n");
        //     like.entrySet().forEach(entry -> {
        //         System.out.println(entry.getKey() + " : " + entry.getValue());
        //     });
        // });
        return likedTweets;
    }

    public void tweetDailyMessage(List<HashMap<String, Object>> latestLikes) {
        int numberOfTweets = latestLikes.size();
        String message = String.format(
            "Hoje o Carlinhos deu %s likes.", numberOfTweets
        );
        if (numberOfTweets == 0) {
            message.concat("\nAí eh pegado.");
        }
        System.out.println(message);
        apiInstance.postTweet(message);
    }

    public int updateSessionData(List<HashMap<String, Object>> latestLikes) {
        if (latestLikes.size() == 0) {
            return 0;
        }
        String dataFile = cfg.getDataFilePath();
        String latestLikeId = latestLikes.get(0).get("id").toString();
        try {
            PrintWriter writer = new PrintWriter(dataFile, StandardCharsets.UTF_8.toString());
            writer.println(String.format("%s       = %s", cfg.getLastTweetIdIdentifier(), latestLikeId));
            System.out.println(
		String.format("[INFO] Successfully saved current session last tweet id: %s", latestLikeId)
		);
	    writer.close();
        }
        catch (FileNotFoundException e){
            return -1;
        }
        catch (UnsupportedEncodingException e){
            return -1;
        }
        return 1;
    }

    /**
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        App bot = new App();
        String tweetUserId = cfg.getUserId();
        List<HashMap<String, Object>> latestLikes = bot.checkLatestLikedTweetsFromUserId(tweetUserId);
        // bot.tweetDailyMessage(latestLikes);
        // bot.updateSessionData(latestLikes);
    }
}
