package bedrockminer.bm.mixins;

import bedrockminer.bm.client.BmClient;
//import net.minecraft.block.Block;
//import net.minecraft.block.Blocks;
import bedrockminer.bm.client.Miner;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.text.Text;
//import net.minecraft.util.ActionResult;
//import net.minecraft.util.Hand;
//import net.minecraft.util.hit.BlockHitResult;
//import net.minecraft.util.hit.HitResult;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    private Miner m;
    private boolean clickMine, interacting = false;
    @Shadow @Final private MinecraftClient client;

    @Inject(at=@At("HEAD"), method="attackBlock", cancellable = true)
    public void bedrockminer$onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir){
        if(clickMine){
            if(m.isRunning()){
                cir.cancel();
                return;
            }
            if(m.getTargetBlocks().contains(client.player.getWorld().getBlockState(pos).getBlock())){
                m.start(pos,direction);
                cir.cancel();
            }else{
                System.out.println("selected block is not piston mineable");
            }
        }
    }

    @Inject(at=@At("TAIL"), method="tick")
    public void bedrockminer$tick(CallbackInfo ci){
        if(m==null && client.player!=null)
            m = new Miner(client.player);
        if(m.isRunning()) {
            try {
                m.tick();
            }catch(Exception e){
                m.reset();
            }
        }
        if(BmClient.toggleMiner.wasPressed()) {
            if(clickMine) {
                clickMine = false;
                m=null;
            }
            else{
                clickMine = true;
            }
            client.player.sendMessage(Text.of(String.format("Toggle bedrock miner %s",clickMine?"§2on":"§4off")),true);
        }

        if(interacting)
            interacting = false;
    }
    @Inject(at=@At("HEAD"), method="interactItem", cancellable = true)
    public void bedrockminer$InteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir){
        if(!interacting && clickMine && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            interacting = !interacting;
            Block toAdd = player.getWorld().getBlockState(((BlockHitResult) client.crosshairTarget).getBlockPos()).getBlock();
            if (m.getTargetBlocks().contains(toAdd)){
                m.removeTargetBlock(toAdd);
                player.sendMessage(Text.of("Removed "+ I18n.translate(toAdd.getTranslationKey())+" from target block list"));
            }else{
                m.addTargetBlock(toAdd);
                player.sendMessage(Text.of("Added "+I18n.translate(toAdd.getTranslationKey())+" to target block list"));
            }
            cir.setReturnValue(ActionResult.FAIL);
            cir.cancel();
        }
    }
}