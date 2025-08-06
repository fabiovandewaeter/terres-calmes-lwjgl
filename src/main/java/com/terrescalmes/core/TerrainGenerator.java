package com.terrescalmes.core;

import org.joml.*;
import org.joml.Math;

import com.terrescalmes.core.graphics.Material;
import com.terrescalmes.core.graphics.Mesh;
import com.terrescalmes.core.graphics.Model;

import java.util.*;

public class TerrainGenerator {

    private static final String GRASS_TEXTURE = "resources/textures/grass.png";
    private static final String ROCK_TEXTURE = "resources/textures/rock.png";
    private static final String SNOW_TEXTURE = "resources/textures/snow.png";
    private static final String DIRT_TEXTURE = "resources/textures/dirt.png";

    private static final int TERRAIN_SIZE = 32;
    private static final float HEIGHT_SCALE = 300.0f;
    private static final float TERRAIN_SCALE = 2.0f;
    private static final int OCTAVES = 4;
    private static final float PERSISTENCE = 0.5f;
    private static final float FREQUENCY = 0.001f;

    // Seuils d'altitude pour les différentes textures
    private static final float GRASS_HEIGHT = 50.0f;
    private static final float ROCK_HEIGHT = 100.0f;
    private static final float SNOW_HEIGHT = 200.0f;

    /**
     * Génère un chunk de terrain avec multi-texturing basé sur l'altitude
     */
    public static Model generateTerrainChunk(String modelId, TextureCache textureCache,
            float offsetX, float offsetZ, float chunkSize) {

        int chunkVertices = TERRAIN_SIZE;
        float vertexSpacing = chunkSize / (chunkVertices - 1);

        // Générer la heightmap avec offset
        float[][] heightMap = generateHeightMapWithOffset(offsetX, offsetZ, vertexSpacing, chunkVertices + 2);

        // Séparer les vertices par type de texture
        Map<TextureType, TerrainData> terrainDataMap = new HashMap<>();
        terrainDataMap.put(TextureType.GRASS, new TerrainData());
        terrainDataMap.put(TextureType.ROCK, new TerrainData());
        terrainDataMap.put(TextureType.SNOW, new TerrainData());

        // Analyser chaque triangle pour déterminer sa texture dominante
        analyzeTerrainTriangles(heightMap, chunkVertices, vertexSpacing, terrainDataMap);

        // Créer les matériaux et meshes pour chaque texture
        List<Material> materials = createMaterials(textureCache, terrainDataMap);

        return new Model(modelId, materials);
    }

    /**
     * Analyse chaque triangle du terrain pour déterminer sa texture dominante
     */
    private static void analyzeTerrainTriangles(float[][] heightMap, int chunkVertices,
            float vertexSpacing, Map<TextureType, TerrainData> terrainDataMap) {

        // Parcourir tous les quads du terrain
        for (int z = 1; z < chunkVertices; z++) {
            for (int x = 1; x < chunkVertices; x++) {
                // Positions des 4 coins du quad
                Vector3f[] quadVertices = new Vector3f[4];
                quadVertices[0] = new Vector3f((x - 1) * vertexSpacing, heightMap[z][x], (z - 1) * vertexSpacing); // topLeft
                quadVertices[1] = new Vector3f(x * vertexSpacing, heightMap[z][x + 1], (z - 1) * vertexSpacing); // topRight
                quadVertices[2] = new Vector3f((x - 1) * vertexSpacing, heightMap[z + 1][x], z * vertexSpacing); // bottomLeft
                quadVertices[3] = new Vector3f(x * vertexSpacing, heightMap[z + 1][x + 1], z * vertexSpacing); // bottomRight

                // Déterminer la texture dominante pour ce quad
                float avgHeight = (quadVertices[0].y + quadVertices[1].y + quadVertices[2].y + quadVertices[3].y)
                        / 4.0f;
                TextureType textureType = getTextureTypeForHeight(avgHeight);

                // Ajouter les deux triangles de ce quad au bon terrain data
                TerrainData terrainData = terrainDataMap.get(textureType);
                addQuadToTerrainData(terrainData, quadVertices, heightMap, x, z, vertexSpacing);
            }
        }
    }

