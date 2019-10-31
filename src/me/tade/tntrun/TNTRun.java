package me.tade.tntrun;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import me.tade.tntrun.arena.Arena;
import me.tade.tntrun.arena.ArenaState;
import me.tade.tntrun.commands.TCommands;
import me.tade.tntrun.items.GameItems;
import me.tade.tntrun.listeners.Listeners;
import me.tade.tntrun.utils.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TNTRun extends JavaPlugin {

    private static TNTRun instance;
    private YamlConfiguration arenasCfg;
    private YamlConfiguration messagesCfg;
    private File are;
    private File msg;
    private List<String> arenas = new ArrayList<>();
    private List<Arena> arens = new ArrayList<>();
    private Sounds sound;
    private MySQL sql;
    private TNTRunType type;
    private HashMap<Location, String> signs = new HashMap<>();
    private List<String> design_signs = new ArrayList<>();
    private HashMap<String, String> string_states = new HashMap<>();
    private boolean fancy_block = true;
    private HashMap<Player, PlayerBoard> boards = new HashMap<>();
    private boolean glass_signs = false;
    private boolean loaded = false;
    private Arena b_arena = null;
    private boolean kits = false;
    private GameItems items;
    private Economy econ = null;
    private HashMap<Integer, CountdownTitle> countdownTitles = new HashMap<>();
    private List<String> allowedCommands = new ArrayList<>();
    private HashMap<Player, Selection> selection = new HashMap<>();
    private static WorldEditPlugin we = null;
    private PluginUpdater pluginUpdater;

    public static TNTRun get() {
        return instance;
    }

    public Block getAttachedBlock(Block b) {
        MaterialData m = b.getState().getData();
        BlockFace face = BlockFace.DOWN;
        if (m instanceof Attachable) {
            face = ((Attachable) m).getAttachedFace();
        }
        return b.getRelative(face);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;
        loaded = false;
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("Loading depends...");

        if (Bukkit.getPluginManager().getPlugin("TNTRun-Addon-Kits") != null) {
            getLogger().info("TNTRun Addon Kits Hooked");
            kits = true;
        }

        Plugin p = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (p != null) {
            we = (WorldEditPlugin) p;
            getLogger().info("WorldEdit found! You can use WE selection instead of built in");
        }

        if (setupEconomy()) {
            getLogger().info("Vault Hooked");
        } else {
            getLogger().info("Vault not found!");
        }

        getLogger().info("Loading bStats... https://bstats.org/plugin/bukkit/TNTRun");
        Metrics mcs = new Metrics(this);
        mcs.addCustomChart(new Metrics.SingleLineChart("players_playing") {
            @Override
            public int getValue() {
                int players = 0;
                for (Arena a : arens) {
                    players += a.getPlayers().size();
                }
                return players;
            }
        });

        getLogger().info("Loading config.yml...");
        getConfig().options().copyDefaults(true);

        pluginUpdater = new PluginUpdater(this);

        saveConfig();

        type = getConfig().getBoolean("bungeemode.enabled", false) ? TNTRunType.BUNGEEARENA : TNTRunType.MULTIARENA;

        getLogger().info("Server type is now " + type.name());

        getLogger().info("Loading folders and files...");
        items = new GameItems();

        new File(this.getDataFolder().getAbsolutePath() + "/data").mkdir();
        new File(this.getDataFolder().getAbsolutePath() + "/saves").mkdir();
        try {
            msg = new File("plugins/TNTRun/messages.yml");
            if (!msg.exists()) {
                msg.createNewFile();
            }
            getLogger().info("Loading messages.yml...");
            messagesCfg = YamlConfiguration.loadConfiguration(msg);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[TNTRun] An error was occured while creating files! Disabling... ");
            getPluginLoader().disablePlugin(get());
            return;
        }

        getLogger().info("Loading database...");
        if (getConfig().getBoolean("mysql.use")) {
            String host = getConfig().getString("mysql.host");
            Integer port = getConfig().getInt("mysql.port");
            String name = getConfig().getString("mysql.name");
            String user = getConfig().getString("mysql.user");
            String pass = getConfig().getString("mysql.password");
            sql = new MySQL(host, port, name, user, pass);

            sql.query("CREATE TABLE IF NOT EXISTS `tntrun_stats` ( `username` varchar(50) NOT NULL, "
                    + "`loses` int(16) NOT NULL, `victories` int(16) NOT NULL, `blocks_destroyed` int(16) NOT NULL, `played` int(16) NOT NULL, "
                    + "UNIQUE KEY `username` (`username`) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;");
            getLogger().info("SQL database loaded");
        } else {
            getLogger().info("Local file database loaded");
        }

        getLogger().info("Loading design of signs...");
        design_signs = getConfig().getStringList("design.signs");

        for (String s : getConfig().getConfigurationSection("design.state").getKeys(false)) {
            string_states.put(s, getConfig().getString("design.state." + s).replace("&", "§"));
        }

        getLogger().info("Loading sounds...");
        String version = Bukkit.getBukkitVersion().split("-")[0];
        sound = new Sounds_1_13();

        getLogger().info("Loading commands...");
        getCommand("tntrun").setExecutor(new TCommands());

        getLogger().info("Loading allowed commands...");
        for (String s : getConfig().getStringList("allowedCommands")) {
            allowedCommands.add(s);
        }

        getLogger().info("Registering listeners...");
        Bukkit.getPluginManager().registerEvents(new Listeners(), this);

        getLogger().info("Loading signs...");
        if (getConfig().getConfigurationSection("signs") != null) {
            for (String s : getConfig().getConfigurationSection("signs").getKeys(false)) {
                for (String a : getConfig().getConfigurationSection("signs." + s).getKeys(false)) {
                    signs.put((Location) getConfig().get("signs." + s + "." + a), s);
                }
            }
        }

        fancy_block = getConfig().getBoolean("design.fancy_blocks", true);
        glass_signs = getConfig().getBoolean("design.glass_signs", false);

        //postAnonymousData();
        getLogger().info("Loading messages...");
        Messages.load();

        getLogger().info("Loading countdown messages...");
        for (String s : getConfig().getConfigurationSection("countdown").getKeys(false)) {
            int i = Integer.parseInt(s);
            String title = getConfig().getString("countdown." + s + ".title");
            String subtitle = getConfig().getString("countdown." + s + ".subtitle");

            int fadeIn = getConfig().getInt("countdown." + s + ".fadeIn");
            int stay = getConfig().getInt("countdown." + s + ".stay");
            int fadeOut = getConfig().getInt("countdown." + s + ".fadeOut");

            countdownTitles.put(i, new CountdownTitle(title, subtitle, fadeIn, stay, fadeOut));
        }

        try {
            are = new File("plugins/TNTRun/arenas.yml");
            msg = new File("plugins/TNTRun/messages.yml");
            if (!are.exists()) {
                are.createNewFile();
            }
            getLogger().info("Loading arenas.yml...");
            arenasCfg = YamlConfiguration.loadConfiguration(are);
            if (!msg.exists()) {
                msg.createNewFile();
            }
            getLogger().info("Loading messages.yml...");
            messagesCfg = YamlConfiguration.loadConfiguration(msg);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[TNTRun] An error was occured while creating files! Disabling... ");
            getPluginLoader().disablePlugin(get());
            return;
        }
        getLogger().info("Loading arenas...");
        for (String s : arenasCfg.getConfigurationSection("").getKeys(false)) {
            getLogger().info("Loading arena '" + s + "'");
            Arena a = new Arena(s);
            arens.add(a);
            arenas.add(s);
            getLogger().info("Arena '" + s + "' loaded");
        }
        if (arens.size() > 0 && type == TNTRunType.BUNGEEARENA) {
            b_arena = TNTRun.get().getConfig().getString("bungeemode.maps.prefered", "NORMAL").equalsIgnoreCase("normal") ? TNTRun.get().arens.get(0) : TNTRun.get().arens.get(new Random().nextInt(TNTRun.get().arens.size()));
        }
        loaded = true;

        getLogger().info("Starting task...");
        if (type == TNTRunType.MULTIARENA) {
            new BukkitRunnable() {
                public void run() {
                    for (Location loc : signs.keySet()) {
                        Block b = loc.getBlock();
                        String arena = signs.get(loc);

                        if (TNTRun.get().getArena(arena) == null) {
                            continue;
                        }

                        Arena a = TNTRun.get().getArena(arena);

                        if (a.isRegenerated()) {
                            if (a.getState() == ArenaState.ENDING) {
                                a.setState(ArenaState.WAITING);
                            }
                        }

                        if (!a.isRegenerating()) {
                            if (a.getState() == ArenaState.WAITING) {
                                a.regen();
                            }
                        }

                        if (b.getState() instanceof Sign) {
                            Sign si = (Sign) b.getState();
                            if (glass_signs) {
                                if (b.getType() == Material.OAK_WALL_SIGN || b.getType() == Material.ACACIA_WALL_SIGN
                                        || b.getType() == Material.BIRCH_WALL_SIGN || b.getType() == Material.DARK_OAK_WALL_SIGN
                                        || b.getType() == Material.JUNGLE_WALL_SIGN || b.getType() == Material.SPRUCE_WALL_SIGN) {
                                    Block at = getAttachedBlock(b);
                                    if (a.getState() == ArenaState.WAITING) {
                                        for (Player p : Bukkit.getOnlinePlayers()) {
                                            p.sendBlockChange(at.getLocation(), a.getMaterialById(95), (byte) 5);
                                        }
                                    } else if (a.getState() == ArenaState.STARTING) {
                                        for (Player p : Bukkit.getOnlinePlayers()) {
                                            p.sendBlockChange(at.getLocation(), a.getMaterialById(95), (byte) 4);
                                        }
                                    } else {
                                        for (Player p : Bukkit.getOnlinePlayers()) {
                                            p.sendBlockChange(at.getLocation(), a.getMaterialById(95), (byte) 14);
                                        }
                                    }
                                }
                            }
                            int line = 0;
                            for (String s : design_signs) {
                                if (line > 3) {
                                    return;
                                }
                                si.setLine(line, s.replace("&", "§")
                                        .replace("%arena%", arena)
                                        .replace("%max%", TNTRun.get().getArena(arena).getMaxPlayers() + "")
                                        .replace("%players%", TNTRun.get().getArena(arena).getPlayers().size() + "")
                                        .replace("%status%", getState(TNTRun.get().getArena(arena))));
                                line++;
                                si.update();
                            }
                        }
                    }
                }
            }.runTaskTimer(get(), 20, 5);
        }

        new BukkitRunnable() {
            public void run() {
                for (PlayerBoard b : boards.values()) {
                    b.updateText();
                }
            }
        }.runTaskTimer(get(), 0, getConfig().getInt("scoreboard.settings.textUpdate"));

        new BukkitRunnable() {
            public void run() {
                for (PlayerBoard b : boards.values()) {
                    b.updateTitle();
                }
            }
        }.runTaskTimer(get(), 0, getConfig().getInt("scoreboard.settings.titleUpdate"));
        loaded = true;
        long endTime = System.currentTimeMillis();
        long fulltime = (endTime - startTime);
        getLogger().info("Done in " + fulltime + "ms");
    }

    @Override
    public void onDisable() {
        Messages.load();
        for (Arena a : arens) {
            for (Player p : new ArrayList<>(a.getPlayers())) {
                a.leave(p);
            }
            for (Player p : new ArrayList<>(a.getSpectators())) {
                a.leave(p);
            }
        }
    }

    public void sendUpdateMessage() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOp() || player.hasPermission("tntrun.update,info")) {
                        player.sendMessage(" ");
                        player.sendMessage("§a§lTNTRun §6A new update has come! Released on §a" + pluginUpdater.getUpdateInfo()[1]);
                        player.sendMessage("§a§lTNTRun §6New version number/your current version §a" + pluginUpdater.getUpdateInfo()[0] + "§7/§c" + getDescription().getVersion());
                        player.sendMessage("§a§lTNTRun §6Download update here: §ahttps://www.spigotmc.org/resources/7320/");
                    }
                }
            }
        }.runTaskLater(this, 30 * 20);
    }

    public void saveArenas() {
        try {
            arenasCfg.save(are);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveMessages() {
        try {
            messagesCfg.save(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Arena getArena(String name) {
        for (Arena a : arens) {
            if (a.getName().equalsIgnoreCase(name)) {
                return a;
            }
        }
        return null;
    }

    public String getState(Arena a) {
        return string_states.get(a.getState().name());
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public enum TNTRunType {
        MULTIARENA,
        BUNGEEARENA
    }

    public YamlConfiguration getArenasCfg() {
        return arenasCfg;
    }

    public YamlConfiguration getMessagesCfg() {
        return messagesCfg;
    }

    public File getMsg() {
        return msg;
    }

    public List<String> getArenas() {
        return arenas;
    }

    public List<Arena> getArens() {
        return arens;
    }

    public Sounds getSound() {
        return sound;
    }

    public MySQL getSql() {
        return sql;
    }

    public TNTRunType getType() {
        return type;
    }

    public HashMap<Location, String> getSigns() {
        return signs;
    }

    public boolean isFancy_block() {
        return fancy_block;
    }

    public HashMap<Player, PlayerBoard> getBoards() {
        return boards;
    }

    public Arena getB_arena() {
        return b_arena;
    }

    public boolean isKits() {
        return kits;
    }

    public GameItems getItems() {
        return items;
    }

    public Economy getEcon() {
        return econ;
    }

    public HashMap<Integer, CountdownTitle> getCountdownTitles() {
        return countdownTitles;
    }

    public List<String> getAllowedCommands() {
        return allowedCommands;
    }

    public HashMap<Player, Selection> getSelection() {
        return selection;
    }

    public void setMessagesCfg(YamlConfiguration messagesCfg) {
        this.messagesCfg = messagesCfg;
    }

    public boolean needUpdate() {
        return pluginUpdater.needUpdate();
    }

    public static WorldEditPlugin getWorldEdit() {
        return we;
    }

    public class CountdownTitle {

        private final String title, subtitle;
        private final int fadeIn, stay, fadeOut;

        public CountdownTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            this.title = title.replace("&", "§");
            this.subtitle = subtitle.replace("&", "§");
            this.fadeIn = fadeIn;
            this.stay = stay;
            this.fadeOut = fadeOut;
        }

        public String getTitle() {
            return title;
        }

        public String getSubTitle() {
            return subtitle;
        }

        public int getFadeIn() {
            return fadeIn;
        }

        public int getStay() {
            return stay;
        }

        public int getFadeOut() {
            return fadeOut;
        }
    }
}
