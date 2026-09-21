package io.github.easy4j.opencli.contract;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/** Offline, self-bounded child fixture. Release files allow test cleanup even against broken SDKs. */
public final class LifecycleProbe {
    private LifecycleProbe() { }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        if ("stdout".equals(mode) || "stderr".equals(mode)) {
            byte[] block = new byte[8192];
            Arrays.fill(block, (byte) 'x');
            int remaining = Integer.parseInt(args[1]);
            while (remaining > 0) {
                int size = Math.min(block.length, remaining);
                if ("stdout".equals(mode)) { System.out.write(block, 0, size); }
                else { System.err.write(block, 0, size); }
                remaining -= size;
            }
            return;
        }
        Path marker = Paths.get(args[1]);
        Files.write(marker, "started".getBytes(StandardCharsets.UTF_8));
        if ("write".equals(mode)) { return; }
        Path release = Paths.get(args[2]);
        long start = System.nanoTime();
        int tick = 0;
        while (!Files.exists(release) && System.nanoTime() - start < 10_000_000_000L) {
            if ("heartbeat".equals(mode)) {
                Files.write(marker, Integer.toString(++tick).getBytes(StandardCharsets.UTF_8));
            }
            Thread.sleep(20L);
        }
    }
}
