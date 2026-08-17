package org.allsparks.mimic.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Validates relative markdown links in repository documentation. */
public class DocLinkCheckerTest {
    private static final Pattern LINK = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)");

    @Test
    void relativeMarkdownLinksResolve() throws IOException {
        Path root = findRepoRoot();
        List<String> missing = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> path.toString().endsWith(".md"))
                    .filter(path -> !path.toString().contains(".git"))
                    .forEach(path -> checkFile(root, path, missing));
        }
        if (!missing.isEmpty()) {
            fail("Missing relative links:\n" + String.join("\n", missing));
        }
        assertTrue(Files.isDirectory(root.resolve("docs")));
    }

    private static void checkFile(Path root, Path file, List<String> missing) {
        String text;
        try {
            text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            missing.add(file + ": unreadable (" + ex.getMessage() + ")");
            return;
        }
        Matcher matcher = LINK.matcher(text);
        while (matcher.find()) {
            String target = matcher.group(1).trim();
            if (target.startsWith("http://")
                    || target.startsWith("https://")
                    || target.startsWith("mailto:")
                    || target.startsWith("#")) {
                continue;
            }
            String cleaned = target.split("#", 2)[0];
            if (cleaned.isEmpty()) {
                continue;
            }
            Path candidate = file.getParent().resolve(cleaned).normalize();
            if (!Files.exists(candidate)) {
                missing.add(root.relativize(file) + " -> " + target);
            }
        }
    }

    private static Path findRepoRoot() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("docs")) && Files.exists(cwd.resolve("LICENSE"))) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("docs"))) {
            return parent;
        }
        return cwd;
    }
}
