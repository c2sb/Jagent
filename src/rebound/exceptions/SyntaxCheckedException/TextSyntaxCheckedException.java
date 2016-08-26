package rebound.exceptions.SyntaxCheckedException;


public class TextSyntaxCheckedException extends Exception {
    public TextSyntaxCheckedException() {
        super();
    }

    public TextSyntaxCheckedException(final String message) {
        super(message);
    }

    public TextSyntaxCheckedException(final String message, final Throwable cause) {
        super(message, cause);
    }


    public static TextSyntaxCheckedException inst() {
        return new TextSyntaxCheckedException();
    }

    public static TextSyntaxCheckedException inst(String message) {
        return new TextSyntaxCheckedException(message);
    }

    public static TextSyntaxCheckedException inst(String message, Throwable cause) {
        return new TextSyntaxCheckedException(message, cause);
    }
}