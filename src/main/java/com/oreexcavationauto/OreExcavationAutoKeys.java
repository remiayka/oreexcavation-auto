package com.oreexcavationauto;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class OreExcavationAutoKeys {
    public static final String CATEGORY = "key.category.oreexcavationauto";

    // Toggle auto-excavation on/off - default G
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
        "key.oreexcavationauto.toggle",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        CATEGORY
    );

    // Open configuration GUI - default H
    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
        "key.oreexcavationauto.open_gui",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        CATEGORY
    );
}
