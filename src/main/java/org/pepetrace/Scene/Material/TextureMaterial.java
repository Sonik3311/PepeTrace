package org.pepetrace.Scene.Material;

import org.pepetrace.Buffers.Texture;
import org.w3c.dom.Text;

import java.util.List;

import static org.lwjgl.opengl.ARBBindlessTexture.glGetTextureHandleARB;
import static org.lwjgl.opengl.ARBBindlessTexture.*;
import static org.lwjgl.opengl.GL46.*;

public class TextureMaterial extends Material {
    protected Texture albedoTexture;
    protected Texture normalTexture;
    protected Texture RMTTexture;
    protected long albedoTextureHandle;
    protected long normalTextureHandle;
    protected long RMTTextureHandle;

    public TextureMaterial() {
        super();
    }

    public static TextureMaterial create(String albedoTexturePath, String normalTexturePath, String RMTTexturePath) {
        TextureMaterial texture = new TextureMaterial();
        texture.albedoTexture = Texture.createFromFile(-1, GL_READ_ONLY, albedoTexturePath);
        texture.normalTexture = Texture.createFromFile(-1, GL_READ_ONLY, normalTexturePath);
        texture.RMTTexture = Texture.createFromFile(-1, GL_READ_ONLY, RMTTexturePath);

        texture.albedoTextureHandle = glGetTextureHandleARB(texture.albedoTexture.id);
        texture.normalTextureHandle = glGetTextureHandleARB(texture.normalTexture.id);
        texture.RMTTextureHandle = glGetTextureHandleARB(texture.RMTTexture.id);

        glMakeTextureHandleResidentARB(texture.albedoTextureHandle);
        glMakeTextureHandleResidentARB(texture.normalTextureHandle);
        glMakeTextureHandleResidentARB(texture.RMTTextureHandle);

        return texture;
    }

    public List<Long> getTextureHandles() {
        return List.of(albedoTextureHandle, normalTextureHandle, RMTTextureHandle);
    }

    @Override
    public void destroy() {
        if (albedoTextureHandle != 0) {glMakeTextureHandleNonResidentARB(albedoTextureHandle);}
        if (normalTextureHandle != 0) {glMakeTextureHandleNonResidentARB(normalTextureHandle);}
        if (RMTTextureHandle != 0) {glMakeTextureHandleNonResidentARB(RMTTextureHandle);}

        if (albedoTexture.id != 0) {glDeleteTextures(albedoTexture.id);}
        if (normalTexture.id != 0) {glDeleteTextures(normalTexture.id);}
        if (RMTTexture.id != 0) {glDeleteTextures(RMTTexture.id);}
    }
}
