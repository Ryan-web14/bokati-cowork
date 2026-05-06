package com.sni.bokaticowork.features.visitor.worker;

import com.sni.bokaticowork.features.visitor.enums.VisitorPassStatus;
import com.sni.bokaticowork.features.visitor.model.VisitorPass;
import com.sni.bokaticowork.features.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VisitorPassExpiryWorker {

    private final VisitorPassRepository passRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireScheduledPasses() {
        List<VisitorPass> expired = passRepository
                .findAllByStatusAndValidUntilBefore(VisitorPassStatus.SCHEDULED, Instant.now());
        if (expired.isEmpty()) return;
        expired.forEach(p -> p.setStatus(VisitorPassStatus.EXPIRED));
        passRepository.saveAll(expired);
        log.info("Expired {} visitor passes", expired.size());
    }
}