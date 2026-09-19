package org.allsparks.mimic.arch;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * MIMIC must declare inputs on allsparks-contracts, not compile against PULSE.
 */
class PulseBoundaryTest {
    @Test
    void mainSourcesDoNotImportPulse() throws IOException {
        Path main = findMain();
        List<String> hits = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(main)) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                String[] lines = read(path).split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.startsWith("import org.allsparks.pulse")) {
                        hits.add(main.relativize(path).toString().replace('\\', '/') + ":" + (i + 1));
                    }
                }
            });
        }
        if (!hits.isEmpty()) {
            fail("MIMIC imported org.allsparks.pulse (use contracts InputRegistrar/InputValues):\n"
                    + String.join("\n", hits));
        }
    }

    private static Path findMain() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path nested = cwd.resolve("src/main/java");
        if (Files.isDirectory(nested)) {
            return nested;
        }
        return cwd.resolve("MIMIC/src/main/java");
    }

    private static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }
}
