package com.tientoan.rikka.youtube_player.ui;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.tientoan.rikka.youtube_player.databinding.YoutubePlayerViewBinding;
import com.tientoan.rikka.youtube_player.model.VideoYoutube;
import com.tientoan.rikka.youtube_player.newpipe_impl.DownloaderImpl;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamExtractor;

import java.io.IOException;
import java.io.InterruptedIOException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class YoutubePlayerView extends FrameLayout {
  private ExoPlayer player;
  private Disposable disposable;
  private final YoutubePlayerViewBinding binding;

  public interface OnExtractComplete {
    void onSuccess(String title);

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
   * Extracts a YouTube video link, fetches video streams
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

    // Set global error handler for undeliverable exceptions
    catchRxException();

    disposable = Single.create(emitter -> {
          NewPipe.init(DownloaderImpl.init(null));
          try {
            StreamExtractor extractor = ServiceList.YouTube.getStreamExtractor(url);
            extractor.fetchPage();
            emitter.onSuccess(new VideoYoutube(extractor.getName(), extractor.getVideoStreams().get(0).getContent()));
          } catch (Exception e) {
            System.out.println("Failed to StreamExtractor " + e);
            emitter.onError(e);
          }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doFinally(() -> binding.progressCircular.setVisibility(View.GONE))
        .subscribe(
            result -> {
              VideoYoutube video = (VideoYoutube) result;
              binding.errorMsg.setVisibility(View.GONE);
              MediaItem mediaItem = MediaItem.fromUri(Uri.parse(video.getStreamUrl()));
              player.setMediaItem(mediaItem);
              player.prepare();
              player.setPlayWhenReady(false);
              onExtractComplete.onSuccess(video.getTitle());
            },
            error -> {
              String errorMsg = error.getMessage();
              binding.errorMsg.setText(errorMsg);
              binding.errorMsg.setVisibility(View.VISIBLE);
              onExtractComplete.onError(errorMsg);
            }
        );
  }

  private void catchRxException() {
    RxJavaPlugins.setErrorHandler(e -> {
      if (e instanceof UndeliverableException) {
        e = e.getCause();
      }
      if (e instanceof InterruptedIOException) {
        // Fine, some blocking code was interrupted by a dispose call
        Log.w("RxJavaError", "Stream was interrupted: " + e.getMessage());
        return;
      }
      if (e instanceof IOException) {
        // Network problem or API that throws on cancellation, no big deal
        Log.w("RxJavaError", "Network or IO issue: " + e.getMessage());
        return;
      }
      if (e instanceof InterruptedException) {
        // Fine, let it terminate
        Log.w("RxJavaError", "Thread was interrupted: " + e.getMessage());
        return;
      }
      if (e instanceof NullPointerException || e instanceof IllegalArgumentException) {
        // Likely a bug in the application
        Thread.currentThread().getUncaughtExceptionHandler()
            .uncaughtException(Thread.currentThread(), e);
        return;
      }
      if (e instanceof IllegalStateException) {
        // Likely a bug in RxJava or a custom operator
        Thread.currentThread().getUncaughtExceptionHandler()
            .uncaughtException(Thread.currentThread(), e);
        return;
      }
      Log.e("RxJavaError", "Undeliverable exception received: " + e.getMessage());
    });
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
