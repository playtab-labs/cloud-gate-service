package com.playtab.cloudgateservice.service;

import com.playtab.cloudgateservice.domain.wristband.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WristbandService {

    private static final int MAX_WRISTBANDS_PER_IDENTITY = 2;

    private final WristbandRepository wristbandRepository;
    private final WristbandOwnershipRepository ownershipRepository;
    private final WristbandCacheService wristbandCacheService;

    public WristbandService(WristbandRepository wristbandRepository,
                            WristbandOwnershipRepository ownershipRepository,
                            WristbandCacheService wristbandCacheService) {
        this.wristbandRepository = wristbandRepository;
        this.ownershipRepository = ownershipRepository;
        this.wristbandCacheService = wristbandCacheService;
    }

    @Transactional
    public WristbandOwnership linkWristband(UUID identityId, String rfid) {
        Wristband wristband = wristbandRepository.findByRfid(rfid.toUpperCase())
                .orElseThrow(() -> new WristbandNotFoundException("Wristband not found: " + rfid));

        if (ownershipRepository.existsByWristbandId(wristband.getId())) {
            throw new WristbandAlreadyClaimedException("Wristband already claimed: " + rfid);
        }

        long currentCount = ownershipRepository.countByIdentityId(identityId);
        if (currentCount >= MAX_WRISTBANDS_PER_IDENTITY) {
            throw new WristbandLimitExceededException("Maximum wristband limit exceeded: " + MAX_WRISTBANDS_PER_IDENTITY);
        }

        if (ownershipRepository.existsByIdentityIdAndActiveDate(identityId, wristband.getActiveDate())) {
            throw new WristbandDuplicateDateException(
                    "Already owns a wristband for date: " + wristband.getActiveDate());
        }

        WristbandOwnership ownership = new WristbandOwnership(identityId, wristband);
        WristbandOwnership saved = ownershipRepository.save(ownership);

        wristbandCacheService.cacheLinked(wristband.getRfid(), wristband.getActiveDate());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<WristbandOwnership> getMyWristbands(UUID identityId) {
        return ownershipRepository.findByIdentityIdWithWristband(identityId);
    }

    // 비즈니스 예외 클래스들
    public static class WristbandNotFoundException extends RuntimeException {
        public WristbandNotFoundException(String message) { super(message); }
    }

    public static class WristbandAlreadyClaimedException extends RuntimeException {
        public WristbandAlreadyClaimedException(String message) { super(message); }
    }

    public static class WristbandLimitExceededException extends RuntimeException {
        public WristbandLimitExceededException(String message) { super(message); }
    }

    public static class WristbandDuplicateDateException extends RuntimeException {
        public WristbandDuplicateDateException(String message) { super(message); }
    }
}
