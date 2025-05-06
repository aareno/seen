package com.aareno.seen.ui.KDrama;

public class UndoAction_kdrama {
    public enum ActionType {
        ADD_TO_WATCHING,
        REMOVE_FROM_WATCHED,
        MOVE_TO_WATCHED,
        REMOVE_FROM_WATCHING,
        ADD_TO_WATCHED
    }

    private ActionType type;
    private KDrama kdrama;
    private int position;

    public UndoAction_kdrama(ActionType type, KDrama kdrama, int position) {
        this.type = type;
        this.kdrama = kdrama;
        this.position = position;
    }

    public ActionType getType() { return type; }
    public KDrama getKdrama() { return kdrama; }
    public int getPosition() { return position; }
}