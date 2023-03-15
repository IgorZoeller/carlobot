package com.zoeller.carlobot;

import java.util.HashMap;
import java.util.List;

public class Leaderboard {

    private LeaderboardDAO cache = new LeaderboardDAO();

    public Leaderboard() {

    }

    public void updateDBEntries(List<HashMap<String, Object>> newEntries) {
        System.out.println("[INFO] Updating database with new entries.");
        for (HashMap<String, Object> entry : newEntries) {
            System.out.println(String.format("[INFO] Pricessing new entry: id[%s]", entry.get("id").toString()));
            String user_id = entry.get("author_id").toString();
            String name = entry.get("name").toString();
            System.out.println(String.format("[INFO] Persisting author_id[%s] name[%s] to database.", user_id, name));
            cache.mapUsername(user_id, name);
            cache.updateUserScore(user_id);
        }
    }
}
