package com.example.ballgame;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "player_prefs";
    private static final String PLAYERS_KEY = "players";
    private List<Player> players;
    private ListView highscoreList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText playerNameInput = findViewById(R.id.player_name_input);
        Button startButton = findViewById(R.id.start_button);
        Button clearButton = findViewById(R.id.clear_button);
        Button bluetoothButton = findViewById(R.id.bluetooth_button);
        highscoreList = findViewById(R.id.highscore_list); // ListView für die Highscore-Liste

        // Lade gespeicherte Spieler
        players = loadPlayers();

        // Highscores anzeigen
        displayHighscores();

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String playerName = playerNameInput.getText().toString().trim();

                // Überprüfe, ob der Name eingegeben wurde
                if (playerName.isEmpty()) {
                    playerNameInput.setError("Bitte einen Namen eingeben");
                    return;
                }

                // Finde vorhandenen Spieler oder erstelle neuen
                Player player = findOrCreatePlayer(playerName);

                // Übergebe das Player-Objekt zur GameActivity
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("player", player); // Übergabe des Player-Objekts
                startActivity(intent);
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearPlayers();  // Methode zum Löschen der Spieler
                displayHighscores(); // Aktualisiere die Anzeige
            }
        });

        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
                intent.putParcelableArrayListExtra("players", new ArrayList<>(players)); // Korrekte Übergabe
                startActivity(intent);
            }
        });

    }

    private Player findOrCreatePlayer(String playerName) {
        // Suche nach vorhandenem Spieler
        for (Player player : players) {
            if (player.getName().equals(playerName)) {
                System.out.println("Vorhandener Spieler gefunden: " + player.getName() + ", Highscore: " + player.getHighscore());
                return player; // Vorhandener Spieler gefunden
            }
        }

        // Neuer Spieler erstellen, wenn nicht gefunden
        Player newPlayer = new Player(playerName);
        players.add(newPlayer);
        savePlayers(); // Speichern der aktualisierten Liste
        System.out.println("Neuer Spieler erstellt: " + newPlayer.getName());
        return newPlayer;
    }

    private List<Player> loadPlayers() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> playerSet = prefs.getStringSet(PLAYERS_KEY, new HashSet<>());
        List<Player> playerList = new ArrayList<>();

        for (String playerData : playerSet) {
            playerList.add(Player.fromString(playerData));
        }

        // Logge die geladenen Spieler
        System.out.println("Geladene Spieler: " + playerList.toString());

        return playerList;
    }

    private void savePlayers() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> playerSet = new HashSet<>();
        for (Player player : players) {
            playerSet.add(player.toString());
        }

        // Logge die Daten, die gespeichert werden
        System.out.println("Speichere Spieler: " + playerSet.toString());

        editor.putStringSet(PLAYERS_KEY, playerSet);
        editor.apply();
    }

    private void clearPlayers() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();  // Löscht alle gespeicherten Daten in den SharedPreferences
        editor.apply();
        players.clear(); // Löscht auch die lokale Liste der Spieler
    }

    private void displayHighscores() {
        List<String> top5 = new ArrayList<>();

        // Sortiere Spieler nach Highscore absteigend
        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player p1, Player p2) {
                return Integer.compare(p2.getHighscore(), p1.getHighscore());
            }
        });

        // Zeige die Top 5 Spieler an
        for (int i = 0; i < Math.min(players.size(), 5); i++) {
            Player player = players.get(i);
            top5.add(player.getName() + " - " + player.getHighscore());
        }

        // Falls keine Spieler vorhanden sind, füge "--- ; ---" hinzu
        while (top5.size() < 5) {
            top5.add("--- ; ---");
        }

        // Verwende einen ArrayAdapter, um die Liste anzuzeigen
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, top5);
        highscoreList.setAdapter(adapter);
    }
}
