package org.pepetrace.Scene.Loader;

import org.lwjgl.assimp.*;
import java.nio.IntBuffer;

public class AssimpLoader implements MeshLoader {
    @Override
    public MeshData load(String path) {
        // Отключаем JoinIdenticalVertices – сами развернём индексы
        int flags = Assimp.aiProcess_Triangulate | Assimp.aiProcess_CalcTangentSpace;
        AIScene scene = Assimp.aiImportFile(path, flags);
        if (scene == null || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_INCOMPLETE) != 0 || scene.mRootNode() == null) {
            throw new RuntimeException("Assimp import failed: " + Assimp.aiGetErrorString());
        }

        AIMesh mesh = AIMesh.create(scene.mMeshes().get(0));
        MeshData data = new MeshData();

        AIVector3D.Buffer positions = mesh.mVertices();
        AIVector3D.Buffer normals = mesh.mNormals();
        AIVector3D.Buffer tangents = mesh.mTangents();
        AIVector3D.Buffer bitangents = mesh.mBitangents();
        AIVector3D.Buffer texCoords = mesh.mTextureCoords(0); // первый набор UV

        int faceCount = mesh.mNumFaces();
        AIFace.Buffer faces = mesh.mFaces();

        for (int i = 0; i < faceCount; i++) {
            AIFace face = faces.get(i);
            IntBuffer indices = face.mIndices();
            for (int j = 0; j < indices.limit(); j++) {
                int idx = indices.get(j);
                // Позиция
                AIVector3D pos = positions.get(idx);
                data.addVertex(pos.x(), pos.y(), pos.z());

                // Нормаль
                if (normals != null) {
                    AIVector3D norm = normals.get(idx);
                    data.addNormal(norm.x(), norm.y(), norm.z());
                } else {
                    data.addNormal(0, 1, 0);
                }

                // UV
                if (texCoords != null) {
                    AIVector3D uv = texCoords.get(idx);
                    data.addUV(uv.x(), uv.y());
                } else {
                    data.addUV(0, 0);
                }

                // Tangent
                if (tangents != null) {
                    AIVector3D tan = tangents.get(idx);
                    data.addTangent(tan.x(), tan.y(), tan.z());
                } else {
                    data.addTangent(1, 0, 0);
                }

                // Bitangent
                if (bitangents != null) {
                    AIVector3D bit = bitangents.get(idx);
                    data.addBitangent(bit.x(), bit.y(), bit.z());
                } else {
                    data.addBitangent(0, 1, 0);
                }
            }
        }

        Assimp.aiReleaseImport(scene);
        System.out.println("Loaded " + data.getVertexCount() + " vertices, " + (data.getVertexCount() / 3) + " triangles");
        return data;
    }
}