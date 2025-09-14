package com.victor.EventDrop.rooms.listeners;

import com.victor.EventDrop.rabbitmq.RoomFileDeleteListenerService;
import com.victor.EventDrop.rabbitmq.RoomFileUploadListenerService;
import com.victor.EventDrop.rabbitmq.RoomJoinListenerServiceService;
import com.victor.EventDrop.rabbitmq.RoomLeaveListenerService;
import com.victor.EventDrop.rooms.RoomQueueDeclarationServiceImpl;
import com.victor.EventDrop.rooms.configproperties.RoomFileDeleteConfigProperties;
import com.victor.EventDrop.rooms.configproperties.RoomFileUploadConfigProperties;
import com.victor.EventDrop.rooms.configproperties.RoomJoinConfigProperties;
import com.victor.EventDrop.rooms.configproperties.RoomLeaveConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomQueueListenerServiceImpl implements RoomQueueListenerService {

    private final RoomJoinListenerServiceService roomJoinListenerService;
    private final RoomFileUploadListenerService roomFileUploadListenerService;
    private final RoomFileDeleteListenerService roomFileDeleteListenerService;
    private final RoomLeaveListenerService roomLeaveListenerService;
    private final RoomQueueDeclarationServiceImpl roomQueueDeclarationService;
    private final RoomJoinConfigProperties roomJoinConfigProperties;
    private final RoomLeaveConfigProperties roomLeaveConfigProperties;
    private final RoomFileUploadConfigProperties roomFileUploadConfigProperties;
    private final RoomFileDeleteConfigProperties roomFileDeleteConfigProperties;

    @Override
    public void startListeners(String roomCode){
        String queueJoinName = roomQueueDeclarationService.declareRoomJoinQueueAndBinding(roomCode);
        roomJoinListenerService.startQueueListener(queueJoinName);
        String queueFileUploadName = roomQueueDeclarationService.declareRoomFileUploadQueueAndBinding(roomCode);
        roomFileUploadListenerService.stopQueueListener(queueFileUploadName);
        String queueFileDeleteName = roomQueueDeclarationService.declareRoomFileDeleteQueueAndBinding(roomCode);
        roomFileDeleteListenerService.startQueueListener(queueFileDeleteName);
        String queueLeaveName = roomQueueDeclarationService.declareRoomLeaveQueueAndBinding(roomCode);
        roomLeaveListenerService.startQueueListener(queueLeaveName);
        log.info("Successfully started room download and join listeners for room: {}", roomCode);
    }

    @Override
    public void stopAllListeners(String roomCode){
        roomJoinListenerService.stopListener(roomJoinConfigProperties.getQueuePrefix() + roomCode);
        roomFileUploadListenerService.stopQueueListener(roomFileUploadConfigProperties.getQueuePrefix() + roomCode);
        roomLeaveListenerService.stopQueueListener(roomLeaveConfigProperties.getQueuePrefix() + roomCode);
        roomFileDeleteListenerService.stopQueueListener(roomFileDeleteConfigProperties.getQueuePrefix() + roomCode);
    }
}
