package com.example;

public class EndCity {
    private int x;
    private int z;
    private boolean hasShip;

    public EndCity(int x, int z, boolean hasShip) {
        this.x = x;
        this.z = z;
        this.hasShip = hasShip;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public boolean hasShip() {
        return hasShip;
    }
}
