package me.tade.tntrun.commands;

import me.tade.tntrun.TNTRun;
import me.tade.tntrun.TNTRun.TNTRunType;
import me.tade.tntrun.arena.Arena;
import me.tade.tntrun.utils.*;
import me.tade.tntrun.utils.MySQL.StatsType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class TCommands implements CommandExecutor {

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player is needed to perform this command!");
            return true;
        }
        final Player p = (Player) sender;
        if (args.length == 0) {
            p.sendMessage("         §c§lTNTRun");
            p.sendMessage("§aBy §eThe_TadeSK§a, version §e" + TNTRun.get().getDescription().getVersion());
            p.sendMessage("§aDisplay all commands §e/tntrun help");
        } else if (args[0].equalsIgnoreCase("help")) {
            p.sendMessage("         §c§lTNTRun");
            p.sendMessage("§a/tntrun create <Arena Name> - §eCreate a new Arena");
            p.sendMessage("§a/tntrun set spawn <Arena Name> - §eSet Spawn for players in arena");
            p.sendMessage("§a/tntrun set spectate <Arena Name> - §eSet Spawn for spectators in arena");
            p.sendMessage("§a/tntrun set point1 - §eSet point 1 of selection");
            p.sendMessage("§a/tntrun set point2 - §eSet point 2 of selection");
            p.sendMessage("§a/tntrun set bounds <Arena Name> - §eSet Bounds for arena! Before that select them using commands above!");
            p.sendMessage("§a/tntrun set loselevel <Arena Name> - §eSet LoseLevel for arena! Before that select them using commands above!");
            p.sendMessage(" ");
            p.sendMessage("§a/tntrun set time <Arena Name> <Amount> - §eSet start time for arena");
            p.sendMessage("§a/tntrun set minplayers <Arena Name> <Amount> - §eSet minimum players for arena");
            p.sendMessage("§a/tntrun set maxplayers <Arena Name> <Amount> - §eSet maximum players for arena");
            p.sendMessage(" ");
            p.sendMessage("§a/tntrun check <Arena Name> - §eCheck if everything is set!");
            p.sendMessage(" ");
            p.sendMessage("§a/tntrun finish <Arena Name> - §eFinish Arena creation. Save all data and map to arena config!");
            p.sendMessage(" ");
            p.sendMessage("§a/tntrun reload - §eReload config.yml and messages.yml");
            p.sendMessage(" ");
            p.sendMessage("§a/tntrun join <Arena Name> - §eJoin to Arena");
            p.sendMessage("§a/tntrun leave - §eLeave Arena");
            p.sendMessage("§a/tntrun stats - §eShow yours Stats");
            p.sendMessage("§a/tntrun force <Arena Name> - §eForce start arena");
        } else if (args[0].equalsIgnoreCase("stats")) {
            if (TNTRun.get().getSql() != null) {
                for (String s : TNTRun.get().getConfig().getStringList("stats.message")) {
                    p.sendMessage(s.replace("&", "§")
                            .replace("{VICTORIES}", TNTRun.get().getSql().getValue(p, StatsType.VICTORIES) + "")
                            .replace("{LOSES}", TNTRun.get().getSql().getValue(p, StatsType.LOSES) + "")
                            .replace("{PLAYED}", TNTRun.get().getSql().getValue(p, StatsType.PLAYED) + "")
                            .replace("{BLOCKS_DESTROYED}", TNTRun.get().getSql().getValue(p, StatsType.BLOCKS_DESTROYED) + ""));
                }
            } else {
                for (String s : TNTRun.get().getConfig().getStringList("stats.message")) {
                    p.sendMessage(s.replace("&", "§")
                            .replace("{VICTORIES}", FileStats.get(p, StatsType.VICTORIES) + "")
                            .replace("{LOSES}", FileStats.get(p, StatsType.LOSES) + "")
                            .replace("{PLAYED}", FileStats.get(p, StatsType.PLAYED) + "")
                            .replace("{BLOCKS_DESTROYED}", FileStats.get(p, StatsType.BLOCKS_DESTROYED) + ""));
                }
            }
        } else if (args[0].equalsIgnoreCase("create")) {
            if (args.length != 2) {
                p.sendMessage("§c§lTNTRun §aTo less arguments!");
                return true;
            }
            if (!p.hasPermission("tntrun.setup")) {
                p.sendMessage(Messages.NOPERM.replace("&", "§"));
                return true;
            }
            String arena = args[1];
            if (TNTRun.get().getArenas().contains(arena)) {
                p.sendMessage("§c§lTNTRun §aArena already exist!");
                return true;
            }
            Arena a = new Arena(arena);
            TNTRun.get().getArenas().add(arena);
            TNTRun.get().getArens().add(a);
            TNTRun.get().getArenasCfg().set(arena + ".minPlayers", a.getMinPlayers());
            TNTRun.get().getArenasCfg().set(arena + ".maxPlayers", a.getMaxPlayers());
            TNTRun.get().getArenasCfg().set(arena + ".startTime", a.getTime());

            TNTRun.get().saveArenas();

            p.sendMessage("§c§lTNTRun §aArena '" + arena + "' created!");
        } else if (args[0].equalsIgnoreCase("join")) {
            if (TNTRun.get().getType() == TNTRunType.BUNGEEARENA) {
                p.sendMessage("§c§lTNTRun §aJoining to arena is disabled in Bungee Mode!");
                return true;
            }
            if (args.length != 2) {
                p.sendMessage("§c§lTNTRun §aTo less arguments!");
                return true;
            }
            String arena = args[1];
            Arena a = TNTRun.get().getArena(arena);

            if (a == null) {
                p.sendMessage("§c§lTNTRun §aArena is invalid!");
                return true;
            }
            for (Arena aa : TNTRun.get().getArens()) {
                if (aa.getPlayers().contains(p)) {
                    return true;
                }
            }
            a.joinArena(p);
        } else if (args[0].equalsIgnoreCase("force")) {
            if (!p.hasPermission("tntrun.force")) {
                p.sendMessage(Messages.NOPERM.replace("&", "§"));
                return true;
            }
            if (args.length != 2) {
                p.sendMessage("§c§lTNTRun §aTo less arguments!");
                return true;
            }
            String arena = args[1];
            Arena a = TNTRun.get().getArena(arena);

            if (a == null) {
                p.sendMessage("§c§lTNTRun §aArena is invalid!");
                return true;
            }
            a.setForce(true);
            p.sendMessage("§c§lTNTRun §aYou force start arena!");
        } else if (args[0].equalsIgnoreCase("leave")) {
            for (Arena a : TNTRun.get().getArens()) {
                if (a.getPlayers().contains(p)) {
                    a.leave(p);
                }
                if (a.getSpectators().contains(p)) {
                    a.leave(p);
                }
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("tntrun.setup")) {
                p.sendMessage(Messages.NOPERM.replace("&", "§"));
                return true;
            }
            TNTRun.get().reloadConfig();
            p.sendMessage("§c§lTNTRun §aConfig reloaded!");
            TNTRun.get().saveConfig();

            TNTRun.get().setMessagesCfg(YamlConfiguration.loadConfiguration(TNTRun.get().getMsg()));
            Messages.load();
            p.sendMessage("§c§lTNTRun §aMessages reloaded!");
            TNTRun.get().getItems().getGiveAfterx().clear();
            TNTRun.get().getItems().getItems().clear();
            TNTRun.get().getItems().getSlots().clear();

            TNTRun.get().getItems().init();
            p.sendMessage("§c§lTNTRun §aGameItems reloaded!");
        } else if (args[0].equalsIgnoreCase("check")) {
            if (args.length != 2) {
                p.sendMessage("§c§lTNTRun §aTo less arguments!");
                return true;
            }
            if (!p.hasPermission("tntrun.setup")) {
                p.sendMessage(Messages.NOPERM.replace("&", "§"));
                return true;
            }
            String arena = args[1];
            Arena a = TNTRun.get().getArena(arena);

            if (a == null) {
                p.sendMessage("§c§lTNTRun §aArena is invalid!");
                return true;
            }
            p.sendMessage("§c§lTNTRun §aChecking arena " + arena);

            if (TNTRun.get().getArenasCfg().get(arena + ".spawn", null) == null) {
                p.sendMessage("§c§lTNTRun §cSpawn is not set!");
            } else {
                p.sendMessage("§c§lTNTRun §aSpawn is set!");
            }

            Location bmax = (Location) TNTRun.get().getArenasCfg().get(arena + ".bounds.max", null);
            Location bmin = (Location) TNTRun.get().getArenasCfg().get(arena + ".bounds.min", null);

            if (bmax == null || bmin == null) {
                p.sendMessage("§c§lTNTRun §cBounds are not set!");
            } else {
                p.sendMessage("§c§lTNTRun §aBounds are set!");
            }

            Location lmax = (Location) TNTRun.get().getArenasCfg().get(arena + ".loselevel.max", null);
            Location lmin = (Location) TNTRun.get().getArenasCfg().get(arena + ".loselevel.min", null);

            if (lmax == null || lmin == null) {
                p.sendMessage("§c§lTNTRun §cLoseLevel is not set!");
            } else {
                p.sendMessage("§c§lTNTRun §aLoseLevel is set!");
            }

            File schematic = new File(TNTRun.get().getDataFolder(), "/saves/" + arena + ".tntrun");
            if (!schematic.exists()) {
                p.sendMessage("§c§lTNTRun §cArena blocks are not saved! Finish arena!");
            } else {
                p.sendMessage("§c§lTNTRun §aArena blocks saved!");
            }
            p.sendMessage("§c§lTNTRun §aCheck ended for arena " + arena);
        } else if (args[0].equalsIgnoreCase("finish")) {
            if (args.length != 2) {
                p.sendMessage("§c§lTNTRun §aTo less arguments!");
                return true;
            }
            if (!p.hasPermission("tntrun.setup")) {
                p.sendMessage(Messages.NOPERM.replace("&", "§"));
                return true;
            }
            final String arena = args[1];
            final Arena a = TNTRun.get().getArena(arena);

            if (a == null) {
                p.sendMessage("§c§lTNTRun §aArena is invalid!");
                return true;
            }

            Location bmax = (Location) TNTRun.get().getArenasCfg().get(arena + ".bounds.max", null);
            Location bmin = (Location) TNTRun.get().getArenasCfg().get(arena + ".bounds.min", null);

            if (bmax == null || bmin == null) {
                p.sendMessage("§c§lTNTRun §aSelections are wrong! Set bounds!");
                return true;
            }

            Location spawn = (Location) TNTRun.get().getArenasCfg().get(arena + ".spawn", null);

            if (spawn == null) {
                p.sendMessage("§c§lTNTRun §aSpawn is not set!");
                p.sendMessage("§c§lTNTRun §aUse command /tntrun set spawn <Arena>  - to set spawn!");
                return true;
            }

            Location lmax = (Location) TNTRun.get().getArenasCfg().get(arena + ".loselevel.max", null);
            Location lmin = (Location) TNTRun.get().getArenasCfg().get(arena + ".loselevel.min", null);

            if (lmax == null || lmin == null) {
                p.sendMessage("§c§lTNTRun §aBounds are wrong!");
                p.sendMessage("§c§lTNTRun §aUse command /tntrun set loselevel <Arena>  - to set LoseLevel!");
                return true;
            }

            int topBlockX = (bmax.getBlockX() < bmin.getBlockX() ? bmin.getBlockX() : bmax.getBlockX());
            int bottomBlockX = (bmax.getBlockX() > bmin.getBlockX() ? bmin.getBlockX() : bmax.getBlockX());

            int topBlockY = (bmax.getBlockY() < bmin.getBlockY() ? bmin.getBlockY() : bmax.getBlockY());
            int bottomBlockY = (bmax.getBlockY() > bmin.getBlockY() ? bmin.getBlockY() : bmax.getBlockY());

            int topBlockZ = (bmax.getBlockZ() < bmin.getBlockZ() ? bmin.getBlockZ() : bmax.getBlockZ());
            int bottomBlockZ = (bmax.getBlockZ() > bmin.getBlockZ() ? bmin.getBlockZ() : bmax.getBlockZ());

            List<RunBlock> blocks = new ArrayList<>();
            for (int x = bottomBlockX; x <= topBlockX; x++) {
                for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                    for (int y = bottomBlockY; y <= topBlockY; y++) {
                        Block block = p.getWorld().getBlockAt(x, y, z);

                        blocks.add(new RunBlock(p.getWorld().getName(), x, y, z, block.getData(), block.getType().name()));
                    }
                }
            }

            File dir = new File(TNTRun.get().getDataFolder(), "/saves/" + a.getName() + ".tntrun");
            if (!dir.exists()) {
                try {
                    dir.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(dir.getAbsoluteFile());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            GZIPOutputStream gz = null;
            ObjectOutputStream oos = null;
            try {
                gz = new GZIPOutputStream(fos);
                oos = new
                        ObjectOutputStream(gz);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                oos.writeObject(blocks);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                oos.flush();
                oos.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            a.setRegenBlocks(blocks);

            p.sendMessage("§c§lTNTRun §aSaved everything!");
        } else if (args[0].equalsIgnoreCase("set")) {
            if (args.length > 4) {
                p.sendMessage("§c§lTNTRun §aInvalid usage!");
                p.performCommand("tntrun help");
                return true;
            }
            if (!p.hasPermission("tntrun.setup")) {
                p.sendMessage(Messages.NOPERM.replace("&", "§"));
                return true;
            }
            if (args[1].equalsIgnoreCase("spawn")) {
                final String arena = args[2];
                Arena a = TNTRun.get().getArena(arena);

                if (a == null) {
                    p.sendMessage("§c§lTNTRun §aArena is invalid!");
                    return true;
                }

                a.setSpawn(p.getLocation().add(0, 0.2, 0));

                TNTRun.get().getArenasCfg().set(arena + ".spawn", p.getLocation().add(0, 0.2, 0));

                TNTRun.get().saveArenas();

                p.sendMessage("§c§lTNTRun §aSpawn location for Arena '" + arena + "' set!");
            } else if (args[1].equalsIgnoreCase("spectate")) {
                final String arena = args[2];
                Arena a = TNTRun.get().getArena(arena);

                if (a == null) {
                    p.sendMessage("§c§lTNTRun §aArena is invalid!");
                    return true;
                }

                a.setSpawn(p.getLocation().add(0, 0.2, 0));

                TNTRun.get().getArenasCfg().set(arena + ".spectate", p.getLocation().add(0, 0.2, 0));

                TNTRun.get().saveArenas();

                p.sendMessage("§c§lTNTRun §aSpectate location for Arena '" + arena + "' set!");
            } else if (args[1].equalsIgnoreCase("point1")) {
                Selection psel = TNTRun.get().getSelection().get(p);

                if (psel == null) {
                    TNTRun.get().getSelection().put(p, new Selection(p));
                    psel = TNTRun.get().getSelection().get(p);
                }

                psel.setPoint1(p.getLocation().getBlock().getLocation());

                p.sendMessage("§c§lTNTRun §aSaved point 1!");
            } else if (args[1].equalsIgnoreCase("point2")) {
                Selection psel = TNTRun.get().getSelection().get(p);

                if (psel == null) {
                    TNTRun.get().getSelection().put(p, new Selection(p));
                    psel = TNTRun.get().getSelection().get(p);
                }

                psel.setPoint2(p.getLocation().getBlock().getLocation());

                p.sendMessage("§c§lTNTRun §aSaved point 2!");
            } else if (args[1].equalsIgnoreCase("bounds")) {
                final String arena = args[2];
                Arena a = TNTRun.get().getArena(arena);

                if (a == null) {
                    p.sendMessage("§c§lTNTRun §aArena is invalid!");
                    return true;
                }

                Location minLoc = null, maxLoc = null;

                final Selection psel = TNTRun.get().getSelection().get(p);

                if(TNTRun.get().getWorldEdit() != null){
                    com.sk89q.worldedit.bukkit.selections.Selection weSel = TNTRun.get().getWorldEdit().getSelection(p);
                    if (weSel == null)
                    {
                        p.sendMessage("§c§lTNTRun §aWorldEdit Selection wasn't found, trying built-in selection..");
                    }else{
                        minLoc = weSel.getMinimumPoint();
                        maxLoc = weSel.getMaximumPoint();
                    }
                }

                if (psel == null || !psel.hasSelected()) {
                    if(minLoc == null || maxLoc == null){
                        p.sendMessage("§c§lTNTRun §aSelections are wrong! Use /tntrun set point1/2 or WorldEdit!");
                        return true;
                    }
                }else if(minLoc == null || maxLoc == null){
                    minLoc = psel.getMinPoint();
                    maxLoc = psel.getMaxPoint();
                }

                TNTRun.get().getArenasCfg().set(arena + ".bounds.min", minLoc);
                TNTRun.get().getArenasCfg().set(arena + ".bounds.max", maxLoc);

                TNTRun.get().saveArenas();

                p.sendMessage("§c§lTNTRun §aBounds for Arena '" + arena + "' set!");
            } else if (args[1].equalsIgnoreCase("loselevel")) {
                final String arena = args[2];
                Arena a = TNTRun.get().getArena(arena);

                if (a == null) {
                    p.sendMessage("§c§lTNTRun §aArena is invalid!");
                    return true;
                }

                Location minLoc = null, maxLoc = null;

                final Selection psel = TNTRun.get().getSelection().get(p);

                if(TNTRun.get().getWorldEdit() != null){
                    com.sk89q.worldedit.bukkit.selections.Selection weSel = TNTRun.get().getWorldEdit().getSelection(p);
                    if (weSel == null)
                    {
                        p.sendMessage("§c§lTNTRun §aWorldEdit Selection wasn't found, trying built-in selection..");
                    }else{
                        minLoc = weSel.getMinimumPoint();
                        maxLoc = weSel.getMaximumPoint();
                    }
                }

                if (psel == null || !psel.hasSelected()) {
                    if(minLoc == null || maxLoc == null){
                        p.sendMessage("§c§lTNTRun §aSelections are wrong! Use /tntrun set point1/2 or WorldEdit!");
                        return true;
                    }
                }else if(minLoc == null || maxLoc == null){
                    minLoc = psel.getMinPoint();
                    maxLoc = psel.getMaxPoint();
                }

                boolean same = minLoc.equals(maxLoc);

                TNTRun.get().getArenasCfg().set(arena + ".loselevel.min", !same ? minLoc : minLoc);
                TNTRun.get().getArenasCfg().set(arena + ".loselevel.max", !same ? maxLoc : minLoc);

                TNTRun.get().saveArenas();

                p.sendMessage("§c§lTNTRun §aLoseLevel for Arena '" + arena + "' set!");
            } else if (args[1].equalsIgnoreCase("time")) {
                final String arena = args[2];
                final String amount = args[3];
                final Arena a = TNTRun.get().getArena(arena);

                if (a == null) {
                    p.sendMessage("§c§lTNTRun §aArena is invalid!");
                    return true;
                }

                a.setTime(Integer.parseInt(amount));
                TNTRun.get().getArenasCfg().set(arena + ".startTime", Integer.parseInt(amount));

                TNTRun.get().saveArenas();

                p.sendMessage("§c§lTNTRun §aStart Time for Arena '" + arena + "' set!");
            } else if (args[1].equalsIgnoreCase("minPlayers")) {
                final String arena = args[2];
                final String amount = args[3];
                final Arena a = TNTRun.get().getArena(arena);

                if (a == null) {
                    p.sendMessage("§c§lTNTRun §aArena is invalid!");
                    return true;
                }

                a.setMinPlayers(Integer.parseInt(amount));
                TNTRun.get().getArenasCfg().set(arena + ".minPlayers", Integer.parseInt(amount));

                TNTRun.get().saveArenas();

                p.sendMessage("§c§lTNTRun §aMinimum Players for Arena '" + arena + "' set!");
            } else if (args[1].equalsIgnoreCase("maxPlayers")) {
                final String arena = args[2];
                final String amount = args[3];
                final Arena a = TNTRun.get().getArena(arena);

                if (a == null) {
                    p.sendMessage("§c§lTNTRun §aArena is invalid!");
                    return true;
                }

                a.setMaxPlayers(Integer.parseInt(amount));
                TNTRun.get().getArenasCfg().set(arena + ".maxPlayers", Integer.parseInt(amount));

                TNTRun.get().saveArenas();

                p.sendMessage("§c§lTNTRun §aMaximum Players for Arena '" + arena + "' set!");
            }
        }
        return false;
    }
}
