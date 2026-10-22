package vn.iotstar.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketConfigTest {
    private final WebSocketConfig.PrivateNotificationInterceptor interceptor=new WebSocketConfig.PrivateNotificationInterceptor();

    @Test
    void authenticatedOperatorsAndCustomersCanSubscribeOnlyToTheirPersonalNotifications(){
        for(String role:List.of("STAFF","MANAGER","ADMIN","CUSTOMER")){
            Authentication user=authenticated(role);
            assertAllowed(frame(StompCommand.CONNECT,null,user));
            assertAllowed(frame(StompCommand.SUBSCRIBE,"/user/queue/notifications",user));
            assertAllowed(frame(StompCommand.UNSUBSCRIBE,null,user));
            assertAllowed(frame(StompCommand.DISCONNECT,null,user));
            StompHeaderAccessor heartbeat=StompHeaderAccessor.createForHeartbeat();heartbeat.setUser(user);
            assertAllowed(MessageBuilder.createMessage(new byte[0],heartbeat.getMessageHeaders()));
        }
    }

    @Test
    void anonymousAndUnauthenticatedClientsCannotConnectOrSubscribe(){
        Authentication anonymous=new AnonymousAuthenticationToken("test","anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        Authentication unauthenticated=new UsernamePasswordAuthenticationToken("customer","unused");
        for(Authentication user:java.util.Arrays.asList(null,anonymous,unauthenticated)){
            assertDenied(frame(StompCommand.CONNECT,null,user));
            assertDenied(frame(StompCommand.SUBSCRIBE,"/user/queue/notifications",user));
        }
    }

    @Test
    void directBrokerSubscriptionsWildcardSubscriptionsAndForgedMessagesAreDenied(){
        Authentication customer=authenticated("CUSTOMER");
        for(String destination:List.of("/queue/**","/topic/**","/queue/notifications-userOTHERSESSION",
                "/user/admin/queue/notifications","/user/queue/notifications/**","/user/queue/notifications/")){
            assertDenied(frame(StompCommand.SUBSCRIBE,destination,customer));
        }
        for(String destination:List.of("/user/admin/queue/notifications","/queue/notifications-userOTHERSESSION",
                "/user/queue/notifications","/app/notifications")){
            assertDenied(frame(StompCommand.SEND,destination,customer));
        }
    }

    private void assertAllowed(Message<?> message){assertThat(interceptor.preSend(message,null)).isSameAs(message);}
    private void assertDenied(Message<?> message){
        assertThatThrownBy(()->interceptor.preSend(message,null)).isInstanceOf(AccessDeniedException.class);
    }
    private Authentication authenticated(String role){
        return new UsernamePasswordAuthenticationToken("user-"+role,"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role)));
    }
    private Message<?> frame(StompCommand command,String destination,Authentication user){
        StompHeaderAccessor headers=StompHeaderAccessor.create(command);
        headers.setSessionId("SESSION-1");headers.setUser(user);
        if(destination!=null)headers.setDestination(destination);
        if(command==StompCommand.SUBSCRIBE||command==StompCommand.UNSUBSCRIBE)headers.setSubscriptionId("orders");
        return MessageBuilder.createMessage(new byte[0],headers.getMessageHeaders());
    }
}