    /**
     * Ajoute un quad (2 triangles) aux données de terrain
     */
    private static void addQuadToTerrainData(TerrainData terrainData, Vector3f[] quadVertices,
            float[][] heightMap, int x, int z, float vertexSpacing) {

        int baseIndex = terrainData.vertices.size() / 3;

        // Ajouter les 4 vertices du quad
        for (Vector3f vertex : quadVertices) {
            // Position
            terrainData.vertices.add(vertex.x);
            terrainData.vertices.add(vertex.y);
            terrainData.vertices.add(vertex.z);

            // Coordonnées de texture
            terrainData.texCoords.add(vertex.x / vertexSpacing / 4.0f);
            terrainData.texCoords.add(vertex.z / vertexSpacing / 4.0f);

            // Normale (calculée approximativement)
            Vector3f normal = calculateNormalAtPosition(heightMap, vertex.x / vertexSpacing + 1,
                    vertex.z / vertexSpacing + 1, vertexSpacing);
            terrainData.normals.add(normal.x);
            terrainData.normals.add(normal.y);
            terrainData.normals.add(normal.z);
        }

        // Ajouter les indices pour les deux triangles (sens anti-horaire)
        // Premier triangle: topLeft -> bottomLeft -> topRight
        terrainData.indices.add(baseIndex);
        terrainData.indices.add(baseIndex + 2);
        terrainData.indices.add(baseIndex + 1);

        // Deuxième triangle: topRight -> bottomLeft -> bottomRight
        terrainData.indices.add(baseIndex + 1);
        terrainData.indices.add(baseIndex + 2);
        terrainData.indices.add(baseIndex + 3);
    }

    /**
     * Détermine le type de texture basé sur l'altitude
     */
    private static TextureType getTextureTypeForHeight(float height) {
        if (height < GRASS_HEIGHT) {
            return TextureType.GRASS;
        } else if (height < ROCK_HEIGHT) {
            return TextureType.ROCK;
        } else {
            return TextureType.SNOW;
        }
    }

    /**
     * Crée les matériaux pour chaque type de texture
     */
    private static List<Material> createMaterials(TextureCache textureCache,
            Map<TextureType, TerrainData> terrainDataMap) {

        List<Material> materials = new ArrayList<>();

        for (Map.Entry<TextureType, TerrainData> entry : terrainDataMap.entrySet()) {
            TextureType textureType = entry.getKey();
            TerrainData terrainData = entry.getValue();

            // Ignorer les textures qui n'ont pas de géométrie
            if (terrainData.vertices.isEmpty()) {
                continue;
            }

            // Convertir les listes en tableaux
            float[] verticesArray = terrainData.vertices.stream().reduce(new float[0],
                    (arr, list) -> {
                        float[] newArr = Arrays.copyOf(arr, arr.length + 1);
                        newArr[arr.length] = list;
                        return newArr;
                    }, (arr1, arr2) -> arr1);

            float[] normalsArray = terrainData.normals.stream().reduce(new float[0],
                    (arr, list) -> {
                        float[] newArr = Arrays.copyOf(arr, arr.length + 1);
                        newArr[arr.length] = list;
                        return newArr;
                    }, (arr1, arr2) -> arr1);

            float[] texCoordsArray = terrainData.texCoords.stream().reduce(new float[0],
                    (arr, list) -> {
                        float[] newArr = Arrays.copyOf(arr, arr.length + 1);
                        newArr[arr.length] = list;
                        return newArr;
                    }, (arr1, arr2) -> arr1);

            int[] indicesArray = terrainData.indices.stream().mapToInt(i -> i).toArray();

            // Créer le mesh
            Mesh mesh = new Mesh(verticesArray, normalsArray, texCoordsArray, indicesArray);

            // Créer le matériau avec la bonne texture
            Material material = createMaterialForTextureType(textureType);
            material.getMeshList().add(mesh);
            materials.add(material);
        }

        return materials;
    }

