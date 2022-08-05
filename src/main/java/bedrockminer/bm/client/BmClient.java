package bedrockminer.bm.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class BmClient implements ClientModInitializer {

    public static Boolean mineBedrock;
    private Miner miner;

    @Override
    public void onInitializeClient() {
        mineBedrock = false;

        KeyBinding mine = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bedrockMiner.mine",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.bedrockMiner"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(MinecraftClient.getInstance().player!=null)
                miner = new Miner(client.player);
            if(mine.wasPressed() && miner != null){
                miner.mineBedrock();
            }
        });
        /*KeyBinding k = KeyBindingHelper.registerKeyBinding(new KeyBinding(
           "k",
           InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "k"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
        //    if(k.wasPressed()){
                //System.out.println("rotate player for a bit");
                //MinecraftClient.getInstance().player.refreshPositionAndAngles(
                //        MinecraftClient.getInstance().player.getBlockPos(),
                //        MinecraftClient.getInstance().player.getYaw()+90.0f,
                //        0.0f
                //        );
                //mineBedrock = true;
         //   }
        //});
        ClientCommandManager.DISPATCHER.register(
                ClientCommandManager.literal("mine")
                        .then(ClientCommandManager.argument("flag",BoolArgumentType.bool())
                                .executes(ctx -> {
                                    mineBedrock = BoolArgumentType.getBool(ctx, "flag");
                                    MinecraftClient.getInstance().player.sendSystemMessage(Text.of(mineBedrock?"start mining bedrock":"stop mining bedrock"), null);
                                    return 0;
                                }))
        );*/
    }
}