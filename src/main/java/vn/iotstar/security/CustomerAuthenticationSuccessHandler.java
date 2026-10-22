package vn.iotstar.security;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
@Component
public class CustomerAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Override public void onAuthenticationSuccess(HttpServletRequest req,HttpServletResponse resp,Authentication auth)
            throws IOException,ServletException {
        if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")))resp.sendRedirect(req.getContextPath()+"/admin");
        else if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_STAFF")||a.getAuthority().equals("ROLE_MANAGER")))
            resp.sendRedirect(req.getContextPath()+"/staff/orders");
        else resp.sendRedirect(req.getContextPath()+"/");
    }
}
