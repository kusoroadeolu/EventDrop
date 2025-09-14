package com.victor.EventDrop.exceptions;

public class FileDropDownloadException extends RuntimeException {
    public FileDropDownloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileDropDownloadException(String message) {
        super(message);
    }
}
