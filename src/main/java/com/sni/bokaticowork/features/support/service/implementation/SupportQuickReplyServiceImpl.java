package com.sni.bokaticowork.features.support.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.support.dto.SupportDtos.QuickReplyRequest;
import com.sni.bokaticowork.features.support.dto.SupportDtos.QuickReplyResponse;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.mapper.SupportTicketMapper;
import com.sni.bokaticowork.features.support.model.QuickReply;
import com.sni.bokaticowork.features.support.repository.QuickReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SupportQuickReplyServiceImpl {

    private final QuickReplyRepository repository;
    private final SupportTicketMapper mapper;

    public QuickReplyResponse create(QuickReplyRequest request) {
        QuickReply reply = QuickReply.builder()
                .category(request.category())
                .title(request.title().trim())
                .body(request.body().trim())
                .build();
        return mapper.toQuickReplyResponse(repository.save(reply));
    }

    @Transactional(readOnly = true)
    public List<QuickReplyResponse> list(TicketCategory category) {
        List<QuickReply> replies = category != null
                ? repository.findAllByCategoryAndActiveTrue(category)
                : repository.findAllByActiveTrue();
        return replies.stream().map(mapper::toQuickReplyResponse).toList();
    }

    public void delete(Long id) {
        QuickReply reply = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quick reply not found: " + id));
        reply.setActive(false);
        repository.save(reply);
    }
}
