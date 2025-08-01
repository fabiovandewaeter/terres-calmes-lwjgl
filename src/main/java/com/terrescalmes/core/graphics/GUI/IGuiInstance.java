package com.terrescalmes.core.graphics.GUI;

import com.terrescalmes.Window;
import com.terrescalmes.core.graphics.Scene;

public interface IGuiInstance {
    void drawGui();

    boolean handleGuiInput(Scene scene, Window window);
}
