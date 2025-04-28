package com.aareno.seen.ui.Anime;

import java.io.Serializable;

public class Anime implements Serializable {
    private int id;
    private String titleRomaji;
    private String titleEnglish;
    private String coverImageUrl;
    private int watchedEpisodes; // New field

    public Anime(int id, String titleRomaji, String titleEnglish, String coverImageUrl) {
        this.id = id;
        this.titleRomaji = titleRomaji;
        this.titleEnglish = titleEnglish;
        this.coverImageUrl = coverImageUrl;
        this.watchedEpisodes = 0; // Default to 0
    }

    // Existing getters

    public int getWatchedEpisodes() {
        return watchedEpisodes;
    }

    public String getTitleRomaji() {
        return titleRomaji;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setWatchedEpisodes(int watchedEpisodes) {
        this.watchedEpisodes = watchedEpisodes;
    }

    // Method to increment episodes
    public void incrementEpisodes() {
        this.watchedEpisodes++;
    }

    // Method to decrement episodes
    public void decrementEpisodes() {
        if (this.watchedEpisodes > 0) {
            this.watchedEpisodes--;
        }
    }
}