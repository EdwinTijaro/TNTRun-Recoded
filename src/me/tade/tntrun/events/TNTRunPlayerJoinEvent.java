package me.tade.tntrun.events;

import me.tade.tntrun.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author The_TadeSK
 */
public class TNTRunPlayerJoinEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player p;
    private final Arena a;
    private boolean cancelled;

    public TNTRunPlayerJoinEvent(Player p, Arena a) {
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean bln) {
        cancelled = bln;
    }
}
