package org.pepetrace.Scene.Material;

import static org.lwjgl.opengl.ARBBindlessTexture.*;
import static org.lwjgl.opengl.GL46.*;

import java.util.List;
import org.pepetrace.Buffers.Texture;

public class TextureMaterial extends Material {

    protected Texture albedoTexture;
    protected Texture normalTexture;
    protected Texture RMTTexture;

    public TextureMaterial() {
        super();
    }

    public static TextureMaterial create(
        String albedoTexturePath,
        String normalTexturePath,
        String RMTTexturePath
    ) {
        TextureMaterial texture = new TextureMaterial();
        texture.albedoTexture = loadTexture(albedoTexturePath);
        texture.normalTexture = loadTexture(normalTexturePath);
        texture.RMTTexture = loadTexture(RMTTexturePath);

        glMakeTextureHandleResidentARB(texture.albedoTexture.getBinding());
        glMakeTextureHandleResidentARB(texture.normalTexture.getBinding());
        glMakeTextureHandleResidentARB(texture.RMTTexture.getBinding());

        return texture;
    }

    private static Texture loadTexture(String path) {
        if (path.startsWith("/")) {
            return Texture.createFromResource(-1, false, GL_READ_ONLY, path);
        }
        return Texture.createFromFile(-1, false, GL_READ_ONLY, path);
    }

    public List<Long> getTextureHandles() {
        return List.of(
            albedoTexture.getBinding(),
            normalTexture.getBinding(),
            RMTTexture.getBinding()
        );
    }

    public Texture getAlbedoTexture() {
        return albedoTexture;
    }

    public Texture getNormalTexture() {
        return normalTexture;
    }

    public Texture getRMTTexture() {
        return RMTTexture;
    }

    @Override
    public void close() throws Exception {
        albedoTexture.close();
        normalTexture.close();
        RMTTexture.close();
    }
}
