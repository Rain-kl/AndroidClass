package com.example.playaudio.model;

import android.net.Uri;

public class MusicBaseModel {
    private final String title;
    private final String artist;
    private final Uri uri;

    public MusicBaseModel(String title, String artist, Uri uri) {
        this.title = title;
        this.artist = artist;
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public Uri getUri() {
        return uri;
    }
}


