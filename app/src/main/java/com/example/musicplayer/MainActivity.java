package com.example.musicplayer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private MediaPlayer mediaPlayer;
    private int currentSongIndex = 0;

    private ImageButton playPauseButton, nextButton, previousButton;
    private TextView songTitle;

    private ArrayList<String> songPaths;
    private ArrayList<String> songNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playPauseButton = findViewById(R.id.btnPlayPause);
        nextButton = findViewById(R.id.btnNext);
        previousButton = findViewById(R.id.btnPrevious);
        songTitle = findViewById(R.id.songTitle);

        songPaths = new ArrayList<>();
        songNames = new ArrayList<>();

        // Request storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            loadSongsFromStorage();
        }

        // Play/Pause Button
        playPauseButton.setOnClickListener(view -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playPauseButton.setImageResource(R.drawable.ic_play); // Update to play icon
                } else {
                    mediaPlayer.start();
                    playPauseButton.setImageResource(R.drawable.ic_pause); // Update to pause icon
                }
            }
        });

        // Next Button
        nextButton.setOnClickListener(view -> playNextSong());

        // Previous Button
        previousButton.setOnClickListener(view -> playPreviousSong());
    }

    private void loadSongsFromStorage() {
        ContentResolver contentResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(musicUri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            do {
                String title = cursor.getString(titleColumn);
                String path = cursor.getString(dataColumn);

                songNames.add(title);
                songPaths.add(path);
            } while (cursor.moveToNext());

            cursor.close();
        }

        if (songPaths.isEmpty()) {
            Toast.makeText(this, "No songs found in storage.", Toast.LENGTH_SHORT).show();
        } else {
            playSong(currentSongIndex);
        }
    }

    private void playSong(int index) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        String songPath = songPaths.get(index);
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(songPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            songTitle.setText(songNames.get(index));
            playPauseButton.setImageResource(R.drawable.ic_pause); // Update to pause icon

            // Set listener for when song completes
            mediaPlayer.setOnCompletionListener(mp -> playNextSong());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error playing song.", Toast.LENGTH_SHORT).show();
        }
    }

    private void playNextSong() {
        if (!songPaths.isEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % songPaths.size();
            playSong(currentSongIndex);
        } else {
            Toast.makeText(this, "No songs available to play.", Toast.LENGTH_SHORT).show();
        }
    }

    private void playPreviousSong() {
        if (!songPaths.isEmpty()) {
            currentSongIndex = (currentSongIndex - 1 + songPaths.size()) % songPaths.size();
            playSong(currentSongIndex);
        } else {
            Toast.makeText(this, "No songs available to play.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-check permissions when the app resumes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                loadSongsFromStorage();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                loadSongsFromStorage();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongsFromStorage();
            } else {
                Toast.makeText(this, "Permission denied. Cannot load songs.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
