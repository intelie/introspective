package net.intelie.introspective.hotspot;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;

public class JVMTest {

    public static JVM jvm;

    @BeforeClass
    public static void setUp() {
        jvm = new JVM();
    }

    @Test
    public void testListStuff() throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        jvm.dump(new PrintStream(baos, false, "UTF-8"));

        String str = baos.toString("UTF-8");

        assertThat(str).contains("uint32_t @ 4");
    }

    @Test
    public void testGetStuff() {
        assertThat(jvm.constant("oopSize")).isIn(4, 8);
        Type type = jvm.type("Arguments");

        long javaComand = type.global("_java_command");
        long numJvmArgs = type.global("_num_jvm_args");

        assertThat(jvm.getStringRef(javaComand)).isNotEmpty();
        assertThat(jvm.getInt(numJvmArgs)).isBetween(0, 255);
    }
}
