package org.pepetrace.Buffers;

import static org.lwjgl.opengl.ARBBindlessTexture.glGetTextureHandleARB;
import static org.lwjgl.opengl.ARBBindlessTexture.glMakeTextureHandleNonResidentARB;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.stb.STBImage.*;

import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Класс для создания и управления Текстурами OpenGL.
 * Поддерживает создание пустых текстур (для изменений в compute-шейдере) и загрузку из файлов.
 */
public class Texture implements AutoCloseable {

    public int id;
    private final int width;
    private final int height;
    private long binding;
    private boolean isBindless;
    private final int internalFormat;   // формат хранения в видеопамяти (GL_RGBA32F, GL_RGBA8, ...)
    private final int pixelFormat;      // формат пикселей при загрузке данных (GL_RGBA, GL_RGB, ...)
    private final int pixelType;        // тип пикселей при загрузке данных (GL_UNSIGNED_BYTE, GL_FLOAT, ...)
    private final int imageFormat;      // формат для image unit (GL_RGBA32F, GL_RGBA8, ...)
    private int access;
    private boolean isReleased = false;

    /**
     * Конструктор для создания пустой текстуры (без начальных данных).
     * Текстура привязывается как image для чтения/записи в compute-шейдере.
     *
     * @param width          ширина текстуры
     * @param height         высота текстуры
     * @param binding        слот текстуры. Если < 0, то bindless
     * @param internalFormat формат хранения (GL_RGBA32F, GL_RGBA8, ...)
     * @param pixelFormat    формат пикселей для операций загрузки (GL_RGBA, GL_RGB, ...)
     * @param pixelType      тип пикселей для операций загрузки (GL_UNSIGNED_BYTE, GL_FLOAT, ...)
     * @param imageFormat    формат для image unit (GL_RGBA32F, GL_RGBA8, ...)
     * @param access         режим доступа (GL_READ_ONLY, GL_WRITE_ONLY, GL_READ_WRITE)
     */
    public Texture(int width, int height, boolean generateMipmaps, int binding, int internalFormat, int pixelFormat, int pixelType, int imageFormat, int access) {
        this.width = width;
        this.height = height;
        this.binding = binding;
        this.isBindless = binding < 0;
        this.internalFormat = internalFormat;
        this.pixelFormat = pixelFormat;
        this.pixelType = pixelType;
        this.imageFormat = imageFormat;
        this.access = access;

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        // Параметры текстуры
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        if (generateMipmaps) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glGenerateMipmap(GL_TEXTURE_2D);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        }

        // Выделяем неизменяемую память
        int levels = generateMipmaps ? 1 + (int) Math.floor(Math.log(Math.max(width, height)) / Math.log(2)) : 1;
        System.out.println(levels);
        glTexStorage2D(GL_TEXTURE_2D, levels, internalFormat, width, height);

