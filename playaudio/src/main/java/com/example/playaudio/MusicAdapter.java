package com.example.playaudio;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playaudio.model.MusicBaseModel;

import java.util.List;


public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    private final List<MusicBaseModel> musicList;
    private OnItemClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION; // 默认没有选择

    public MusicAdapter(List<MusicBaseModel> musicList, OnItemClickListener listener) {
        this.musicList = musicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music, parent, false);
        return new MusicViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(MusicViewHolder holder, int position) {
        MusicBaseModel music = musicList.get(position);
        holder.titleTextView.setText(music.getTitle());
        holder.artistTextView.setText(music.getArtist());

        // 改变选定项目标题的字体颜色
        int color = (position == selectedPosition) ?
                ContextCompat.getColor(holder.itemView.getContext(), R.color.custom_primary) :
                ContextCompat.getColor(holder.itemView.getContext(), R.color.black);

        holder.titleTextView.setTextColor(color);
        holder.artistTextView.setTextColor(color);


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int previousPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);

                listener.onItemClick(music);
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public interface OnItemClickListener {
        void onItemClick(MusicBaseModel music);
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView artistTextView;

        MusicViewHolder(View itemView, OnItemClickListener listener) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.song_title);
            artistTextView = itemView.findViewById(R.id.artist);
        }
    }
}