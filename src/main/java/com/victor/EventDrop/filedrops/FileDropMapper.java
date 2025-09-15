package com.victor.EventDrop.filedrops;


import org.springframework.stereotype.Service;

@Service
public class FileDropMapper {
     FileDropResponseDto toResponseDto(FileDrop fileDrop){
          return new FileDropResponseDto(
                  fileDrop.getFileId().toString(),
                  fileDrop.getOriginalFileName(),
                  fileDrop.getUploadedAt()
          );
     }
}