    /**
     * Crée un matériau pour un type de texture donné
     */
    private static Material createMaterialForTextureType(TextureType textureType) {
        Material material = new Material();

        switch (textureType) {
            case GRASS:
                material.setTexturePath(GRASS_TEXTURE);
                break;
            case ROCK:
                material.setTexturePath(ROCK_TEXTURE);
                break;
            case SNOW:
                material.setTexturePath(SNOW_TEXTURE);
                break;
        }

        material.setReflectance(0.1f);
        return material;
    }

    /**
     * Génère une heightmap avec offset pour assurer la continuité entre chunks
     */
    private static float[][] generateHeightMapWithOffset(float offsetX, float offsetZ, float vertexSpacing, int size) {
        float[][] heightMap = new float[size][size];

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                float worldX = offsetX + ((x - 1) * vertexSpacing);
                float worldZ = offsetZ + ((z - 1) * vertexSpacing);

                float height = 0;
                float amplitude = HEIGHT_SCALE;
                float frequency = FREQUENCY;

                for (int i = 0; i < OCTAVES; i++) {
                    height += perlinNoise(worldX * frequency, worldZ * frequency) * amplitude;
                    amplitude *= PERSISTENCE;
                    frequency *= 2;
                }

                heightMap[z][x] = height;
            }
        }

        return heightMap;
    }

    /**
     * Calcule la normale à une position donnée
     */
    private static Vector3f calculateNormalAtPosition(float[][] heightMap, float x, float z, float spacing) {
        int xi = Math.max(1, Math.min((int) x, heightMap.length - 2));
        int zi = Math.max(1, Math.min((int) z, heightMap[0].length - 2));

        float heightL = heightMap[zi][xi - 1];
        float heightR = heightMap[zi][xi + 1];
        float heightD = heightMap[zi - 1][xi];
        float heightU = heightMap[zi + 1][xi];

        Vector3f normal = new Vector3f(heightL - heightR, 2.0f * spacing, heightD - heightU);
        normal.normalize();
        return normal;
    }

    // Implémentation du bruit de Perlin (identique à avant)
    private static float perlinNoise(float x, float y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);

        float xf = x - xi;
        float yf = y - yi;

        float tl = dotGridGradient(xi, yi, x, y);
        float tr = dotGridGradient(xi + 1, yi, x, y);
        float bl = dotGridGradient(xi, yi + 1, x, y);
        float br = dotGridGradient(xi + 1, yi + 1, x, y);

        float xt1 = interpolate(tl, tr, xf);
        float xt2 = interpolate(bl, br, xf);
        float value = interpolate(xt1, xt2, yf);

        return value;
    }

    private static float dotGridGradient(int ix, int iy, float x, float y) {
        int hash = ix * 374761393 + iy * 668265263;
        hash = (hash ^ (hash >> 13)) * 1274126177;
        hash = hash ^ (hash >> 16);

        float angle = (hash & 0xFF) * (float) (2.0 * Math.PI / 256.0);
        float gradX = (float) Math.cos(angle);
        float gradY = (float) Math.sin(angle);

        float dx = x - ix;
        float dy = y - iy;

        return dx * gradX + dy * gradY;
    }

    private static float interpolate(float a, float b, float t) {
        float ft = (float) (t * Math.PI);
        float f = (float) (1 - Math.cos(ft)) * 0.5f;
        return a * (1 - f) + b * f;
    }

    /**
     * Enum pour les types de texture
     */
    private enum TextureType {
        GRASS, ROCK, SNOW
    }

    /**
     * Classe pour stocker les données de terrain par texture
     */
    private static class TerrainData {
        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
    }

    // Conserver les méthodes existantes pour compatibilité
    public static Model generateTerrain(String modelId, TextureCache textureCache) {
        return generateColoredTerrain(modelId, textureCache);
    }

    public static Model generateColoredTerrain(String modelId, TextureCache textureCache) {
        float[][] heightMap = generateHeightMap();

        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float minHeight = Float.MAX_VALUE;
        float maxHeight = Float.MIN_VALUE;

        for (int z = 0; z < TERRAIN_SIZE; z++) {
            for (int x = 0; x < TERRAIN_SIZE; x++) {
                minHeight = Math.min(minHeight, heightMap[z][x]);
                maxHeight = Math.max(maxHeight, heightMap[z][x]);
            }
        }

        for (int z = 0; z < TERRAIN_SIZE; z++) {
            for (int x = 0; x < TERRAIN_SIZE; x++) {
                float height = heightMap[z][x];

                vertices.add((float) x * TERRAIN_SCALE);
                vertices.add(height);
                vertices.add((float) z * TERRAIN_SCALE);

                float normalizedHeight = (height - minHeight) / (maxHeight - minHeight);
                texCoords.add(normalizedHeight);
                texCoords.add(0.5f);

                Vector3f normal = calculateNormal(heightMap, x, z);
                normals.add(normal.x);
                normals.add(normal.y);
                normals.add(normal.z);
            }
        }

        for (int z = 0; z < TERRAIN_SIZE - 1; z++) {
            for (int x = 0; x < TERRAIN_SIZE - 1; x++) {
                int topLeft = (z * TERRAIN_SIZE) + x;
                int topRight = topLeft + 1;
                int bottomLeft = ((z + 1) * TERRAIN_SIZE) + x;
                int bottomRight = bottomLeft + 1;

                indices.add(topLeft);
                indices.add(bottomLeft);
                indices.add(topRight);

                indices.add(topRight);
                indices.add(bottomLeft);
                indices.add(bottomRight);
            }
        }

        float[] verticesArray = vertices.stream().reduce(new float[0],
                (arr, list) -> {
                    float[] newArr = Arrays.copyOf(arr, arr.length + 1);
                    newArr[arr.length] = list;
                    return newArr;
                }, (arr1, arr2) -> arr1);

        float[] normalsArray = normals.stream().reduce(new float[0],
                (arr, list) -> {
                    float[] newArr = Arrays.copyOf(arr, arr.length + 1);
                    newArr[arr.length] = list;
                    return newArr;
                }, (arr1, arr2) -> arr1);

        float[] texCoordsArray = texCoords.stream().reduce(new float[0],
                (arr, list) -> {
                    float[] newArr = Arrays.copyOf(arr, arr.length + 1);
                    newArr[arr.length] = list;
                    return newArr;
                }, (arr1, arr2) -> arr1);

        int[] indicesArray = indices.stream().mapToInt(i -> i).toArray();

        Mesh mesh = new Mesh(verticesArray, normalsArray, texCoordsArray, indicesArray);

        Material material = new Material();
        material.setAmbientColor(new Vector4f(0.1f, 0.3f, 0.1f, 1.0f));
        material.setDiffuseColor(new Vector4f(0.3f, 0.7f, 0.3f, 1.0f));
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

    public static Model generateFlatTerrain(String modelId, TextureCache textureCache, float size) {
        List<Float> vertices = Arrays.asList(
                -size, 0.0f, -size,
                size, 0.0f, -size,
                size, 0.0f, size,
                -size, 0.0f, size);

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
                0, 1, 2,
                2, 3, 0);

        float[] verticesArray = vertices.stream().reduce(new float[0],
                (arr, list) -> {
                    float[] newArr = Arrays.copyOf(arr, arr.length + 1);
                    newArr[arr.length] = list;
                    return newArr;
                }, (arr1, arr2) -> arr1);

        float[] normalsArray = normals.stream().reduce(new float[0],
                (arr, list) -> {
                    float[] newArr = Arrays.copyOf(arr, arr.length + 1);
                    newArr[arr.length] = list;
                    return newArr;
                }, (arr1, arr2) -> arr1);

        float[] texCoordsArray = texCoords.stream().reduce(new float[0],
                (arr, list) -> {
                    float[] newArr = Arrays.copyOf(arr, arr.length + 1);
                    newArr[arr.length] = list;
                    return newArr;
                }, (arr1, arr2) -> arr1);

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
