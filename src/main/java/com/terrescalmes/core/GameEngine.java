package com.terrescalmes.core;

import com.terrescalmes.MouseInput;
import com.terrescalmes.Window;
import com.terrescalmes.Window.WindowOptions;
import com.terrescalmes.core.graphics.Render;
import com.terrescalmes.core.graphics.Scene;
import com.terrescalmes.core.graphics.SkyBox;
import com.terrescalmes.core.TerrainGenerator;
import com.terrescalmes.core.graphics.GUI.IGuiInstance;
import com.terrescalmes.core.graphics.GUI.LightControls;
import com.terrescalmes.core.graphics.lights.AmbientLight;
import com.terrescalmes.core.graphics.lights.DirLight;
import com.terrescalmes.core.graphics.lights.SceneLights;
import com.terrescalmes.core.graphics.Camera;
import com.terrescalmes.core.graphics.Fog;
import com.terrescalmes.core.graphics.Model;
import com.terrescalmes.core.graphics.ModelLoader;
import com.terrescalmes.entities.Entity;
import com.terrescalmes.entities.Player;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class GameEngine implements IGuiInstance {
    // Game loop timing
    public static final double TARGET_UPS = 30.0;
    public static final double UPDATE_TIME = 1.0 / TARGET_UPS;
    private static final String WINDOW_TITLE = "Terres Calmes - Debug";
    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 720;
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.1f;

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
    private Vector4f displInc = new Vector4f();
    private float rotation;

    private boolean running;

    // Terrain debug
    private Entity singleTerrainEntity;
    private boolean showWireframe = false;
    private TerrainManager terrainManager;

    public GameEngine() {
        WindowOptions opts = new WindowOptions(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        window = new Window(WINDOW_TITLE, opts, () -> {
            resize();
            return null;
        });
        render = new Render(window);
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
        System.out.println("=== INITIALISATION DU TERRAIN ===");

        terrainManager = new TerrainManager(scene, scene.getTextureCache());

        // Configuration de l'éclairage simple
        SceneLights sceneLights = new SceneLights();
        AmbientLight ambientLight = sceneLights.getAmbientLight();
        ambientLight.setIntensity(0.8f); // Plus lumineux pour voir le terrain
        ambientLight.setColor(1.0f, 1.0f, 1.0f); // Blanc pur

        DirLight dirLight = sceneLights.getDirLight();
        dirLight.setPosition(0.0f, 1.0f, 0.0f); // Directement au-dessus
        dirLight.setIntensity(1.0f);
        dirLight.setColor(1.0f, 1.0f, 1.0f); // Blanc pur
        scene.setSceneLights(sceneLights);
        System.out.println("Éclairage configuré");

        // Skybox plus petite pour debug
        if (scene.getSkyBox() == null) {
            try {
                SkyBox skyBox = new SkyBox("resources/models/skybox/skybox.obj", scene.getTextureCache());
                skyBox.getSkyBoxEntity().setScale(500); // Plus petit pour debug
                scene.setSkyBox(skyBox);
                System.out.println("Skybox configurée");
            } catch (Exception e) {
                System.err.println("Erreur skybox: " + e.getMessage());
            }
        }

        // Fog désactivé pour debug
        scene.setFog(new Fog(false, new Vector3f(0.7f, 0.8f, 0.9f), 0.001f));

        // Position de caméra pour voir le terrain
        scene.getCamera().setPosition(5, 10, 10); // Plus loin et plus haut
        scene.getCamera().setRotation((float) Math.toRadians(90), 0); // Regarder vers le bas

        System.out.println("Position initiale de la caméra: " + scene.getCamera().getPosition());
        System.out.println("=== INIT TERMINÉ ===");
    }

    private void gameLoop() {
        double lastTime = glfwGetTime();
        double accumulator = 0.0;
        IGuiInstance iGuiInstance = this; // Utiliser this pour l'interface GUI

        while (running && !window.shouldClose()) {
            window.pollEvents();
            window.getMouseInput().input();
            boolean inputConsumed = iGuiInstance != null && iGuiInstance.handleGuiInput(scene, window);
            input(window, scene, (long) (glfwGetTime() * 1000 - lastTime * 1000), inputConsumed);

            double currentTime = glfwGetTime();
            double frameTime = currentTime - lastTime;

            frameTime = Math.min(frameTime, 0.25);
            accumulator += frameTime;

            while (accumulator >= UPDATE_TIME) {
                if (player != null)
                    player.saveState();
                update();
                upsCounter++;
                accumulator -= UPDATE_TIME;
            }

            double interpolationFactor = accumulator / UPDATE_TIME;
            render(interpolationFactor);

            fpsCounter++;
            fpsTimer += frameTime;
            if (fpsTimer >= 1.0) {
                fps = fpsCounter;
                ups = upsCounter;
                fpsCounter = 0;
                upsCounter = 0;
                fpsTimer = 0.0;

                Vector3f pos = scene.getCamera().getPosition();
                String title = String.format("Terres Calmes DEBUG | FPS: %d UPS: %d | Pos: %.1f, %.1f, %.1f",
                        fps, ups, pos.x, pos.y, pos.z);
                glfwSetWindowTitle(window.getWindowHandle(), title);
            }

            lastTime = currentTime;
        }
    }

    private void resize() {
        int width = window.getWidth();
        int height = window.getHeight();
        scene.resize(width, height);
        render.resize(width, height);
    }

    public void drawGui() {
        // ImGui.newFrame();
        // ImGui.setNextWindowPos(10, 10, ImGuiCond.Always);

        // if (ImGui.begin("Debug Terrain")) {
        // Vector3f pos = scene.getCamera().getPosition();
        // ImGui.text("Camera Position: %.2f, %.2f, %.2f", pos.x, pos.y, pos.z);
        // ImGui.text("FPS: %d | UPS: %d", fps, ups);

        // ImGui.separator();
        // ImGui.text("Contrôles:");
        // ImGui.text("WASD: Déplacement");
        // ImGui.text("Espace/Ctrl: Monter/Descendre");
        // ImGui.text("Shift: Vitesse x5");
        // ImGui.text("Clic droit: Regarder autour");

        // ImGui.separator();
        // if (ImGui.button("Reset Position")) {
        // scene.getCamera().setPosition(0, 50, 100);
        // scene.getCamera().setRotation((float) Math.toRadians(-25), 0);
        // }

        // if (ImGui.button("Position proche")) {
        // scene.getCamera().setPosition(0, 10, 20);
        // scene.getCamera().setRotation((float) Math.toRadians(-15), 0);
        // }

        // if (ImGui.button("Vue aérienne")) {
        // scene.getCamera().setPosition(0, 100, 0);
        // scene.getCamera().setRotation((float) Math.toRadians(-90), 0);
        // }

        // ImGui.separator();
        // ImGui.text("Terrain Debug:");
        // if (singleTerrainEntity != null) {
        // Vector3f terrainPos = singleTerrainEntity.getPosition();
        // ImGui.text("Terrain pos: %.2f, %.2f, %.2f", terrainPos.x, terrainPos.y,
        // terrainPos.z);
        // ImGui.text("Terrain scale: %.2f", singleTerrainEntity.getScale());
        // }

        // // Info sur la scène
        // ImGui.separator();
        // ImGui.text("Scene Info:");
        // ImGui.text("Models: %d", scene.getModelMap().size());
        // ImGui.text("Has SkyBox: %s", scene.getSkyBox() != null ? "Oui" : "Non");
        // ImGui.text("Fog active: %s", scene.getFog().isActive() ? "Oui" : "Non");
        // }
        // ImGui.end();

        // ImGui.endFrame();
        // ImGui.render();
    }

    public boolean handleGuiInput(Scene scene, Window window) {
        ImGuiIO imGuiIO = ImGui.getIO();
        MouseInput mouseInput = window.getMouseInput();
        Vector2f mousePos = mouseInput.getCurrentPos();
        imGuiIO.addMousePosEvent(mousePos.x, mousePos.y);
        imGuiIO.addMouseButtonEvent(0, mouseInput.isLeftButtonPressed());
        imGuiIO.addMouseButtonEvent(1, mouseInput.isRightButtonPressed());

        return imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard();
    }

    public void input(Window window, Scene scene, long diffTimeMillis, boolean inputConsumed) {
        if (inputConsumed) {
            return;
        }

        float move = diffTimeMillis * MOVEMENT_SPEED;
        Camera camera = scene.getCamera();

        // Mouvement plus rapide avec Shift
        if (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
            move *= 5.0f;
        }

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
        if (window.isKeyPressed(GLFW_KEY_SPACE)) {
            camera.moveUp(move);
        } else if (window.isKeyPressed(GLFW_KEY_LEFT_CONTROL)) {
            camera.moveDown(move);
        }

        // Reset rapide avec R
        if (window.isKeyPressed(GLFW_KEY_R)) {
            camera.setPosition(0, 10, 10);
            camera.setRotation((float) Math.toRadians(-90), 0);
        }

        MouseInput mouseInput = window.getMouseInput();
        if (mouseInput.isRightButtonPressed()) {
            Vector2f displVec = mouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(-displVec.x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(-displVec.y * MOUSE_SENSITIVITY));
        }
    }

    private void update() {
        // Update simple pour debug
        Vector3f cameraPos = scene.getCamera().getPosition();
        terrainManager.update(cameraPos);
    }

    private void render(double interpolationFactor) {
        render.render(window, scene);
        window.update();
    }

    private void cleanup() {
        if (terrainManager != null) {
            terrainManager.cleanup();
        }
        render.cleanup();
        scene.cleanup();
        window.cleanup();
    }
}
