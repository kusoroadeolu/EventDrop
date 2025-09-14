package com.victor.EventDrop.filedrops;

import com.victor.EventDrop.occupants.Occupant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/filedrop")
public class FileDropController {

    private final FileDropServiceImpl fileDropService;

    @PostMapping("/upload")
    //@PreAuthorize("hasRole('OWNER')")
    public  ResponseEntity<FileDropResponseDto> uploadFile(@AuthenticationPrincipal Occupant occupant, @RequestParam("file")MultipartFile file) throws IOException {
        var fileDropResponseDto = fileDropService.uploadFile(occupant.getRoomCode(), occupant.getRoomExpiry() ,file);
        return new ResponseEntity<>(fileDropResponseDto, HttpStatus.CREATED);

    }

    @GetMapping("/download/{id}")
    public ResponseEntity<CompletableFuture<String>> downloadFile(@AuthenticationPrincipal Occupant occupant, @PathVariable("id") String fileId){
        CompletableFuture<String> downloadUrl = fileDropService.downloadFileAsync( UUID.fromString(fileId), occupant.getRoomCode());
        return ResponseEntity.status(302).body(downloadUrl);
    }
}
