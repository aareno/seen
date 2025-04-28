package com.aareno.seen.ui.Anime;

import java.io.Serializable;
import java.util.Date;

public class Anime implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;

    private Date finishedDate;
    private String titleRomaji;
    private String titleEnglish;
    private String coverImageUrl;
    private int watchedEpisodes;

    public Anime(int id, String titleRomaji, String titleEnglish, String coverImageUrl) {
        this.id = id;
        this.titleRomaji = titleRomaji != null ? titleRomaji : "";
        this.titleEnglish = titleEnglish != null ? titleEnglish : "";
        this.coverImageUrl = coverImageUrl != null ? coverImageUrl : "";
        this.watchedEpisodes = 0;
    }

    public int getId() {
        return id;
    }

    public String getTitleRomaji() {
        return titleRomaji != null ? titleRomaji : "";
    }

    public String getTitleEnglish() {
        return titleEnglish != null ? titleEnglish : "";
    }

    public String getCoverImageUrl() {
        return coverImageUrl != null ? coverImageUrl : "";
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
    }

    public Date getFinishedDate() {
        return finishedDate;
    }
}