package com.zoeller.carlobot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class LeaderboardTest {

    private final LeaderboardDAO cache = new LeaderboardDAO();

    @Test
    void shouldCreateNewEntryAtNamesTable() {
        String id = "1312";
        String name = "carlo";
        this.cache.mapUsername(this.cache.namesTableQA, id, name);
        String dbName = this.cache.getUsernameFromId(this.cache.namesTableQA, id);
        assertEquals(name, dbName);
        this.cache.deleteNamesTableEntry(this.cache.namesTableQA, id);
    }

    @Test
    void shouldUpdateExistingEntryAtNamesTable() {
        String id = "2221";
        String name = "carlo";
        this.cache.mapUsername(this.cache.namesTableQA, id, name);
        String newName = "marlo";
        this.cache.mapUsername(this.cache.namesTableQA, id, newName);
        String dbName = this.cache.getUsernameFromId(this.cache.namesTableQA, id);
        assertEquals(newName, dbName);
    }

    @Test
    void shouldDeleteNamesTableEntry() {
        String id = "3332";
        String name = "ingo";
        this.cache.mapUsername(this.cache.namesTableQA, id, name);
        this.cache.deleteNamesTableEntry(this.cache.namesTableQA, id);
        String dbName = this.cache.getUsernameFromId(this.cache.namesTableQA, id);
        assertEquals(null, dbName);
    }

    @Test
    void shouldCreateNewEntryAtScoresTable() {
        String id = "4443";
        this.cache.updateUserScore(this.cache.scoreTableQA, id);
        int dbScore = this.cache.getScoreFromId(this.cache.scoreTableQA, id);
        assertEquals(1, dbScore);
        this.cache.deleteUserScore(this.cache.scoreTableQA, id);
    }

    @Test
    void shouldUpdateExistingEntryAtScoresTable() {
        String id = "5352";
        int originalScore = this.cache.getScoreFromId(this.cache.scoreTableQA, id);
        this.cache.updateUserScore(this.cache.scoreTableQA, id);
        int dbScore = this.cache.getScoreFromId(this.cache.scoreTableQA, id);
        assertEquals(originalScore + 1, dbScore);
    }

    @Test
    void shouldNotBreakWhenRetrievingEmptyScore() {
        String id = "6261";
        boolean passed = false;
        this.cache.getScoreFromId(this.cache.scoreTableQA, id);
        passed = true;
        assertEquals(true, passed);
    }
}