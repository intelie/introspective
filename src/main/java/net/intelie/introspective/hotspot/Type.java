package net.intelie.introspective.hotspot;

import net.intelie.introspective.util.Preconditions;

import java.util.*;
import java.util.stream.Collectors;

public class Type {
    private static final Field[] NO_FIELDS = new Field[0];

    public final String name;
    public final String superName;
    public final int size;
    public final boolean isOop;
    public final boolean isInt;
    public final boolean isUnsigned;
    public final Map<String, Field> fields;

    Type(String name, String superName, int size, boolean isOop, boolean isInt, boolean isUnsigned, Set<Field> fields) {
        this.name = name;
        this.superName = superName;
        this.size = size;
        this.isOop = isOop;
        this.isInt = isInt;
        this.isUnsigned = isUnsigned;
        this.fields = fields == null ? Collections.emptyMap() : fields.stream().collect(Collectors.toMap(x -> x.name, x -> x, (x, y) -> x, LinkedHashMap::new));
    }

    public Field field(String name) {
        return fields.get(name);
    }

    public long global(String name) {
        Field field = field(name);
        Preconditions.checkArgument(field.isStatic, "Static field expected");
        return field.offset;
    }

    public long offset(String name) {
        Field field = field(name);
        Preconditions.checkArgument(!field.isStatic, "Instance field expected");
        return field.offset;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (superName != null) sb.append(" extends ").append(superName);
        sb.append(" @ ").append(size).append('\n');
        for (Field field : fields.values()) {
            sb.append("  ").append(field).append('\n');
        }
        return sb.toString();
    }
}
