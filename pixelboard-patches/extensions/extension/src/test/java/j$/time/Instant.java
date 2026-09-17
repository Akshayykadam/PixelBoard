package j$.time;

public final class Instant {
    private final long epochMillis;

    public Instant(long epochMillis) {
        this.epochMillis = epochMillis;
    }

    public long toEpochMilli() {
        return epochMillis;
    }
}
