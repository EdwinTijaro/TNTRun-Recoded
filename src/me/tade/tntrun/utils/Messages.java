package me.tade.tntrun.utils;

import me.tade.tntrun.TNTRun;

public class Messages {

    public static String ARENA_JOIN = "&c&lTNTRun &e%player% &ajoined to the game";
    public static String ARENA_LEFT = "&c&lTNTRun &e%player% &aleft the game";
    public static String ARENA_LOST = "&c&lTNTRun &e%player% &alost the game";
    public static String ARENA_WIN = "&c&lTNTRun &e%player% &awon TNTRun on arena &e%arena%";
    public static String ARENA_FULL = "&c&lTNTRun &eArena is full!";
    public static String NOPERM = "&c&lTNTRun &aNo permission!";

    public static void load() {
        ARENA_JOIN = TNTRun.get().getMessagesCfg().getString("ARENA_JOIN", ARENA_JOIN);
        ARENA_LEFT = TNTRun.get().getMessagesCfg().getString("ARENA_LEFT", ARENA_LEFT);
        ARENA_LOST = TNTRun.get().getMessagesCfg().getString("ARENA_LOST", ARENA_LOST);
        ARENA_WIN = TNTRun.get().getMessagesCfg().getString("ARENA_WIN", ARENA_WIN);
        ARENA_FULL = TNTRun.get().getMessagesCfg().getString("ARENA_FULL", ARENA_FULL);
        NOPERM = TNTRun.get().getMessagesCfg().getString("NOPERM", NOPERM);

        TNTRun.get().saveMessages();
        save();
    }

    private static void save() {
        TNTRun.get().getMessagesCfg().set("ARENA_JOIN", ARENA_JOIN);
        TNTRun.get().getMessagesCfg().set("ARENA_LEFT", ARENA_LEFT);
        TNTRun.get().getMessagesCfg().set("ARENA_LOST", ARENA_LOST);
        TNTRun.get().getMessagesCfg().set("ARENA_WIN", ARENA_WIN);
        TNTRun.get().getMessagesCfg().set("ARENA_FULL", ARENA_FULL);
        TNTRun.get().getMessagesCfg().set("NOPERM", NOPERM);

        TNTRun.get().saveMessages();
    }
}
