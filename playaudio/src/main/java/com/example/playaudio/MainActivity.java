package com.example.playaudio;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playaudio.model.MusicBaseModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;

    private static Uri authorizedUri; // 用来存储已授权的URI

    private SharedPreferences preferences;

    private RecyclerView recyclerView;
    private MusicAdapter musicAdapter;
    private List<MusicBaseModel> musicList = new ArrayList<>(); // Dummy data for testing
    private MediaPlayer mediaPlayer;


    private final ActivityResultLauncher<Intent> openDirectoryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri uri = data.getData();
                            assert uri != null;

                            getContentResolver().takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            );
                            Toast.makeText(MainActivity.this, "You selected: " + uri, Toast.LENGTH_LONG).show();
                            authorizedUri = uri;
                            preferences = getSharedPreferences("config", MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("uri", uri.toString());
                            editor.apply();

                            // Restart or refresh the activity to reflect the changes
                            Intent restartIntent = new Intent(MainActivity.this, MainActivity.class);
                            finish();  // Close the current activity
                            startActivity(restartIntent);  // Start a new instance of MainActivity

                        }
                    }
                }
            });

    private boolean hasUriPermission(Uri uri) {
        for (UriPermission persistedUri : getContentResolver().getPersistedUriPermissions()) {
            if (persistedUri.getUri().equals(uri) && persistedUri.isReadPermission() && persistedUri.isWritePermission()) {
                return true;
            }
        }
        return false;
    }

    public void openDirectoryChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        openDirectoryLauncher.launch(intent);
        Toast.makeText(this, "Please select a directory", Toast.LENGTH_LONG).show();

    }

    public void loadMusicFiles(Context context, Uri uri) throws IOException {
        // Initialize RecyclerView
        recyclerView = findViewById(R.id.music_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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

    private void playMusic(MusicBaseModel music) {
        // 记录Uri用于调试
        Log.d("MusicUri", "Music Uri: " + music.getUri().toString());
        Toast.makeText(this, "You clicked on: " + music.getTitle(), Toast.LENGTH_SHORT).show();

        ContentResolver contentResolver = getContentResolver();
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release(); // 释放旧的MediaPlayer资源
                mediaPlayer = null;
            }

            AssetFileDescriptor fileDescriptor = contentResolver.openAssetFileDescriptor(music.getUri(), "r");
            if (fileDescriptor != null) {
                // 创建新的MediaPlayer实例
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
                fileDescriptor.close();

                mediaPlayer.setOnPreparedListener(mp -> mediaPlayer.start());
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    Toast.makeText(this, "Playback completed", Toast.LENGTH_SHORT).show();
                });
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Toast.makeText(this, "Error playing music", Toast.LENGTH_SHORT).show();
                    mp.release();
                    return true;
                });

                mediaPlayer.prepareAsync();
            }
        } catch (IOException e) {
//                e.printStackTrace();
            Toast.makeText(this, "Unable to play music", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 检查并请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }

        findViewById(R.id.btn_scan_music).setOnClickListener(this);
        findViewById(R.id.btn_search_music).setOnClickListener(this);

        preferences = getSharedPreferences("config", MODE_PRIVATE);
        String uriString = preferences.getString("uri", null);

        if (uriString == null) {
            findViewById(R.id.btn_scan_music).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.btn_scan_music).setVisibility(View.GONE);
            authorizedUri = Uri.parse(uriString);
            if (hasUriPermission(authorizedUri)) {
                Log.d("MainActivity", "Permission granted for: " + authorizedUri.toString());
//                Toast.makeText(this, "Permission granted for: " + authorizedUri.toString(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission expired for: " + authorizedUri.toString(), Toast.LENGTH_LONG).show();
                openDirectoryChooser();
            }
            try {
                loadMusicFiles(this, authorizedUri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.btn_scan_music) {
            openDirectoryChooser();
        }
        if (viewId == R.id.btn_search_music) {
            openDirectoryChooser();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户同意了通知权限
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                // 用户拒绝了通知权限
            }
        }
    }
}