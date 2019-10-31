package me.tade.tntrun;

import me.tade.tntrun.utils.FileStats;
import me.tade.tntrun.utils.MySQL.StatsType;
import org.bukkit.entity.Player;

public class TNTRunAPI {

    public static int getStats(Player p, StatsType type) {
        if (TNTRun.get().getSql() != null) {
            return TNTRun.get().getSql().getValue(p, type);
        } else {
            return FileStats.get(p, type);
        }
    }
}
