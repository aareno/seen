package com.aareno.seen.ui.TvMovies;
public class UndoAction_show {
    public enum ActionType {
        ADD_TO_WATCHING,
        REMOVE_FROM_WATCHED,
        MOVE_TO_WATCHED,
        REMOVE_FROM_WATCHING,
        ADD_TO_WATCHED
    }

    private ActionType type;
    private Show show;
    private int position;

    public UndoAction_show(ActionType type, Show show, int position) {
        this.type = type;
        this.show = show;
        this.position = position;
    }

    public ActionType getType() { return type; }
    public Show getShow() { return show; }
    public int getPosition() { return position; }
}
