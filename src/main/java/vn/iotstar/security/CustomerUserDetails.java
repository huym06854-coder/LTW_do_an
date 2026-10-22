package vn.iotstar.security;
import java.util.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.UserStatus;
public class CustomerUserDetails implements UserDetails {
    private final Long id;
    private final String login;
    private final String fullName;
    private final String password;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;
    public CustomerUserDetails(User user){
        id=user.getId();login=user.getUsername();fullName=user.getFullName();password=user.getPassword();status=user.getStatus();
        authorities=List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().getName().name()));
    }
    public Long getId(){return id;}
    public String getFullName(){return fullName;}
    @Override public String getUsername(){return login;}
    @Override public String getPassword(){return password;}
    @Override public Collection<? extends GrantedAuthority> getAuthorities(){return authorities;}
    @Override public boolean isEnabled(){return status==UserStatus.ACTIVE;}
    @Override public boolean isAccountNonLocked(){return status!=UserStatus.LOCKED;}
}
