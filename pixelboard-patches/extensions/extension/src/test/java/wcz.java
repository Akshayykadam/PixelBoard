public final class wcz {
    public final String payload;
    public final Throwable failure;

    private wcz(String payload, Throwable failure) {
        this.payload = payload;
        this.failure = failure;
    }

    public static wcz success(String payload) {
        return new wcz(payload, null);
    }

    public static wcz failure(Throwable failure) {
        return new wcz(null, failure);
    }
}
