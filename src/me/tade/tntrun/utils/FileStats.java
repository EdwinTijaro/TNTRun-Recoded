package me.tade.tntrun.utils;

import me.tade.tntrun.TNTRun;
import me.tade.tntrun.utils.MySQL.StatsType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class FileStats {

    public static void save(Player p, StatsType type, int amount) {
        File f = new File(TNTRun.get().getDataFolder().getAbsolutePath() + "/data/" + p.getUniqueId() + ".yml");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        if (cfg.get("stats." + p.getUniqueId() + "." + type.name()) != null) {
            cfg.set("stats." + p.getUniqueId() + "." + type.name(), (cfg.getInt("stats." + p.getUniqueId() + "." + type.name()) + amount));
        } else {
            cfg.set("stats." + p.getUniqueId() + "." + type.name(), 0);
        }
        try {
            cfg.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int get(Player p, StatsType type) {
        File f = new File(TNTRun.get().getDataFolder().getAbsolutePath() + "/data/" + p.getUniqueId() + ".yml");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        if (cfg.get("stats." + p.getUniqueId() + "." + type.name()) != null) {
            return cfg.getInt("stats." + p.getUniqueId() + "." + type.name());
        } else {
            cfg.set("stats." + p.getUniqueId() + "." + type.name(), 0);
            try {
                cfg.save(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }
}
