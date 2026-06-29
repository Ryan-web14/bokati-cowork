package com.sni.bokaticowork.core.event;

public final class WebSocketTopics {
    private WebSocketTopics() {}

    public static final String USER_NOTIFICATIONS = "/queue/notifications";
    public static final String USER_UNREAD_COUNT  = "/queue/notifications/unread-count";

    public static final String ADMIN_ALERTS       = "/topic/admin/alerts";
    public static final String INVENTORY_ALERTS   = "/topic/inventory/alerts";

    public static final String SUPPORT_TICKETS    = "/topic/support/tickets/";
}
