package com.terrescalmes.core;

import java.util.*;

import com.terrescalmes.core.graphics.Texture;

public class TextureCache {

    public static final String DEFAULT_TEXTURE = "resources/textures/default_texture.png";
    public static final String GRASS_TEXTURE = "resources/textures/grass.png";
    public static final String ROCK_TEXTURE = "resources/textures/rock.png";
    public static final String SNOW_TEXTURE = "resources/textures/snow.png";

    private Map<String, Texture> textureMap;

    public TextureCache() {
        textureMap = new HashMap<>();
        textureMap.put(DEFAULT_TEXTURE, new Texture(DEFAULT_TEXTURE));
        textureMap.put(GRASS_TEXTURE, new Texture(GRASS_TEXTURE));
        textureMap.put(ROCK_TEXTURE, new Texture(ROCK_TEXTURE));
        textureMap.put(SNOW_TEXTURE, new Texture(SNOW_TEXTURE));
    }

    public void cleanup() {
        textureMap.values().forEach(Texture::cleanup);
    }

    public Texture createTexture(String texturePath) {
        return textureMap.computeIfAbsent(texturePath, Texture::new);
    }

    public Texture getTexture(String texturePath) {
        Texture texture = null;
        if (texturePath != null) {
            texture = textureMap.get(texturePath);
        }
        if (texture == null) {
            texture = textureMap.get(DEFAULT_TEXTURE);
        }
        return texture;
    }
}
