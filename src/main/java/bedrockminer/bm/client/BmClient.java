package bedrockminer.bm.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class BmClient implements ClientModInitializer {

    private KeyBinding mineBedrock;
    public static boolean breaking;

    @Override
    public void onInitializeClient() {
        breaking = false;
        /*mineBedrock = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bedrock.stuff",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.bedrock.break"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(mineBedrock.wasPressed() && MinecraftClient.getInstance().world != null)
                breaking = !breaking;
        });*/
    }

    public static boolean breakBedrock(){
        return breaking;
    }
}