package net.intelie.introspective.reflect;

import org.openjdk.jol.info.GraphWalker;
import org.openjdk.jol.vm.LightVM;

public class TestSizeUtils {
    public static long size(Object obj) {
        LightVM.current();
        return new GraphWalker().walk(obj).totalSize();
    }
}
