package me.tade.tntrun.utils;

import java.io.Serializable;

public class RunBlock implements Serializable {

    public String w;
    public int x, y, z, d;
    public String material;

    public RunBlock(String w, int x, int y, int z, int d, String material) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
        this.d = d;
        this.material = material;
    }
}