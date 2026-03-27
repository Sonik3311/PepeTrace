package org.pepetrace.Buffers;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Класс для создания и управления Текстурами OpenGL.
 */
public class Texture {

    public int id;

    /**
     * Конструктор для создания текстуры.
     *
     * @param width        ширина текстуры
     * @param height       высота текстуры
     * @param pixel_format формат пикселя (GL_RGB, GL_RGBA, GL_DEPTH_COMPONENT, GL_STENCIL_INDEX)
     * @param pixel_datatype тип данных пикселя (GL_UNSIGNED_BYTE, GL_FLOAT, GL_HALF_FLOAT, ...)
     * @param bind_dataformat формат данных для привязки (GL_RGB, GL_RGBA)
     */
    public Texture(
        int width,
        int height,
        int pixel_format,
        int pixel_datatype,
        int bind_dataformat
    ) {
        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        //TODO: NULL здесь нужно заменить на картинку,
        // полученную через org.lwjgl.stb.STBImage.stbi_load
        // Тогда можно будет загружать картинки на видеокарту
        //glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height, 0,pixel_format,
        //        pixel_datatype, NULL);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, width, height);

        glBindImageTexture(0, id, 0, false, 0, GL_READ_WRITE, bind_dataformat);
    }
}
