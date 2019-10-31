package me.tade.tntrun.items;

import me.tade.tntrun.TNTRun;
import me.tade.tntrun.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GameItems implements Listener {

    private File itemsFile;
    private YamlConfiguration itemsCfg;
    private HashMap<ItemStack, String> items = new HashMap<>();
    private HashMap<Integer, ItemStack> slots = new HashMap<>();
    private HashMap<ItemStack, Integer> giveAfterx = new HashMap<>();

    public GameItems() {
        itemsFile = new File("plugins/TNTRun/items.yml");

        if (!itemsFile.exists()) {
            try {
                itemsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        itemsCfg = YamlConfiguration.loadConfiguration(itemsFile);

        Bukkit.getPluginManager().registerEvents(this, TNTRun.get());

        init();
    }

    public void init() {
        boolean ver13 = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].contains("13");
        for (String s : itemsCfg.getConfigurationSection("").getKeys(false)) {
            TNTRun.get().getLogger().info("Found GameItem '" + s + "'");
            try {

                Material mat;
                Object object = itemsCfg.get(s + ".id");
                if(object instanceof Integer) {
                    if(ver13){
                        Bukkit.getLogger().info("Can't parse Integer as Material ID because of Minecraft 1.13+ ! Please find material here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
                        return;
                    }
                    mat = Material.getMaterial((String) object);
                }else
                    mat = Material.getMaterial((String) object);
                byte data = (byte) itemsCfg.getInt(s + ".subid");
                int slot = itemsCfg.getInt(s + ".slot");
                int giveAfter = itemsCfg.getInt(s + ".giveAfter");
                slot = slot - 1;
                String cmd = itemsCfg.getString(s + ".command");
                String name = itemsCfg.getString(s + ".name");
                name = name.replace("&", "§");
                List<String> l = new ArrayList<>();
                for (String a : itemsCfg.getStringList(s + ".lore")) {
                    l.add(a.replace("&", "§"));
                }

                ItemStack item = new ItemStack(mat, 1, data);
                ItemMeta im = item.getItemMeta();
                im.setDisplayName(name);
                im.setLore(l);
                item.setItemMeta(im);

                items.put(item, cmd);
                slots.put(slot, item);
                giveAfterx.put(item, giveAfter);
                TNTRun.get().getLogger().info("Loaded GameItem '" + s + "' with command '" + cmd + "' and material '" + mat.name() + "' will be given after '" + giveAfter + "' seconds!");
            } catch (Exception ex) {
                ex.printStackTrace();
                TNTRun.get().getLogger().info("Can't load GameItem '" + s + "'!");
            }
        }
    }

    public void giveItems(Player p) {
        for (int i : new ArrayList<>(slots.keySet())) {
            ItemStack item = slots.get(i);
            new BukkitRunnable() {
                public void run() {
                    p.getInventory().setItem(i, item);
                    p.updateInventory();
                }
            }.runTaskLater(TNTRun.get(), giveAfterx.get(item) * 20);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if (e.getItem() == null) {
            return;
        }

        if (items.containsKey(e.getItem())) {
            e.setCancelled(true);
            String cmd = items.get(e.getItem());
            if (cmd.contains("tntrun leave")) {
                for (Arena a : TNTRun.get().getArens()) {
                    if (a.getPlayers().contains(p)) {
                        a.leave(p);
                    }
                    if (a.getSpectators().contains(p)) {
                        a.leave(p);
                    }
                }
                return;
            }
            p.performCommand(cmd);
        }
    }

    public HashMap<ItemStack, String> getItems() {
        return items;
    }

    public HashMap<Integer, ItemStack> getSlots() {
        return slots;
    }

    public HashMap<ItemStack, Integer> getGiveAfterx() {
        return giveAfterx;
    }
}
