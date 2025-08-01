package com.terrescalmes.entities;

public class Player {

    public float x, y;
    public float prevX, prevY;
    public float width, height;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void saveState() {
        prevX = x;
        prevY = y;
    }
}
