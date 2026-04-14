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

    public TextureMaterial() {
        super();
    }

    public static TextureMaterial create(String albedoTexturePath, String normalTexturePath, String RMTTexturePath) {
        TextureMaterial texture = new TextureMaterial();
        texture.albedoTexture = Texture.createFromFile(-1, false, GL_READ_ONLY, albedoTexturePath);
        texture.normalTexture = Texture.createFromFile(-1, false, GL_READ_ONLY, normalTexturePath);
        texture.RMTTexture = Texture.createFromFile(-1, false, GL_READ_ONLY, RMTTexturePath);

        glMakeTextureHandleResidentARB(texture.albedoTexture.getBinding());
        glMakeTextureHandleResidentARB(texture.normalTexture.getBinding());
        glMakeTextureHandleResidentARB(texture.RMTTexture.getBinding());

        return texture;
    }

    public List<Long> getTextureHandles() {
        return List.of(albedoTexture.getBinding(), normalTexture.getBinding(), RMTTexture.getBinding());
    }

    public Texture getAlbedoTexture() { return albedoTexture; }
    public Texture getNormalTexture() { return normalTexture; }
    public Texture getRMTTexture() { return RMTTexture; }

    @Override
    public void close() throws Exception {
        albedoTexture.close();
        normalTexture.close();
        RMTTexture.close();
    }
}
