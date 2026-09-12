package assembly.general.api.exception;

public class BookUnavailableException extends RuntimeException {
    private final int availableCopies;

    public BookUnavailableException(int availableCopies) {
        super("No copies available for reservation");
        this.availableCopies = availableCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }
}