package vn.iotstar.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.*;
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override public void configureMessageBroker(MessageBrokerRegistry registry){
        registry.enableSimpleBroker("/topic","/queue");registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry){registry.addEndpoint("/ws");}
    @Override public void configureClientInboundChannel(ChannelRegistration registration){
        registration.interceptors(new PrivateNotificationInterceptor());
    }

    static final class PrivateNotificationInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(Message<?> message,MessageChannel channel){
            StompHeaderAccessor headers=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);
            if(headers==null||!(headers.getUser() instanceof Authentication authentication)
                    ||!authentication.isAuthenticated()||authentication instanceof AnonymousAuthenticationToken){
                throw new AccessDeniedException("Đăng nhập để nhận thông báo");
            }
            if(headers.getMessageType()==SimpMessageType.HEARTBEAT)return message;
            var command=headers.getCommand();
            if(command==null)throw new AccessDeniedException("Lệnh thông báo không hợp lệ");
            return switch(command){
                case CONNECT,STOMP,UNSUBSCRIBE,DISCONNECT -> message;
                case SUBSCRIBE -> {
                    if(!"/user/queue/notifications".equals(headers.getDestination()))
                        throw new AccessDeniedException("Chỉ được nhận thông báo cá nhân");
                    yield message;
                }
                default -> throw new AccessDeniedException("Không được gửi thông báo từ trình duyệt");
            };
        }
    }
}
