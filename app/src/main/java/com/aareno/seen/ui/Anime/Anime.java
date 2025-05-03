package com.aareno.seen.ui.Anime;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.aareno.seen.data.Anime.DateConverter;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "anime")
@TypeConverters(DateConverter.class)
public class Anime implements Serializable {
    @PrimaryKey
    private int id;
    private Date finishedDate;
    private String titleRomaji;
    private String titleEnglish;
    private String coverImageUrl;
    private int watchedEpisodes;
    private boolean isWatching; // New field to distinguish between watching and watched lists
    private int episodeCount;

    // Default constructor for Room
    public Anime() {}

    public Anime(int id, String titleRomaji, String titleEnglish, String coverImageUrl, int episodeCount) {
        this.id = id;
        this.titleRomaji = titleRomaji != null ? titleRomaji : "";
        this.titleEnglish = titleEnglish != null ? titleEnglish : "";
        this.coverImageUrl = coverImageUrl != null ? coverImageUrl : "";
        this.watchedEpisodes = 0;
        this.isWatching = true; // Default to watching when created
        this.episodeCount = episodeCount;
    }

    // Getters and Setters with null checks
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitleRomaji() {
        return titleRomaji != null ? titleRomaji : "";
    }

    public void setTitleRomaji(String titleRomaji) {
        this.titleRomaji = titleRomaji;
    }

    public String getTitleEnglish() {
        return titleEnglish != null ? titleEnglish : "";
    }

    public void setTitleEnglish(String titleEnglish) {
        this.titleEnglish = titleEnglish;
    }

    public String getCoverImageUrl() {
        return coverImageUrl != null ? coverImageUrl : "";
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public int getWatchedEpisodes() {
        return watchedEpisodes;
    }

    public void setWatchedEpisodes(int watchedEpisodes) {
        this.watchedEpisodes = watchedEpisodes;
    }

    public void incrementEpisodes() {
        this.watchedEpisodes++;
    }

    public void decrementEpisodes() {
        if (this.watchedEpisodes > 0) {
            this.watchedEpisodes--;
        }
    }

    public void markAsFinished() {
        this.finishedDate = new Date(); // Current date
        this.isWatching = false; // Mark as not watching when finished
    }

    public Date getFinishedDate() {
        return finishedDate;
    }

    public void setFinishedDate(Date finishedDate) {
        this.finishedDate = finishedDate;
    }

    public boolean isWatching() {
        return isWatching;
    }

    public void setWatching(boolean watching) {
        isWatching = watching;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    // Equals and HashCode for proper comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Anime anime = (Anime) o;
        return id == anime.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}