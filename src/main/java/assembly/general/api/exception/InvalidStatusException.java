package assembly.general.api.exception;

public class InvalidStatusException extends RuntimeException {
    private final String currentStatus;

    public InvalidStatusException(String message, String currentStatus) {
        super(message);
        this.currentStatus = currentStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}