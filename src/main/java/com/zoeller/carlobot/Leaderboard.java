package com.zoeller.carlobot;

import org.jnosql.diana.redis.key.SortedSet;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Leaderboard {

    /**
     * {
     * ${author_id} : {
     * "name" : ${author_name},
     * "score" : ${score}
     * }
     * }
     */
    private HashMap<String, HashMap<String, Object>> table;
    private SortedSet leaderboard;
    private String tableSource;

    public Leaderboard(String fileName) {
        this.tableSource = fileName;
        this.leaderboard = null;
    }

    public void load() throws IOException, FileNotFoundException {
        File file = new File(tableSource);
        if (!file.isFile()) {
            this.table = new HashMap<String, HashMap<String, Object>>();
            if (!file.createNewFile()) {
                throw new IOException("[ERROR] Could not create file at: " + file.getAbsolutePath());
            }
            throw new FileNotFoundException(
                    "[INFO] File does not exist. There is no data to load. Created new file at: "
                            + file.getAbsolutePath());
        }
        JsonParser parser = Json.createParser(new FileReader(file));
        while (parser.hasNext()) {
            if (parser.next() == Event.KEY_NAME) {
                this.table.put(parser.getString(), HttpHandler.parseObject(parser));
            }
        }
    }

    public boolean update(String key, HashMap<String, Object> entry) {
        if (!this.table.containsKey(key)) {
            this.table.put(key, entry);
            return false;
        }
        HashMap<String, Object> record = this.table.get(key);

        if (!record.get("name").equals(entry.get("name"))) {
            System.out.println(
                    String.format(
                            "[INFO] Detected new NAME for entry id %s. Old name is %s, new name is %s. Updating database...",
                            key, record.get("name"), entry.get("name")));
            record.put("name", entry.get("name"));
        }

        int oldScore = (int)record.get("score");
        int newScore = oldScore + (int)entry.get("score");
        record.put("score", newScore);

        return true;
    }

    public void save() {

    }

    public SortedSet createLeaderboard() {
        try (SeContainer container = SeContainerInitializer.newInstance().initialize()) {
            leaderboard = container.select(SortedSet.class).get();
            for(Map.Entry<String, HashMap<String, Object>> row : this.table.entrySet()) {
                HashMap<String, Object> entryData = row.getValue();
                String key = entryData.get("name").toString();
                int score = (int)entryData.get("score");
                leaderboard.add(key, score);
            }
        }
        return leaderboard;
    }
}