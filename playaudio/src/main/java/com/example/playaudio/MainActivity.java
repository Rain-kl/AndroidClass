package com.example.playaudio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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

    private static Uri authorizedUri; // 用来存储已授权的URI

    private SharedPreferences preferences;

    private RecyclerView recyclerView;
    private MusicAdapter musicAdapter;
    private List<MusicBaseModel> musicList = new ArrayList<>(); // Dummy data for testing


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

                            if (title == null) title = "Unknown Title";
                            if (artist == null) artist = "Unknown Artist";

                            Log.i("MusicFile", "Music File: " + file.getName() + ", Title: " + title + ", Artist: " + artist);
                            MusicBaseModel music = new MusicBaseModel(title, artist);
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
        musicAdapter = new MusicAdapter(musicList);
        recyclerView.setAdapter(musicAdapter);
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
}