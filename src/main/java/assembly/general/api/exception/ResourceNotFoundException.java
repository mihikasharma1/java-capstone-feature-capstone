package assembly.general.api.exception;

// Dedicated exception so GlobalExceptionHandler can map it to 404 specifically,
// distinct from the generic 400 IllegalArgumentException used elsewhere.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}