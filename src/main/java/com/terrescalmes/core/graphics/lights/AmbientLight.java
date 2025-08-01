package com.terrescalmes.core.graphics.lights;

import org.joml.Vector3f;

// This type of light comes from everywhere in the space and illuminates all the objects in the same way.
public class AmbientLight {

    private Vector3f color;

    private float intensity;

    public AmbientLight(float intensity, Vector3f color) {
        this.intensity = intensity;
        this.color = color;
    }

    public AmbientLight() {
        this(1.0f, new Vector3f(1.0f, 1.0f, 1.0f));
    }

    public Vector3f getColor() {
        return color;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public void setColor(float r, float g, float b) {
        color.set(r, g, b);
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }
}
