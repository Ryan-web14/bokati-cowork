package com.sni.bokaticowork.core.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final WebSocketSubscriptionInterceptor subscriptionInterceptor;

    @Value("${bokati.websocket.use-stomp-relay:false}")
    private boolean useStompRelay;

    /**
     * When set (e.g. CLOUDAMQP_URL: amqps://user:pass@host/vhost), the STOMP relay host,
     * credentials, virtual host and TLS flag are derived from it · the single source of truth
     * shared with the AMQP connection. Only the STOMP port is kept separate. Leave empty to use
     * the explicit relay-* properties below.
     */
    @Value("${bokati.websocket.stomp.broker-url:}")
    private String brokerUrl;

    @Value("${bokati.websocket.stomp.relay-host:localhost}")
    private String relayHost;

    @Value("${bokati.websocket.stomp.relay-port:61613}")
    private int relayPort;

    @Value("${bokati.websocket.stomp.use-tls:false}")
    private boolean useTls;

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
            applyBrokerUrlIfPresent();
            StompBrokerRelayRegistration relay = registry.enableStompBrokerRelay("/topic", "/queue")
                    .setVirtualHost(virtualHost)
                    .setClientLogin(clientLogin)
                    .setClientPasscode(clientPasscode)
                    .setSystemLogin(systemLogin)
                    .setSystemPasscode(systemPasscode)
                    .setSystemHeartbeatSendInterval(10_000)
                    .setSystemHeartbeatReceiveInterval(10_000);
            if (useTls) {
                // Spring's default relay TCP client is plaintext; managed brokers
                // (e.g. CloudAMQP) expose STOMP only over TLS, so wrap a secured client.
                relay.setTcpClient(new ReactorNettyTcpClient<>(
                        client -> client.host(relayHost).port(relayPort).secure(),
                        new StompReactorNettyCodec()));
            } else {
                relay.setRelayHost(relayHost).setRelayPort(relayPort);
            }
            log.info("STOMP broker relay enabled · host={} port={} vhost={} tls={}",
                    relayHost, relayPort, virtualHost, useTls);
        } else {
            registry.enableSimpleBroker("/topic", "/queue");
        }
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Derives the STOMP relay host, credentials, virtual host and TLS flag from {@link #brokerUrl}
     * (typically CLOUDAMQP_URL) so the relay uses the exact same broker/vhost as the working AMQP
     * connection. On CloudAMQP shared plans the vhost equals the username · carrying it from the URL
     * avoids the common mistake of defaulting the vhost to "/". The STOMP port is left untouched.
     */
    private void applyBrokerUrlIfPresent() {
        if (!StringUtils.hasText(brokerUrl)) {
            return;
        }
        try {
            URI uri = URI.create(brokerUrl.trim());
            if (StringUtils.hasText(uri.getHost())) {
                relayHost = uri.getHost();
            }
            String userInfo = uri.getUserInfo();
            if (StringUtils.hasText(userInfo)) {
                int sep = userInfo.indexOf(':');
                String user = sep >= 0 ? userInfo.substring(0, sep) : userInfo;
                String pass = sep >= 0 ? userInfo.substring(sep + 1) : "";
                clientLogin = systemLogin = decode(user);
                clientPasscode = systemPasscode = decode(pass);
            }
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                virtualHost = decode(path.substring(1));
            }
            if (uri.getScheme() != null) {
                useTls = uri.getScheme().equalsIgnoreCase("amqps");
            }
        } catch (Exception ex) {
            log.warn("Could not parse bokati.websocket.stomp.broker-url · falling back to explicit relay-* properties: {}",
                    ex.getMessage());
        }
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
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
