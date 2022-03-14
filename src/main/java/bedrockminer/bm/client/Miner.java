package bedrockminer.bm.client;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


public class Miner {

    private final ClientPlayerEntity player;
    private final ClientPlayNetworkHandler netHandler;
    private final ClientPlayerInteractionManager interactionManager;

    boolean automining;

    public Miner(ClientPlayerEntity player){
        this.player = player;
        this.netHandler = player.networkHandler;
        this.interactionManager = MinecraftClient.getInstance().interactionManager;

        automining = false;
    }

    public void startAutoMining(){automining = true;}
    public void stopAutoMining(){automining = false;}

    public void mineBedrock(){
        BlockPos redstoneTorch = findRedstoneTorch();
        BlockPos pistonPos = findPistonBody(redstoneTorch);
        if(redstoneTorch!=null) {
            int currentSlot = player.getInventory().selectedSlot;
            breakBlock(redstoneTorch);
            breakBlock(pistonPos);
            replacePiston(pistonPos);
            player.getInventory().selectedSlot = currentSlot;
        }
    }

    public void breakBlock(BlockPos blockPos){
        if(blockPos==null)
            return;
        selectPickaxe();
        netHandler.sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,blockPos,player.getHorizontalFacing()));
        netHandler.sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,blockPos,player.getHorizontalFacing()));
    }

    private void selectPickaxe(){
        PlayerInventory inv = player.getInventory();
        int pickSlot = -1;
        ItemStack tempStack;
        for(int i=0;i<36;i++){
            tempStack = inv.getStack(i);
            if(EnchantmentHelper.getLevel(Enchantments.EFFICIENCY,tempStack)==5) {
                pickSlot = i;
                break;
            }
        }
        if(inv.selectedSlot==pickSlot)
            return;
        if(pickSlot==-1)
            return;
        if(pickSlot<9)
            inv.selectedSlot = pickSlot;
        else
            interactionManager.pickFromInventory(pickSlot);
    }

    private void selectPiston(){
        PlayerInventory inv = player.getInventory();
        int pistonSlot = inv.getSlotWithStack(new ItemStack(Items.PISTON));
        if(pistonSlot==-1)
            return;
        if(pistonSlot<9)
            inv.selectedSlot = pistonSlot;
        else
            interactionManager.pickFromInventory(pistonSlot);
    }

    private BlockPos findPistonBody(BlockPos redstoneTorch){
        BlockPos result = null;
        ClientWorld world = (ClientWorld) player.getWorld();
        if(redstoneTorch == null)
            return null;

        for(int x=-1;x<2;x++){
            for(int z=-1;z<2;z++) {
                for(int y=-1;y<2;y++){
                    if (world.getBlockState(redstoneTorch.add(x, y, z)).isOf(Blocks.PISTON) && world.getBlockState(redstoneTorch.add(x, y, z)).get(Properties.EXTENDED)) {
                        result = redstoneTorch.add(x, y, z);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private BlockPos findRedstoneTorch(){
        BlockPos result = null;
        BlockPos playerPos = player.getBlockPos();
        ClientWorld world = (ClientWorld) player.getWorld();
        Block boi;

        for(int x=-3;x<3;x++){
            for(int z=-3;z<3;z++){
                for(int y=-1;y<3;y++) {
                    boi = world.getBlockState(playerPos.add(x,y,z)).getBlock();
                    if (boi == Blocks.REDSTONE_TORCH || boi == Blocks.REDSTONE_WALL_TORCH) {
                        result = new BlockPos(playerPos.add(x,y,z));
                        break;
                    }
                }
            }
        }
        if(result == null)
            player.sendSystemMessage(new LiteralText("No Redstone Torch Found"),null);
        return result;
    }

    public void replacePiston(BlockPos pistonPos){
        if(pistonPos==null)
            return;
        selectPiston();
        interactionManager.interactBlock(
                player,
                (ClientWorld) player.getWorld(),
                Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pistonPos),
                        player.getWorld().getBlockState(pistonPos).get(Properties.FACING).getOpposite(),
                        pistonPos,
                        true));
    }
}