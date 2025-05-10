package com.aareno.seen.ui.TvMovies;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.aareno.seen.data.Anime.DateConverter;
import com.aareno.seen.data.Anime.ListConverter;
import com.aareno.seen.ui.Anime.Anime;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity(tableName = "show")
@TypeConverters({DateConverter.class, ListConverter.class})
public class Show implements Serializable {

    @PrimaryKey
    private int id;
    private Date finishedDate;
    private String titleEnglish;
    private String titleAlt;
    private String coverImageUrl;
    private int watchedEpisodes;
    private boolean isWatching;
    private int episodeCount;

    private List<Integer> airingDays;
    private Date startDate;
    private Date endDate;
    private Anime.AiringStatus airingStatus;
    private boolean isMature;

    public Show() {}

    @Ignore
    public Show(int id, String titleEnglish, String titleAlt, String coverImageUrl, List<Integer> airingDays, Date startDate, Date endDate) {
        this.id = id;
        this.titleEnglish = titleEnglish != null ? titleEnglish : "";
        this.titleAlt = titleAlt != null ? titleAlt : "";
        this.coverImageUrl = coverImageUrl != null ? coverImageUrl : "";
        this.watchedEpisodes = 0;
        this.isWatching = true;
        this.airingDays = airingDays;
        this.startDate = startDate;
        this.endDate = endDate;
        updateAiringStatus();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public void setTitleEnglish(String titleEnglish) { this.titleEnglish = titleEnglish; }
    public String getTitleEnglish() { return titleEnglish; }
    public void setTitleAlt(String titleKorean) { this.titleAlt = titleAlt; }
    public String getTitleAlt() { return titleAlt; }
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
        Show show = (Show) o;
        return id == show.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    // Add new getters and setters
    public List<Integer> getAiringDays() {return airingDays;}

    public void setAiringDays(List<Integer> airingDays) {this.airingDays = airingDays;}

    public Date getStartDate() {
        return startDate;}

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
        updateAiringStatus();
    }
    public Date getEndDate() {return endDate;}

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        updateAiringStatus();
    }

    public Anime.AiringStatus getAiringStatus() {return airingStatus;}

    public void setAiringStatus(Anime.AiringStatus airingStatus) {this.airingStatus = airingStatus;}

    // Method to update airing status
    public void updateAiringStatus() {
        Date currentDate = new Date();

        if (endDate != null && currentDate.after(endDate)) {
            this.airingStatus = Anime.AiringStatus.FINISHED;
        } else if (startDate != null && currentDate.before(startDate)) {
            this.airingStatus = Anime.AiringStatus.NOT_STARTED;
        } else {
            this.airingStatus = Anime.AiringStatus.ONGOING;
        }
    }

    public boolean airsOnDay(int day) {
        return airingDays != null && airingDays.contains(day);
    }

    public boolean isMature() {
        return isMature;
    }

    public void setMature(boolean mature) {
        this.isMature = mature;
    }
}

