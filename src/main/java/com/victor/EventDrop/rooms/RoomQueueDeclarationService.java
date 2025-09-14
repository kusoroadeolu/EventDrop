package com.victor.EventDrop.rooms;

public interface RoomQueueDeclarationService {
    String declareRoomJoinQueueAndBinding(String roomCode);

    String declareRoomFileUploadQueueAndBinding(String roomCode);

    String declareRoomFileDeleteQueueAndBinding(String roomCode);

    String declareRoomLeaveQueueAndBinding(String roomCode);

    void deleteAllQueues(String roomCode);
}
