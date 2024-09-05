// Player.java
package com.example.ballgame;

import java.io.Serializable;

public class Player implements Serializable {
    private String name;
    private int highscore;

    public Player(String name) {
        this.name = name;
        highscore = 0;
    }

    public Player(String name, int highscore) {
        this.name = name;
        this.highscore = highscore;
    }

    public int getHighscore() {
        return highscore;
    }

    public String getName() {
        return name;
    }

    public void setHighscore(int highscore) {
        this.highscore = highscore;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + "," + highscore;
    }

    public static Player fromString(String playerData) {
        String[] parts = playerData.split(",");
        return new Player(parts[0], Integer.parseInt(parts[1]));
    }
}
