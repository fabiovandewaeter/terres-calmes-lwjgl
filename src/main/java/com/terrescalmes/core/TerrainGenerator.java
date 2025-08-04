package com.terrescalmes.core;

import org.joml.*;
import org.joml.Math;

import com.terrescalmes.core.graphics.Material;
import com.terrescalmes.core.graphics.Mesh;
import com.terrescalmes.core.graphics.Model;

import java.util.*;

public class TerrainGenerator {

    private static final int TERRAIN_SIZE = 129; // Taille réduite pour de meilleures performances
    private static final float HEIGHT_SCALE = 30.0f; // Échelle de hauteur
    private static final float TERRAIN_SCALE = 2.0f; // Échelle du terrain
    private static final int OCTAVES = 4; // Nombre d'octaves pour le bruit
    private static final float PERSISTENCE = 0.5f; // Persistance du bruit
    private static final float FREQUENCY = 0.008f; // Fréquence de base

    public static Model generateTerrain(String modelId, TextureCache textureCache) {
        // Générer les hauteurs avec Perlin noise
        float[][] heightMap = generateHeightMap();

        // Créer les vertices et indices
        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // Générer les vertices
        for (int z = 0; z < TERRAIN_SIZE; z++) {
            for (int x = 0; x < TERRAIN_SIZE; x++) {
                float height = heightMap[z][x];

                // Position
                vertices.add((float) x * TERRAIN_SCALE);
                vertices.add(height);
                vertices.add((float) z * TERRAIN_SCALE);

                // Coordonnées de texture (répétition pour plus de détails)
                texCoords.add((float) x / (TERRAIN_SIZE - 1) * 4.0f);
                texCoords.add((float) z / (TERRAIN_SIZE - 1) * 4.0f);

                // Calculer la normale
                Vector3f normal = calculateNormal(heightMap, x, z);
                normals.add(normal.x);
                normals.add(normal.y);
                normals.add(normal.z);
            }
        }

        // Générer les indices pour les triangles
        for (int z = 0; z < TERRAIN_SIZE - 1; z++) {
            for (int x = 0; x < TERRAIN_SIZE - 1; x++) {
                int topLeft = (z * TERRAIN_SIZE) + x;
                int topRight = topLeft + 1;
                int bottomLeft = ((z + 1) * TERRAIN_SIZE) + x;
                int bottomRight = bottomLeft + 1;

                // Premier triangle (sens anti-horaire vu du dessus pour OpenGL)
                indices.add(topLeft);
                indices.add(bottomLeft);
                indices.add(topRight);

                // Deuxième triangle (sens anti-horaire vu du dessus pour OpenGL)
                indices.add(topRight);
                indices.add(bottomLeft);
                indices.add(bottomRight);
            }
        }

        // Convertir les listes en tableaux
        float[] verticesArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            verticesArray[i] = vertices.get(i);
        }

        float[] normalsArray = new float[normals.size()];
        for (int i = 0; i < normals.size(); i++) {
            normalsArray[i] = normals.get(i);
        }

        float[] texCoordsArray = new float[texCoords.size()];
        for (int i = 0; i < texCoords.size(); i++) {
            texCoordsArray[i] = texCoords.get(i);
        }

        int[] indicesArray = indices.stream().mapToInt(i -> i).toArray();

        // Créer le mesh avec le constructeur approprié
        Mesh mesh = new Mesh(verticesArray, normalsArray, texCoordsArray, indicesArray);

        // Créer le matériau
        Material material = new Material();
        material.setAmbientColor(new Vector4f(0.2f, 0.5f, 0.2f, 1.0f));
        material.setDiffuseColor(new Vector4f(0.4f, 0.8f, 0.4f, 1.0f));
        material.setSpecularColor(new Vector4f(0.1f, 0.1f, 0.1f, 1.0f));
        material.setReflectance(0.1f);

        // Si tu n'as pas de texture d'herbe, on peut utiliser une texture par défaut
        material.setTexturePath("default_texture");
        material.getMeshList().add(mesh);

        // Créer le modèle
        Model model = new Model(modelId, Arrays.asList(material));

