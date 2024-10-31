package com.example.playaudio;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playaudio.model.MusicBaseModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// 音乐播放与展示的类
public class MusicHandler {
    private final Context context;
    private final List<MusicBaseModel> musicList = new ArrayList<>();
    private final RecyclerView recyclerView;
    private MediaPlayer mediaPlayer;
    private final ImageButton musicControl;
    private boolean isPlaying = false;
    private final TextView songTitle;
    private final TextView artist;

    public MusicHandler(Context context, RecyclerView recyclerView, ImageButton musicControl, TextView songTitle, TextView artist) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.musicControl = musicControl;
        this.songTitle = songTitle;
        this.artist = artist;
    }

    @SuppressWarnings("resource")
    public void loadMusicFiles(Uri uri) throws IOException {
        // 初始化RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // 清空音乐列表
        musicList.clear();

        // 使用DocumentFile从树Uri中获取DocumentFile对象
        DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);

        // 确保DocumentFile对象不为空并确认是一个目录
        if (documentFile != null && documentFile.isDirectory()) {
            // 列出目录下所有文件
            for (DocumentFile file : documentFile.listFiles()) {
                // 检查文件是否是文件
                if (file.isFile()) {
                    String mimeType = file.getType();
                    // 检查文件是否是音频文件
                    if (mimeType != null && (mimeType.equals("audio/mpeg") || mimeType.equals("audio/mp3"))) {

                        // 使用MediaMetadataRetriever获取音频文件的元数据
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        try {
                            // 设置数据源
                            mmr.setDataSource(context, file.getUri());
                            // 提取元数据
                            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);  // 提取标题
                            String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST); // 提取艺术家
                            Uri musicUri = file.getUri(); // 获取音乐文件的URI

                            if (title == null) title = "Unknown Title";
                            if (artist == null) artist = "Unknown Artist";

                            Log.i("MusicFile", "Music File: " + file.getName() + ", Title: " + title + ", Artist: " + artist);

                            MusicBaseModel music = new MusicBaseModel(title, artist, musicUri);
                            musicList.add(music);
                        } catch (IllegalArgumentException e) {
//                            e.printStackTrace(); // 处理异常
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
        // 创建音乐适配器
        MusicAdapter musicAdapter = new MusicAdapter(musicList, this::playMusic);
        // 设置适配器
        recyclerView.setAdapter(musicAdapter);
        musicControl.setOnClickListener(view -> togglePlayback()
        );
    }

    public void playMusic(MusicBaseModel music) {
        Log.d("MusicUri", "Music Uri: " + music.getUri().toString());
        Toast.makeText(context, "You clicked on: " + music.getTitle(), Toast.LENGTH_SHORT).show();
        songTitle.setText(music.getTitle());
        artist.setText(music.getArtist());


        // 如果有音乐正在播放，则释放MediaPlayer
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // 创建MediaPlayer并设置数据源
        ContentResolver contentResolver = context.getContentResolver();
        try {
            // 打开文件描述符
            AssetFileDescriptor fileDescriptor = contentResolver.openAssetFileDescriptor(music.getUri(), "r");
            if (fileDescriptor != null) {
                // 创建MediaPlayer并设置数据源
                mediaPlayer = new MediaPlayer();
                // 设置数据源
                mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
                // 关闭文件描述符
                fileDescriptor.close();

                // 设置监听器, 当准备好时开始播放
                mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                musicControl.setImageResource(R.drawable.ic_pause);//使用lambda表达式，当准备好时开始播放
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
            }
        } catch (IOException e) {
            Toast.makeText(context, "Unable to play music", Toast.LENGTH_SHORT).show();
            Log.e("Error", "IOException while trying to play music: " + e.getMessage());
        }
    }

    private void togglePlayback() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                // 如果当前是播放状态，则设置为暂停状态
                mediaPlayer.pause();
                musicControl.setImageResource(R.drawable.ic_play);
                isPlaying = false;
            } else {
                // 如果当前是暂停状态，则设置为播放状态
                mediaPlayer.start();
                musicControl.setImageResource(R.drawable.ic_pause);
                isPlaying = true;
            }
        }
    }

}