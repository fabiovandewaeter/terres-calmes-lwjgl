package com.terrescalmes.core;

import com.terrescalmes.MouseInput;
import com.terrescalmes.Window;
import com.terrescalmes.Window.WindowOptions;
import com.terrescalmes.core.graphics.Render;
import com.terrescalmes.core.graphics.Scene;
import com.terrescalmes.core.graphics.Texture;
import com.terrescalmes.core.graphics.Camera;
import com.terrescalmes.core.graphics.Material;
import com.terrescalmes.core.graphics.Mesh;
import com.terrescalmes.core.graphics.Model;
import com.terrescalmes.entities.Entity;
import com.terrescalmes.entities.Player;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class GameEngine {
    // Game loop timing
    public static final double TARGET_UPS = 30.0; // Updates per second (physique)
    public static final double UPDATE_TIME = 1.0 / TARGET_UPS;
    private static final String WINDOW_TITLE = "Terres Calmes";
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.005f;

    // private long window;
    private Window window;
    private Render render;
    private Scene scene;

    // Performance counters
    private int fps = 0;
    private int ups = 0;
    private double fpsTimer = 0.0;
    private int fpsCounter = 0;
    private int upsCounter = 0;

    // Player state
    private Player player;
    private Entity cubeEntity;
    private Vector4f displInc = new Vector4f();
    private float rotation;

    private boolean running;

    public GameEngine() {
        WindowOptions opts = new WindowOptions(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        window = new Window(WINDOW_TITLE, opts, () -> {
            resize();
            return null;
        });
        render = new Render();
        scene = new Scene(window.getWidth(), window.getHeight());
        init(window, scene, render);
        player = new Player(400, 300, 50, 50);
        running = true;
    }

    public void run() {
        init(window, scene, render);
        gameLoop();
        cleanup();
    }

    public void init(Window window, Scene scene, Render render) {
        float[] positions = new float[] {
                // V0
                -0.5f, 0.5f, 0.5f,
                // V1
                -0.5f, -0.5f, 0.5f,
                // V2
                0.5f, -0.5f, 0.5f,
                // V3
                0.5f, 0.5f, 0.5f,
                // V4
                -0.5f, 0.5f, -0.5f,
                // V5
                0.5f, 0.5f, -0.5f,
                // V6
                -0.5f, -0.5f, -0.5f,
                // V7
                0.5f, -0.5f, -0.5f,

                // For text coords in top face
                // V8: V4 repeated
                -0.5f, 0.5f, -0.5f,
                // V9: V5 repeated
                0.5f, 0.5f, -0.5f,
                // V10: V0 repeated
                -0.5f, 0.5f, 0.5f,
                // V11: V3 repeated
                0.5f, 0.5f, 0.5f,

                // For text coords in right face
                // V12: V3 repeated
                0.5f, 0.5f, 0.5f,
                // V13: V2 repeated
                0.5f, -0.5f, 0.5f,

                // For text coords in left face
                // V14: V0 repeated
                -0.5f, 0.5f, 0.5f,
                // V15: V1 repeated
                -0.5f, -0.5f, 0.5f,

                // For text coords in bottom face
                // V16: V6 repeated
                -0.5f, -0.5f, -0.5f,
                // V17: V7 repeated
                0.5f, -0.5f, -0.5f,
                // V18: V1 repeated
                -0.5f, -0.5f, 0.5f,
                // V19: V2 repeated
                0.5f, -0.5f, 0.5f,
        };
        float[] textCoords = new float[] {
                0.0f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.5f, 0.0f,

                0.0f, 0.0f,
                0.5f, 0.0f,
                0.0f, 0.5f,
                0.5f, 0.5f,

                // For text coords in top face
                0.0f, 0.5f,
                0.5f, 0.5f,
                0.0f, 1.0f,
                0.5f, 1.0f,

                // For text coords in right face
                0.0f, 0.0f,
                0.0f, 0.5f,

                // For text coords in left face
                0.5f, 0.0f,
                0.5f, 0.5f,

                // For text coords in bottom face
                0.5f, 0.0f,
                1.0f, 0.0f,
                0.5f, 0.5f,
                1.0f, 0.5f,
        };
        int[] indices = new int[] {
                // Front face
                0, 1, 3, 3, 1, 2,
                // Top Face
                8, 10, 11, 9, 8, 11,
                // Right face
                12, 13, 7, 5, 12, 7,
                // Left face
                14, 15, 6, 4, 14, 6,
                // Bottom face
                16, 18, 19, 17, 16, 19,
                // Back face
                4, 6, 7, 5, 4, 7, };
        Texture texture = scene.getTextureCache().createTexture("resources/models/cube.png");
        Material material = new Material();
        material.setTexturePath(texture.getTexturePath());
        List<Material> materialList = new ArrayList<>();
        materialList.add(material);

        Mesh mesh = new Mesh(positions, textCoords, indices);
        material.getMeshList().add(mesh);
        Model cubeModel = new Model("cube-model", materialList);
        scene.addModel(cubeModel);

        cubeEntity = new Entity("cube-entity", cubeModel.getId());
        cubeEntity.setPosition(0, 0, -2);
        scene.addEntity(cubeEntity);
    }

    private void gameLoop() {
        double lastTime = glfwGetTime();
        double accumulator = 0.0;

        while (running && !window.shouldClose()) {
            window.pollEvents();
            double currentTime = glfwGetTime();
            double frameTime = currentTime - lastTime;

            // Cap frame time to prevent spiral of death
            frameTime = Math.min(frameTime, 0.25);

            accumulator += frameTime;

            // Fixed timestep updates (physique à 30 UPS)
            while (accumulator >= UPDATE_TIME) {
                // Sauvegarder l'état précédent pour l'interpolation
                player.saveState();

                window.getMouseInput().input();
                input(window, scene, (long) (glfwGetTime() * 1000 - lastTime * 1000));

                // Update game logic
                update();
                upsCounter++;

                accumulator -= UPDATE_TIME;
            }

            double interpolationFactor = accumulator / UPDATE_TIME;
            render(interpolationFactor);

            // Update performance counters
            fpsCounter++;
            fpsTimer += frameTime;
            if (fpsTimer >= 1.0) {
                fps = fpsCounter;
                ups = upsCounter;
                fpsCounter = 0;
                upsCounter = 0;
                fpsTimer = 0.0;

                // Mettre à jour le titre de la fenêtre avec les FPS/UPS
                String title = String.format("LWJGL Framerate Demo | FPS: %d UPS: %d", fps, ups);
                glfwSetWindowTitle(window.getWindowHandle(), title);
            }

            lastTime = currentTime;
        }
    }

    private void resize() {
        scene.resize(window.getWidth(), window.getHeight());
    }

    public void input(Window window, Scene scene, long diffTimeMillis) {
        float move = diffTimeMillis * MOVEMENT_SPEED;
        Camera camera = scene.getCamera();
        if (window.isKeyPressed(GLFW_KEY_W)) {
            camera.moveForward(move);
        } else if (window.isKeyPressed(GLFW_KEY_S)) {
            camera.moveBackwards(move);
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            camera.moveLeft(move);
        } else if (window.isKeyPressed(GLFW_KEY_D)) {
            camera.moveRight(move);
        }
        if (window.isKeyPressed(GLFW_KEY_UP)) {
            camera.moveUp(move);
        } else if (window.isKeyPressed(GLFW_KEY_DOWN)) {
            camera.moveDown(move);
        }

        MouseInput mouseInput = window.getMouseInput();
        if (mouseInput.isRightButtonPressed()) {
            Vector2f displVec = mouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(-displVec.x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(-displVec.y * MOUSE_SENSITIVITY));
        }
    }

    private void update() {
        // // Input handling
        // boolean left = glfwGetKey(window.getWindowHandle(), GLFW_KEY_A) == GLFW_PRESS
        // ||
        // glfwGetKey(window.getWindowHandle(), GLFW_KEY_LEFT) == GLFW_PRESS;
        // boolean right = glfwGetKey(window.getWindowHandle(), GLFW_KEY_D) ==
        // GLFW_PRESS ||
        // glfwGetKey(window.getWindowHandle(), GLFW_KEY_RIGHT) == GLFW_PRESS;
        // boolean up = glfwGetKey(window.getWindowHandle(), GLFW_KEY_W) == GLFW_PRESS
        // ||
        // glfwGetKey(window.getWindowHandle(), GLFW_KEY_UP) == GLFW_PRESS;
        // boolean down = glfwGetKey(window.getWindowHandle(), GLFW_KEY_S) == GLFW_PRESS
        // ||
        // glfwGetKey(window.getWindowHandle(), GLFW_KEY_DOWN) == GLFW_PRESS;

        // // Player movement (physique à 30 UPS)
        // float speed = 200.0f; // pixels par seconde
        // float deltaSpeed = speed * (float) UPDATE_TIME;

        // if (left && player.x > 0) {
        // player.x -= deltaSpeed;
        // }
        // if (right && player.x < window.getWidth() - player.width) {
        // player.x += deltaSpeed;
        // }
        // if (up && player.y > 0) {
        // player.y -= deltaSpeed;
        // }
        // if (down && player.y < window.getHeight() - player.height) {
        // player.y += deltaSpeed;
        // }

        // // Keep player in bounds
        // player.x = Math.max(0, Math.min(window.getWidth() - player.width, player.x));
        // player.y = Math.max(0, Math.min(window.getHeight() - player.height,
        // player.y));
        rotation += 1.5;
        if (rotation > 360) {
            rotation = 0;
        }
        cubeEntity.setRotation(1, 1, 1, (float) Math.toRadians(rotation));
        cubeEntity.updateModelMatrix();
    }

    private void render(double interpolationFactor) {
        // glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // // Interpoler la position du joueur pour un rendu fluide
        // float renderX = (float) (player.prevX + (player.x - player.prevX) *
        // interpolationFactor);
        // float renderY = (float) (player.prevY + (player.y - player.prevY) *
        // interpolationFactor);

        // // Dessiner le joueur (carré rouge)
        // glColor3f(1.0f, 0.3f, 0.3f);
        // glBegin(GL_QUADS);
        // glVertex2f(renderX, renderY);
        // glVertex2f(renderX + player.width, renderY);
        // glVertex2f(renderX + player.width, renderY + player.height);
        // glVertex2f(renderX, renderY + player.height);
        // glEnd();
        render.render(window, scene);
        window.update();
    }

    private void cleanup() {
        render.cleanup();
        scene.cleanup();
        window.cleanup();
    }
}
