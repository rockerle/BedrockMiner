package bedrockminer.bm.mixins;

import bedrockminer.bm.client.Miner;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class PlayerMiningMixin {

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
    }
}