package bedrockminer.bm.mixins;

import bedrockminer.bm.client.BlockInteractions;
import bedrockminer.bm.client.BmClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class PlayerMiningMixin {

    @Shadow @Final protected MinecraftClient client;

    @Inject(at = @At("TAIL"), method = "tick")
    public void onTick(CallbackInfo ci){
        if(this.client.world!=null && BmClient.breakBedrock()){
            BlockPos redstoneTorch = BlockInteractions.findRedstoneTorch(this.client.player.getBlockPos(),this.client.world);
            BlockPos pistonPos = BlockInteractions.findPistonBody(redstoneTorch,this.client.world);
            if(redstoneTorch!=null) {
                BlockInteractions.breakBlock(redstoneTorch);
                BlockInteractions.breakBlock(pistonPos);
                BlockInteractions.replacePiston(pistonPos,BlockInteractions.findBedrock(pistonPos));
                BmClient.breaking = false;
            }
        }
    }
}