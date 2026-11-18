package io.github.easy4j.opencli.contract;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/** Offline, self-bounded child fixture. Release files allow cleanup even against a broken SDK. */
public final class LifecycleProbe {
    private LifecycleProbe() { }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        if ("privacy".equals(mode)) {
            exercisePrivacy(args[1], args[2]);
            return;
        }
        if ("network-follow".equals(mode)) {
            networkFollow(args);
            return;
        }
        if ("utf8".equals(mode)) {
            System.out.write("中文".getBytes(StandardCharsets.UTF_8));
            return;
        }
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
        if ("count-hold".equals(mode)) {
            Files.write(marker, "started\n".getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            Files.write(marker, "started".getBytes(StandardCharsets.UTF_8));
        }
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

    private static void exercisePrivacy(String outcome, String canary) throws Exception {
        switch (outcome) {
            case "nonzero":
                System.err.print(canary);
                System.exit(7);
                return;
            case "timeout":
                System.err.print(canary);
                System.err.flush();
                Thread.sleep(8000L);
                return;
            case "overflow":
                System.out.print(canary);
                main(new String[] {"stdout", "4096"});
                return;
            default:
                throw new IllegalArgumentException("unknown privacy fixture outcome");
        }
    }

    private static void networkFollow(String[] args) throws Exception {
        if (!Arrays.asList("browser", "fixture", "network", "--follow")
            .equals(Arrays.asList(args).subList(3, args.length))) {
            throw new IllegalArgumentException("unexpected network follow argv");
        }
        System.out.print("event-one\n");
        System.out.flush();
        switch (args[1]) {
            case "complete":
                Thread.sleep(30L);
                System.out.print("event-two\n");
                break;
            case "overflow":
                main(new String[]{"stdout", "4096"});
                break;
            case "failed":
                System.err.print("fixture failed");
                System.exit(23);
                break;
            case "wait":
                Files.write(Paths.get(args[2]), new byte[]{1});
                Thread.sleep(8000L);
                break;
            default:
                throw new IllegalArgumentException("unexpected network follow mode");
        }
    }
}
