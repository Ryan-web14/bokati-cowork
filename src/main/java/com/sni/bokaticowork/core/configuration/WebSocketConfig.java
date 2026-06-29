package com.sni.bokaticowork.core.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final WebSocketSubscriptionInterceptor subscriptionInterceptor;

    @Value("${bokati.websocket.use-stomp-relay:false}")
    private boolean useStompRelay;

    @Value("${bokati.websocket.stomp.relay-host:localhost}")
    private String relayHost;

    @Value("${bokati.websocket.stomp.relay-port:61613}")
    private int relayPort;

    @Value("${bokati.websocket.stomp.client-login:guest}")
    private String clientLogin;

    @Value("${bokati.websocket.stomp.client-passcode:guest}")
    private String clientPasscode;

    @Value("${bokati.websocket.stomp.system-login:guest}")
    private String systemLogin;

    @Value("${bokati.websocket.stomp.system-passcode:guest}")
    private String systemPasscode;

    @Value("${bokati.websocket.stomp.virtual-host:/}")
    private String virtualHost;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (useStompRelay) {
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setVirtualHost(virtualHost)
                    .setClientLogin(clientLogin)
                    .setClientPasscode(clientPasscode)
                    .setSystemLogin(systemLogin)
                    .setSystemPasscode(systemPasscode)
                    .setSystemHeartbeatSendInterval(10_000)
                    .setSystemHeartbeatReceiveInterval(10_000);
        } else {
            registry.enableSimpleBroker("/topic", "/queue");
        }
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25_000);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(64 * 1024)
                .setSendBufferSizeLimit(512 * 1024)
                .setSendTimeLimit(20_000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor, subscriptionInterceptor);
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(16)
                .queueCapacity(200);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(20)
                .queueCapacity(500);
    }
}
