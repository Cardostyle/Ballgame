package com.example.ballgame;

import android.os.Parcel;
import android.os.Parcelable;

public class Player implements Parcelable {
    private String name;
    private int highscore;

    public Player(String name) {
        this.name = name;
        this.highscore = 0;
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
        if (parts.length < 2) {
            throw new IllegalArgumentException("Ungültiges Format: " + playerData);
        }
        String name = parts[0].trim();
        int highscore;
        try {
            highscore = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ungültiger Punktestand: " + parts[1].trim());
        }
        return new Player(name, highscore);
    }


    // Parcelable-Implementierung
    protected Player(Parcel in) {
        name = in.readString();
        highscore = in.readInt();
    }

    public static final Creator<Player> CREATOR = new Creator<Player>() {
        @Override
        public Player createFromParcel(Parcel in) {
            return new Player(in);
        }

        @Override
        public Player[] newArray(int size) {
            return new Player[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(highscore);
    }
}
