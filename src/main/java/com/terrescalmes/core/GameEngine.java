package com.terrescalmes.core;

import com.terrescalmes.Window;
import com.terrescalmes.Window.WindowOptions;
import com.terrescalmes.core.graphics.Render;
import com.terrescalmes.core.graphics.Scene;
import com.terrescalmes.core.graphics.Mesh;
import com.terrescalmes.entities.Player;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class GameEngine {
    // Game loop timing
    public static final double TARGET_UPS = 30.0; // Updates per second (physique)
    public static final double UPDATE_TIME = 1.0 / TARGET_UPS;
    private static final String WINDOW_TITLE = "Terres Calmes";
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;

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

    private boolean running;

    public GameEngine() {
        WindowOptions opts = new WindowOptions(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        window = new Window(WINDOW_TITLE, opts, () -> {
            resize();
            return null;
        });
        render = new Render();
        scene = new Scene();
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
                0.0f, 0.5f, 0.0f,
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f
        };
        Mesh mesh = new Mesh(positions, 3);
        scene.addMesh("triangle", mesh);
    }

    private void gameLoop() {
        double lastTime = glfwGetTime();
        double accumulator = 0.0;

        while (running && !window.shouldClose()) {
            double currentTime = glfwGetTime();
            double frameTime = currentTime - lastTime;
            lastTime = currentTime;

            // Cap frame time to prevent spiral of death
            frameTime = Math.min(frameTime, 0.25);

            accumulator += frameTime;

            // Fixed timestep updates (physique à 30 UPS)
            while (accumulator >= UPDATE_TIME) {
                // Sauvegarder l'état précédent pour l'interpolation
                player.saveState();

                // Update game logic
                // update();
                upsCounter++;

                accumulator -= UPDATE_TIME;
            }

            double interpolationFactor = accumulator / UPDATE_TIME;
            // render(interpolationFactor);
            render.render(window, scene);

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

            glfwSwapBuffers(window.getWindowHandle());
            glfwPollEvents();
        }
    }

    private void resize() {
        // Nothing to be done yet
    }

    private void update() {
        // Input handling
        boolean left = glfwGetKey(window.getWindowHandle(), GLFW_KEY_A) == GLFW_PRESS ||
                glfwGetKey(window.getWindowHandle(), GLFW_KEY_LEFT) == GLFW_PRESS;
        boolean right = glfwGetKey(window.getWindowHandle(), GLFW_KEY_D) == GLFW_PRESS ||
                glfwGetKey(window.getWindowHandle(), GLFW_KEY_RIGHT) == GLFW_PRESS;
        boolean up = glfwGetKey(window.getWindowHandle(), GLFW_KEY_W) == GLFW_PRESS ||
                glfwGetKey(window.getWindowHandle(), GLFW_KEY_UP) == GLFW_PRESS;
        boolean down = glfwGetKey(window.getWindowHandle(), GLFW_KEY_S) == GLFW_PRESS ||
                glfwGetKey(window.getWindowHandle(), GLFW_KEY_DOWN) == GLFW_PRESS;

        // Player movement (physique à 30 UPS)
        float speed = 200.0f; // pixels par seconde
        float deltaSpeed = speed * (float) UPDATE_TIME;

        if (left && player.x > 0) {
            player.x -= deltaSpeed;
        }
        if (right && player.x < window.getWidth() - player.width) {
            player.x += deltaSpeed;
        }
        if (up && player.y > 0) {
            player.y -= deltaSpeed;
        }
        if (down && player.y < window.getHeight() - player.height) {
            player.y += deltaSpeed;
        }

        // Keep player in bounds
        player.x = Math.max(0, Math.min(window.getWidth() - player.width, player.x));
        player.y = Math.max(0, Math.min(window.getHeight() - player.height, player.y));
    }

    private void render(double interpolationFactor) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Interpoler la position du joueur pour un rendu fluide
        float renderX = (float) (player.prevX + (player.x - player.prevX) * interpolationFactor);
        float renderY = (float) (player.prevY + (player.y - player.prevY) * interpolationFactor);

        // Dessiner le joueur (carré rouge)
        glColor3f(1.0f, 0.3f, 0.3f);
        glBegin(GL_QUADS);
        glVertex2f(renderX, renderY);
        glVertex2f(renderX + player.width, renderY);
        glVertex2f(renderX + player.width, renderY + player.height);
        glVertex2f(renderX, renderY + player.height);
        glEnd();
    }

    private void cleanup() {
        render.cleanup();
        scene.cleanup();
        window.cleanup();
    }
}
