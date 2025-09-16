package com.victor.EventDrop.rooms.dtos;

import com.victor.EventDrop.occupants.OccupantRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class RoomJoinRequestDto{
        @NotEmpty(message = "Username cannot be empty")
        @Size(message = "Username cannot be less than 3 characters and cannot be greater than 20 characters", min = 3, max = 20)
        private String username;

        private OccupantRole role;

        @NotEmpty(message = "Room code cannot be empty")
        @Size(message = "Room code must be 8 characters", min = 8, max = 8)

        private String roomCode;


        //Only the roomcode and the username is needed info on the front end. Dont include the occupant role
        public RoomJoinRequestDto(String username, String roomCode){
            this.roomCode = roomCode;
            this.username = username;
        }

}
