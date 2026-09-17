public final class njl implements njz {
    public final oxe config;

    public njl(oxe config) {
        this.config = config;
    }

    @Override
    public wcz request(String payload) {
        return wcz.success("server:" + payload);
    }

    @Override
    public String i() {
        return "SERVER";
    }

    @Override
    public void o() {
    }

    @Override
    public void close() {
    }
}
