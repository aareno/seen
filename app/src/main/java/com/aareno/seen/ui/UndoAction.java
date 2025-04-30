package com.aareno.seen.ui;

import com.aareno.seen.ui.Anime.Anime;

public class UndoAction {
    public enum ActionType {
        ADD_TO_WATCHING,
        ADD_TO_WATCHED,
        REMOVE_FROM_WATCHING,
        REMOVE_FROM_WATCHED,
        MOVE_TO_WATCHED
    }

    private ActionType type;
    private Anime anime;
    private int position;

    public UndoAction(ActionType type, Anime anime, int position) {
        this.type = type;
        this.anime = anime;
        this.position = position;
    }

    public ActionType getType() { return type; }
    public Anime getAnime() { return anime; }
    public int getPosition() { return position; }
}