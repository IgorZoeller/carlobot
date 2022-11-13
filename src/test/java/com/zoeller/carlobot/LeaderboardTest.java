package com.zoeller.carlobot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.jnosql.diana.redis.key.Ranking;
import org.jnosql.diana.redis.key.SortedSet;
import org.junit.jupiter.api.Test;

/**
 * Unit test for simple App.
 */
class LeaderboardTest {
    @Test
    void shouldLoadFiveEntriesFromMockTable() {
        int numberOfEntries = 0;
        Leaderboard mock = new Leaderboard(System.getProperty("user.dir") + "/src/test/resources/leaderboardTest.json");
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
        Leaderboard mock = new Leaderboard(filename);
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
        Leaderboard mock = new Leaderboard(System.getProperty("user.dir") + "/src/test/resources/leaderboardTest.json");
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
    void shouldRetrieveMockTopThree() {
        Leaderboard mock = new Leaderboard(System.getProperty("user.dir") + "/src/test/resources/leaderboardTest.json");
        Ranking rank1 = null;
        Ranking rank2 = null;
        Ranking rank3 = null;
        try {
            mock.load();
            SortedSet mockleaderboard = mock.createLeaderboard();
            List<Ranking> ranking = mockleaderboard.getRevRanking();
            rank1 = ranking.get(0);
            rank2 = ranking.get(1);
            rank3 = ranking.get(2);
        } catch (Exception e) {
        }

        assertEquals("A", rank1.getMember().toString());
        assertEquals(100, (int)rank1.getPoints());

        assertEquals("B", rank2.getMember().toString());
        assertEquals(90, (int)rank2.getPoints());

        assertEquals("C", rank3.getMember().toString());
        assertEquals(80, (int)rank3.getPoints());
    }
}