        glBindTexture(GL_TEXTURE_2D, 0);
        // Привязываем как image для работы в compute-шейдере (чтение/запись)
        if (binding >= 0) {
            glBindImageTexture(binding, id, 0, false, 0, access, imageFormat);
        } else {
            this.binding = glGetTextureHandleARB(id);
        };

    }

    /**
     * Освобождает ресурсы текстуры в видеопамяти.
     * Должен вызываться в потоке с активным OpenGL контекстом.
     */
    @Override
    public void close() throws Exception {
        if (!isReleased) {
            if (isBindless) glMakeTextureHandleNonResidentARB(binding);
            glDeleteTextures(id);
            isReleased = true;
        }
    }

    /**
     * Фабричный метод для загрузки HDR текстуры из файла.
     * Изображение загружается как RGBA (4 канала) и помещается в текстуру с форматом GL_RGBA32F.
     * Текстура привязывается как image unit с форматом GL_RGBA32F (возможна запись из compute-шейдера).
     *
     * @param path    путь к файлу изображения (HDR, EXR, ...)
     * @param access  режим доступа (GL_READ_ONLY, GL_WRITE_ONLY, GL_READ_WRITE)
     * @return новый объект Texture с загруженными данными
     * @throws RuntimeException если загрузка изображения не удалась
     */
    public static Texture createFromFileHDR(int binding, boolean generateMipmaps, int access, String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(true);

            FloatBuffer data = stbi_loadf(path, w, h, channels, 4); // принудительно RGBA
            if (data == null) {
                throw new RuntimeException("Не удалось загрузить изображение: " + stbi_failure_reason());
            }

            int width = w.get();
            int height = h.get();

            // Создаём текстуру с форматом GL_RGBA8 (подходит и для записи из compute-шейдера)
            Texture texture = new Texture(width, height, generateMipmaps, binding, GL_RGBA32F, GL_RGBA, GL_FLOAT, GL_RGBA32F, access);

            // Загружаем пиксельные данные
            glBindTexture(GL_TEXTURE_2D, texture.id);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_FLOAT, data);
            if (generateMipmaps) {
                glGenerateMipmap(GL_TEXTURE_2D);
            }
            glBindTexture(GL_TEXTURE_2D, 0);
            stbi_image_free(data);
            return texture;
        }
    }

    /**
     * Фабричный метод для загрузки текстуры из файла.
     * Изображение загружается как RGBA (4 канала) и помещается в текстуру с форматом GL_RGBA8.
     * Текстура привязывается как image unit с форматом GL_RGBA8 (возможна запись из compute-шейдера).
     *
     * @param path    путь к файлу изображения (PNG, JPG, ...)
     * @param access  режим доступа (GL_READ_ONLY, GL_WRITE_ONLY, GL_READ_WRITE)
     * @return новый объект Texture с загруженными данными
     * @throws RuntimeException если загрузка изображения не удалась
     */
    public static Texture createFromFile(int binding, boolean generateMipmaps, int access, String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(true);

            ByteBuffer data = stbi_load(path, w, h, channels, 4); // принудительно RGBA
            if (data == null) {
                throw new RuntimeException("Не удалось загрузить изображение: " + stbi_failure_reason());
            }

            int width = w.get();
            int height = h.get();

            // Создаём текстуру с форматом GL_RGBA8 (подходит и для записи из compute-шейдера)
            Texture texture = new Texture(width, height, generateMipmaps, binding, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, GL_RGBA8, access);

            // Загружаем пиксельные данные
            glBindTexture(GL_TEXTURE_2D, texture.id);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
            if (generateMipmaps) {
                glGenerateMipmap(GL_TEXTURE_2D);
            }
            glBindTexture(GL_TEXTURE_2D, 0);
            stbi_image_free(data);
            return texture;
        }
    }

    /**
     * Обновляет содержимое текстуры из буфера.
     * Формат данных должен соответствовать pixelFormat/pixelType, указанным при создании.
     *
     * @param data буфер с новыми пиксельными данными
     */
    public void updateData(ByteBuffer data) {
        glBindTexture(GL_TEXTURE_2D, id);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, pixelFormat, pixelType, data);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Обновляет содержимое текстуры из файла.
     * Размеры изображения должны совпадать с размерами текущей текстуры.
     * Изображение загружается как RGBA.
     *
     * @param path путь к файлу изображения
     */
    public void updateFromFile(String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(true);
            ByteBuffer data = stbi_load(path, w, h, channels, 4);
            if (data == null) {
                throw new RuntimeException("Не удалось загрузить изображение: " + stbi_failure_reason());
            }

            int newWidth = w.get();
            int newHeight = h.get();
            if (newWidth != width || newHeight != height) {
                stbi_image_free(data);
                throw new IllegalArgumentException("Размеры изображения не совпадают с размерами текстуры: "
                        + newWidth + "x" + newHeight + " vs " + width + "x" + height);
            }

            glBindTexture(GL_TEXTURE_2D, id);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
            glBindTexture(GL_TEXTURE_2D, 0);

            stbi_image_free(data);
        }
    }

    /**
     * Привязывает текстуру к указанному image unit для работы в шейдерах.
     *
     * @param binding  номер binding (0, 1, 2, ...)
     * @param access   режим доступа (GL_READ_ONLY, GL_WRITE_ONLY, GL_READ_WRITE)
     * @param level    уровень мипмапа (обычно 0) <p>[мипмап - уровень качества текстуры. Используется в традиционном рендере для уменьшения размеры рисуемой текстуры в зависимости от расстояния.]
     * @param layered  используется ли массив слоёв (false для 2D текстуры)
     * @param layer    слой для привязки (0 для 2D текстур)
     */
    public void bindImage(int binding, int access, int level, boolean layered, int layer) {
        glBindImageTexture(binding, id, level, layered, layer, access, imageFormat);
        this.binding = binding;
        this.access = access;
    }

    /**
     * Привязывает текстуру к указанному image unit с указанным режимом доступа.
     * Удобный вариант для большинства случаев.
     *
     * @param binding номер binding (0, 1, 2, ...)
     * @param access  режим доступа (GL_READ_ONLY, GL_WRITE_ONLY, GL_READ_WRITE)
     */
    public void bindImage(int binding, int access) {
        glBindImageTexture(binding, id, 0, false, 0, access, imageFormat);
        this.binding = binding;
        this.access = access;
    }

    // Геттеры
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getBinding() { return binding; }
    public int getInternalFormat() { return internalFormat; }
    public int getPixelFormat() { return pixelFormat; }
    public int getPixelType() { return pixelType; }
    public int getImageFormat() { return imageFormat; }
    public int getAccess() { return access; }
}