package com.kws.backend.backend.service;

import com.kws.backend.backend.model.ParkProfile;
import com.kws.backend.backend.repository.ParkProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParkProfileService {
    private final ParkProfileRepository parkProfileRepository;

    public List<ParkProfile> getAllProfiles() {
        return parkProfileRepository.findAll();
    }

    public Optional<ParkProfile> getProfileByCodeOrName(String identifier) {
        Optional<ParkProfile> byCode = parkProfileRepository.findByCodeIgnoreCase(identifier);
        if (byCode.isPresent()) {
            return byCode;
        }
        return parkProfileRepository.findByParkNameIgnoreCase(identifier);
    }

    public ParkProfile saveProfile(ParkProfile profile) {
        return parkProfileRepository.save(profile);
    }
}
