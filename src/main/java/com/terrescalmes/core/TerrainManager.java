package com.terrescalmes.core;

import org.joml.Vector2i;
import org.joml.Vector3f;
import com.terrescalmes.core.graphics.Model;
import com.terrescalmes.core.graphics.Scene;
import com.terrescalmes.entities.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class TerrainManager {

    // Configuration du terrain
    private static final float CHUNK_SIZE = 256.0f; // Taille d'un chunk en unités monde
    private static final int RENDER_DISTANCE = 3; // Nombre de chunks visibles dans chaque direction
    private static final int CACHE_SIZE = 50; // Nombre maximum de chunks en cache

    // Cache des chunks
    private final Map<Vector2i, TerrainChunk> loadedChunks;
    private final Map<Vector2i, TerrainChunk> cachedChunks;
    private final Set<Vector2i> loadingChunks;

    // Référence à la scène et position du joueur
    private final Scene scene;
    private final TextureCache textureCache;
    private Vector2i lastPlayerChunk;

    // Queue pour génération sur le thread principal
    private final Set<Vector2i> chunksToGenerate;
    private final Map<Vector2i, TerrainChunk> chunksToAdd;

    public TerrainManager(Scene scene, TextureCache textureCache) {
        this.scene = scene;
        this.textureCache = textureCache;
        this.loadedChunks = new HashMap<>();
        this.cachedChunks = new HashMap<>();
        this.loadingChunks = new HashSet<>();
        this.chunksToGenerate = new HashSet<>();
        this.chunksToAdd = new HashMap<>();
        this.lastPlayerChunk = new Vector2i(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Met à jour le terrain en fonction de la position du joueur
     */
    public void update(Vector3f playerPosition) {
        Vector2i currentChunk = worldToChunk(playerPosition);

        // Si le joueur a changé de chunk, mettre à jour le terrain
        if (!currentChunk.equals(lastPlayerChunk)) {
            updateTerrain(currentChunk);
            lastPlayerChunk = new Vector2i(currentChunk);
        }

        // Générer les chunks en attente sur le thread principal
        generatePendingChunks();

        // Ajouter les chunks générés à la scène
        addGeneratedChunks();
    }

    /**
     * Met à jour quels chunks doivent être visibles
     */
    private void updateTerrain(Vector2i centerChunk) {
        Set<Vector2i> chunksToLoad = new HashSet<>();
        Set<Vector2i> chunksToUnload = new HashSet<>();

        // Déterminer quels chunks doivent être chargés
        for (int x = -RENDER_DISTANCE; x <= RENDER_DISTANCE; x++) {
            for (int z = -RENDER_DISTANCE; z <= RENDER_DISTANCE; z++) {
                Vector2i chunkPos = new Vector2i(centerChunk.x + x, centerChunk.y + z);

                // Si le chunk n'est pas déjà chargé et pas en cours de chargement
                if (!loadedChunks.containsKey(chunkPos) && !loadingChunks.contains(chunkPos)) {
                    chunksToLoad.add(chunkPos);
                }
            }
        }

        // Déterminer quels chunks doivent être déchargés
        for (Vector2i chunkPos : loadedChunks.keySet()) {
            int dx = Math.abs(chunkPos.x - centerChunk.x);
            int dz = Math.abs(chunkPos.y - centerChunk.y);

            if (dx > RENDER_DISTANCE || dz > RENDER_DISTANCE) {
                chunksToUnload.add(chunkPos);
            }
        }

        // Décharger les chunks trop loin
        for (Vector2i chunkPos : chunksToUnload) {
            unloadChunk(chunkPos);
        }

        // Charger les nouveaux chunks
        for (Vector2i chunkPos : chunksToLoad) {
            loadChunk(chunkPos);
        }
    }

    /**
     * Charge un chunk (marque pour génération sur le thread principal)
     */
    private void loadChunk(Vector2i chunkPos) {
        // Vérifier d'abord le cache
        if (cachedChunks.containsKey(chunkPos)) {
            TerrainChunk chunk = cachedChunks.remove(chunkPos);
            loadedChunks.put(chunkPos, chunk);
            scene.addEntity(chunk.getEntity());
            System.out.println("Chunk récupéré du cache: " + chunkPos);
            return;
        }

        // Marquer comme en cours de chargement et pour génération
        loadingChunks.add(chunkPos);
        chunksToGenerate.add(chunkPos);
    }

    /**
     * Génère un chunk de terrain
     */
    private TerrainChunk generateChunk(Vector2i chunkPos) {
        try {
            Vector3f worldPos = chunkToWorld(chunkPos);
            String modelId = "terrain_chunk_" + chunkPos.x + "_" + chunkPos.y;
            String entityId = "entity_" + modelId;

            // Générer le modèle de terrain avec offset pour la continuité
            Model terrainModel = TerrainGenerator.generateTerrainChunk(
                    modelId, textureCache, worldPos.x, worldPos.z, CHUNK_SIZE);

            // Créer l'entité avec la position exacte du chunk
            Entity entity = new Entity(entityId, modelId);
            entity.setPosition(worldPos.x, 0, worldPos.z);
            entity.updateModelMatrix();

            return new TerrainChunk(chunkPos, terrainModel, entity);

        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du chunk " + chunkPos + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Génère les chunks en attente sur le thread principal
     */
    private void generatePendingChunks() {
        // Générer un chunk par frame pour éviter les lags
        if (!chunksToGenerate.isEmpty()) {
            Vector2i chunkPos = chunksToGenerate.iterator().next();
            chunksToGenerate.remove(chunkPos);

            try {
                TerrainChunk chunk = generateChunk(chunkPos);
                if (chunk != null) {
                    chunksToAdd.put(chunkPos, chunk);
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la génération du chunk " + chunkPos + ": " + e.getMessage());
                loadingChunks.remove(chunkPos);
            }
        }
    }

    /**
     * Ajoute les chunks générés à la scène
     */
    private void addGeneratedChunks() {
        if (!chunksToAdd.isEmpty()) {
            for (Map.Entry<Vector2i, TerrainChunk> entry : chunksToAdd.entrySet()) {
                Vector2i chunkPos = entry.getKey();
                TerrainChunk chunk = entry.getValue();

                // Ajouter à la scène
                scene.addModel(chunk.getModel());
                scene.addEntity(chunk.getEntity());
                loadedChunks.put(chunkPos, chunk);
                loadingChunks.remove(chunkPos);
                System.out.println("Chunk généré et chargé: " + chunkPos);
            }
            chunksToAdd.clear();
        }
    }

    /**
     * Décharge un chunk (le met en cache)
     */
    private void unloadChunk(Vector2i chunkPos) {
        TerrainChunk chunk = loadedChunks.remove(chunkPos);
        if (chunk != null) {
            // Retirer de la scène
            scene.removeEntity(chunk.getEntity());

            // Ajouter au cache si il y a de la place
            if (cachedChunks.size() < CACHE_SIZE) {
                cachedChunks.put(chunkPos, chunk);
                System.out.println("Chunk mis en cache: " + chunkPos);
            } else {
                // Cache plein, supprimer définitivement
                chunk.cleanup();
                System.out.println("Chunk supprimé: " + chunkPos);
            }
        }
    }

    /**
     * Convertit une position monde en coordonnées de chunk
     */
    private Vector2i worldToChunk(Vector3f worldPos) {
        // Utiliser floor pour gérer correctement les coordonnées négatives
        int chunkX = (int) Math.floor(worldPos.x / CHUNK_SIZE);
        int chunkZ = (int) Math.floor(worldPos.z / CHUNK_SIZE);
        return new Vector2i(chunkX, chunkZ);
    }

    /**
     * Convertit des coordonnées de chunk en position monde (coin inférieur gauche
     * du chunk)
     */
    private Vector3f chunkToWorld(Vector2i chunkPos) {
        float worldX = chunkPos.x * CHUNK_SIZE;
        float worldZ = chunkPos.y * CHUNK_SIZE;
        return new Vector3f(worldX, 0, worldZ);
    }

    /**
     * Nettoie le gestionnaire de terrain
     */
    public void cleanup() {
        // Nettoyer tous les chunks
        for (TerrainChunk chunk : loadedChunks.values()) {
            chunk.cleanup();
        }
        for (TerrainChunk chunk : cachedChunks.values()) {
            chunk.cleanup();
        }
        for (TerrainChunk chunk : chunksToAdd.values()) {
            chunk.cleanup();
        }

        loadedChunks.clear();
        cachedChunks.clear();
        chunksToGenerate.clear();
        chunksToAdd.clear();
        loadingChunks.clear();
    }

    /**
     * Infos de debug
     */
    public String getDebugInfo() {
        return String.format("Chunks - Chargés: %d, Cache: %d, Génération: %d, En attente: %d",
                loadedChunks.size(), cachedChunks.size(), chunksToGenerate.size(), chunksToAdd.size());
    }

    /**
     * Classe interne pour représenter un chunk de terrain
     */
    private static class TerrainChunk {
        private final Vector2i position;
        private final Model model;
        private final Entity entity;

        public TerrainChunk(Vector2i position, Model model, Entity entity) {
            this.position = position;
            this.model = model;
            this.entity = entity;
        }

        public Vector2i getPosition() {
            return position;
        }

        public Model getModel() {
            return model;
        }

        public Entity getEntity() {
            return entity;
        }

        public void cleanup() {
            if (model != null) {
                model.cleanup();
            }
        }
    }
}
