package com.zoeller.carlobot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.Jedis;

/**
 * Unit test for simple App.
 */
class LeaderboardTest {
    @Test
    void shouldLoadFiveEntriesFromMockTable() {
        int numberOfEntries = 0;
        LeaderboardDAO mock = new LeaderboardDAO(System.getProperty("user.dir") + "/src/test/resources/leaderboardTest.json");
        try {
            mock.load();
            HashMap<String, HashMap<String, Object>> mockTable = mock.getTable();
            numberOfEntries = mockTable.size();
        } catch (Exception e) {
        }
        assertEquals((int) 5, numberOfEntries);
    }

    @Test
    void shouldCreateNewFileForDatabase() {
        String filename = System.getProperty("user.dir") + "/src/test/resources/new_database_file.json";
        LeaderboardDAO mock = new LeaderboardDAO(filename);
        try {
            mock.load();
        } catch (Exception e) {}
        HashMap<String, HashMap<String, Object>> mockTable = mock.getTable();
        assertEquals(mockTable.isEmpty(), true);
        File file = new File(filename);
        assertEquals(file.isFile(), true);
        assertEquals(file.delete(), true);
    }

    @Test
    void shouldUpdateTableEntry() {
        LeaderboardDAO mock = new LeaderboardDAO(System.getProperty("user.dir") + "/src/test/resources/leaderboardTest.json");
        try {
            mock.load();
        } catch (Exception e) {
        }
        
        HashMap<String, Object> newEntry = new HashMap<String, Object>();
        String newName  = "new";
        String newScore = "0";
        newEntry.put("name", newName);
        newEntry.put("score", newScore);

        mock.update("1", newEntry);

        HashMap<String, HashMap<String, Object>> mockTable = mock.getTable();
        String tableName  = mockTable.get("1").get("name").toString();
        String tableScore = mockTable.get("1").get("score").toString();

        assertEquals(newName, tableName);
        assertEquals(newScore, tableScore);
    }

    @Test
    void shouldUpdateEntriesOnDatabase() {
        LeaderboardDAO mock = new LeaderboardDAO(System.getProperty("user.dir") + "/src/test/resources/leaderboardTest.json");
        try {
            mock.load();
        } catch (Exception e) {
        }

        Jedis redisClient = mock.getRedisClient();
        redisClient.select(13);
        long numberOfUpdates = mock.updateRedisDB();
        assertEquals(5, numberOfUpdates);
        assertEquals(1, (int)redisClient.dbSize());
        assertEquals(true, redisClient.exists("leaderboard"));
        Double rank = redisClient.zscore("leaderboard", "1");
        assertEquals(100.0, rank);
        redisClient.flushDB();
        assertEquals(0, (int)redisClient.dbSize());
    }

    @Test
    void shouldSaveMockTable() {
        LeaderboardDAO mock = new LeaderboardDAO(System.getProperty("user.dir") + "/src/test/resources/leaderboardTest.json");
        try {
            mock.load();
        } catch (Exception e) {
        }
        String alternateSavePath = new String(System.getProperty("user.dir") + "/src/test/resources/leaderboardSaveTest.json");
        mock.save(alternateSavePath);
        LeaderboardDAO mock2 = new LeaderboardDAO(alternateSavePath);
        try {
            mock2.load();
        } catch (Exception e) {
        }
        assertEquals(mock.getTable().size(), mock2.getTable().size());
        File file = new File(alternateSavePath);
        assertEquals(file.delete(), true);
    }
}