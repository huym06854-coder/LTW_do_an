package vn.iotstar.security;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.repository.UserRepository;
@Service
public class CustomerUserDetailsService implements UserDetailsService {
    private final UserRepository repository;
    public CustomerUserDetailsService(UserRepository repository){this.repository=repository;}
    @Override @Transactional(readOnly=true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        return repository.findByLogin(login).map(CustomerUserDetails::new)
            .orElseThrow(()->new UsernameNotFoundException("Sai tài khoản hoặc mật khẩu"));
    }
}
