package com.tientoan.rikka.youtube_player.model;

public class VideoYoutube {
  private String title;
  private String streamUrl;

  public VideoYoutube(String title, String streamUrl) {
    this.title = title;
    this.streamUrl = streamUrl;
  }

  public String getStreamUrl() {
    return streamUrl;
  }

  public void setStreamUrl(String streamUrl) {
    this.streamUrl = streamUrl;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
