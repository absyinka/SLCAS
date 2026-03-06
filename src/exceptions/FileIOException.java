package exceptions;

public class FileIOException extends RuntimeException {
    public FileIOException(String message, Throwable cause) {
        super("File I/O error: " + message, cause);
    }
    public FileIOException(String message) {
        super("File I/O error: " + message);
    }
}
