package vn.iotstar.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(annotations = Controller.class)
@Order(Ordered.LOWEST_PRECEDENCE)
public class MvcExceptionHandler {
    @ExceptionHandler({ BusinessException.class, IllegalArgumentException.class })
    public ModelAndView business(RuntimeException e) {
        return error(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView missing(ResourceNotFoundException e) {
        return error(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView conflict(DataIntegrityViolationException e) {
        return error("Dữ liệu trùng hoặc không hợp lệ", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView denied(AccessDeniedException e) {
        return error("Bạn không được quyền truy cập", HttpStatus.FORBIDDEN);
    }

    private ModelAndView error(String message, HttpStatus status) {
        ModelAndView view = new ModelAndView("error/business");
        view.setStatus(status);
        view.addObject("errorMessage", message);
        return view;
    }
}
