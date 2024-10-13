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

  public interface OnExtractComplete {
    void onSuccess();

    void onError(String errorMsg);
  }

  public YoutubePlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    binding = YoutubePlayerViewBinding.inflate(LayoutInflater.from(context), this, true);
    initPlayer();
  }

  private void initPlayer() {
    player = new ExoPlayer.Builder(getContext()).build();
    binding.playerView.setPlayer(player);
  }

  /**
   * Extracts a YouTube video link, fetches video streams, and plays the video using ExoPlayer.
   *
   * @param url - The YouTube video URL to extract.
   */
  public void extractYoutubeLink(String url, OnExtractComplete onExtractComplete) {
    binding.progressCircular.setVisibility(View.VISIBLE);

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

    disposable = Single.create(emitter -> {
          NewPipe.init(DownloaderImpl.init(null));
          try {
            StreamExtractor extractor = ServiceList.YouTube.getStreamExtractor(url);
            extractor.fetchPage();
            emitter.onSuccess(extractor.getVideoStreams().get(0).getContent());
          } catch (Exception e) {
            System.out.println("Failed to StreamExtractor " + e);
            emitter.onError(e);
          }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doFinally(() -> binding.progressCircular.setVisibility(View.GONE))
        .subscribe(
            success -> {
              binding.errorMsg.setVisibility(View.GONE);
              MediaItem mediaItem = MediaItem.fromUri(Uri.parse((String) success));
              player.setMediaItem(mediaItem);
              player.prepare();
              player.setPlayWhenReady(true);
              onExtractComplete.onSuccess();
            },
            error -> {
              String errorMsg = error.getMessage();
              binding.errorMsg.setText(errorMsg);
              binding.errorMsg.setVisibility(View.VISIBLE);
              onExtractComplete.onError(errorMsg);
            }
        );
  }

  public ExoPlayer getPlayer() {
    return player;
  }

  public void setPlayerListener(Player.Listener listener) {
    player.addListener(listener);
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
