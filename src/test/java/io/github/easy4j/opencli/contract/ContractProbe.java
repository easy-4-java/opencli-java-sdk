package io.github.easy4j.opencli.contract;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Offline child process: emits every actual JVM argument, including empty values. */
public final class ContractProbe {
    private ContractProbe() { }

    public static void main(String[] args) {
        System.out.println("argc:" + args.length);
        for (String arg : args) {
            System.out.println("arg:" + Base64.getEncoder().encodeToString(arg.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
