package vn.iotstar.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.service.UserService;

/** Revalidates existing web sessions before CSRF and authorization checks. */
public class SessionAccountValidationFilter extends OncePerRequestFilter {
    private final UserService accounts;
    private final SecurityContextRepository contexts;
    private final SecurityContextLogoutHandler logout = new SecurityContextLogoutHandler();

    public SessionAccountValidationFilter(UserService accounts, SecurityContextRepository contexts) {
        this.accounts = accounts;
        this.contexts = contexts;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/store/") || path.startsWith("/admin/css/")
                || path.startsWith("/admin/js/") || path.startsWith("/images/")
                || path.startsWith("/css/") || path.startsWith("/js/") || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomerUserDetails principal) {
            CustomerUserDetails current = null;
            try {
                current = new CustomerUserDetails(accounts.profileById(principal.getId()));
            } catch (ResourceNotFoundException ignored) {
                // A removed account must lose its session just like a locked account.
            }
            if (current == null || !current.isEnabled() || !current.isAccountNonLocked()) {
                logout.logout(request, response, authentication);
                response.sendRedirect(request.getContextPath() + "/login?accountDisabled");
                return;
            }
            var refreshed = UsernamePasswordAuthenticationToken.authenticated(current, null, current.getAuthorities());
            refreshed.setDetails(authentication.getDetails());
            context.setAuthentication(refreshed);
            contexts.saveContext(context, request, response);
        }
        chain.doFilter(request, response);
    }
}
