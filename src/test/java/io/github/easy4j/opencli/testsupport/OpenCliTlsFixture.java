package io.github.easy4j.opencli.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import lombok.Getter;

/** A loopback TLS certificate and explicit client trust for integration tests. */
@Getter
public final class OpenCliTlsFixture {

    private final SSLContext server;
    private final SSLContext trustedClient;

    private OpenCliTlsFixture(SSLContext server, SSLContext trustedClient) {
        this.server = server;
        this.trustedClient = trustedClient;
    }

    public static OpenCliTlsFixture create(Path temporary) throws Exception {
        Path storePath = temporary.resolve("opencli-fixture.p12");
        String keytool = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        Path tool = Paths.get(System.getProperty("java.home"), "bin", keytool);
        Process keygen = new ProcessBuilder(tool.toString(), "-genkeypair", "-alias", "center", "-keyalg", "RSA",
            "-keysize", "2048", "-dname", "CN=localhost", "-ext", "SAN=IP:127.0.0.1", "-validity", "1",
            "-storetype", "PKCS12", "-keystore", storePath.toString(), "-storepass", "test-password")
            .redirectErrorStream(true).redirectOutput(temporary.resolve("keytool.log").toFile()).start();
        try {
            assertTrue(keygen.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, keygen.exitValue());
        } finally {
            if (keygen.isAlive()) {
                keygen.destroyForcibly();
            }
        }
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(storePath)) {
            store.load(input, "test-password".toCharArray());
        }
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, "test-password".toCharArray());
        SSLContext server = SSLContext.getInstance("TLS");
        server.init(keys.getKeyManagers(), null, null);
        TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(store);
        SSLContext trustedClient = SSLContext.getInstance("TLS");
        trustedClient.init(null, trust.getTrustManagers(), null);
        return new OpenCliTlsFixture(server, trustedClient);
    }
}
