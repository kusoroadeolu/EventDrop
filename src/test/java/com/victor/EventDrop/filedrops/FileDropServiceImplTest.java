package com.victor.EventDrop.filedrops;

import com.victor.EventDrop.exceptions.*;
import com.victor.EventDrop.filedrops.client.FileDropStorageClient;
import com.victor.EventDrop.filedrops.dtos.BatchDeleteResult;
import com.victor.EventDrop.filedrops.dtos.BatchUploadResult;
import com.victor.EventDrop.filedrops.dtos.FileDownloadResponseDto;
import com.victor.EventDrop.filedrops.dtos.FileDropResponseDto;
import com.victor.EventDrop.rooms.events.RoomEvent;
import com.victor.EventDrop.rooms.events.RoomEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDropServiceImplTest {


    @Mock
    private FileDropRepository fileDropRepository;
    @Mock
    private FileDropMapper fileDropMapper;
    @Mock
    private FileDropStorageClient fileDropStorageClient;
    @Mock
    private FileDropUtils fileDropUtils;
    @Mock
    private AsyncTaskExecutor asyncTaskExecutor;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private FileDropServiceImpl fileDropService;

    // A spy is used for testing batch uploads, which call a public method on the same class.
    @Spy
    @InjectMocks
    private FileDropServiceImpl spyFileDropService;

    private String roomCode;
    private MockMultipartFile mockFile;
    private FileDrop fileDrop;
    private FileDropResponseDto fileDropResponseDto;
    private UUID fileId;

    @BeforeEach
    void setUp() {
        roomCode = "ABCD";
        fileId = UUID.randomUUID();
        mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes());

        fileDrop = FileDrop.builder()
                .fileId(fileId)
                .fileName(roomCode + "/" + mockFile.getOriginalFilename())
                .originalFileName(mockFile.getOriginalFilename())
                .fileSize(mockFile.getSize())
                .roomCode(roomCode)
                .blobUrl("http://storage.com/blob")
                .isDeleted(false)
                .uploadedAt(LocalDateTime.now())
                .build();

        fileDropResponseDto = new FileDropResponseDto(
                fileId.toString(),
                mockFile.getOriginalFilename(),
                mockFile.getSize(),
                LocalDateTime.now()
        );


        // Mock the async executor to run tasks immediately for deterministic tests
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(asyncTaskExecutor).execute(any(Runnable.class));
    }

    @Nested
    @DisplayName("Upload Tests")
    class UploadTests {

        @Test
        void uploadFile_whenSuccessful_shouldReturnCorrectDto() throws IOException {
            // Arrange
            String blobUrl = "http://storage.com/blob";
            when(fileDropUtils.validateFileUpload(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));
            when(fileDropStorageClient.uploadFile(anyString(), anyLong(), any(InputStream.class))).thenReturn(CompletableFuture.completedFuture(blobUrl));
            when(fileDropRepository.save(any(FileDrop.class))).thenReturn(fileDrop);
            when(fileDropMapper.toResponseDto(any(FileDrop.class))).thenReturn(fileDropResponseDto);

            // Act
            FileDropResponseDto result = fileDropService.uploadFile(roomCode, mockFile);

            // Assert
            assertNotNull(result);
            assertEquals(fileDropResponseDto.fileId(), result.fileId());
            assertEquals(fileDropResponseDto.fileName(), result.fileName());
            verify(fileDropUtils).validateFileUpload(eq(mockFile), anyList(), eq(mockFile.getSize()));
            verify(fileDropStorageClient).uploadFile(eq(roomCode + "/" + mockFile.getOriginalFilename()), eq(mockFile.getSize()), any(InputStream.class));
            verify(fileDropRepository).save(any(FileDrop.class));
            verify(fileDropMapper).toResponseDto(fileDrop);
        }

        @Test
        void uploadFile_whenValidationFails_shouldThrowException() throws IOException {
            // Arrange
            var exception = new FileDropThresholdExceededException("Limit exceeded");
            when(fileDropUtils.validateFileUpload(any(), any(), anyLong())).thenReturn(CompletableFuture.failedFuture(exception));

            // Act & Assert
            var ex = assertThrows(CompletionException.class, () -> fileDropService.uploadFile(roomCode, mockFile));
            verify(fileDropStorageClient, never()).uploadFile(anyString(), anyLong(), any());
            assertNotNull(ex.getCause());
            assertInstanceOf(FileDropThresholdExceededException.class, ex.getCause());

        }

        @Test
        void uploadFiles_whenAllSucceed_shouldReturnSuccessfulResults() {
            // Arrange
            MockMultipartFile file1 = new MockMultipartFile("f1", "f1.txt", "text/plain", "1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("f2", "f2.txt", "text/plain", "2".getBytes());
            List<MultipartFile> files = List.of(file1, file2);

            var dto1 = new FileDropResponseDto(UUID.randomUUID().toString(), "f1.txt", 1L, LocalDateTime.now());
            var dto2 = new FileDropResponseDto(UUID.randomUUID().toString(), "f2.txt", 1L, LocalDateTime.now());

            doReturn(dto1).when(spyFileDropService).uploadFile(roomCode, file1);
            doReturn(dto2).when(spyFileDropService).uploadFile(roomCode, file2);

            // Act
            BatchUploadResult result = spyFileDropService.uploadFiles(roomCode, files);

            // Assert
            verify(fileDropUtils).validateBatchUpload(anyList(), eq(files));
            assertEquals(2, result.successfulUploads().size());
            assertTrue(result.failedUploads().isEmpty());
        }

        @Test
        void uploadFiles_whenSomeFail_shouldReturnPartialResults() {
            // Arrange
            MockMultipartFile file1 = new MockMultipartFile("f1", "f1.txt", "text/plain", "1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("f2", "f2.txt", "text/plain", "2".getBytes());
            List<MultipartFile> files = List.of(file1, file2);

            var dto1 = new FileDropResponseDto(UUID.randomUUID().toString(), "f1.txt", 1L, LocalDateTime.now());
            var exception = new FileDropUploadException("Upload failed");

            doReturn(dto1).when(spyFileDropService).uploadFile(roomCode, file1);
            doThrow(exception).when(spyFileDropService).uploadFile(roomCode, file2);

            // Act
            BatchUploadResult result = spyFileDropService.uploadFiles(roomCode, files);

            // Assert
            assertEquals(1, result.successfulUploads().size());
            assertEquals(1, result.failedUploads().size());
            assertEquals("f1.txt", result.successfulUploads().get(0).fileName());
            assertEquals("f2.txt", result.failedUploads().get(0).fileName());
        }
    }

    @Nested
    @DisplayName("Download Tests")
    class DownloadTests {

        @Test
        void downloadFile_whenSuccessful_shouldReturnDownloadDto() {
            // Arrange
            String expectedUrl = "http://storage.com/signed-url";
            when(fileDropRepository.findById(fileId)).thenReturn(Optional.of(fileDrop));
            when(fileDropStorageClient.downloadFile(fileDrop.getFileName(), fileDrop.getBlobUrl())).thenReturn(expectedUrl);

            // Act
            FileDownloadResponseDto result = fileDropService.downloadFile(fileId, roomCode);

            // Assert
            assertNotNull(result);
            assertEquals(expectedUrl, result.downloadUrl());
        }

        @Test
        void downloadFile_whenFileNotExists_shouldThrowNoSuchFileDropException() {
            // Arrange
            when(fileDropRepository.findById(fileId)).thenReturn(Optional.empty());

            // Act & Assert
            var ex =
                    assertThrows(CompletionException.class, () -> fileDropService.downloadFile(fileId, roomCode));
            assertNotNull(ex.getCause());
            assertInstanceOf(FileDropDownloadException.class, ex.getCause());

        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        void deleteFiles_whenSuccessful_shouldDeleteFromStorageAndDb() {
            // Arrange
            FileDrop fd1 = FileDrop.builder().fileName("f1.txt").build();
            FileDrop fd2 = FileDrop.builder().fileName("f2.txt").build();
            List<FileDrop> fileDrops = List.of(fd1, fd2);
            List<UUID> fileIds = List.of(UUID.randomUUID(), UUID.randomUUID());

            when(fileDropRepository.findAllById(fileIds)).thenReturn(fileDrops);

            // Act
            BatchDeleteResult result = fileDropService.deleteFiles(roomCode, fileIds);

            // Assert
            verify(fileDropStorageClient).deleteFiles(List.of("f1.txt", "f2.txt"));
            verify(fileDropRepository).deleteAll(fileDrops);
            assertEquals(2, result.successfulDeletes().size());
            assertTrue(result.failedDeletes().isEmpty());
        }

        @Test
        void deleteFiles_whenStorageFails_shouldMarkFilesAsDeleted() {
            // Arrange
            FileDrop fd1 = FileDrop.builder().fileName("f1.txt").isDeleted(false).build();
            List<FileDrop> fileDrops = List.of(fd1);
            List<UUID> fileIds = List.of(UUID.randomUUID());

            when(fileDropRepository.findAllById(fileIds)).thenReturn(fileDrops);
            doThrow(new RuntimeException("Storage unavailable")).when(fileDropStorageClient).deleteFiles(anyList());

            // Act
            BatchDeleteResult result = fileDropService.deleteFiles(roomCode, fileIds);

            // Assert
            verify(fileDropRepository, never()).deleteAll(anyList());
            ArgumentCaptor<FileDrop> captor = ArgumentCaptor.forClass(FileDrop.class);
            verify(fileDropRepository).save(captor.capture());

            assertTrue(captor.getValue().isDeleted());
            assertTrue(result.successfulDeletes().isEmpty());
            assertEquals(1, result.failedDeletes().size());
            assertEquals("f1.txt", result.failedDeletes().get(0));
        }

        @Test
        void deleteByRoomCode_whenCalled_shouldInvokeRepository() {
            // Act
            fileDropService.deleteByRoomCode(roomCode);

            // Assert
            verify(fileDropRepository).deleteByRoomCode(roomCode);
        }
    }

    @Nested
    @DisplayName("Getter and Publisher Tests")
    class GetterAndPublisherTests {

        @Test
        void getFileDrops_shouldReturnOnlyNonDeletedFiles() {
            // Arrange
            FileDrop activeDrop = FileDrop.builder().isDeleted(false).originalFileName("active.txt").build();
            FileDrop deletedDrop = FileDrop.builder().isDeleted(true).originalFileName("deleted.txt").build();
            var activeDto = new FileDropResponseDto(UUID.randomUUID().toString(), "active.txt", 123L, LocalDateTime.now());

            when(fileDropRepository.findByRoomCode(roomCode)).thenReturn(List.of(activeDrop, deletedDrop));
            when(fileDropMapper.toResponseDto(activeDrop)).thenReturn(activeDto);

            // Act
            List<FileDropResponseDto> result = fileDropService.getFileDrops(roomCode);

            // Assert
            assertEquals(1, result.size());
            assertEquals("active.txt", result.get(0).fileName());
            verify(fileDropMapper, never()).toResponseDto(deletedDrop);
        }

        @Test
        void publishRoomEvent_shouldCallApplicationEventPublisher() {
            // Arrange
            RoomEvent event = new RoomEvent(
                    "File action occurred",
                    LocalDateTime.now(),
                    com.victor.EventDrop.rooms.events.RoomEventType.ROOM_FILE_UPLOAD,
                    roomCode,
                    1
            );

            // Act
            fileDropService.publishRoomEvent(event);

            // Assert
            verify(applicationEventPublisher).publishEvent(event);
        }
    }
}