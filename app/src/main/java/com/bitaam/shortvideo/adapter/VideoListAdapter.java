package com.bitaam.shortvideo.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitaam.shortvideo.R;
import com.bitaam.shortvideo.modal.VideoModal;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.VideoViewHolder> implements Filterable {

    ArrayList<VideoModal> videoModels;
    Context context;
    SimpleExoPlayer currPlayer = null;
    ArrayList<VideoModal> filterdItemModels= new ArrayList<>();
    ArrayList<String> videoNo = new ArrayList<>();
    ArrayList<String> keys;

    public VideoListAdapter(ArrayList<VideoModal> videoModels, Context context) {
        this.videoModels = videoModels;
        this.context = context;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item,parent,false);;
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {

        //Minimum Video you want to buffer while Playing
        int MIN_BUFFER_DURATION = 5000;
        //Max Video you want to buffer during PlayBack
        int MAX_BUFFER_DURATION = 10000;
        //Min Video you want to buffer before start Playing it
        int MIN_PLAYBACK_START_BUFFER = 3000;
        //Min video You want to buffer when user resumes video
        int MIN_PLAYBACK_RESUME_BUFFER = 3000;

        DefaultRenderersFactory renderersFactory;

        @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;

        renderersFactory = new DefaultRenderersFactory(context) .setExtensionRendererMode(extensionRendererMode);

        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setAllocator(new DefaultAllocator(true, 16))
                .setBufferDurationsMs(MIN_BUFFER_DURATION,
                        MAX_BUFFER_DURATION,
                        MIN_PLAYBACK_START_BUFFER,
                        MIN_PLAYBACK_RESUME_BUFFER)
                .setTargetBufferBytes(-1)
                .setPrioritizeTimeOverSizeThresholds(true).createDefaultLoadControl();

        TrackSelector trackSelector = new DefaultTrackSelector();
        holder.player  = new SimpleExoPlayer.Builder(context,renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build();

        if(!filterdItemModels.isEmpty()){

            VideoModal videoModal = filterdItemModels.get(position);
            holder.videoTitleTv.setText(videoModal.getTitle());
            holder.videoTagTv.setText(videoModal.getTag());

            holder.exoPlayerView.setPlayer(holder.player);
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoModal.getVideoUrl()));
            holder.player.setMediaItem(mediaItem);
            holder.exoPlayerView.setVisibility(View.VISIBLE);
            holder.player.prepare();
            holder.exoPlayerView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    Objects.requireNonNull( holder.exoPlayerView.getPlayer()).prepare();
                    Objects.requireNonNull( holder.exoPlayerView.getPlayer()).play();

                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    Objects.requireNonNull(holder.exoPlayerView.getPlayer()).stop();
                }
            });

            currPlayer = holder.player;

        }

    }

    public void update(VideoModal model){
        videoModels.add(model);
        filterdItemModels.add(model);
        videoNo.add(model.getTitle()+" "+model.getTag());
        notifyDataSetChanged();
    }

    public void pausePlayer(){
        if (currPlayer!=null){
            currPlayer.pause();
        }
//        if (open){
//            filterdItemModels.clear();
//            filterdItemModels.addAll(videoModels);
//        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String query = charSequence.toString();

                List<VideoModal> filtered = new ArrayList<>();

                if (query.isEmpty()) {
                    filtered.addAll(videoModels);

                } else {
                    for (String nos : videoNo) {
                        if (nos.toLowerCase().contains(query.toLowerCase())) {
                            filtered.add(videoModels.get(videoNo.indexOf(nos)));
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.count = filtered.size();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults results) {
                filterdItemModels = (ArrayList<VideoModal>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public int getItemCount() {
        return filterdItemModels.size();
    }

    public  class VideoViewHolder extends RecyclerView.ViewHolder {

        TextView videoTitleTv,videoTagTv;
        PlayerView exoPlayerView;
        SimpleExoPlayer player;

        public VideoViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            videoTitleTv = itemView.findViewById(R.id.videoTitle);
            videoTagTv = itemView.findViewById(R.id.videoTag);
            exoPlayerView = itemView.findViewById(R.id.exoplayer_post);

        }

    }

}
