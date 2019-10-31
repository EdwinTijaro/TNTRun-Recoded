package me.tade.tntrun.events;

import me.tade.tntrun.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author The_TadeSK
 */
public class TNTRunPlayerLeaveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player p;
    private final Arena a;

    public TNTRunPlayerLeaveEvent(Player p, Arena a) {
        this.p = p;
        this.a = a;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getPlayer() {
        return p;
    }

    public Arena getArena() {
        return a;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
