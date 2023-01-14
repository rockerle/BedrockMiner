package bedrockminer.bm.mixins;

import bedrockminer.bm.client.BmClient;
//import net.minecraft.block.Block;
//import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.text.Text;
//import net.minecraft.util.ActionResult;
//import net.minecraft.util.Hand;
//import net.minecraft.util.hit.BlockHitResult;
//import net.minecraft.util.hit.HitResult;
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
            if(BmClient.miner.getTargetBlocks().contains(client.player.world.getBlockState(pos).getBlock())){
                BmClient.miner.start(pos,direction);
                cir.cancel();
            }else{
//                System.out.println("selected block is not piston mineable");
            }
        }
    }

//    @Inject(at=@At("HEAD"), method="interactItem", cancellable = true)
//    public void onInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir){
//        if(BmClient.clickMine && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
//            Block toAdd = client.player.world.getBlockState(((BlockHitResult) client.crosshairTarget).getBlockPos()).getBlock();
//            if (BmClient.miner.getTargetBlocks().contains(toAdd)){
//                BmClient.miner.removeTargetBlock(toAdd);
//                player.sendMessage(Text.of("Removed "+toAdd.getName()+" from target block list"));
//            }else{
//                BmClient.miner.addTargetBlock(toAdd);
//                player.sendMessage(Text.of("Added "+toAdd.getName()+" to target block list"));
//            }
//        }
//    }
}