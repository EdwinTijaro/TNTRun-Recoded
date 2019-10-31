package me.tade.tntrun.utils;

import me.tade.tntrun.TNTRun;
import me.tade.tntrun.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class PlayerBoard {

    private Scoreboard board;
    private Objective score;
    private Player p;

    private List<Team> teams = new ArrayList<>();
    private HashMap<Team, String> lot = new HashMap<>();
    private List<String> list = new ArrayList<>();
    private List<String> title = new ArrayList<>();
    private List<String> chlist = new ArrayList<>();

    private int index = 15;
    private int titleindex = 0;
    private SType type;
    private Arena a;
    private boolean changing = false;

    public PlayerBoard(Player p, SType type, Arena a) {
        board = Bukkit.getScoreboardManager().getNewScoreboard();
        score = board.registerNewObjective("score", "dummy");
        this.type = type;
        this.a = a;
        create(p, type);
        p.setScoreboard(board);
    }

    public void create(Player p, SType type) {
        this.type = type;
        changing = true;
        removeAll();

        list = TNTRun.get().getConfig().getStringList("scoreboard." + type.name() + ".text");
        title = TNTRun.get().getConfig().getStringList("scoreboard." + type.name() + ".title");

        titleindex = title.size();
        createOP();
        this.p = p;
        score.setDisplaySlot(DisplaySlot.SIDEBAR);
        score.setDisplayName(title.get(0));

        for (String s : list) {
            Team t = board.registerNewTeam("Team:" + index);
            String normal = s;
            s = doReplace(s);
            s = ChatColor.translateAlternateColorCodes('&', s);
            String entry = chlist.get(index - 1);
            t.addEntry(entry);
            String[] ts = splitString(s);
            t.setPrefix(ts[0]);
            t.setSuffix(ts[1]);
            score.getScore(entry).setScore(index);
            teams.add(t);
            lot.put(t, normal);
            index--;
        }
        changing = false;
    }

    private void removeAll() {
        list.clear();
        title.clear();
        chlist.clear();
        titleindex = 0;
        index = 15;
        lot.clear();
        teams.clear();

        if (board != null) {
            for (Team t : board.getTeams()) {
                t.unregister();
            }

            for (String s : new ArrayList<>(board.getEntries())) {
                board.resetScores(s);
            }
        }
    }

    public String doReplace(String string) {
        String s = "";
        s = string.replace("%player%", p.getName())
                .replace("%arena%", a.getName())
                .replace("%players%", a.getPlayers().size() + "")
                .replace("%maxplayers%", a.getMaxPlayers() + "")
                .replace("%spectators%", a.getSpectators().size() + "")
                .replace("%time%", a.getTime() + "")
                .replace("%blocks%", a.getBlcs().containsKey(p) ? a.getBlcs().get(p) + "" : 0 + "");
        return s;
    }

    private void createOP() {
        chlist.add("§1");
        chlist.add("§2");
        chlist.add("§3");
        chlist.add("§4");
        chlist.add("§5");
        chlist.add("§6");
        chlist.add("§7");
        chlist.add("§8");
        chlist.add("§9");
        chlist.add("§a");
        chlist.add("§c");
        chlist.add("§b");
        chlist.add("§d");
        chlist.add("§e");
        chlist.add("§f");
    }

    public void updateText() {
        if (changing) {
            return;
        }
        if (teams.isEmpty()) {
            return;
        }
        for (Team t : teams) {
            try {
                String s = lot.get(t);
                s = doReplace(s);
                s = ChatColor.translateAlternateColorCodes('&', s);
                String[] ts = splitString(s);
                t.setPrefix(ts[0]);
                t.setSuffix(ts[1]);

            } catch (NullPointerException ex) {

            }
        }
    }

    public void updateTitle() {
        if (titleindex > (title.size() - 1)) {
            titleindex = 0;
        }
        score.setDisplayName(maxchars(32, title.get(titleindex)));
        titleindex++;
    }

    public String maxchars(int characters, String string) {
        if (ChatColor.translateAlternateColorCodes('&', string).length() > characters) {
            return string.substring(0, characters);
        }
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    private String getResult(boolean BOLD, boolean ITALIC, boolean MAGIC, boolean STRIKETHROUGH, boolean UNDERLINE, ChatColor color) {
        return ((color != null) && (!color.equals(ChatColor.WHITE)) ? color : "") + "" + (BOLD ? ChatColor.BOLD : "") + (ITALIC ? ChatColor.ITALIC : "") + (MAGIC ? ChatColor.MAGIC : "") + (STRIKETHROUGH ? ChatColor.STRIKETHROUGH : "") + (UNDERLINE ? ChatColor.UNDERLINE : "");
    }

    private String[] splitString(String string) {
        StringBuilder prefix = new StringBuilder(string.substring(0, string.length() >= 16 ? 16 : string.length()));
        StringBuilder suffix = new StringBuilder(string.length() > 16 ? string.substring(16) : "");
        if (prefix.toString().length() > 1 && prefix.charAt(prefix.length() - 1) == '§') {
            prefix.deleteCharAt(prefix.length() - 1);
            suffix.insert(0, '§');
        }
        int length = prefix.length();
        boolean PASSED, UNDERLINE, STRIKETHROUGH, MAGIC, ITALIC;
        boolean BOLD = ITALIC = MAGIC = STRIKETHROUGH = UNDERLINE = PASSED = false;
        ChatColor textColor = null;
        for (int index = length - 1; index > -1; index--) {
            char section = prefix.charAt(index);
            if ((section == '§') && (index < prefix.length() - 1)) {
                char c = prefix.charAt(index + 1);
                ChatColor color = ChatColor.getByChar(c);
                if (color != null) {
                    if (color.equals(ChatColor.RESET)) {
                        break;
                    }
                    if ((textColor == null) && (color.isFormat())) {
                        if ((color.equals(ChatColor.BOLD)) && (!BOLD)) {
                            BOLD = true;
                        } else if ((color.equals(ChatColor.ITALIC)) && (!ITALIC)) {
                            ITALIC = true;
                        } else if ((color.equals(ChatColor.MAGIC)) && (!MAGIC)) {
                            MAGIC = true;
                        } else if ((color.equals(ChatColor.STRIKETHROUGH)) && (!STRIKETHROUGH)) {
                            STRIKETHROUGH = true;
                        } else if ((color.equals(ChatColor.UNDERLINE)) && (!UNDERLINE)) {
                            UNDERLINE = true;
                        }
                    } else if ((textColor == null) && (color.isColor())) {
                        textColor = color;
                    }
                }
            } else if ((index > 0) && (!PASSED)) {
                char c = prefix.charAt(index);
                char c1 = prefix.charAt(index - 1);
                if ((c != '§') && (c1 != '§') && (c != ' ')) {
                    PASSED = true;
                }
            }
            if ((!PASSED) && (prefix.charAt(index) != ' ')) {
                prefix.deleteCharAt(index);
            }
            if (textColor != null) {
                break;
            }
        }
        String result = suffix.toString().isEmpty() ? "" : getResult(BOLD, ITALIC, MAGIC, STRIKETHROUGH, UNDERLINE, textColor);
        if ((!suffix.toString().isEmpty()) && (!suffix.toString().startsWith("§"))) {
            suffix.insert(0, result);
        }
        return new String[]{prefix.toString().length() > 16 ? prefix.toString().substring(0, 16) : prefix.toString(), suffix.toString().length() > 16 ? suffix.toString().substring(0, 16) : suffix.toString()};
    }

    public Player getPlayer() {
        return p;
    }

    public SType getType() {
        return type;
    }

    public enum SType {
        WAITING,
        STARTING,
        PLAYING
    }
}
