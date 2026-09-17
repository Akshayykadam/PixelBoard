public final class hcw {
    public static final FakeMkr b = new FakeMkr();

    private hcw() {
    }

    public static final class FakeMkr {
        public Boolean value = Boolean.FALSE;

        public Boolean g() {
            return value;
        }
    }
}
