package com.victor.EventDrop.exceptions;

public class FileDropUploadException extends RuntimeException {
  public FileDropUploadException(String message) {
    super(message);
  }

  public FileDropUploadException(String message, Throwable cause) {
    super(message, cause);
  }
}
