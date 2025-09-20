package com.victor.EventDrop.occupants;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@RequiredArgsConstructor
public enum OccupantRole implements GrantedAuthority {
    OCCUPANT("ROLE_OCCUPANT"),
    OWNER("ROLE_OWNER");

    private final String authority;


    @Override
    public String getAuthority() {
        return this.authority;
    }
}
