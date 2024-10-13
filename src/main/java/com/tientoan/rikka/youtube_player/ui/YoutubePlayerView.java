package com.tientoan.rikka.youtube_player.ui;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.tientoan.rikka.youtube_player.databinding.YoutubePlayerViewBinding;
import com.tientoan.rikka.youtube_player.newpipe_impl.DownloaderImpl;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamExtractor;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class YoutubePlayerView extends FrameLayout {
  private ExoPlayer player;
  private Disposable disposable;
  private final YoutubePlayerViewBinding binding;

  public YoutubePlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    binding = YoutubePlayerViewBinding.inflate(LayoutInflater.from(context), this, true);
    initPlayer();
  }

  private void initPlayer() {
    player = new ExoPlayer.Builder(getContext()).build();
    binding.playerView.setPlayer(player);
  }

  public void extractYoutubeLink(String url) {
    binding.progressCircular.setVisibility(View.VISIBLE);

    // release previous play session if exists
    if (disposable != null) {
      disposable.dispose();
      disposable = null;
    }
    if (player == null) {
      initPlayer();
    }
    if (player.isPlaying()) {
      player.stop();
    }

    // start extract data from url
    disposable = Single.fromCallable(() -> {
          try {
            NewPipe.init(DownloaderImpl.init(null));
            StreamExtractor extractor = ServiceList.YouTube.getStreamExtractor(url);
            extractor.fetchPage();
            return new com.tientoan.rikka.demo.ui.ExtractorResult(com.tientoan.rikka.demo.ui.ExtractorResult.Status.SUCCESS, extractor.getVideoStreams().get(0).getContent(), null);
          } catch (Exception e) {
            System.out.println("Failed to StreamExtractor " + e);
            return new com.tientoan.rikka.demo.ui.ExtractorResult(com.tientoan.rikka.demo.ui.ExtractorResult.Status.ERROR, null, e);
          }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(result -> {
          binding.progressCircular.setVisibility(View.GONE);
          if (result.getStatus() == com.tientoan.rikka.demo.ui.ExtractorResult.Status.SUCCESS) {
            // Create a MediaItem with the extracted URL
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(result.getUrl()));
            // Set the media item to be played
            player.setMediaItem(mediaItem);
            // Prepare and start playing
            player.prepare();
            player.setPlayWhenReady(true);
          } else {
            binding.errorMsg.setText(result.getError().getMessage());
          }
        });
  }

  public ExoPlayer getPlayer() {
    return player;
  }

  public void release() {
    if (disposable != null) {
      disposable.dispose();
      disposable = null;
    }

    if (player != null) {
      player.release();
      player = null;
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    release();
  }
}
