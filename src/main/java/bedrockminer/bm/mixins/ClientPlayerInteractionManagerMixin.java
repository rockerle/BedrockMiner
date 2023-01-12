package bedrockminer.bm.mixins;

import bedrockminer.bm.client.BmClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(at=@At("HEAD"), method="attackBlock", cancellable = true)
    public void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir){
        if(BmClient.clickMine){
            if(BmClient.miner.isRunning()){
                cir.cancel();
                return;
            }
            if(BmClient.mineableBlocks.contains(this.client.world.getBlockState(pos).getBlock())){
                BmClient.miner.start(pos,direction);
                cir.cancel();
            }
        }
    }
}