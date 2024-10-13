package com.example.playaudio.model;

public class MusicBaseModel {
    private final String title;
    private final String artist;

    public MusicBaseModel(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }
}


