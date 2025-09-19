package com.victor.EventDrop.rooms.listeners;

import com.victor.EventDrop.rooms.RoomQueueDeclarationServiceImpl;
import com.victor.EventDrop.rooms.configproperties.RoomJoinConfigProperties;
import com.victor.EventDrop.rooms.configproperties.RoomLeaveConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomQueueListenerServiceImpl implements RoomQueueListenerService {

    private final RoomJoinListenerService roomJoinListenerService;
    private final RoomLeaveListenerService roomLeaveListenerService;
    private final RoomQueueDeclarationServiceImpl roomQueueDeclarationService;
    private final RoomJoinConfigProperties roomJoinConfigProperties;
    private final RoomLeaveConfigProperties roomLeaveConfigProperties;


    @Override
    public void startListeners(String roomCode){
        String queueJoinName = roomQueueDeclarationService.declareRoomJoinQueueAndBinding(roomCode);
        roomJoinListenerService.startQueueListener(queueJoinName);
        String queueLeaveName = roomQueueDeclarationService.declareRoomLeaveQueueAndBinding(roomCode);
        roomLeaveListenerService.startQueueListener(queueLeaveName);
        log.info("Successfully started room queue listeners for room: {}", roomCode);
    }

    @Override
    public void stopAllListeners(String roomCode){
        roomJoinListenerService.stopListener(roomJoinConfigProperties.getQueuePrefix() + roomCode);
        roomLeaveListenerService.stopQueueListener(roomLeaveConfigProperties.getQueuePrefix() + roomCode);
        roomQueueDeclarationService.deleteAllQueues(roomCode);
        log.info("Successfully stopped all listeners and queues for room: {}", roomCode);
    }
}
