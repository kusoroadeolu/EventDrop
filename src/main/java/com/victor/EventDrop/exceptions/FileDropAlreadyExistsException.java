package com.victor.EventDrop.exceptions;

public class FileDropAlreadyExistsException extends RuntimeException {
  public FileDropAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }

  public FileDropAlreadyExistsException() {
    super();
  }

  public FileDropAlreadyExistsException(String message) {
        super(message);
    }
}
