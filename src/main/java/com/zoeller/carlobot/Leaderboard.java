package com.zoeller.carlobot;

public class Leaderboard {

    private final LeaderboardDAO leaderboardCache;
    private boolean status = false;

    public Leaderboard(String backupFile) {
        leaderboardCache = new LeaderboardDAO(backupFile);
        try {
            leaderboardCache.load();
            status = true;
        } catch (Exception error) {
            System.out.println("Unable to load Leaderboard\nError Message: " + error);
            status = false;
        }
    }

    public Leaderboard() {
        this("");
    }

    /**
     * In case there is something wrong during leaderboard loading, we stop any kind of processing.
     * Use this to check the status.
     * Need to think of some alternative processing to avoid data loss.
     * @return
     */
    public boolean isLeaderboardOperational() {
        return status;
    }

}
