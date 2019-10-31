package me.tade.tntrun.utils;

import org.bukkit.Material;

import java.io.Serializable;

public class TBlock implements Serializable {

    public String w;
    public int x, y, z, i, d;

    public TBlock(String w, int x, int y, int z, int i, int d) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
        this.i = i;
        this.d = d;
    }
}
