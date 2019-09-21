package net.intelie.introspective.reflect;

import org.openjdk.jol.info.GraphPathRecord;
import org.openjdk.jol.info.GraphVisitor;
import org.openjdk.jol.info.GraphWalker;
import org.openjdk.jol.vm.LightVM;

import java.util.concurrent.atomic.AtomicLong;

public class TestSizeUtils {
    public static long size(Object obj, String... ignorePrefixes) {
        LightVM.current();
        GraphWalker walker = new GraphWalker(obj);
        AtomicLong total = new AtomicLong();
        walker.addVisitor(new GraphVisitor() {
            @Override
            public void visit(GraphPathRecord gpr) {
                //if (gpr.klass().equals(char[].class) || gpr.klass().equals(String.class) || gpr.klass().equals(Double.class)) return;
//                if (gpr.depth() == 2)
                //System.out.println(gpr.path() + " " + gpr.size());
                total.addAndGet(gpr.size());
            }
        });
        walker.walk();
        return total.get();
    }
}
