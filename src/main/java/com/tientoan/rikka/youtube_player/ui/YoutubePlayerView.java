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

  /**
   * Constructor for YoutubePlayerView.
   * Initializes the custom view and player.
   *
   * @param context - Context in which the view is running.
   * @param attrs - AttributeSet used for XML attributes.
   */
  public YoutubePlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    binding = YoutubePlayerViewBinding.inflate(LayoutInflater.from(context), this, true);
    initPlayer();
  }

  /**
   * Initializes the ExoPlayer instance and binds it to the player view.
   */
  private void initPlayer() {
    player = new ExoPlayer.Builder(getContext()).build();
    binding.playerView.setPlayer(player);
  }

  /**
   * Extracts a YouTube video link, fetches video streams, and plays the video using ExoPlayer.
   *
   * @param url - The YouTube video URL to extract.
   */
  public void extractYoutubeLink(String url) {
    // Show progress bar while extracting video stream
    binding.progressCircular.setVisibility(View.VISIBLE);

    // Dispose of any previous extraction process
    if (disposable != null) {
      disposable.dispose();
      disposable = null;
    }

    // Reinitialize player if necessary
    if (player == null) {
      initPlayer();
    }

    // Stop the player if it's currently playing
    if (player.isPlaying()) {
      player.stop();
    }

    // Start extracting the video link
    disposable = Single.fromCallable(() -> {
          try {
            // Initialize NewPipe extractor with a custom downloader
            NewPipe.init(DownloaderImpl.init(null));

            // Get the stream extractor for YouTube video
            StreamExtractor extractor = ServiceList.YouTube.getStreamExtractor(url);

            // Fetch video streams
            extractor.fetchPage();

            // Return a success result with the extracted video stream
            return new ExtractorResult(ExtractorResult.Status.SUCCESS, extractor.getVideoStreams().get(0).getContent(), null);
          } catch (Exception e) {
            // Handle extraction failure
            System.out.println("Failed to StreamExtractor " + e);
            return new ExtractorResult(ExtractorResult.Status.ERROR, null, e);
          }
        })
        .subscribeOn(Schedulers.io()) // Run extraction on IO thread
        .observeOn(AndroidSchedulers.mainThread()) // Observe results on the main thread
        .subscribe(result -> {
          // Hide progress bar after extraction completes
          binding.progressCircular.setVisibility(View.GONE);

          // Check if extraction was successful
          if (result.getStatus() == ExtractorResult.Status.SUCCESS) {
            // Create a MediaItem from the extracted video URL
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(result.getUrl()));

            // Set the media item to the player and prepare for playback
            player.setMediaItem(mediaItem);
            player.prepare();
            player.setPlayWhenReady(true); // Start playing the video immediately
          } else {
            // Display the error message if extraction failed
            binding.errorMsg.setText(result.getError().getMessage());
          }
        });
  }

  /**
   * Returns the ExoPlayer instance associated with this view.
   *
   * @return ExoPlayer instance.
   */
  public ExoPlayer getPlayer() {
    return player;
  }

  /**
   * Releases the ExoPlayer and disposes of any ongoing extraction process.
   */
  public void release() {
    // Dispose of the extraction process if it's running
    if (disposable != null) {
      disposable.dispose();
      disposable = null;
    }

    // Release the player resources
    if (player != null) {
      player.release();
      player = null;
    }
  }

  /**
   * Ensures the player is released when the view is detached from the window.
   */
  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    release();
  }
}
