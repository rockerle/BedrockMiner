package bedrockminer.bm.mixins;

import bedrockminer.bm.client.Miner;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Shadow public abstract void sendChatMessage(String message);

    @Shadow public abstract void sendSystemMessage(Text message, UUID sender);

    private Miner miner;
    protected KeyBinding mineBedrock;

    @Inject(at=@At("TAIL"), method="init")
    public void onInit(CallbackInfo ci){
        miner = new Miner((ClientPlayerEntity) (Object) this);

        mineBedrock = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bedrockMiner.mine",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "cat.bedrock.singleMiner"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(mineBedrock.wasPressed() && miner != null){
                miner.mineBedrock();
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("mineBedrock")
                    .then(CommandManager.argument("<start/stop>",greedyString())).executes(ctx -> {
                        String state = getString(ctx,"<start/stop>");
                        if(state.equals("start"))
                            miner.startAutoMining();
                        else if(state.equals("stop"))
                            miner.stopAutoMining();
                        else
                            this.sendSystemMessage(new LiteralText("Argument for automining command must be either \"start\" or \"stop\""), null);
                return 1;
            }));

        });
    }
}