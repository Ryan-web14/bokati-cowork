package com.sni.bokaticowork.features.crm.service.interfaces;

import com.sni.bokaticowork.features.crm.model.Lead;

public interface CrmEmailService {
    void sendLeadAssigned(Lead lead);
    void sendStageChanged(Lead lead, String previousStage);
    void sendDormantAlert(Lead lead);
}
