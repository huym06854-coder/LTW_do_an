package vn.iotstar.security;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.iotstar.security.jwt.*;
@Configuration
public class SecurityConfig {
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {return configuration.getAuthenticationManager();}
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http,JwtService jwt,CustomerUserDetailsService users) throws Exception {
        http.securityMatcher("/api/**").csrf(csrf->csrf.disable())
          .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth->auth
              .requestMatchers("/api/auth/login","/api/auth/register").permitAll()
              .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/products/**").permitAll()
              .requestMatchers("/api/stocks/**").hasAnyRole("ADMIN","MANAGER","STAFF")
              .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/products/**").hasRole("ADMIN")
              .requestMatchers("/api/**").authenticated())
          .exceptionHandling(ex->ex.authenticationEntryPoint(new JwtAuthenticationEntryPoint()))
          .addFilterBefore(new JwtAuthenticationFilter(jwt,users),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean @Order(2)
    SecurityFilterChain webSecurity(HttpSecurity http,CustomerAuthenticationSuccessHandler onSuccess,
            vn.iotstar.service.UserService accounts) throws Exception {
        var contexts = new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
        http.securityContext(c -> c.securityContextRepository(contexts))
            .addFilterBefore(new SessionAccountValidationFilter(accounts, contexts), org.springframework.security.web.csrf.CsrfFilter.class);
        http.authorizeHttpRequests(auth->auth
            .requestMatchers(org.springframework.http.HttpMethod.GET,"/admin/css/**","/admin/js/**").permitAll()
            .requestMatchers("/admin/**","/admin").hasRole("ADMIN")
            .requestMatchers("/staff/**","/staff").hasAnyRole("ADMIN","MANAGER","STAFF")
            .requestMatchers("/cart/**","/cart","/checkout/**","/checkout","/profile/**","/profile","/orders/**","/orders").authenticated()
            .requestMatchers(org.springframework.http.HttpMethod.POST,"/products/*/reviews").authenticated()
            .requestMatchers("/", "/index","/about","/contact","/news","/offers","/products/**","/products","/product-detail","/login","/register","/store/**","/css/**","/js/**","/images/**").permitAll()
            .anyRequest().authenticated())
            .formLogin(form->form.loginPage("/login").loginProcessingUrl("/login").usernameParameter("login")
                .successHandler(onSuccess).failureUrl("/login?error").permitAll())
            .logout(logout->logout.logoutUrl("/logout").logoutSuccessUrl("/"))
            .sessionManagement(s->s.sessionFixation().migrateSession());
        return http.build();
    }
}
