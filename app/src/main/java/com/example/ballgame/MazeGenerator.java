package com.example.ballgame;

import java.util.Random;

public class MazeGenerator {
    private int[][] maze; // 2D-Array, das das Labyrinth darstellt
    private int width, height; // Breite und Höhe des Labyrinths
    private int spawnX, spawnY; // Startposition des Balls
    private int goalX, goalY; // Position des Ziels
    private int holeCount;
    private Random random;

    // Konstanten zur Darstellung von Labyrinthelementen
    public static final int WALL = 1; // Wand
    public static final int PATH = 0; // Pfad
    public static final int HOLE = 2; // Loch, in das der Ball fällt und das Level neu startet
    public static final int SPAWN = 3; // Startpunkt des Balls
    public static final int GOAL = 4; // Zielpunkt

    public MazeGenerator(int width, int height, int holeCount) {
        // Die Breite und Höhe des Labyrinths sollten durch 2 teilbar sein, damit die Pfade 2x2 Zellen breit sein können
        this.width = (width % 2 == 0) ? width : width + 1;
        this.height = (height % 2 == 0) ? height : height + 1;
        this.maze = new int[this.height + 1][this.width + 1];  // +1 für den Rand
        this.random = new Random();
        this.holeCount = holeCount;
        generateNewMaze();
    }

    // Methode zum Generieren eines neuen Labyrinths
    public void generateNewMaze() {
        // Initialisiere das Labyrinth mit Wänden
        for (int y = 0; y < height + 1; y++) { // +1 für den Rand
            for (int x = 0; x < width + 1; x++) { // +1 für den Rand
                maze[y][x] = WALL; // Alle Zellen initial als Wand setzen
            }
        }

        // Starte die Rekursive Rückverfolgung vom zufälligen Punkt (von 1,1 starten wegen der Randwand)
        generatePath(1, 1);

        // Füge Löcher hinzu
        addHoles(holeCount);

        // Setze den Startpunkt und das Ziel
        setSpawnPoint();
        setGoalPoint();
    }

    // Rekursive Methode zur Erzeugung eines 2x2-Zellen-breiten Pfades
    private void generatePath(int x, int y) {
        carvePath(x, y); // Setze 2x2-Pfad an der aktuellen Position

        // Zufällige Richtungsliste
        int[] dirs = new int[]{0, 1, 2, 3};
        shuffleArray(dirs);

        // Probiere jede Richtung
        for (int i = 0; i < dirs.length; i++) {
            int nx = x, ny = y;

            switch (dirs[i]) {
                case 0: // Nach rechts
                    nx += 4;
                    break;
                case 1: // Nach links
                    nx -= 4;
                    break;
                case 2: // Nach unten
                    ny += 4;
                    break;
                case 3: // Nach oben
                    ny -= 4;
                    break;
            }

            // Überprüfe, ob der neue Ort innerhalb der Grenzen des Arrays liegt
            if (nx >= 1 && nx <= width - 1 && ny >= 1 && ny <= height - 1 && maze[ny][nx] == WALL) {
                // Setze den Verbindungsweg zwischen den Zellen (2 Zellen lang)
                int betweenX1 = x + (nx - x) / 4 * 2;
                int betweenY1 = y + (ny - y) / 4 * 2;
                carvePath(betweenX1, betweenY1);

                int betweenX2 = x + (nx - x) / 2;
                int betweenY2 = y + (ny - y) / 2;
                carvePath(betweenX2, betweenY2);

                generatePath(nx, ny); // Rekursiver Aufruf
            }
        }
    }

    // Methode zum Erstellen eines 2x2-Pfads
    private void carvePath(int x, int y) {
        // Pfad nur innerhalb der Grenzen setzen
        for (int dy = 0; dy < 2; dy++) {
            for (int dx = 0; dx < 2; dx++) {
                if (x + dx < width + 1 && y + dy < height + 1) {  // +1 für den Rand
                    maze[y + dy][x + dx] = PATH;
                }
            }
        }
    }

    // Hilfsmethode zum Mischen eines Arrays
    private void shuffleArray(int[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    // Füge zufällige Löcher zum Labyrinth hinzu, ohne dass sie sich berühren
    private void addHoles(int count) {
        for (int i = 0; i < count; i++) {
            int x, y;
            int attempts = 0;
            do {
                x = random.nextInt(width - 2) + 1;
                y = random.nextInt(height - 2) + 1;
                attempts++;
                // Verlasse die Schleife, falls zu viele Versuche unternommen wurden, um eine Endlosschleife zu verhindern
                if (attempts > 1000) break;
            } while (maze[y][x] != PATH || isAdjacentToHole(x, y)); // Nur auf Pfaden Löcher hinzufügen und überprüfen, dass sie sich nicht berühren

            // Platziere das Loch, wenn eine gültige Position gefunden wurde
            if (maze[y][x] == PATH && !isAdjacentToHole(x, y)) {
                maze[y][x] = HOLE;
            }
        }
    }

    // Überprüfe, ob ein Loch in einer benachbarten oder diagonalen Position vorhanden ist
    private boolean isAdjacentToHole(int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && maze[ny][nx] == HOLE) {
                    return true; // Es gibt ein Loch in der Nähe (auch diagonal)
                }
            }
        }
        return false;
    }

    // Setze einen zufälligen Startpunkt
    private void setSpawnPoint() {
        do {
            spawnX = random.nextInt(width);
            spawnY = random.nextInt(height);
        } while (maze[spawnY][spawnX] != PATH); // Stelle sicher, dass der Startpunkt auf einem Pfad liegt

        maze[spawnY][spawnX] = SPAWN;
    }

    // Setze einen zufälligen Zielpunkt
    private void setGoalPoint() {
        do {
            goalX = random.nextInt(width);
            goalY = random.nextInt(height);
        } while (maze[goalY][goalX] != PATH || (goalX == spawnX && goalY == spawnY)); // Nicht am selben Punkt wie der Spawnpunkt

        maze[goalY][goalX] = GOAL;
    }

    // Getter-Methoden für das Spiel
    public int[][] getMaze() {
        return maze;
    }

    public int getSpawnX() {
        return spawnX;
    }

    public int getSpawnY() {
        return spawnY;
    }

    public int getGoalX() {
        return goalX;
    }

    public int getGoalY() {
        return goalY;
    }
}
