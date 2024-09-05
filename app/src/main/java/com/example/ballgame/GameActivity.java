// GameActivity.java
package com.example.ballgame;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class GameActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private GameView gameView;
    private MazeGenerator mazeGenerator;
    private int baseWidth = 20;
    private int baseHeight = 40;
    private int baseHoleCount = 7;
    private Player player;
    private int score = 0;
    private TextView scoreView, highscoreView, timerView;
    private Button pauseButton;
    private int mazeWidth, mazeHeight, holeCount;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 60000; // 60 Sekunden
    private boolean isPaused = false;
    private static final String PREFS_NAME = "player_prefs";
    private static final String PLAYERS_KEY = "players";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Erhalte den Player aus dem Intent
        player = (Player) getIntent().getSerializableExtra("player");

        // Initialisiere den SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Setze das Layout
        setupLayout();

        // Generiere das erste Labyrinth basierend auf dem Start-Score
        generateNewMaze();

        // Starte den Timer
        startTimer();
    }

    private void setupLayout() {
        // Erstelle das Layout-Frame für das Spiel und die Knöpfe
        FrameLayout gameLayout = new FrameLayout(this);

        // TextViews für Score und Highscore erstellen und anzeigen
        scoreView = new TextView(this);
        highscoreView = new TextView(this);
        timerView = new TextView(this);

        // Setze die anfänglichen Werte für Score, Highscore und Timer
        scoreView.setText("Score: " + score);
        highscoreView.setText("Highscore: " + player.getHighscore());
        timerView.setText("Time Left: " + (timeLeftInMillis / 1000) + "s");


        // Setze die Transparenz der TextViews auf 20%
        scoreView.setTextColor(scoreView.getTextColors().withAlpha(200));  // Alpha = 51 (20%)
        highscoreView.setTextColor(highscoreView.getTextColors().withAlpha(200));  // Alpha = 51 (20%)
        timerView.setTextColor(timerView.getTextColors().withAlpha(200));  // Alpha = 51 (20%)


        // Layout-Parameter für die Views setzen
        FrameLayout.LayoutParams scoreParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        scoreParams.gravity = Gravity.TOP | Gravity.START;
        scoreParams.setMargins(16, 16, 16, 0);
        scoreView.setLayoutParams(scoreParams);
        scoreView.setTextSize(18);
        scoreView.setVisibility(View.VISIBLE);

        FrameLayout.LayoutParams highscoreParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        highscoreParams.gravity = Gravity.TOP | Gravity.START;
        highscoreParams.setMargins(16, 60, 16, 0);
        highscoreView.setLayoutParams(highscoreParams);
        highscoreView.setTextSize(18);
        highscoreView.setVisibility(View.VISIBLE);

        FrameLayout.LayoutParams timerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        timerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        timerParams.setMargins(16, 16, 16, 0);
        timerView.setLayoutParams(timerParams);
        timerView.setTextSize(18);
        timerView.setVisibility(View.VISIBLE);

        // Pausenknopf erstellen
        pauseButton = new Button(this);
        pauseButton.setBackgroundResource(R.drawable.pause);

        // Setze die Transparenz des Pause-Knopfes auf 40%
        pauseButton.getBackground().setAlpha(160); // Alpha = 102 (40%)

        // Setze die Layout-Parameter für den Knopf, damit er oben rechts angezeigt wird
        FrameLayout.LayoutParams pauseButtonParams = new FrameLayout.LayoutParams(
                150, 150
        );
        pauseButtonParams.gravity = Gravity.TOP | Gravity.END; // Oben rechts
        pauseButtonParams.setMargins(16, 16, 16, 16); // Ränder für den Abstand

        pauseButton.setLayoutParams(pauseButtonParams);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPauseMenu();
            }
        });

        // Füge die TextViews und den Pause-Button hinzu, nachdem das Labyrinth hinzugefügt wurde
        gameLayout.addView(scoreView);
        gameLayout.addView(highscoreView);
        gameLayout.addView(timerView);
        gameLayout.addView(pauseButton);

        // Setze das Layout als den Inhalt der Activity
        setContentView(gameLayout);
    }

    private void generateNewMaze() {
        // Berechne neue Labyrinthgröße und Anzahl der Löcher basierend auf dem aktuellen Score
        mazeWidth = baseWidth + 2 * score;
        mazeHeight = baseHeight + 4 * score;
        holeCount = baseHoleCount + score;

        // Generiere ein neues Labyrinth
        mazeGenerator = new MazeGenerator(mazeWidth, mazeHeight, holeCount);

        // Setze die benutzerdefinierte GameView als Hintergrund des Layouts
        if (gameView != null) {
            ((FrameLayout) findViewById(android.R.id.content)).removeView(gameView);
        }
        gameView = new GameView(this, mazeGenerator, this);
        ((FrameLayout) findViewById(android.R.id.content)).addView(gameView, 0);  // Füge die GameView als erstes hinzu
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                resetGame();
            }
        };
        countDownTimer.start();
    }

    private void updateTimerDisplay() {
        timerView.setText("Time Left: " + (timeLeftInMillis / 1000) + "s");
    }

    private void resetGame() {
        scoreReset();
        setHighscore(score);
        timeLeftInMillis = 60000; // 60 Sekunden zurücksetzen
        updateScoreDisplay();
        startTimer(); // Timer neu starten
        generateNewMaze(); // Neues Labyrinth generieren
    }

    public void scoreIncrease() {
        score++;
        setHighscore(score);
        updateScoreDisplay();
        generateNewMaze();  // Generiere ein neues Labyrinth, wenn das Ziel erreicht wurde
    }

    private void scoreReset() {
        score = 0;
    }

    private boolean setHighscore(int score) {
        if (player.getHighscore() <= score) {
            player.setHighscore(score);
            saveHighscore();
            return true;
        } else {
            return false;
        }
    }

    private void updateScoreDisplay() {
        scoreView.setText("Score: " + score);
        highscoreView.setText("Highscore: " + player.getHighscore());
    }

    private void showPauseMenu() {
        // Pausiere das Spiel
        sensorManager.unregisterListener(this);
        isPaused = true;

        // Pause den Timer
        countDownTimer.cancel();

        // Inflater zum Erstellen des Dialogs verwenden
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.pause_menu, null);

        // Setze Hintergrundfarbe des LinearLayouts auf transparent und entferne jegliche Alpha
        dialogView.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        builder.setView(dialogView);

        // Erstelle den Dialog
        AlertDialog dialog = builder.create();

        // Setze den Hintergrund des Dialogs auf transparent
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Knöpfe im Dialog initialisieren
        Button resumeButton = dialogView.findViewById(R.id.button_resume);
        Button restartButton = dialogView.findViewById(R.id.button_restart);
        Button mainMenuButton = dialogView.findViewById(R.id.button_main_menu);

        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Spiel fortsetzen
                sensorManager.registerListener(GameActivity.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                isPaused = false;
                startTimer(); // Timer fortsetzen
                // Dialog schließen
                dialog.dismiss();
            }
        });

        mainMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Zurück zum Hauptmenü
                Intent intent = new Intent(GameActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Neustarten der GameActivity
                Intent intent = getIntent();
                finish();
                startActivity(intent);

                // Dialog schließen
                dialog.dismiss();
            }
        });

        // Zeige den Dialog an
        dialog.show();
    }




    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        if (isPaused) {
            startTimer(); // Timer fortsetzen
            isPaused = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        countDownTimer.cancel(); // Timer anhalten
        isPaused = true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Verarbeite die Bewegungssensoren, um den Ball zu bewegen
        if (!isPaused) {
            gameView.updateBallPosition(event.values[0], event.values[1]);
        }
    }

    private void saveHighscore() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> playerSet = new HashSet<>(prefs.getStringSet(PLAYERS_KEY, new HashSet<>()));

        // Beginne eine Transaktion
        editor.clear();

        // Aktualisiere die Daten: entferne den alten Eintrag und füge den neuen hinzu
        int hs= player.getHighscore() -1;
        playerSet.remove(player.getName() + "," + hs);
        playerSet.add(player.getName() + "," + player.getHighscore());

        // Speichern der geänderten Liste
        editor.putStringSet(PLAYERS_KEY, playerSet);
        editor.apply();
        System.out.println("Highscore Saved");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nicht benötigt
    }
}
