package org.pepetrace.Shader;

import org.lwjgl.opengl.*;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Texture {
    public int id;
    private static int total_bound_textures = 0;

    public Texture(int width, int height, int pixel_format, int pixel_datatype, int bind_dataformat) {
        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height, 0,pixel_format,
                pixel_datatype, NULL);

        glBindImageTexture(0, id, 0, false, 0, GL_READ_WRITE, bind_dataformat);
    }
}
