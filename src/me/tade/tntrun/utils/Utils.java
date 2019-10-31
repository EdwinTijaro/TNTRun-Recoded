package me.tade.tntrun.utils;

import me.tade.tntrun.TNTRun;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Collection;
import java.util.HashMap;

public class Utils {

    private static HashMap<Player, GameMode> gm = new HashMap<>();
    private static HashMap<Player, Float> xp = new HashMap<>();
    private static HashMap<Player, Integer> lvl = new HashMap<>();
    private static HashMap<Player, ItemStack[]> cnt = new HashMap<>();
    private static HashMap<Player, ItemStack[]> arm = new HashMap<>();
    private static HashMap<Player, Integer> fd = new HashMap<>();
    private static HashMap<Player, Double> hah = new HashMap<>();
    private static HashMap<Player, Double> mhah = new HashMap<>();
    private static HashMap<Player, Location> loc = new HashMap<>();
    private static HashMap<Player, Collection<PotionEffect>> pots = new HashMap<>();
    private static HashMap<Player, Scoreboard> sb = new HashMap<>();

    public static void saveAll(Player p) {
        p.leaveVehicle();

        gm.put(p, p.getGameMode());
        xp.put(p, p.getExp());
        lvl.put(p, p.getLevel());
        cnt.put(p, p.getInventory().getContents());
        arm.put(p, p.getInventory().getArmorContents());
        fd.put(p, p.getFoodLevel());
        hah.put(p, p.getHealth());
        mhah.put(p, p.getMaxHealth());
        loc.put(p, p.getLocation());
        pots.put(p, p.getActivePotionEffects());
        sb.put(p, p.getScoreboard());

        for (PotionEffect po : p.getActivePotionEffects()) {
            p.removePotionEffect(po.getType());
        }

        p.setGameMode(GameMode.ADVENTURE);
        p.setLevel(0);
        p.setExp(0);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setAllowFlight(false);
        p.setFlying(false);
    }

    public static void restoreAll(Player p) {
        if (!mhah.containsKey(p)) {
            return;
        }
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        for (PotionEffect po : p.getActivePotionEffects()) {
            p.removePotionEffect(po.getType());
        }

        p.teleport(loc.get(p));

        p.setGameMode(gm.get(p));
        p.setLevel(lvl.get(p));
        p.setExp(xp.get(p));
        if (cnt.get(p) != null) {
            p.getInventory().setContents(cnt.get(p));
        }
        if (arm.get(p) != null) {
            p.getInventory().setArmorContents(arm.get(p));
        }
        p.setFoodLevel(fd.get(p));
        p.setMaxHealth(mhah.get(p));
        p.setHealth(hah.get(p));

        p.addPotionEffects(pots.get(p));
        if (p.isOnline()) {
            if (sb.get(p) != null) {
                p.setScoreboard(sb.get(p));
            } else {
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        }

        gm.remove(p);
        xp.remove(p);
        lvl.remove(p);
        cnt.remove(p);
        arm.remove(p);
        fd.remove(p);
        hah.remove(p);
        mhah.remove(p);
        loc.remove(p);
        pots.remove(p);
        sb.remove(p);
        p.setAllowFlight(false);
        p.setFlying(false);
    }

    public static void executeWinCommands(Player p) {
        for (String s : TNTRun.get().getConfig().getStringList("commands.win")) {
            s = s.replace("%player%", p.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s);
        }
    }
}
