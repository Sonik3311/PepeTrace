package org.pepetrace.Shader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Читает шейдерные файлы и рекурсивно вставляет код вместо импортов (#include).
 * Повторные включения одного и того же файла игнорируются.
 */
public class ShaderSourceReader {

    private final Set<String> processedIncludes = new HashSet<>();

    /**
     * Читает файл и рекурсивно обрабатывает директивы #include.
     *
     * @param filepath путь к файлу
     * @param readingIncludeFile флаг, указывающий, что читается включаемый файл
     * @return содержимое файла с раскрытыми включениями
     * @throws FileNotFoundException если файл не найден
     */
    public CharSequence readFile(String filepath, boolean readingIncludeFile)
            throws FileNotFoundException {
        StringBuilder result = new StringBuilder();
        File file = new File(filepath);
        try (Scanner scanner = new Scanner(file)) {
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
                    String canonicalPath = getCanonicalPath(includePath);

                    // Игнорируем повторное включение
                    if (!processedIncludes.contains(canonicalPath)) {
                        processedIncludes.add(canonicalPath);
                        CharSequence includedContent = readFile(
                                includePath,
                                true
                        );
                        result.append('\n').append(includedContent);
                    }
                    // Если файл уже был включён, ничего не добавляем
                } else {
                    result.append('\n').append(line);
                }
            }
        }

        if (!readingIncludeFile) {
            processedIncludes.clear();
        }
        return !result.isEmpty() ? result.substring(1) : ""; // убираем первый лишний '\n'
    }

    /**
     * Извлекает имя файла из строки #include.
     * Предполагается формат: #include "имя_файла"
     */
    private static String extractIncludeFilename(String line) {
        int firstQuote = line.indexOf('"');
        int secondQuote = line.indexOf('"', firstQuote + 1);
        return line.substring(firstQuote + 1, secondQuote);
    }

    /**
     * Строит полный путь к включаемому файлу относительно текущего файла.
     */
    private static String buildIncludePath(
            String currentFilepath,
            String includeFilename
    ) {
        int lastSlash = currentFilepath.lastIndexOf('/');
        String baseDir = (lastSlash >= 0)
                ? currentFilepath.substring(0, lastSlash + 1)
                : "";
        return baseDir + includeFilename;
    }

    /**
     * Возвращает канонический путь к файлу для надёжного сравнения.
     */
    private static String getCanonicalPath(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (Exception e) {
            // Если не удалось получить канонический путь, используем исходный
            return path;
        }
    }

    /**
     * Определяет, можно ли включать данную строку в содержимое включаемого файла.
     * Строки, начинающиеся с #version или layout, пропускаются.
     */
    private static boolean isIncludable(String line) {
        return !(line.startsWith("#version") || line.startsWith("layout"));
    }
}
