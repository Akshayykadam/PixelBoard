public final class mpw {
    private static final mpw INSTANCE = new mpw();

    public Object lastDependency;
    public Class<?> lastFallbackClass;
    public String mode;
    public int serverCalls;
    public int deviceCalls;

    private mpw() {
    }

    public static mpw a() {
        return INSTANCE;
    }

    public void d(mqc dependency) {
        mode = "device";
        lastDependency = dependency;
        deviceCalls++;
    }

    public void e(Class<?> dependencyClass) {
        mode = "server";
        lastFallbackClass = dependencyClass;
        serverCalls++;
    }

    public Object c() {
        return mode;
    }

    public void reset() {
        lastDependency = null;
        lastFallbackClass = null;
        mode = null;
        serverCalls = 0;
        deviceCalls = 0;
    }
}
