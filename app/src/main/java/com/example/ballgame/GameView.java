// GameView.java
package com.example.ballgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;

public class GameView extends View {
    private Paint paint;
    private float ballX, ballY;
    private float ballRadius;
    private Bitmap wallBitmap, pathBitmap, holeBitmap, spawnBitmap, goalBitmap, ballBitmap;
    private MazeGenerator mazeGenerator;
    private int[][] maze;
    private int screenWidth, screenHeight;
    private int tileSize;
    private GameActivity gameActivity;

    public GameView(Context context, MazeGenerator mazeGenerator, GameActivity gameActivity) {
        super(context);
        this.mazeGenerator = mazeGenerator;
        this.maze = mazeGenerator.getMaze();
        this.gameActivity = gameActivity;

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        // Berechne die Zellengröße so, dass die letzte Zeile und Spalte außerhalb des Bildschirms liegen
        tileSize = Math.min((screenWidth / (maze[0].length - 1)), (screenHeight / (maze.length - 1)));
        ballRadius = tileSize / 3;

        wallBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.wall), tileSize, tileSize, true);
        pathBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.path), tileSize, tileSize, true);
        holeBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hole), tileSize, tileSize, true);
        spawnBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.spawn), tileSize, tileSize, true);
        goalBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.goal), tileSize, tileSize, true);
        ballBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ball), (int) (ballRadius * 2), (int) (ballRadius * 2), true);

        paint = new Paint();
        resetBallPosition();
    }

    private void resetBallPosition() {
        ballX = mazeGenerator.getSpawnX() * tileSize + tileSize / 2;
        ballY = mazeGenerator.getSpawnY() * tileSize + tileSize / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Zeichne das Labyrinth ohne die letzte Spalte und Zeile auf dem Bildschirm
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                int tile = maze[y][x];
                Rect destRect = new Rect(x * tileSize, y * tileSize, (x + 1) * tileSize, (y + 1) * tileSize);

                // Zeichne nur, wenn die Kachel innerhalb des sichtbaren Bereichs liegt
                if (x < maze[0].length - 1 && y < maze.length - 1) {
                    switch (tile) {
                        case MazeGenerator.WALL:
                            canvas.drawBitmap(wallBitmap, null, destRect, paint);
                            break;
                        case MazeGenerator.PATH:
                            canvas.drawBitmap(pathBitmap, null, destRect, paint);
                            break;
                        case MazeGenerator.HOLE:
                            canvas.drawBitmap(holeBitmap, null, destRect, paint);
                            break;
                        case MazeGenerator.SPAWN:
                            canvas.drawBitmap(spawnBitmap, null, destRect, paint);
                            break;
                        case MazeGenerator.GOAL:
                            canvas.drawBitmap(goalBitmap, null, destRect, paint);
                            break;
                    }
                }
            }
        }

        // Zeichne den Ball
        Rect ballRect = new Rect((int) (ballX - ballRadius), (int) (ballY - ballRadius), (int) (ballX + ballRadius), (int) (ballY + ballRadius));
        canvas.drawBitmap(ballBitmap, null, ballRect, paint);
    }

    private boolean canMoveTo(float newX, float newY) {
        int mazeX = (int) (newX / tileSize);
        int mazeY = (int) (newY / tileSize);

        // Überprüfe, ob der Ball die Randwände erreicht
        if (mazeX < 1 || mazeX >= maze[0].length - 1 || mazeY < 1 || mazeY >= maze.length - 1) {
            return false; // Der Ball kann nicht in die Randwände (x=1, y=1) oder außerhalb des Labyrinths gehen
        }

        return maze[mazeY][mazeX] != MazeGenerator.WALL;
    }

    public void updateBallPosition(float dx, float dy) {
        float newX = ballX - dx * 5;
        float newY = ballY + dy * 5;

        if (canMoveTo(newX, newY)) {
            ballX = newX;
            ballY = newY;

            if (isAtGoal()) {
                mazeGenerator.generateNewMaze();
                maze = mazeGenerator.getMaze();
                resetBallPosition();
                gameActivity.scoreIncrease(); // Score erhöhen, wenn das Ziel erreicht wird
            }

            if (isAtHole()) {
                mazeGenerator.generateNewMaze();
                maze = mazeGenerator.getMaze();
                resetBallPosition();
            }
        }

        invalidate();
    }

    private boolean isAtGoal() {
        int mazeX = (int) (ballX / tileSize);
        int mazeY = (int) (ballY / tileSize);
        return maze[mazeY][mazeX] == MazeGenerator.GOAL;
    }

    private boolean isAtHole() {
        int mazeX = (int) (ballX / tileSize);
        int mazeY = (int) (ballY / tileSize);
        return maze[mazeY][mazeX] == MazeGenerator.HOLE;
    }
}
