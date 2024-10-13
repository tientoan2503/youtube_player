package com.tientoan.rikka.youtube_player.ui;

public class ExtractorResult {

  public enum Status {
    ERROR, SUCCESS
  }

  private Status status;
  private String url;
  private Exception error;

  public ExtractorResult(Status status, String url, Exception error) {
    this.status = status;
    this.url = url;
    this.error = error;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Exception getError() {
    return error;
  }

  public void setError(Exception error) {
    this.error = error;
  }

}
