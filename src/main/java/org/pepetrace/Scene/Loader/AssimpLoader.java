package org.pepetrace.Scene.Loader;

import org.lwjgl.assimp.*;
import java.nio.IntBuffer;

public class AssimpLoader implements MeshLoader {
    @Override
    public MeshData load(String path) {
        int flags = Assimp.aiProcess_Triangulate
                | Assimp.aiProcess_JoinIdenticalVertices
                | Assimp.aiProcess_CalcTangentSpace;

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
        AIVector3D.Buffer texCoords = mesh.mTextureCoords(0);

        int vertexCount = mesh.mNumVertices();
        for (int i = 0; i < vertexCount; i++) {
            AIVector3D pos = positions.get(i);
            data.addVertex(pos.x(), pos.y(), pos.z());

            AIVector3D norm = normals.get(i);
            data.addNormal(norm.x(), norm.y(), norm.z());

            if (texCoords != null) {
                AIVector3D uv = texCoords.get(i);
                data.addUV(uv.x(), uv.y());
            } else {
                data.addUV(0, 0);
            }

            if (tangents != null) {
                AIVector3D tan = tangents.get(i);
                data.addTangent(tan.x(), tan.y(), tan.z());
            } else {
                data.addTangent(1, 0, 0);
            }

            if (bitangents != null) {
                AIVector3D bit = bitangents.get(i);
                data.addBitangent(bit.x(), bit.y(), bit.z());
            } else {
                data.addBitangent(0, 1, 0);
            }
        }

        int faceCount = mesh.mNumFaces();
        AIFace.Buffer faces = mesh.mFaces();
        for (int i = 0; i < faceCount; i++) {
            AIFace face = faces.get(i);
            IntBuffer indices = face.mIndices();
            for (int j = 0; j < indices.limit(); j++) {
                data.addIndex(indices.get(j));
            }
        }

        Assimp.aiReleaseImport(scene);
        System.out.println("Loaded unique vertices: " + data.getVertexCount() + ", triangles: " + data.getTriangleCount());
        return data;
    }
}