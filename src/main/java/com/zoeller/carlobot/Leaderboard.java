package com.zoeller.carlobot;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ZAddParams;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;


public class Leaderboard {

    /**
     * {
     *     "1" : {"name" : "A", "score" : 100},
     *     "5" : {"name" : "E", "score" :  60}
     * }
     */
    private HashMap<String, HashMap<String, Object>> table;
    private String tableSource;

    private Jedis redisClient;
    private String redisKey;

    private long numberOfUpdates = 0;

    public Leaderboard(String fileName) {
        String redisKey = "leaderboard";
        this.tableSource = fileName;
        this.redisKey = redisKey;
        this.table = new HashMap<String, HashMap<String, Object>>();
        this.redisClient = new Jedis("localhost", 6379);
    }
    public Leaderboard(String fileName, String redisKey) {
        this.tableSource = fileName;
        this.redisKey = redisKey;
        this.table = new HashMap<String, HashMap<String, Object>>();
        this.redisClient = new Jedis("localhost", 6379);
    }

    public void load() throws IOException, FileNotFoundException {
        File file = new File(tableSource);
        if (!file.isFile()) {
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
                String key = parser.getString();
                if (parser.next() == Event.START_OBJECT) {
                    HashMap<String, Object> entryData = HttpHandler.parseObject(parser);
                    this.table.put(key, entryData);
                } else {
                    throw new IOException("[ERROR] Failed to read leaderboard database. Invalid key-value state.");
                }
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

        String newScore = entry.get("score").toString();
        record.put("score", newScore);

        return true;
    }

    public void save() {
        save(this.tableSource);
    }

    public void save(String fileName) {
        try {
            String json = new ObjectMapper().writeValueAsString(this.table);
            File output = new File(fileName);
            FileWriter writer = new FileWriter(output);
            writer.write(json);
            writer.close();
        } catch (Exception error) {
            System.out.println(error);
            System.out.println("[ERROR] Unable to save data table. Printing table into terminal for manual backup:");
            System.out.println("{");
            this.table.entrySet().forEach(tableEntry -> {
                String authorId = tableEntry.getKey();
                System.out.print(String.format("%s : ", authorId));
                String authorName = tableEntry.getValue().get("name").toString();
                String likes = tableEntry.getValue().get("score").toString();
                System.out.println(
                    String.format("{name : %s, score : %s},", authorName, likes)
                );
            });
            System.out.println("}");
        }
    }

    public long updateRedisDB() {
        ZAddParams params = new ZAddParams().ch();
        this.table.entrySet().forEach(tableEntry -> {
            String authorId = tableEntry.getKey();
            int likes = Integer.valueOf(tableEntry.getValue().get("score").toString());
            numberOfUpdates += redisClient.zadd(redisKey, likes, authorId, params);
        });
        return numberOfUpdates;
    }

    public List<String> retrieveTopEntriesIDs(int stopIndex) {
        return this.redisClient.zrevrange(redisKey, 0, stopIndex);
    }

    public HashMap<String, HashMap<String, Object>> getTable() {
        return this.table;
    }

    public Jedis getRedisClient() {
        return this.redisClient;
    }
}