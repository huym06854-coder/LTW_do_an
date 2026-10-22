package vn.iotstar.exception;

public class CheckoutValidationException extends BusinessException {
    private final String field;

    public CheckoutValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
