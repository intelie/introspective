package net.intelie.introspective.reflect;

public class ConstantDummyPeeler implements ReferencePeeler {
    private Object obj;
    private boolean eof;

    @Override
    public void clear() {
        this.eof = false;
        this.obj = null;
    }

    @Override
    public long resetTo(Class<?> clazz, Object value) {
        this.eof = false;
        this.obj = value;
        return 0;
    }

    @Override
    public boolean moveNext() {
        if (eof) return false;
        return eof = true;
    }

    @Override
    public Object current() {
        return obj;
    }

    @Override
    public Object currentIndex() {
        return null;
    }
}
