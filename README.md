# 大致思路

## 解决权限问题
目前没有权限读取到音乐，所以需要用户手动去授权
```java
openDirectoryChooser();
```
从目录选择器Activity中返回的数据
```java
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

```
## 存储授权的文件路径
## 解决页面展示问题
歌曲展示
```xml
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/music_recycler_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:padding="16dp"/>
```
## 解决文件读取问题
## 解决播放问题
