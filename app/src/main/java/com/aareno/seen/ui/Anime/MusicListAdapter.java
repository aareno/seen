package com.aareno.seen.ui.Anime;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.aareno.seen.R;
import com.aareno.seen.services.SpotifyService;
import com.bumptech.glide.Glide;

import java.util.List;

public class MusicListAdapter extends BaseAdapter {
    private Context context;
    private List<SpotifyService.Track> tracks;

    public MusicListAdapter(Context context, List<SpotifyService.Track> tracks) {
        this.context = context;
        this.tracks = tracks;
    }

    @Override
    public int getCount() {
        return tracks.size();
    }

    @Override
    public SpotifyService.Track getItem(int position) {
        return tracks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_music, parent, false);
            holder = new ViewHolder();
            holder.albumArt = convertView.findViewById(R.id.album_art);
            holder.title = convertView.findViewById(R.id.track_title);
            holder.artist = convertView.findViewById(R.id.track_artist);
            holder.playButton = convertView.findViewById(R.id.btn_play);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SpotifyService.Track track = getItem(position);
        holder.title.setText(track.getTitle());
        holder.artist.setText(track.getArtist());
        
        // Load album art
        Glide.with(context)
                .load(track.getAlbumArt())
                .into(holder.albumArt);

        // Set up play button click listener
        holder.playButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(track.getSpotifyUrl()));
            context.startActivity(intent);
        });

        return convertView;
    }

    private static class ViewHolder {
        ImageView albumArt;
        TextView title;
        TextView artist;
        ImageButton playButton;
    }

    public void updateTracks(List<SpotifyService.Track> newTracks) {
        this.tracks = newTracks;
        notifyDataSetChanged();
    }
} 