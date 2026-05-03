package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.ressource.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BookingVirtualMeetingSupport {

    private final String baseUrl;

    public BookingVirtualMeetingSupport(@Value("${bokati.booking.virtual-room.base-url:https://meet.jit.si}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String meetingUrl(Resource resource, String bookingNumber) {
        if (!isVirtual(resource)) {
            return null;
        }
        String base = StringUtils.hasText(baseUrl) ? baseUrl.replaceAll("/+$", "") : "https://meet.jit.si";
        return base + "/bokati-" + bookingNumber.toLowerCase();
    }

    private boolean isVirtual(Resource resource) {
        if (resource == null) {
            return false;
        }
        String code = resource.getCode();
        String name = resource.getName();
        String typeCode = resource.getResourceType() == null ? null : resource.getResourceType().getCode();
        String typeName = resource.getResourceType() == null ? null : resource.getResourceType().getName();
        return containsVirtual(code) || containsVirtual(name) || containsVirtual(typeCode) || containsVirtual(typeName);
    }

    private boolean containsVirtual(String value) {
        return StringUtils.hasText(value) && value.toUpperCase().contains("VIRTUAL");
    }
}
