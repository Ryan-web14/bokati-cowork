package com.sni.bokaticowork.features.subscription.subscription.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePassRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.PassSearchCriteria;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassEvent;
import com.sni.bokaticowork.features.subscription.subscription.model.PassStatusHistory;
import com.sni.bokaticowork.features.subscription.subscription.model.PassRenewalSchedule;
import com.sni.bokaticowork.features.subscription.repository.PassEventRepository;
import com.sni.bokaticowork.features.subscription.repository.PassStatusHistoryRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRenewalScheduleRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPath.V1 + "/passes")
@RequiredArgsConstructor
public class PassController {

    private final PassService passService;
    private final PassEventRepository passEventRepository;
    private final PassStatusHistoryRepository passStatusHistoryRepository;
    private final PassRenewalScheduleRepository passRenewalScheduleRepository;

    @PostMapping
    public ResponseEntity<PassResponse> create(@Valid @RequestBody CreatePassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passService.create(request));
    }

    @GetMapping("/{passNumber}")
    public ResponseEntity<PassResponse> get(@PathVariable String passNumber) {
        return ResponseEntity.ok(passService.get(passNumber));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<PassResponse>> list(
            @RequestParam(required = false) SubscriberType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) PassType passType,
            @RequestParam(required = false) PassStatus status,
            @RequestParam(required = false) Instant expiringBefore,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(passService.list(
                PassSearchCriteria.builder()
                        .ownerType(ownerType)
                        .ownerCode(ownerCode)
                        .passType(passType)
                        .status(status)
                        .expiringBefore(expiringBefore)
                        .build(),
                pageable
        ));
    }

    @PostMapping("/{passNumber}/activate")
    public ResponseEntity<PassResponse> activate(@PathVariable String passNumber) {
        return ResponseEntity.ok(passService.activate(passNumber, "Activation manuelle"));
    }

    @PatchMapping("/{passNumber}/cancel")
    public ResponseEntity<PassResponse> cancel(@PathVariable String passNumber,
                                               @RequestBody(required = false) Map<String, String> request) {
        return ResponseEntity.ok(passService.cancel(passNumber, request == null ? null : request.get("reason")));
    }

    @GetMapping("/{passNumber}/events")
    public ResponseEntity<List<PassEvent>> events(@PathVariable String passNumber) {
        Pass pass = passService.getForService(passNumber);
        return ResponseEntity.ok(passEventRepository.findAllByPassOrderByOccurredAtDesc(pass));
    }

    @GetMapping("/{passNumber}/history")
    public ResponseEntity<List<PassStatusHistory>> history(@PathVariable String passNumber) {
        Pass pass = passService.getForService(passNumber);
        return ResponseEntity.ok(passStatusHistoryRepository.findAllByPassOrderByChangedAtDesc(pass));
    }

    @GetMapping("/{passNumber}/renewal")
    public ResponseEntity<PassRenewalSchedule> renewal(@PathVariable String passNumber) {
        Pass pass = passService.getForService(passNumber);
        return ResponseEntity.ok(passRenewalScheduleRepository.findByPassId(pass.getId()).orElse(null));
    }
}
