public final class oti {
    public oth a;
    public oud[] b = new oud[0];
    public String[] c = new String[0];

    public oti() {
    }

    public void j(otk actionDef) {
        a = actionDef.c;
        c = actionDef.n;
    }

    public void q(int keyCode, ouc intention, Object payload) {
        b = new oud[] {new oud(keyCode, intention, payload)};
    }

    public otk c() {
        return new otk(this);
    }
}
