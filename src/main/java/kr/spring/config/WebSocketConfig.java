package kr.spring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // STOMP 웹소켓 메시징 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 클라이언트(브라우저)가 최초로 웹소켓 연결을 맺기 위해 접속할 엔드포인트(주소)
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // CORS 에러 방지 (모든 도메인 허용)
                .withSockJS(); // 구버전 브라우저에서도 웹소켓이 동작하도록 SockJS 지원
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 2. 메시지를 "받을 때" (구독: Subscribe) 사용할 주소의 접두사
        // 예: /sub/chat/room/5 (5번 방 구독)
        registry.enableSimpleBroker("/sub");
        
        // 3. 메시지를 "보낼 때" (발행: Publish) 사용할 주소의 접두사
        // 클라이언트가 /pub/chat/message 로 보내면 컨트롤러의 @MessageMapping 이 받음
        registry.setApplicationDestinationPrefixes("/pub");
    }
}