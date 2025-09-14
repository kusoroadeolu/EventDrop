package com.victor.EventDrop.rooms.listeners;

public interface RoomQueueListenerService {
    void startListeners(String roomCode);

    void stopAllListeners(String roomCode);
}
