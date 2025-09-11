package com.victor.EventDrop.occupants;

import com.victor.EventDrop.exceptions.OccupantCreationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class OccupantCommandServiceImpl implements OccupantCommandService {
    private final OccupantRepository occupantRepository;

    @Override
    public Occupant saveOccupant(Occupant occupant){
        try {
            log.info("Attempting to save occupant: {}", occupant.getOccupantName());
            Occupant saved = occupantRepository.save(occupant);
            log.info("Successfully saved occupant: {}", occupant.getOccupantName());
            return saved;
        }catch (Exception e){
            log.info("An unexpected error occurred while trying to save occupant: {}", occupant.getOccupantName(), e);
            throw new OccupantCreationException(String.format("An unexpected error occurred while trying to save occupant: %s", occupant.getOccupantName()), e);
        }
    }
}
