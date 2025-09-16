package com.victor.EventDrop.filedrops;


import com.victor.EventDrop.filedrops.dtos.FileDropResponseDto;
import org.springframework.stereotype.Service;

@Service
public class FileDropMapper {
     FileDropResponseDto toResponseDto(FileDrop fileDrop){
          return new FileDropResponseDto(
                  fileDrop.getFileId().toString(),
                  fileDrop.getOriginalFileName(),
                  fileDrop.getFileSizeInMB(),
                  fileDrop.getUploadedAt()
          );
     }
}
