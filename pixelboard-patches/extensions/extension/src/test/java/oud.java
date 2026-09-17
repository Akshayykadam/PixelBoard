import java.util.Objects;

public final class oud {
    public final int c;
    public final ouc d;
    public final Object e;

    public oud(int keyCode, ouc intention, Object payload) {
        c = keyCode;
        d = intention;
        e = payload;
    }

    public oud(int keyCode, ouc intention, Object payload, int unusedPriority) {
        this(keyCode, intention, payload);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof oud)) {
            return false;
        }
        oud that = (oud) other;
        return c == that.c && d == that.d && Objects.equals(e, that.e);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(c), d, e);
    }
}
