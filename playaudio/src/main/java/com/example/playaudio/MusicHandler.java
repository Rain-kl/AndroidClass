package com.example.playaudio;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playaudio.model.MusicBaseModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicHandler {
    private Context context;
    private List<MusicBaseModel> musicList = new ArrayList<>();
    private RecyclerView recyclerView;
    private MusicAdapter musicAdapter;
    private MediaPlayer mediaPlayer;
    private MusicBaseModel currentlyPlayingMusic;

    public MusicHandler(Context context, RecyclerView recyclerView) {
        this.context = context;
        this.recyclerView = recyclerView;

        // Setup the RecyclerView
        this.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        this.musicAdapter = new MusicAdapter(musicList, this::playMusic);
        this.recyclerView.setAdapter(musicAdapter);
    }

    public void loadMusicFiles(Uri uri) throws IOException {
        // Initialize RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // Clear the existing list if any
        musicList.clear();

        // 使用DocumentFile从树Uri中获取DocumentFile对象
        DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);

        // 确保DocumentFile对象不为空并确认是一个目录
        if (documentFile != null && documentFile.isDirectory()) {
            // 列出目录下所有文件
            for (DocumentFile file : documentFile.listFiles()) {
                // 检查文件是否是音乐文件
                if (file.isFile()) {
                    String mimeType = file.getType();
                    if (mimeType != null && (mimeType.equals("audio/mpeg") || mimeType.equals("audio/mp3"))) {
                        // 使用MediaMetadataRetriever获取音频文件的元数据
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();

                        try {
                            mmr.setDataSource(context, file.getUri());

                            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                            String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                            Uri musicUri = file.getUri();

                            if (title == null) title = "Unknown Title";
                            if (artist == null) artist = "Unknown Artist";

                            Log.i("MusicFile", "Music File: " + file.getName() + ", Title: " + title + ", Artist: " + artist);
                            MusicBaseModel music = new MusicBaseModel(title, artist, musicUri);
                            musicList.add(music);
                        } catch (IllegalArgumentException e) {
                            Log.e("Error", "Failed to retrieve metadata for " + file.getName() + ": " + e.getMessage());
                        } finally {
                            mmr.release();
                        }
                    }
                }
            }
        } else {
            Log.e("Error", "The Uri does not represent a valid directory or is null.");
        }
        // Update adapter
        musicAdapter = new MusicAdapter(musicList, this::playMusic);
        recyclerView.setAdapter(musicAdapter);
    }

    public void playMusic(MusicBaseModel music) {
        Log.d("MusicUri", "Music Uri: " + music.getUri().toString());
        Toast.makeText(context, "You clicked on: " + music.getTitle(), Toast.LENGTH_SHORT).show();

        if (mediaPlayer != null && currentlyPlayingMusic == music) {
            togglePlayPause();
            return;
        }

        // If another song is playing, stop it
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        ContentResolver contentResolver = context.getContentResolver();
        try {
            AssetFileDescriptor fileDescriptor = contentResolver.openAssetFileDescriptor(music.getUri(), "r");
            if (fileDescriptor != null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
                fileDescriptor.close();

                mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    Toast.makeText(context, "Playback completed", Toast.LENGTH_SHORT).show();
                });
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Toast.makeText(context, "Error playing music", Toast.LENGTH_SHORT).show();
                    mp.release();
                    return true;
                });

                mediaPlayer.prepareAsync();
                currentlyPlayingMusic = music;
            }
        } catch (IOException e) {
            Toast.makeText(context, "Unable to play music", Toast.LENGTH_SHORT).show();
            Log.e("Error", "IOException while trying to play music: " + e.getMessage());
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                Toast.makeText(context, "Music paused", Toast.LENGTH_SHORT).show();
            } else {
                mediaPlayer.start();
                Toast.makeText(context, "Music resumed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}