package me.tade.tntrun.listeners;

import me.tade.tntrun.TNTRun;
import me.tade.tntrun.TNTRun.TNTRunType;
import me.tade.tntrun.arena.Arena;
import me.tade.tntrun.utils.FileStats;
import me.tade.tntrun.utils.Messages;
import me.tade.tntrun.utils.MySQL.StatsType;
import me.tade.tntrun.utils.Titles;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Listeners implements Listener {

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        if (TNTRun.get().getType() == TNTRunType.BUNGEEARENA && TNTRun.get().getB_arena() != null && TNTRun.get().getB_arena().getPlayers().size() >= TNTRun.get().getB_arena().getMaxPlayers()) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Arena is full!");
        }
    }

    @EventHandler
    public void onPing(ServerListPingEvent e) {
        if (TNTRun.get().getB_arena() != null) {
            e.setMotd(TNTRun.get().getState(TNTRun.get().getB_arena()));
            e.setMaxPlayers(TNTRun.get().getB_arena().getMaxPlayers());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (TNTRun.get().getSql() != null) {
            TNTRun.get().getSql().query("INSERT IGNORE INTO `tntrun_stats` (`username`, `loses`, `victories`, `blocks_destroyed`, `played`) VALUES ('" + p.getName() + "', '0', '0', '0', '0');");
        } else {
            for (StatsType t : StatsType.values()) {
                FileStats.get(p, t);
            }
        }

        if (TNTRun.get().getType() == TNTRunType.BUNGEEARENA) {
            Arena a = TNTRun.get().getB_arena();

            a.joinArena(p);
        }

        if(!p.hasPermission("tntrun.update"))
            return;

        if(!TNTRun.get().needUpdate())
            return;

        TNTRun.get().sendUpdateMessage();
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        Player p = e.getPlayer();

        if (e.getLine(0).equalsIgnoreCase("[TNTRun]")) {
            if (!p.hasPermission("tntrun.sign.create")) {
                p.sendMessage(Messages.NOPERM.replace("&", "§"));
                e.setCancelled(true);
                e.getBlock().breakNaturally();
                return;
            }
            if (TNTRun.get().getArenas().contains(e.getLine(1))) {
                p.sendMessage("§c§lTNTRun §aSign created");
                int nm = 0;
                String arena = e.getLine(1);

                while (TNTRun.get().getConfig().contains("signs." + arena + "." + nm)) {
                    nm++;
                }

                TNTRun.get().getConfig().set("signs." + arena + "." + nm, e.getBlock().getLocation());
                TNTRun.get().saveConfig();

                int line = 0;
                for (String s : TNTRun.get().getConfig().getStringList("design.signs")) {
                    if (line > 3) {
                        return;
                    }
                    e.setLine(line, s.replace("&", "§").replace("%arena%", arena)
                            .replace("%max%", TNTRun.get().getArena(arena).getMaxPlayers() + "")
                            .replace("%players%", TNTRun.get().getArena(arena).getPlayers().size() + "")
                            .replace("%status%", TNTRun.get().getState(TNTRun.get().getArena(arena))));
                    line++;
                }
                TNTRun.get().getSigns().put(e.getBlock().getLocation(), arena);
            } else {
                p.sendMessage("§c§lTNTRun §aArena is invalid");
                e.setCancelled(true);
                e.getBlock().breakNaturally();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        for (Arena a : TNTRun.get().getArens()) {
            if (a.getPlayers().contains(p)) {
                a.leave(p);
            }
            if (a.getSpectators().contains(p)) {
                a.leave(p);
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String[] cmd = e.getMessage().replace("/", "").split(" ");
        Arena a = null;

        for (Arena an : TNTRun.get().getArens()) {
            if (an.getPlayers().contains(p) || an.getSpectators().contains(p)) {
                a = an;
            }
        }

        if (a != null && !TNTRun.get().getAllowedCommands().contains(e.getMessage().replace("/", ""))) {
            if (!p.hasPermission("tntrun.ingame.cmds")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Arena a = null;

        for (Arena an : TNTRun.get().getArens()) {
            if (an.getPlayers().contains(p) || an.getSpectators().contains(p)) {
                a = an;
            }
        }

        if (a != null) {
            if (!p.hasPermission("tntrun.ingame.break")) {
                e.setCancelled(true);
            }
        } else {
            if (!p.hasPermission("tntrun.sign.destroy")) {
                return;
            }
            if (p.isSneaking() && TNTRun.get().getSigns().containsKey(e.getBlock().getLocation())) {
                e.getBlock().breakNaturally();
                String arena = TNTRun.get().getSigns().remove(e.getBlock().getLocation());
                int nm = 0;
                while (TNTRun.get().getConfig().contains("signs." + arena + "." + nm)) {
                    if (TNTRun.get().getConfig().get("signs." + arena + "." + nm).equals(e.getBlock().getLocation())) {
                        TNTRun.get().getConfig().set("signs." + arena + "." + nm, null);
                        p.sendMessage("§c§lTNTRun §aSign destroyed");
                    }
                    nm++;
                }
                TNTRun.get().getConfig().set("signs." + arena, null);
                TNTRun.get().saveConfig();

                int am = 0;
                for (Location loc : TNTRun.get().getSigns().keySet()) {
                    String are = TNTRun.get().getSigns().get(loc);
                    TNTRun.get().getConfig().set("signs." + are + "." + am, loc);
                    am++;
                }
                TNTRun.get().saveConfig();
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Arena a = null;

        for (Arena an : TNTRun.get().getArens()) {
            if (an.getPlayers().contains(p) || an.getSpectators().contains(p)) {
                a = an;
            }
        }

        if (a != null) {
            if (!p.hasPermission("tntrun.ingame.place")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity().getType() != EntityType.PLAYER) {
            return;
        }
        Player p = (Player) e.getEntity();
        Arena a = null;

        for (Arena an : TNTRun.get().getArens()) {
            if (an.getPlayers().contains(p) || an.getSpectators().contains(p)) {
                a = an;
            }
        }

        if (a != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        if (e.getEntity().getType() != EntityType.PLAYER) {
            return;
        }
        Player p = (Player) e.getEntity();
        Arena a = null;

        for (Arena an : TNTRun.get().getArens()) {
            if (an.getPlayers().contains(p) || an.getSpectators().contains(p)) {
                a = an;
            }
        }

        if (a != null) {
            e.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Arena a = null;

        for (Arena an : TNTRun.get().getArens()) {
            if (an.getPlayers().contains(p) || an.getSpectators().contains(p)) {
                a = an;
            }
        }

        if (a != null) {
            e.setCancelled(true);
        } else {
            if (p.isSneaking()) {
                return;
            }
            if (e.getClickedBlock() != null && TNTRun.get().getSigns().containsKey(e.getClickedBlock().getLocation())) {
                String arena = TNTRun.get().getSigns().get(e.getClickedBlock().getLocation());
                e.setCancelled(true);
                if (!p.hasPermission("tntrun.join.signs")) {
                    p.sendMessage(Messages.NOPERM.replace("&", "§"));
                    return;
                }
                Arena ar = TNTRun.get().getArena(arena);
                ar.joinArena(p);
            }
        }
    }
}
