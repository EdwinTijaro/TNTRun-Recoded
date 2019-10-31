package me.tade.tntrun.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Selection {

    private Player player;
    private Location point1;
    private Location point2;

    public Selection(Player player) {
        this.player = player;
    }

    public Location getPoint1() {
        return point1;
    }

    public void setPoint1(Location point1) {
        this.point1 = point1;
    }

    public Location getPoint2() {
        return point2;
    }

    public void setPoint2(Location point2) {
        this.point2 = point2;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean hasSelected() {
        return getPoint1() != null && getPoint2() != null;
    }

    public Location getMaxPoint() {
        if (!hasSelected())
            return null;

        if (getPoint1().getBlockX() >= getPoint2().getBlockX()
                && getPoint1().getBlockY() >= getPoint2().getBlockY()
                && getPoint1().getBlockZ() >= getPoint2().getBlockZ())
            return getPoint1();
        return getPoint2();
    }

    public Location getMinPoint() {
        if (!hasSelected())
            return null;

        if (getPoint1().getBlockX() >= getPoint2().getBlockX()
                && getPoint1().getBlockY() >= getPoint2().getBlockY()
                && getPoint1().getBlockZ() >= getPoint2().getBlockZ())
            return getPoint2();
        return getPoint1();
    }
}
