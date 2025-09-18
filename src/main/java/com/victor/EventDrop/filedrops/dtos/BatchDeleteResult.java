package com.victor.EventDrop.filedrops.dtos;

import java.util.ArrayList;

public record BatchDeleteResult(ArrayList<String> successfulDeletes,
                                ArrayList<String> failedDeletes) {
}
