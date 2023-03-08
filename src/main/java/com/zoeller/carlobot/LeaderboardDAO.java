package com.zoeller.carlobot;

import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Set;


/**
 * Class responsible to manage Redis DB data.
 * <p>namesTable - Redis Hash with key being all mapped user IDs and their current twitter name as the value.</p>
 * <p>scoreTable - Redis Sorted Set with key being all mapped user IDs and their score being the aggregate value of likes</p>
 */
public class LeaderboardDAO {

    private final String host = "localhost";
    private final int port = 6379;

    private final String namesTable = "db.usernames";
    private final String scoreTable = "db.scores";
    public final String namesTableQA = "qa.usernames";
    public final String scoreTableQA = "qa.scores";

    private Jedis redisClient;

    public LeaderboardDAO() throws Exception{
        this.redisClient = new Jedis(host, port);
        if(this.redisClient.isConnected()) {
            System.out.println(String.format("[INFO] Connected to Redis DB %s:%s", host, port));
            System.out.print(this.redisClient.info());
        }
    }

    public Set<String> getSchemas() {
        return this.redisClient.keys("*");
    }

    public boolean mapUsername(String schema, String id, String name) {
        if(this.redisClient.hexists(schema, id)) {
            String currentName = this.redisClient.hget(schema, id);
            if(currentName.equalsIgnoreCase(name)) {
                return false;
            }
            System.out.println(String.format(
                "[INFO] Change in username for id[%s] - new: %s, old: %s", id, name, currentName
            ));
        }
        this.redisClient.hset(schema, id, name);
        return true;
    }

    public boolean mapUsername(String id, String name) {
        return mapUsername(this.namesTable, id, name);
    }

    public void deleteNamesTableEntry(String schema, String id) throws IllegalArgumentException{
        if(!this.redisClient.type(schema).equalsIgnoreCase("hash")) {
            throw new IllegalArgumentException("[WARN] Attempted to delete on a schema not containing a names table.");
        }
        this.redisClient.hdel(schema, id);
    }

    public HashMap<String,String> getUsernames(String schema) {
        return new HashMap<String, String>(this.redisClient.hgetAll(schema));
    }

    public String getUsernameFromId(String schema, String id) {
        return this.redisClient.hget(schema, id);
    }

    public void updateUserScore(String schema, String id) {
        this.redisClient.zincrby(schema, 1, id);
    }

    public void updateUserScore(String id) {
        updateUserScore(this.scoreTable, id);
    }

    public void deleteUserScore(String schema, String id) throws IllegalArgumentException {
        if(!this.redisClient.type(schema).equalsIgnoreCase("sorted")) {
            throw new IllegalArgumentException(String.format(
                "[WARN] Attempted to delete on a schema[%s] not containing a scores table.", schema
            ));
        }
        this.redisClient.zrem(schema, id);
    }

    public int getScoreFromId(String schema, String id) {
        // Here we need doubleValue() to transform the return Double to the
        // primitive double. (Double is a wrapper class on top of the primitive double.)
        return (int)this.redisClient.zscore(schema, id).doubleValue();
    }
}