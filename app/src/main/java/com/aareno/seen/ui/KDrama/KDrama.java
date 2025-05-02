package com.aareno.seen.ui.KDrama;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.aareno.seen.data.Anime.DateConverter;
import com.aareno.seen.ui.Anime.Anime;

import java.io.Serializable;
import java.util.Date;
@Entity(tableName = "kdrama")
@TypeConverters(DateConverter.class)
public class KDrama implements Serializable {

    @PrimaryKey
    private int id;
    private Date finishedDate;
    private String titleEnglish;
    private String titleKorean;
    private String coverImageUrl;
    private int watchedEpisodes;
    private boolean isWatching;

    public KDrama() {}

    public KDrama(int id, String titleEnglish, String titleKorean, String coverImageUrl) {
        this.id = id;
        this.titleEnglish = titleEnglish != null ? titleEnglish : "";
        this.titleKorean = titleKorean != null ? titleKorean : "";
        this.coverImageUrl = coverImageUrl != null ? coverImageUrl : "";
        this.watchedEpisodes = 0;
        this.isWatching = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public void setTitleEnglish(String titleEnglish) { this.titleEnglish = titleEnglish; }
    public String getTitleEnglish() { return titleEnglish; }
    public void setTitleKorean(String titleKorean) { this.titleKorean = titleKorean; }
    public String getTitleKorean() { return titleKorean; }
    public String getCoverImageUrl() {return coverImageUrl != null ? coverImageUrl : ""; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
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

    // Equals and HashCode for proper comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KDrama kdrama = (KDrama) o;
        return id == kdrama.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
