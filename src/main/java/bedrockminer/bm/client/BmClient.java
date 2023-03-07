package bedrockminer.bm.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class BmClient implements ClientModInitializer {

    public static KeyBinding toggleMiner;

    @Override
    public void onInitializeClient() {
        toggleMiner = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "toggle miner",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "Bedrockminer"));
    }
}