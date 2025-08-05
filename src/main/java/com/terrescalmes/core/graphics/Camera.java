package com.terrescalmes.core.graphics;

import org.joml.*;
import org.joml.Math;

public class Camera {

    private Vector3f direction;
    private Vector3f position;
    private Vector3f right;
    private Vector2f rotation;
    private Vector3f up;
    private Matrix4f viewMatrix;

    // Limites de rotation verticale (en radians)
    private static final float MAX_PITCH = (float) Math.toRadians(89.0f); // Presque vertical vers le haut
    private static final float MIN_PITCH = (float) Math.toRadians(-89.0f); // Presque vertical vers le bas

    public Camera() {
        direction = new Vector3f();
        right = new Vector3f();
        up = new Vector3f();
        position = new Vector3f();
        viewMatrix = new Matrix4f();
        rotation = new Vector2f();
    }

    public void addRotation(float x, float y) {
        rotation.add(x, y);

        // Limiter la rotation verticale (pitch)
        if (rotation.x > MAX_PITCH) {
            rotation.x = MAX_PITCH;
        } else if (rotation.x < MIN_PITCH) {
            rotation.x = MIN_PITCH;
        }

        recalculate();
    }

    public Vector3f getPosition() {
        return position;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public void moveBackwards(float inc) {
        // Calculer la direction horizontale (sans composante Y)
        Vector3f horizontalDirection = new Vector3f();

        // Utiliser seulement la rotation Y (yaw) pour la direction horizontale
        horizontalDirection.set(
                -(float) Math.sin(rotation.y), // Négatif pour aller vers l'arrière
                0.0f, // Pas de mouvement vertical
                (float) Math.cos(rotation.y)).normalize().mul(inc);

        position.add(horizontalDirection);
        recalculate();
    }

    public void moveDown(float inc) {
        position.y -= inc;
        recalculate();
    }

    public void moveForward(float inc) {
        // Calculer la direction horizontale (sans composante Y)
        Vector3f horizontalDirection = new Vector3f();

        // Utiliser seulement la rotation Y (yaw) pour la direction horizontale
        horizontalDirection.set(
                (float) Math.sin(rotation.y), // Direction où regarde la caméra
                0.0f, // Pas de mouvement vertical
                -(float) Math.cos(rotation.y) // Négatif car Z positif = vers nous dans OpenGL
        ).normalize().mul(inc);

        position.add(horizontalDirection);
        recalculate();
    }

    public void moveLeft(float inc) {
        // Calculer la direction gauche horizontale
        Vector3f horizontalLeft = new Vector3f();

        // La direction gauche est perpendiculaire à la direction avant (rotation Y -
        // 90°)
        horizontalLeft.set(
                (float) Math.sin(rotation.y - Math.PI / 2),
                0.0f, // Pas de mouvement vertical
                -(float) Math.cos(rotation.y - Math.PI / 2)).normalize().mul(inc);

        position.add(horizontalLeft);
        recalculate();
    }

    public void moveRight(float inc) {
        // Calculer la direction droite horizontale
        Vector3f horizontalRight = new Vector3f();

        // La direction droite est perpendiculaire à la direction avant (rotation Y +
        // 90°)
        horizontalRight.set(
                (float) Math.sin(rotation.y + Math.PI / 2),
                0.0f, // Pas de mouvement vertical
                -(float) Math.cos(rotation.y + Math.PI / 2)).normalize().mul(inc);

        position.add(horizontalRight);
        recalculate();
    }

    public void moveUp(float inc) {
        position.y += inc;
        recalculate();
    }

    private void recalculate() {
        viewMatrix.identity()
                .rotateX(rotation.x)
                .rotateY(rotation.y)
                .translate(-position.x, -position.y, -position.z);
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        recalculate();
    }

    public void setRotation(float x, float y) {
        rotation.set(x, y);

        // Limiter la rotation verticale même lors du set
        if (rotation.x > MAX_PITCH) {
            rotation.x = MAX_PITCH;
        } else if (rotation.x < MIN_PITCH) {
            rotation.x = MIN_PITCH;
        }

        recalculate();
    }
}
