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
import com.terrescalmes.core.graphics.ModelLoader;
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
        Model cubeModel = ModelLoader.loadModel("cube-model", "resources/models/cube.obj",
                scene.getTextureCache());
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
            window.getMouseInput().input();
            input(window, scene, (long) (glfwGetTime() * 1000 - lastTime * 1000));

            double currentTime = glfwGetTime();
            double frameTime = currentTime - lastTime;

            // Cap frame time to prevent spiral of death
            frameTime = Math.min(frameTime, 0.25);

            accumulator += frameTime;

            // Fixed timestep updates (physique à 30 UPS)
            while (accumulator >= UPDATE_TIME) {
                // Sauvegarder l'état précédent pour l'interpolation
                player.saveState();

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
                String title = String.format("Terres Calmes | FPS: %d UPS: %d", fps, ups);
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
