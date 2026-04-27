package org.pepetrace.Scene.Loader;

import org.lwjgl.assimp.*;
import java.nio.IntBuffer;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.PointerBuffer;

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

        MeshData data = new MeshData();
        processNode(scene.mRootNode(), scene, data);
        Assimp.aiReleaseImport(scene);

        // ----- Генерация недостающих данных -----
        boolean hasValidNormals = true;
        for (int i = 0; i < data.getNormals().size(); i += 3) {
            float nx = data.getNormals().get(i);
            float ny = data.getNormals().get(i+1);
            float nz = data.getNormals().get(i+2);
            if (Float.isNaN(nx) || Float.isNaN(ny) || Float.isNaN(nz) ||
                    (nx == 0 && ny == 0 && nz == 0)) {
                hasValidNormals = false;
                break;
            }
        }
        if (!hasValidNormals) {
            System.out.println("No valid normals found, computing from geometry.");
            data.computeNormals();
        }

        boolean hasUV = data.getUVs() != null && data.getUVs().size() >= 2;
        if (hasUV) {
            boolean uvNonZero = false;
            for (int i = 0; i < data.getUVs().size(); i += 2) {
                if (data.getUVs().get(i) != 0 || data.getUVs().get(i+1) != 0) {
                    uvNonZero = true;
                    break;
                }
            }
            if (uvNonZero) {
                // Принудительно пересчитываем тангенты для гарантии корректности (проблема 6)
                System.out.println("Computing tangents and bitangents (Mikkelsen).");
                data.computeTangentsAndBitangents();
            } else {
                System.out.println("UVs exist but are zero – cannot compute tangents.");
            }
        } else {
            System.out.println("No UV coordinates – skipping tangent generation.");
        }
        return data;
    }

    private void processNode(AINode node, AIScene scene, MeshData data) {
        // Меши текущего узла
        for (int i = 0; i < node.mNumMeshes(); i++) {
            int meshIndex = node.mMeshes().get(i);
            AIMesh mesh = AIMesh.create(scene.mMeshes().get(meshIndex));
            processMesh(mesh, data);
        }

        // Дочерние узлы
        int childCount = node.mNumChildren();
        if (childCount > 0) {
            PointerBuffer children = node.mChildren(); // LWJGL возвращает PointerBuffer
            for (int i = 0; i < childCount; i++) {
                long childHandle = children.get(i);
                AINode child = AINode.create(childHandle);
                if (child != null) {
                    processNode(child, scene, data);
                }
            }
        }
    }

    private void processMesh(AIMesh mesh, MeshData data) {
        AIVector3D.Buffer positions = mesh.mVertices();
        AIVector3D.Buffer normals = mesh.mNormals();
        AIVector3D.Buffer tangents = mesh.mTangents();
        AIVector3D.Buffer bitangents = mesh.mBitangents();
        AIVector3D.Buffer texCoords = mesh.mTextureCoords(0);

        int vertexCount = mesh.mNumVertices();
        int baseIndex = data.getVertexCount();

        for (int i = 0; i < vertexCount; i++) {
            AIVector3D pos = positions.get(i);
            data.addVertex(pos.x(), pos.y(), pos.z());

            if (normals != null) {
                AIVector3D norm = normals.get(i);
                data.addNormal(norm.x(), norm.y(), norm.z());
            } else {
                data.addNormal(0, 0, 0);
            }

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

// Индексы
        int faceCount = mesh.mNumFaces();
        AIFace.Buffer faces = mesh.mFaces();
        int vertexCountMesh = mesh.mNumVertices(); // количество вершин в текущем меше

        System.out.println("Mesh has " + vertexCountMesh + " vertices, " + faceCount + " faces");

        for (int i = 0; i < faceCount; i++) {
            AIFace face = faces.get(i);
            IntBuffer indices = face.mIndices();
            if (indices.limit() != 3) continue; // только треугольники

            int i0 = indices.get(0);
            int i1 = indices.get(1);
            int i2 = indices.get(2);

            if (i0 < 0 || i0 >= vertexCountMesh ||
                    i1 < 0 || i1 >= vertexCountMesh ||
                    i2 < 0 || i2 >= vertexCountMesh) {
                System.err.println("Skipping triangle with invalid indices: " + i0 + ", " + i1 + ", " + i2);
                continue;
            }
            data.addIndex(baseIndex + i0);
            data.addIndex(baseIndex + i1);
            data.addIndex(baseIndex + i2);
        }
    }
}