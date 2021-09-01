package bedrockminer.bm.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class BlockInteractions {

    static ClientPlayerEntity player = MinecraftClient.getInstance().player;

    public static void breakBlock(BlockPos blockPos){
        if(blockPos==null)
            return;
        //ClientPlayerEntity player = MinecraftClient.getInstance().player;
        selectPickaxe();
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,blockPos,player.getHorizontalFacing()));
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,blockPos,player.getHorizontalFacing()));
    }

    public static void replacePiston(BlockPos pistonPos,BlockPos bedrockPos){
        if(pistonPos==null || bedrockPos==null)
            return;
        selectPiston();
        MinecraftClient.getInstance().interactionManager.interactBlock(
                MinecraftClient.getInstance().player,
                MinecraftClient.getInstance().world,
                Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(bedrockPos),
                        getView(pistonPos,bedrockPos),
                        pistonPos,
                        true));
    }

    private static Direction getView(BlockPos b1, BlockPos b2){
        if(b1.getY()-b2.getY()<0)
            return Direction.DOWN;
        else if(b1.getY()-b2.getY()>0)
            return Direction.UP;
        else if(b1.getX()-b2.getX()<0)
            return Direction.WEST;
        else if(b1.getX()-b2.getX()>0)
            return Direction.EAST;
        else if(b1.getZ()-b2.getZ()<0)
            return Direction.NORTH;
        else if(b1.getZ()-b2.getZ()>0)
            return Direction.SOUTH;
        else
            return null;
    }

    private static void selectPickaxe(){
        //ClientPlayerEntity player = MinecraftClient.getInstance().player;
        PlayerInventory inv = player.getInventory();
        int pickSlot = -1;
        ItemStack tempStack;
        for(int i=0;i<36;i++){
            tempStack = inv.getStack(i);
            if(tempStack.isOf(Items.DIAMOND_PICKAXE) || tempStack.isOf(Items.NETHERITE_PICKAXE) || tempStack.isOf(Items.STONE_PICKAXE) || tempStack.isOf(Items.WOODEN_PICKAXE) || tempStack.isOf(Items.IRON_PICKAXE)) {
                pickSlot = i;
                break;
            }
        }
        if(pickSlot==-1)
            return;

        if(pickSlot<9)
            inv.selectedSlot = pickSlot;
        else
            MinecraftClient.getInstance().interactionManager.pickFromInventory(pickSlot);
    }

    private static void selectPiston(){
        //ClientPlayerEntity player = MinecraftClient.getInstance().player;
        PlayerInventory inv = player.getInventory();
        int pistonSlot = inv.getSlotWithStack(new ItemStack(Items.PISTON));
        if(pistonSlot==-1)
            return;

        if(pistonSlot<9)
            inv.selectedSlot = pistonSlot;
        else
            MinecraftClient.getInstance().interactionManager.pickFromInventory(pistonSlot);
    }

    public static BlockPos findBedrock(BlockPos piston){
        BlockPos result = null;
        if(piston == null)
            return null;

        for(int x=-1;x<2;x++){
            for(int y=-1;y<2;y++){
                for(int z=-1;z<2;z++){
                    if(MinecraftClient.getInstance().world.getBlockState(piston.add(x,y,z)).isOf(Blocks.BEDROCK)){
                        result = new BlockPos(piston.add(x,y,z));
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static BlockPos findPistonBody(BlockPos redstoneTorch, ClientWorld world){
        BlockPos result = null;
        if(redstoneTorch == null)
            return null;

        for(int x=-1;x<2;x++){
            for(int z=-1;z<2;z++){
                if(world.getBlockState(redstoneTorch.add(x,0,z)).isOf(Blocks.PISTON)){
                    result = redstoneTorch.add(x,0,z);
                    break;
                }
            }
        }
        return result;
    }

    public static BlockPos findRedstoneTorch(BlockPos playerPos, ClientWorld world){
        BlockPos result = null;
        if(playerPos == null)
            return null;

        for(int x=-3;x<3;x++){
            for(int z=-3;z<3;z++){
                for(int y=0;y<3;y++) {
                    if (world.getBlockState(playerPos.add(x,y,z)).getBlock() == Blocks.REDSTONE_TORCH) {
                        result = new BlockPos(playerPos.add(x,y,z));
                        break;
                    }
                }
            }
        }
        if(result == null)
            MinecraftClient.getInstance().player.sendSystemMessage(new LiteralText("No Redstone Torch Found"),null);
        else
            MinecraftClient.getInstance().player.sendSystemMessage(new LiteralText("Redstone torch found at: " + result.getX()+"/"+result.getY()+"/"+result.getZ()),null);

        return result;
    }
}