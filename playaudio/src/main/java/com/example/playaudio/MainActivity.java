package com.example.playaudio;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {

    private static Uri authorizedUri; // 用来存储已授权的URI

    private SharedPreferences preferences;

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
                            Toast.makeText(MainActivity.this, "You selected: " + uri.toString(), Toast.LENGTH_LONG).show();
                            authorizedUri = uri;
                            preferences = getSharedPreferences("config", MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("uri", uri.toString());
                            editor.apply();

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



        preferences = getSharedPreferences("config", MODE_PRIVATE);
        String uriString = preferences.getString("uri", null);
        if (uriString != null) {
            authorizedUri = Uri.parse(uriString);
            if (hasUriPermission(authorizedUri)) {
                Toast.makeText(this, "Permission already granted for: " + authorizedUri.toString(), Toast.LENGTH_LONG).show();
            } else {
                authorizedUri = null;
            }
        }
        if (authorizedUri == null || !hasUriPermission(authorizedUri)) {
            openDirectoryChooser();
        }
    }
}