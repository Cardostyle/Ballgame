package com.example.ballgame;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class BluetoothActivity extends AppCompatActivity {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private List<Player> players;
    private ListView deviceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth wird nicht unterstützt", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Berechtigungen überprüfen und ggf. anfordern
        checkBluetoothPermissions();

        players = getIntent().getParcelableArrayListExtra("players");

        Button sendButton = findViewById(R.id.send_button);
        Button backButton = findViewById(R.id.back_button); // Zurück-Button
        Button makeVisibleButton = findViewById(R.id.make_visible_button); // Gerät sichtbar machen
        deviceListView = findViewById(R.id.device_list_view);

        // Zeige die gekoppelten Geräte an
        showPairedDevices();

        sendButton.setOnClickListener(v -> {
            if (bluetoothSocket != null) {
                new Thread(this::sendHighscores).start();
            } else {
                Toast.makeText(BluetoothActivity.this, "Keine Verbindung hergestellt", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> {
            // Zurück zur MainActivity
            Intent intent = new Intent(BluetoothActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        makeVisibleButton.setOnClickListener(v -> {
            makeDeviceDiscoverable();
            startServerSocket();
        });
    }

    private void checkBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                    },
                    REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private void makeDeviceDiscoverable() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, REQUEST_BLUETOOTH_PERMISSIONS);
            return;
        }
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); // Sichtbarkeit für 300 Sekunden
        startActivity(discoverableIntent);
    }

    private void showPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<String> deviceNames = new ArrayList<>();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceNames.add(device.getName() + " [" + device.getAddress() + "]");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
            deviceListView.setAdapter(adapter);

            deviceListView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedDevice = deviceNames.get(position);
                String deviceAddress = selectedDevice.substring(selectedDevice.indexOf("[") + 1, selectedDevice.indexOf("]"));
                connectToDevice(deviceAddress);
            });
        } else {
            Toast.makeText(this, "Keine gekoppelten Geräte gefunden", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(String deviceAddress) {
        new Thread(() -> {
            try {
                bluetoothSocket = getBluetoothSocket(deviceAddress);
                runOnUiThread(() -> Toast.makeText(this, "Verbindung erfolgreich hergestellt", Toast.LENGTH_SHORT).show());
                new Thread(this::receiveHighscores).start(); // Automatisch nach Verbindung starten
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Verbindung fehlgeschlagen", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private BluetoothSocket getBluetoothSocket(String deviceAddress) throws IOException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Bluetooth CONNECT-Berechtigung erforderlich");
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);
        socket.connect(); // Verbindung herstellen
        return socket;
    }

    private void startServerSocket() {
        new Thread(() -> {
            BluetoothServerSocket serverSocket = null;

            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("MyApp", MY_UUID);
                BluetoothSocket socket = serverSocket.accept();

                if (socket != null) {
                    runOnUiThread(() -> Toast.makeText(this, "Verbindung akzeptiert", Toast.LENGTH_SHORT).show());
                    serverSocket.close();
                    manageConnectedSocket(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Fehler beim Warten auf Verbindung", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        new Thread(() -> {
            try {
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);
                String receivedData = new String(buffer, 0, bytesRead);

                runOnUiThread(() -> Toast.makeText(this, "Daten empfangen: " + receivedData, Toast.LENGTH_SHORT).show());
                mergeHighscores(parsePlayers(receivedData)); // Automatische Verarbeitung der empfangenen Daten
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Fehler beim Datenaustausch", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private List<Player> parsePlayers(String data) {
        List<Player> playerList = new ArrayList<>();
        String[] entries = data.split("\n");
        for (String entry : entries) {
            playerList.add(Player.fromString(entry));
        }
        return playerList;
    }

    private void receiveHighscores() {
        try {
            if (bluetoothSocket == null) throw new IOException("Kein BluetoothSocket verfügbar");
            InputStream inputStream = bluetoothSocket.getInputStream();
            List<Player> receivedPlayers = new ArrayList<>();
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String data = new String(buffer, 0, bytesRead);
                for (String playerData : data.split("\n")) {
                    try {
                        receivedPlayers.add(Player.fromString(playerData));
                    } catch (IllegalArgumentException e) {
                        Log.d("BluetoothActivity", "Empfangene Daten: " + data);
                        runOnUiThread(() -> Toast.makeText(this, "Ungültige Daten empfangen: " + playerData, Toast.LENGTH_SHORT).show());
                    }
                }
            }

            mergeHighscores(receivedPlayers);
            runOnUiThread(() -> Toast.makeText(this, "Bestenliste empfangen", Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Empfangen fehlgeschlagen", Toast.LENGTH_SHORT).show());
        }
    }

    private void mergeHighscores(List<Player> receivedPlayers) {
        for (Player receivedPlayer : receivedPlayers) {
            boolean found = false;

            for (Player existingPlayer : players) {
                if (existingPlayer.getName().equals(receivedPlayer.getName())) {
                    found = true;
                    if (receivedPlayer.getHighscore() > existingPlayer.getHighscore()) {
                        existingPlayer.setHighscore(receivedPlayer.getHighscore());
                    }
                    break;
                }
            }

            if (!found) {
                players.add(receivedPlayer);
            }
        }

        // Speichere die aktualisierte Liste in den SharedPreferences
        savePlayers();
    }

    // Hilfsmethode, um Spieler in SharedPreferences zu speichern
    private void savePlayers() {
        SharedPreferences prefs = getSharedPreferences("player_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> playerSet = new HashSet<>();
        for (Player player : players) {
            playerSet.add(player.toString());
        }

        editor.putStringSet("players", playerSet);
        editor.apply();
    }


    private void sendHighscores() {
        try {
            if (bluetoothSocket == null) throw new IOException("Kein BluetoothSocket verfügbar");
            OutputStream outputStream = bluetoothSocket.getOutputStream();

            for (Player player : players) {
                outputStream.write((player.toString() + "\n").getBytes());
            }

            runOnUiThread(() -> Toast.makeText(this, "Bestenliste gesendet", Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Senden fehlgeschlagen", Toast.LENGTH_SHORT).show());
        }
    }

}
