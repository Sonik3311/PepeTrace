package org.pepetrace.Shader;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class ShaderSourceReader {

    private final Set<String> processedIncludes = new HashSet<>();

    public CharSequence readFile(String filepath, boolean readingIncludeFile)
            throws FileNotFoundException {
        StringBuilder result = new StringBuilder();
        InputStream is = getClass().getResourceAsStream(filepath);
        if (is == null) {
            throw new FileNotFoundException("Resource not found: " + filepath);
        }
        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (readingIncludeFile && !isIncludable(line)) {
                    continue;
                }

                if (line.startsWith("#include")) {
                    String includeFilename = extractIncludeFilename(line);
                    String includePath = buildIncludePath(
                            filepath,
                            includeFilename
                    );

                    if (!processedIncludes.contains(includePath)) {
                        processedIncludes.add(includePath);
                        CharSequence includedContent = readFile(
                                includePath,
                                true
                        );
                        result.append('\n').append(includedContent);
                    }
                } else {
                    result.append('\n').append(line);
                }
            }
        }

        if (!readingIncludeFile) {
            processedIncludes.clear();
        }
        return !result.isEmpty() ? result.substring(1) : "";
    }

    private static String extractIncludeFilename(String line) {
        int firstQuote = line.indexOf('"');
        int secondQuote = line.indexOf('"', firstQuote + 1);
        return line.substring(firstQuote + 1, secondQuote);
    }

    private static String buildIncludePath(
            String currentFilepath,
            String includeFilename
    ) {
        int lastSlash = currentFilepath.lastIndexOf('/');
        String baseDir = (lastSlash >= 0)
                ? currentFilepath.substring(0, lastSlash + 1)
                : "";
        return normalizePath(baseDir + includeFilename);
    }

    private static String normalizePath(String path) {
        String[] parts = path.split("/");
        Deque<String> stack = new ArrayDeque<>();
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!stack.isEmpty() && !stack.peek().equals("..")) {
                    stack.pop();
                } else {
                    stack.push(part);
                }
            } else {
                stack.push(part);
            }
        }
        StringBuilder result = new StringBuilder();
        if (path.startsWith("/")) {
            result.append('/');
        }
        while (!stack.isEmpty()) {
            result.append(stack.pollLast());
            if (!stack.isEmpty()) {
                result.append('/');
            }
        }
        return result.toString();
    }

    private static boolean isIncludable(String line) {
        return !(line.startsWith("#version") || line.startsWith("layout"));
    }
}