        return model;
    }

    public static Model generateColoredTerrain(String modelId, TextureCache textureCache) {
        // Version alternative qui génère des couleurs basées sur l'altitude
        float[][] heightMap = generateHeightMap();

        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float minHeight = Float.MAX_VALUE;
        float maxHeight = Float.MIN_VALUE;

        // Trouver min/max pour normaliser les couleurs
        for (int z = 0; z < TERRAIN_SIZE; z++) {
            for (int x = 0; x < TERRAIN_SIZE; x++) {
                minHeight = Math.min(minHeight, heightMap[z][x]);
                maxHeight = Math.max(maxHeight, heightMap[z][x]);
            }
        }

        // Générer les vertices avec couleurs basées sur l'altitude
        for (int z = 0; z < TERRAIN_SIZE; z++) {
            for (int x = 0; x < TERRAIN_SIZE; x++) {
                float height = heightMap[z][x];

                vertices.add((float) x * TERRAIN_SCALE);
                vertices.add(height);
                vertices.add((float) z * TERRAIN_SCALE);

                // Couleur basée sur l'altitude
                float normalizedHeight = (height - minHeight) / (maxHeight - minHeight);
                texCoords.add(normalizedHeight); // U basé sur l'altitude
                texCoords.add(0.5f); // V constant

                Vector3f normal = calculateNormal(heightMap, x, z);
                normals.add(normal.x);
                normals.add(normal.y);
                normals.add(normal.z);
            }
        }

        // Même génération d'indices
        for (int z = 0; z < TERRAIN_SIZE - 1; z++) {
            for (int x = 0; x < TERRAIN_SIZE - 1; x++) {
                int topLeft = (z * TERRAIN_SIZE) + x;
                int topRight = topLeft + 1;
                int bottomLeft = ((z + 1) * TERRAIN_SIZE) + x;
                int bottomRight = bottomLeft + 1;

                // Même génération d'indices (sens anti-horaire)
                indices.add(topLeft);
                indices.add(bottomLeft);
                indices.add(topRight);

                indices.add(topRight);
                indices.add(bottomLeft);
                indices.add(bottomRight);
            }
        }

        float[] verticesArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            verticesArray[i] = vertices.get(i);
        }

        float[] normalsArray = new float[normals.size()];
        for (int i = 0; i < normals.size(); i++) {
            normalsArray[i] = normals.get(i);
        }

        float[] texCoordsArray = new float[texCoords.size()];
        for (int i = 0; i < texCoords.size(); i++) {
            texCoordsArray[i] = texCoords.get(i);
        }

        int[] indicesArray = indices.stream().mapToInt(i -> i).toArray();

        Mesh mesh = new Mesh(verticesArray, normalsArray, texCoordsArray, indicesArray);

        Material material = new Material();
        // Couleurs qui changent selon l'altitude
        material.setAmbientColor(new Vector4f(0.1f, 0.3f, 0.1f, 1.0f)); // Vert sombre
        material.setDiffuseColor(new Vector4f(0.3f, 0.7f, 0.3f, 1.0f)); // Vert clair
        material.setSpecularColor(new Vector4f(0.2f, 0.2f, 0.2f, 1.0f));
        material.setReflectance(0.1f);
        material.setTexturePath("default_texture");
        material.getMeshList().add(mesh);

        return new Model(modelId, Arrays.asList(material));
    }

    private static float[][] generateHeightMap() {
        float[][] heightMap = new float[TERRAIN_SIZE][TERRAIN_SIZE];

        for (int z = 0; z < TERRAIN_SIZE; z++) {
            for (int x = 0; x < TERRAIN_SIZE; x++) {
                float height = 0;
                float amplitude = HEIGHT_SCALE;
                float frequency = FREQUENCY;

                // Générer plusieurs octaves de bruit
                for (int i = 0; i < OCTAVES; i++) {
                    height += perlinNoise(x * frequency, z * frequency) * amplitude;
                    amplitude *= PERSISTENCE;
                    frequency *= 2;
                }

                heightMap[z][x] = height;
            }
        }

        return heightMap;
    }

    // Implémentation simple du bruit de Perlin
    private static float perlinNoise(float x, float y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);

        float xf = x - xi;
        float yf = y - yi;

        // Obtenir les valeurs aux coins
        float tl = dotGridGradient(xi, yi, x, y);
        float tr = dotGridGradient(xi + 1, yi, x, y);
        float bl = dotGridGradient(xi, yi + 1, x, y);
        float br = dotGridGradient(xi + 1, yi + 1, x, y);

        // Interpolation
        float xt1 = interpolate(tl, tr, xf);
        float xt2 = interpolate(bl, br, xf);
        float value = interpolate(xt1, xt2, yf);

        return value;
    }

    private static float dotGridGradient(int ix, int iy, float x, float y) {
        // Générer un vecteur gradient pseudo-aléatoire basé sur les coordonnées
        int hash = ix * 374761393 + iy * 668265263;
        hash = (hash ^ (hash >> 13)) * 1274126177;
        hash = hash ^ (hash >> 16);

        float angle = (hash & 0xFF) * (float) (2.0 * Math.PI / 256.0);
        float gradX = (float) Math.cos(angle);
        float gradY = (float) Math.sin(angle);

        // Calculer le vecteur distance
        float dx = x - ix;
        float dy = y - iy;

        // Produit scalaire
        return dx * gradX + dy * gradY;
    }

    private static float interpolate(float a, float b, float t) {
        // Interpolation cosinus pour des transitions plus douces
        float ft = (float) (t * Math.PI);
        float f = (float) (1 - Math.cos(ft)) * 0.5f;
        return a * (1 - f) + b * f;
    }

    private static Vector3f calculateNormal(float[][] heightMap, int x, int z) {
        float heightL = getHeight(heightMap, x - 1, z);
        float heightR = getHeight(heightMap, x + 1, z);
        float heightD = getHeight(heightMap, x, z - 1);
        float heightU = getHeight(heightMap, x, z + 1);

        Vector3f normal = new Vector3f(heightL - heightR, 2.0f * TERRAIN_SCALE, heightD - heightU);
        normal.normalize();

        return normal;
    }

    private static float getHeight(float[][] heightMap, int x, int z) {
        if (x < 0 || x >= TERRAIN_SIZE || z < 0 || z >= TERRAIN_SIZE) {
            return 0;
        }
        return heightMap[z][x];
    }

    // Méthode utilitaire pour créer un terrain plat (pour tester)
    public static Model generateFlatTerrain(String modelId, TextureCache textureCache, float size) {
        List<Float> vertices = Arrays.asList(
                -size, 0.0f, -size, // Bottom-left
                size, 0.0f, -size, // Bottom-right
                size, 0.0f, size, // Top-right
                -size, 0.0f, size // Top-left
        );

        List<Float> normals = Arrays.asList(
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f);

        List<Float> texCoords = Arrays.asList(
                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f);

        List<Integer> indices = Arrays.asList(
                0, 1, 2, // Premier triangle (sens anti-horaire)
                2, 3, 0 // Deuxième triangle (sens anti-horaire)
        );

        float[] verticesArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            verticesArray[i] = vertices.get(i);
        }

        float[] normalsArray = new float[normals.size()];
        for (int i = 0; i < normals.size(); i++) {
            normalsArray[i] = normals.get(i);
        }

        float[] texCoordsArray = new float[texCoords.size()];
        for (int i = 0; i < texCoords.size(); i++) {
            texCoordsArray[i] = texCoords.get(i);
        }

        int[] indicesArray = indices.stream().mapToInt(i -> i).toArray();

        Mesh mesh = new Mesh(verticesArray, normalsArray, texCoordsArray, indicesArray);

        Material material = new Material();
        material.setAmbientColor(new Vector4f(0.2f, 0.5f, 0.2f, 1.0f));
        material.setDiffuseColor(new Vector4f(0.4f, 0.8f, 0.4f, 1.0f));
        material.setSpecularColor(new Vector4f(0.1f, 0.1f, 0.1f, 1.0f));
        material.setReflectance(0.1f);
        material.setTexturePath("default_texture");
        material.getMeshList().add(mesh);

        return new Model(modelId, Arrays.asList(material));
    }
}
