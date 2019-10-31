package me.tade.tntrun.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class Sounds_1_13 extends Sounds {

    @Override
    public void NOTE_PLING(Player p, float volume, float pitch) {
        p.playSound(p.getLocation(), Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"), volume, pitch);
    }

    @Override
    public void ENDER_DRAGON(Player p, float volume, float pitch) {
        p.playSound(p.getLocation(), Sound.valueOf("ENTITY_ENDER_DRAGON_GROWL"), volume, pitch);
    }
}
