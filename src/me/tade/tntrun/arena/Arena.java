package me.tade.tntrun.arena;

import me.tade.kits.Kits;
import me.tade.tntrun.TNTRun;
import me.tade.tntrun.TNTRun.TNTRunType;
import me.tade.tntrun.events.TNTRunPlayerJoinEvent;
import me.tade.tntrun.events.TNTRunPlayerLeaveEvent;
import me.tade.tntrun.events.TNTRunPlayerLoseEvent;
import me.tade.tntrun.events.TNTRunPlayerWinEvent;
import me.tade.tntrun.utils.*;
import me.tade.tntrun.utils.MySQL.StatsType;
import me.tade.tntrun.utils.PlayerBoard.SType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Arena {

    private double ADD = 0.3;
    private String name;
    private List<Player> players = new ArrayList<>();
    private List<Player> spectators = new ArrayList<>();
    private int minPlayers = 4;
    private int maxPlayers = 16;
    private int time = 60;
    private Location spawn;
    private Location specs;
    private ArenaState state;
    private Location Amin;
    private Location Amax;
    private Location Lmin;
    private Location Lmax;
    private HashMap<Player, Integer> blcs = new HashMap<>();
    private List<Location> dLocs = new ArrayList<>();
    private boolean regenerated = true;
    private boolean regenerating = false;
    private boolean force = false;
    private List<RunBlock> regenBlocks = new ArrayList<>();
    private List<Block> waiting = new ArrayList<>();
    private int ticksToDestroy = 40;

    public Arena(final String name) {
        this.name = name;

        state = ArenaState.WAITING;

        if (!TNTRun.get().getArenasCfg().getConfigurationSection("").getKeys(false).contains(name)) {
            return;
        }

        spawn = (Location) TNTRun.get().getArenasCfg().get(name + ".spawn");
        specs = (Location) TNTRun.get().getArenasCfg().get(name + ".spectate");
        minPlayers = TNTRun.get().getArenasCfg().getInt(name + ".minPlayers");
        maxPlayers = TNTRun.get().getArenasCfg().getInt(name + ".maxPlayers");
        time = TNTRun.get().getArenasCfg().getInt(name + ".startTime");
        Amin = (Location) TNTRun.get().getArenasCfg().get(name + ".bounds.min");
        Amax = (Location) TNTRun.get().getArenasCfg().get(name + ".bounds.max");
        Lmin = (Location) TNTRun.get().getArenasCfg().get(name + ".loselevel.min");
        Lmax = (Location) TNTRun.get().getArenasCfg().get(name + ".loselevel.max");
        timer();
        regenerating = false;
        regenerated = false;
        getAllBlocks();
        regen();
    }

    public static FireworkEffect getRandomFireworkEffect() {
        Random r = new Random();
        FireworkEffect.Builder builder = FireworkEffect.builder();
        return builder.flicker(false)
                .trail(false)
                .with(FireworkEffect.Type.BALL)
                .withColor(Color.fromRGB(r.nextInt(255), r.nextInt(255), r.nextInt(255)))
                .withFade(Color.fromRGB(r.nextInt(255), r.nextInt(255), r.nextInt(255))).build();
    }

    public static void spawnRandomFirework(Location location) {
        if (location == null) {
            return;
        }
        try {
            final Firework f = location.getWorld().spawn(location, Firework.class);

            FireworkMeta fm = f.getFireworkMeta();
            fm.addEffect(getRandomFireworkEffect());
            fm.setPower(1);
            f.setFireworkMeta(fm);
        } catch (NullPointerException ex) {

        }
    }

    public void setMaxPlayers(int i) {
        maxPlayers = i;
    }

    public void setMinPlayers(int i) {
        minPlayers = i;
    }

    public void setStartTime(int i) {
        time = i;
    }

    public void joinArena(Player p) {
        TNTRunPlayerJoinEvent e = new TNTRunPlayerJoinEvent(p, this);
        Bukkit.getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            return;
        }
        if (regenBlocks.isEmpty()) {
            p.sendMessage("§c§lTNTRun §aPlease finish the arena to save all blocks!");
            return;
        }
        if (players.size() >= maxPlayers) {
            p.sendMessage(Messages.ARENA_FULL.replace("&", "§"));
            return;
        }
        if(!isRegenerated()){
            return;
        }
        if (!players.contains(p) && !spectators.contains(p)) {
            if (state == ArenaState.ENDING) {
                return;
            }
            if (state == ArenaState.PLAYING) {
                if (specs != null) {
                    Utils.saveAll(p);
                    spectators.add(p);
                    p.teleport(specs);
                    if (TNTRun.get().getConfig().getBoolean("scoreboard.settings.enabled")) {
                        TNTRun.get().getBoards().put(p, new PlayerBoard(p, SType.PLAYING, this));
                    }
                    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 10000, true), true);
                    p.setAllowFlight(true);
                    p.setFlying(true);
                }
            } else {
                if (spawn != null) {
                    Utils.saveAll(p);
                    players.add(p);
                    if (TNTRun.get().getConfig().getBoolean("scoreboard.settings.enabled")) {
                        TNTRun.get().getBoards().put(p, new PlayerBoard(p, SType.WAITING, this));
                    }
                    p.teleport(spawn);
                    for (Player pl : new ArrayList<>(players)) {
                        pl.sendMessage(Messages.ARENA_JOIN.replace("%player%", p.getName()).replace("&", "§"));
                    }
                    TNTRun.get().getItems().giveItems(p);

                    if (TNTRun.get().isKits()) {
                        Kits.get().getMenuListener().getEquipedKit().remove(p);
                    }
                }
            }
        }
    }

    public void leave(final Player p) {
        TNTRun.get().getBoards().remove(p);
        if (players.contains(p)) {
            for (Player pl : new ArrayList<>(players)) {
                pl.sendMessage(Messages.ARENA_LEFT.replace("%player%", p.getName()).replace("&", "§"));
            }
            Utils.restoreAll(p);
            players.remove(p);
        }
        if (spectators.contains(p)) {
            Utils.restoreAll(p);
            spectators.remove(p);
        }
        TNTRunPlayerLeaveEvent ev = new TNTRunPlayerLeaveEvent(p, this);
        Bukkit.getPluginManager().callEvent(ev);

        if (TNTRun.get().getType() == TNTRunType.BUNGEEARENA) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            try {
                out.writeUTF("Connect");
                out.writeUTF(TNTRun.get().getConfig().getStringList("bungeemode.lobbyServers").get(new Random().nextInt(TNTRun.get().getConfig().getStringList("bungeemode.lobbyServers").size())));
            } catch (IOException e) {
                e.printStackTrace();
            }
            p.sendPluginMessage(TNTRun.get(), "BungeeCord", b.toByteArray());

            Bukkit.getScheduler().scheduleSyncDelayedTask(TNTRun.get(), new Runnable() {
                public void run() {
                    if (p.isOnline()) {
                        p.kickPlayer("Game ended!");
                    }
                }
            }, 60L);
        }
    }

    public void setSpawn(Location loc) {
        this.spawn = loc;
    }

    public void getAllBlocks() {
        File dir = new File(TNTRun.get().getDataFolder(), "/saves/" + name + ".tntrun");
        if (!dir.exists()) {
            return;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(dir.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        GZIPInputStream gs = null;
        ObjectInputStream ois = null;
        try {
            gs = new GZIPInputStream(fis);
            ois = new ObjectInputStream(gs);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Object> blocks = null;
        try {
            blocks = (List<Object>) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        try {
            ois.close();
            gs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<RunBlock> runBlocks = new ArrayList<>();

        if(blocks.size() > 0){
            Object block = blocks.get(0);

            if(block instanceof TBlock) {
                for (Object tBlock : blocks)
                    runBlocks.add(new RunBlock(((TBlock) tBlock).w, ((TBlock) tBlock).x, ((TBlock) tBlock).y, ((TBlock) tBlock).z, ((TBlock) tBlock).d, getMaterialById(((TBlock) tBlock).i).name()));

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
                    oos.writeObject(runBlocks);
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
            }else{
                for(Object o : blocks)
                    runBlocks.add((RunBlock) o);
            }
        }

        this.regenBlocks = runBlocks;
    }

    public void regen() {
        if(isRegenerating()){
            return;
        }
        if(isRegenerated())
            return;

        regenerating = true;

        int ticks = 0;
        int blocks = 0;
        final int[] alreadyChanged = {0};
        List<RunBlock> changed = new ArrayList<>();
        for (RunBlock block : regenBlocks) {
            Block block1 = this.spawn.getWorld().getBlockAt(block.x, block.y, block.z);

            if ((block1.getType() == Material.AIR && !block.material.equalsIgnoreCase("AIR")) || (block1.getType() != Material.AIR && block.material.equalsIgnoreCase("AIR")))
                changed.add(block);
        }
        for (RunBlock block : changed) {
            new BukkitRunnable() {
                public void run() {
                    changeBlock(spawn.getWorld().getBlockAt(block.x, block.y, block.z), Material.matchMaterial(block.material), (byte)block.d);

                    alreadyChanged[0]++;
                    if(changed.size() == alreadyChanged[0]){
                        regenerated = true;
                        regenerating = false;

                        System.out.println("[TNTRun - Console] Arena " + getName() + " regenerated " + alreadyChanged[0]);
                    }
                }
            }.runTaskLater(TNTRun.get(), ticks);
            blocks++;

            if (blocks % 30 == 0)
                ticks++;
        }
        if(ticks < 10) {
            regenerating = false;
            regenerated = true;
            System.out.println("[TNTRun - Console] Arena " + getName() + " regenerated " + alreadyChanged[0]);
        }
        System.out.println("[TNTRun - Console] Arena " + getName() + " will be regenerated in " + ticks + " ticks");
    }

    public Material getMaterialById(int id){
        boolean ver13 = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].contains("14");
        for(Material material : Material.values()){
            if(ver13 && material.name().startsWith("LEGACY_")){
                if(material.getId() == id)
                    return material;
            }else{
                if(material.getId() == id)
                    return material;
            }
        }
        return null;
    }

    public void changeBlock(Block block, Material material, byte data) {
        boolean ver13 = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].contains("13");

        if(ver13) {
            CraftBlock block1 = ((CraftBlock)block);
            block1.setTypeAndData(CraftMagicNumbers.getBlock(material, data), false);
            return;
        }


        Chunk chunk = block.getWorld().getChunkAt(block.getLocation().getBlockX() >> 4, block.getLocation().getBlockZ() >> 4);
        try {
            Object blockPosition = null;
            Object chandle = chunk.getClass().getMethod("getHandle").invoke(chunk);
            Object whandle = chunk.getWorld().getClass().getMethod("getHandle").invoke(chunk.getWorld());

            Class<?> blockPos = getNMSClass("BlockPosition");
            try {
                blockPosition = blockPos.getConstructor(new Class[]{Integer.TYPE, Integer.TYPE, Integer.TYPE}).newInstance(block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ());
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            Class[] params = new Class[2];
            params[0] = blockPos;
            params[1] = getNMSClass("IBlockData");
            Method a = chandle.getClass().getMethod("a", params);
            Object Idata = getNMSClass("Block").getMethod("getByCombinedId", Integer.TYPE).invoke(null, Integer.valueOf(material.getId() + (data << 12)));
            a.invoke(chandle, blockPosition, Idata);

            if (Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].contains("8"))
                whandle.getClass().getMethod("notify", blockPos).invoke(whandle, blockPosition);
            else {
                whandle.getClass().getMethod("notify", new Class[]{blockPos, params[1], params[1], Integer.TYPE}).invoke(whandle, blockPosition, Idata, Idata, 2);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public Class<?> getNMSClass(String name) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            return Class.forName("net.minecraft.server." + version + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void timer() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(TNTRun.get(), new Runnable() {
            public void run() {
                if (state == ArenaState.PLAYING) {
                    if (players.size() < 2) {
                        for (Player p : new ArrayList<>(players)) {
                            win(p);
                        }
                        return;
                    }
                    for (Player p : new ArrayList<>(players)) {
                        if (!isInside(p.getLocation(), Amin.toVector(), Amax.toVector())) {
                            death(p);
                            if (specs != null) {
                                players.remove(p);
                                p.teleport(specs);
                                spectators.add(p);
                                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 10000, true), true);
                                p.setAllowFlight(true);
                                p.setFlying(true);
                            } else {
                                leave(p);
                            }
                        }
                        if (players.contains(p) && p.getLocation().getY() <= Lmax.getY()) {
                            death(p);
                            if (specs != null) {
                                players.remove(p);
                                p.teleport(specs);
                                spectators.add(p);
                                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 10000, true), true);
                                p.setAllowFlight(true);
                                p.setFlying(true);
                            } else {
                                leave(p);
                            }
                        }
                    }
                    for (Player p : new ArrayList<>(spectators)) {
                        if (!isInside(p.getLocation(), Amin.toVector(), Amax.toVector())) {
                            p.teleport(specs);
                        }
                    }
                    if (ticksToDestroy > 8) {
                        ticksToDestroy -= 2;
                    }
                    for (final Player p : new ArrayList<>(players)) {
                        int y = p.getLocation().getBlockY() + 1;
                        Block block = null;
                        for (int i = 0; i <= 2; i++) {
                            block = getBlockUnderPlayer(y, p.getLocation());
                            y--;
                            if (block != null) {
                                break;
                            }
                        }
                        if (block != null) {
                            final Block b = block;
                            if (!waiting.contains(b)) {
                                waiting.add(b);
                                new BukkitRunnable() {
                                    public void run() {
                                        if (blcs.containsKey(p)) {
                                            blcs.put(p, blcs.get(p) + 1);
                                        }
                                        waiting.remove(b);
                                        remove(b);
                                    }
                                }.runTaskLater(TNTRun.get(), ticksToDestroy);
                            }
                        }
                    }
                }
            }
        }, 0L, 1L);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(TNTRun.get(), new Runnable() {
            @Override
            public void run() {
                if (state == ArenaState.WAITING || state == ArenaState.STARTING) {
                    for (Player p : new ArrayList<>(players)) {
                        if (!isInside(p.getLocation(), Amin.toVector(), Amax.toVector())) {
                            p.teleport(spawn);
                        }
                    }
                    if (players.size() >= minPlayers || (force && players.size() > 0)) {
                        state = ArenaState.STARTING;
                        for (Player p : new ArrayList<>(players)) {
                            p.setLevel(time);
                            if (TNTRun.get().getConfig().getBoolean("scoreboard.settings.enabled")) {
                                if (TNTRun.get().getBoards().containsKey(p) && TNTRun.get().getBoards().get(p).getType() != SType.STARTING) {
                                    TNTRun.get().getBoards().get(p).create(p, SType.STARTING);
                                }
                            }
                        }
                        if (TNTRun.get().getCountdownTitles().containsKey(time)) {
                            for (Player p : new ArrayList<>(players)) {
                                Titles.sendTitle(p, TNTRun.get().getCountdownTitles().get(time).getFadeIn(),
                                        TNTRun.get().getCountdownTitles().get(time).getStay(),
                                        TNTRun.get().getCountdownTitles().get(time).getFadeOut(),
                                        TNTRun.get().getCountdownTitles().get(time).getTitle().replace("%time%", time + ""),
                                        TNTRun.get().getCountdownTitles().get(time).getSubTitle().replace("%time%", time + ""));
                                if (time != 0) TNTRun.get().getSound().NOTE_PLING(p, 1, 1);
                            }
                        }
                        if (time <= 0) {
                            for (Player p : new ArrayList<>(players)) {
                                if (TNTRun.get().getConfig().getBoolean("scoreboard.settings.enabled")) {
                                    createSB(p);
                                }
                                TNTRun.get().getSound().ENDER_DRAGON(p, 1, 500);
                                if (TNTRun.get().getSql() != null) {
                                    TNTRun.get().getSql().setValue(p, StatsType.PLAYED, TNTRun.get().getSql().getValue(p, StatsType.PLAYED) + 1);
                                } else {
                                    FileStats.save(p, StatsType.PLAYED, 1);
                                }
                                blcs.put(p, 0);
                                if (TNTRun.get().isKits()) {
                                    if (Kits.get().getMenuListener().getEquipedKit().containsKey(p)) {
                                        p.getInventory().addItem(Kits.get().getMenuListener().getEquipedKit().get(p).getItem());
                                    }
                                }
                            }
                            state = ArenaState.PLAYING;
                            time = TNTRun.get().getArenasCfg().getInt(name + ".startTime");
                            return;
                        }
                        time--;
                    } else {
                        state = ArenaState.WAITING;
                        for (Player p : players) {
                            if (TNTRun.get().getConfig().getBoolean("scoreboard.settings.enabled")) {
                                if (TNTRun.get().getBoards().containsKey(p) && TNTRun.get().getBoards().get(p).getType() == SType.STARTING) {
                                    TNTRun.get().getBoards().get(p).create(p, SType.WAITING);
                                }
                            }
                        }
                    }
                }
                if (state == ArenaState.ENDING) {
                    for (Player p : new ArrayList<>(players)) {
                        spawnRandomFirework(p.getEyeLocation());
                    }
                }
            }
        }, 0L, 20L);
    }

    public void createSB(Player p) {
        if (TNTRun.get().getBoards().containsKey(p)) {
            TNTRun.get().getBoards().get(p).create(p, SType.PLAYING);
        } else {
            TNTRun.get().getBoards().put(p, new PlayerBoard(p, SType.PLAYING, this));
        }
    }

    public void death(Player pl) {
        TNTRunPlayerLoseEvent ev = new TNTRunPlayerLoseEvent(pl, this);
        Bukkit.getPluginManager().callEvent(ev);

        if (TNTRun.get().getSql() != null) {
            TNTRun.get().getSql().setValue(pl, StatsType.LOSES, TNTRun.get().getSql().getValue(pl, StatsType.LOSES) + 1);
            if (blcs.containsKey(pl)) {
                TNTRun.get().getSql().setValue(pl, StatsType.BLOCKS_DESTROYED, TNTRun.get().getSql().getValue(pl, StatsType.BLOCKS_DESTROYED) + blcs.get(pl));
            }
        } else {
            FileStats.save(pl, StatsType.LOSES, 1);
            if (blcs.containsKey(pl)) {
                FileStats.save(pl, StatsType.BLOCKS_DESTROYED, blcs.get(pl));
            }
        }
        blcs.remove(pl);

        for (Player p : new ArrayList<>(players)) {
            p.sendMessage(Messages.ARENA_LOST.replace("%player%", pl.getName()).replace("&", "§"));
        }
    }

    public void win(Player p) {
        state = ArenaState.ENDING;

        force = false;

        regenerated = false;

        TNTRunPlayerWinEvent ev = new TNTRunPlayerWinEvent(p, this);
        Bukkit.getPluginManager().callEvent(ev);

        for (Player pa : players) {
            TNTRun.get().getBoards().remove(pa);
        }

        for (Player pa : spectators) {
            TNTRun.get().getBoards().remove(pa);
        }

        Utils.executeWinCommands(p);

        if (TNTRun.get().getSql() != null) {
            TNTRun.get().getSql().setValue(p, StatsType.VICTORIES, TNTRun.get().getSql().getValue(p, StatsType.VICTORIES) + 1);
            if (blcs.containsKey(p)) {
                TNTRun.get().getSql().setValue(p, StatsType.BLOCKS_DESTROYED, TNTRun.get().getSql().getValue(p, StatsType.BLOCKS_DESTROYED) + blcs.get(p));
            }
        } else {
            FileStats.save(p, StatsType.VICTORIES, 1);
            if (blcs.containsKey(p)) {
                FileStats.save(p, StatsType.BLOCKS_DESTROYED, blcs.get(p));
            }
        }
        blcs.remove(p);

        Bukkit.broadcastMessage(Messages.ARENA_WIN.replace("%player%", p.getName()).replace("%arena%", this.name).replace("&", "§"));

        new BukkitRunnable() {
            public void run() {
                for (Player p : new ArrayList<>(players)) {
                    leave(p);
                }
                for (Player p : new ArrayList<>(spectators)) {
                    leave(p);
                }
                players.clear();
                spectators.clear();
                new BukkitRunnable() {
                    public void run() {
                        regen();
                    }
                }.runTaskLater(TNTRun.get(), 20);

                new BukkitRunnable() {
                    public void run() {
                        if (TNTRun.get().getType() == TNTRunType.BUNGEEARENA) {
                            Bukkit.shutdown();
                        } else {
                            state = ArenaState.WAITING;
                        }
                    }
                }.runTaskLater(TNTRun.get(), 3 * 20);
            }
        }.runTaskLater(TNTRun.get(), 10 * 20);

        if (TNTRun.get().getConfig().getBoolean("vault.enabled") && TNTRun.get().getEcon() != null) {
            double amount = TNTRun.get().getConfig().getDouble("vault.win");

            TNTRun.get().getEcon().depositPlayer(p, amount);
        }
    }

    public boolean isInside(Location loc, Vector min, Vector max) {
        boolean x = (loc.getX() >= Math.min(min.getX(), max.getX())) && (loc.getX() <= Math.max(min.getX(), max.getX()));
        boolean y = (loc.getY() >= Math.min(min.getY(), max.getY())) && (loc.getY() <= Math.max(min.getY(), max.getY()));
        boolean z = (loc.getZ() >= Math.min(min.getZ(), max.getZ())) && (loc.getZ() <= Math.max(min.getZ(), max.getZ()));
        return (x) && (y) && (z);
    }

    public void remove(Block b) {
        if (TNTRun.get().isFancy_block()) {
            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType().getId());
        }
        if (b.getType() != Material.AIR) {
            dLocs.add(b.getLocation());
        }
        changeBlock(b, Material.AIR, (byte) 0);
        Block down = b.getRelative(BlockFace.DOWN);
        if (down.getType() != Material.AIR) {
            dLocs.add(down.getLocation());
        }
        changeBlock(down, Material.AIR, (byte) 0);
    }

    private Block getBlockUnderPlayer(int y, Location location) {
        Pos loc = new Pos(location.getX(), y, location.getZ());
        Block b11 = loc.getBlock(location.getWorld(), +ADD, -ADD);
        if (b11.getType() != Material.AIR) {
            return b11;
        }
        Block b12 = loc.getBlock(location.getWorld(), -ADD, +ADD);
        if (b12.getType() != Material.AIR) {
            return b12;
        }
        Block b21 = loc.getBlock(location.getWorld(), +ADD, +ADD);
        if (b21.getType() != Material.AIR) {
            return b21;
        }
        Block b22 = loc.getBlock(location.getWorld(), -ADD, -ADD);
        if (b22.getType() != Material.AIR) {
            return b22;
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Player> getSpectators() {
        return spectators;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getTime() {
        return time;
    }

    public ArenaState getState() {
        return state;
    }

    public HashMap<Player, Integer> getBlcs() {
        return blcs;
    }

    public boolean isRegenerated() {
        return regenerated;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public void setRegenBlocks(List<RunBlock> regenBlocks) {
        this.regenBlocks = regenBlocks;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public boolean isRegenerating() {
        return regenerating;
    }

    private static class Pos {

        private double x;
        private int y;
        private double z;

        public Pos(double x, int y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Block getBlock(World world, double ax, double az) {
            return world.getBlockAt(NumberConversions.floor(x + ax), y, NumberConversions.floor(z + az));
        }

    }

}
