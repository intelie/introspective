package net.intelie.introspective.reflect;

import me.qmx.jitescript.JiteClass;
import net.intelie.introspective.hotspot.JVM;

import java.lang.reflect.Field;
import java.util.function.Function;

import static me.qmx.jitescript.CodeBlock.newCodeBlock;
import static me.qmx.jitescript.util.CodegenUtils.*;

public class FastFieldAccessor {
    private final Function<Object, Object>[] accessors;
    private final String name;
    private int currentAccessorIndex;
    private Function<Object, Object> currentAccessor;

    public FastFieldAccessor(Field field) {
        this.name = field.getName();
        this.accessors = new Function[]{
                reflectionAccessor(field),
                bytecodeAccessor(field),
                unsafeAccessor(field),
                x -> null
        };

        currentAccessor = accessors[currentAccessorIndex = 0];
    }


    private static Function<Object, Object> reflectionAccessor(Field field) {
        try {
            field.setAccessible(true);
            return x -> {
                try {
                    return field.get(x);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            };
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Function<Object, Object> unsafeAccessor(Field field) {
        try {
            long offset = JVMPrimitives.getFieldOffset(field);
            return x -> JVMPrimitives.getFieldObject(x, offset);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Function<Object, Object> bytecodeAccessor(Field field) {
        try {
            JiteClass jite = new JiteClass(p(field.getDeclaringClass()) + "$field$" + field.getName(), new String[]{p(Function.class)}) {{
                defineMethod("<init>", ACC_PUBLIC, sig(void.class),
                        newCodeBlock()
                                .aload(0)
                                .invokespecial(p(Object.class), "<init>", sig(void.class))
                                .voidreturn()
                );

                defineMethod("apply", ACC_PUBLIC | ACC_FINAL, sig(Object.class, Object.class),
                        newCodeBlock()
                                .aload(1)
                                .checkcast(p(field.getDeclaringClass()))
                                .getfield(p(field.getDeclaringClass()), field.getName(), ci(field.getType()))
                                .areturn()
                );
            }};
            Class<?> clazz = JVMPrimitives.unsafe().defineAnonymousClass(field.getDeclaringClass(), jite.toBytes(), new Object[0]);
            return (Function<Object, Object>) clazz.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public Object get(Object target) {
        do {
            try {
                return currentAccessor.apply(target);
            } catch (Throwable e) {
                e.printStackTrace();
                currentAccessor = accessors[++currentAccessorIndex];
            }
        } while (true);
    }

    public String name() {
        return name;
    }
}
