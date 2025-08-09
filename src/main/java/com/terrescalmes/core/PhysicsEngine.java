package com.terrescalmes.core;

import org.joml.*;
import org.joml.Math;

import com.terrescalmes.core.TerrainManager;
import com.terrescalmes.core.graphics.Camera;

/**
 * Moteur physique simple pour gérer la gravité et les collisions avec le
 * terrain
 */
public class PhysicsEngine {

    // Constantes physiques
    private static final float GRAVITY = -9.81f * 10.0f; // Gravité amplifiée pour le jeu
    private static final float TERMINAL_VELOCITY = -50.0f; // Vitesse terminale
    private static final float GROUND_FRICTION = 0.8f; // Friction au sol
    private static final float AIR_RESISTANCE = 0.98f; // Résistance de l'air

    // État physique du joueur
    private Vector3f velocity;
    private boolean isGrounded;
    private float groundHeight;
    private float playerHeight; // Hauteur du joueur (collision)

    // Référence au terrain pour les collisions
    private TerrainManager terrainManager;

    public PhysicsEngine(TerrainManager terrainManager) {
        this.terrainManager = terrainManager;
        this.velocity = new Vector3f(0, 0, 0);
        this.isGrounded = false;
        this.groundHeight = 0;
        this.playerHeight = 2.0f; // Hauteur par défaut du joueur
    }

    /**
     * Met à jour la physique du joueur
     */
    public void update(Camera camera, float deltaTime) {
        Vector3f position = camera.getPosition();

        // Obtenir la hauteur du terrain à la position actuelle
        float terrainHeight = getTerrainHeightAt(position.x, position.z);
        groundHeight = terrainHeight + playerHeight; // Hauteur où le joueur doit s'arrêter

        // Vérifier si le joueur est au sol
        boolean wasGrounded = isGrounded;
        isGrounded = position.y <= groundHeight + 0.1f; // Petite marge d'erreur

        // Appliquer la gravité si pas au sol
        if (!isGrounded) {
            velocity.y += GRAVITY * deltaTime;

            // Limiter la vitesse de chute
            if (velocity.y < TERMINAL_VELOCITY) {
                velocity.y = TERMINAL_VELOCITY;
            }

            // Appliquer la résistance de l'air
            velocity.mul(AIR_RESISTANCE);
        } else {
            // Au sol : arrêter la chute et appliquer la friction
            if (velocity.y < 0) {
                velocity.y = 0;
            }

            // Appliquer la friction horizontale au sol
            velocity.x *= GROUND_FRICTION;
            velocity.z *= GROUND_FRICTION;

            // Si le joueur vient d'atterrir
            if (!wasGrounded) {
                onLanding(camera);
            }
        }

        // Appliquer la vélocité à la position
        Vector3f newPosition = new Vector3f(position);
        newPosition.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        // Collision avec le sol
        if (newPosition.y < groundHeight) {
            newPosition.y = groundHeight;
            velocity.y = 0;
            isGrounded = true;
        }

        // Mettre à jour la position de la caméra
        camera.setPosition(newPosition.x, newPosition.y, newPosition.z);
    }

    /**
     * Ajoute une impulsion au joueur (pour le saut ou le mouvement)
     */
    public void addImpulse(Vector3f impulse) {
        velocity.add(impulse);
    }

    /**
     * Fait sauter le joueur
     */
    public void jump(float jumpForce) {
        if (isGrounded) {
            velocity.y = jumpForce;
            isGrounded = false;
        }
    }

    /**
     * Ajoute une force horizontale (pour le mouvement du joueur)
     */
    public void addHorizontalForce(Vector3f force, float deltaTime) {
        // Limiter la force si le joueur est au sol pour éviter l'accélération infinie
        float maxSpeed = isGrounded ? 20.0f : 10.0f;

        Vector3f newVelocity = new Vector3f(velocity);
        newVelocity.add(force.x * deltaTime, 0, force.z * deltaTime);

        // Limiter la vitesse horizontale
        float horizontalSpeed = (float) Math.sqrt(newVelocity.x * newVelocity.x + newVelocity.z * newVelocity.z);
        if (horizontalSpeed > maxSpeed) {
            float scale = maxSpeed / horizontalSpeed;
            newVelocity.x *= scale;
            newVelocity.z *= scale;
        }

        velocity.x = newVelocity.x;
        velocity.z = newVelocity.z;
    }

    /**
     * Obtient la hauteur du terrain à une position donnée
     * Utilise une interpolation bilinéaire pour un résultat plus lisse
     */
    private float getTerrainHeightAt(float worldX, float worldZ) {
        // Pour l'instant, utilisation simple - vous pouvez améliorer avec
        // l'interpolation
        // Cette méthode devra être adaptée selon votre implémentation de TerrainManager

        // Méthode de base : essayer de récupérer la hauteur depuis le terrain
        // Vous devrez ajouter une méthode getHeightAt dans TerrainManager
        if (terrainManager != null) {
            return terrainManager.getHeightAt(worldX, worldZ);
        }

        // Valeur par défaut si pas de terrain
        return 0.0f;
    }

    /**
     * Appelée quand le joueur atterrit
     */
    private void onLanding(Camera camera) {
        // Ici vous pouvez ajouter des effets sonores, des particules, etc.
        System.out.println("Joueur atterrit à la hauteur: " + camera.getPosition().y);
    }

    // Getters pour l'état physique
    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public float getGroundHeight() {
        return groundHeight;
    }

    public void setPlayerHeight(float height) {
        this.playerHeight = height;
    }

    public float getPlayerHeight() {
        return playerHeight;
    }

    /**
     * Remet à zéro la vélocité
     */
    public void resetVelocity() {
        velocity.set(0, 0, 0);
    }

    /**
     * Téléporte le joueur à une position (sans vélocité)
     */
    public void teleportTo(Camera camera, Vector3f position) {
        camera.setPosition(position.x, position.y, position.z);
        resetVelocity();
        isGrounded = false; // Forcer la vérification au prochain update
    }
}
