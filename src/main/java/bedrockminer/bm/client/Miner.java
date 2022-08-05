package bedrockminer.bm.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;


public class Miner {

    private final ClientPlayerEntity player;
    private final ClientPlayNetworkHandler netHandler;
    private final ClientPlayerInteractionManager interactionManager;

    private boolean alreadyRunning;

    public Miner(ClientPlayerEntity player){
        this.player = player;
        this.netHandler = player.networkHandler;
        this.interactionManager = MinecraftClient.getInstance().interactionManager;
        this.alreadyRunning = false;
    }

    public void oneClickMining(BlockPos bedrockPos){

        if(!alreadyRunning) {
            alreadyRunning = true;
            PistonPlacement pp = findPlaceForPiston(bedrockPos);
            placePiston(pp.pos, pp.dir.getOpposite());
            BlockPos torch = findRedstoneTorchPlace(pp.pos, pp.dir);
            if(torch==null) {
                MinecraftClient.getInstance().player.sendMessage(Text.of("Couldn't find valid placement for redstone torch!"), true);
                alreadyRunning = false;
                return;
            }
            placeTorch(torch);
            mineBedrock();
            alreadyRunning = false;
        }
    }

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
        int oldSlot = player.getInventory().selectedSlot;
        selectPickaxe();
        netHandler.sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,blockPos,player.getHorizontalFacing()));
        netHandler.sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,blockPos,player.getHorizontalFacing()));
        player.getInventory().selectedSlot = oldSlot;
    }

    public void replacePiston(BlockPos pistonPos){
        if(pistonPos==null)
            return;
        int oldSlot = player.getInventory().selectedSlot;
        selectPiston();
        interactionManager.interactBlock(
                player,
                //(ClientWorld) player.getWorld(),
                Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pistonPos),
                        player.getWorld().getBlockState(pistonPos).get(Properties.FACING).getOpposite(),
                        pistonPos,
                        true));
        player.getInventory().selectedSlot = oldSlot;
    }

    public boolean isRunning(){return this.alreadyRunning;}

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
        if(inv.selectedSlot==pickSlot) {
            player.sendMessage(Text.of("already holding a pick"), true);
        }
        if(pickSlot==-1) {
            player.sendMessage(Text.of("no pick in inventory found"), true);

        }
        if(pickSlot<9) {
            player.sendMessage(Text.of("pick in hotbar on slot "+pickSlot+" found"), true);
            inv.selectedSlot = pickSlot;
        }
        else {
            player.sendMessage(Text.of("pick in inventory found on slot "+pickSlot), true);
            interactionManager.pickFromInventory(pickSlot);
        }
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

    private boolean selectItem(Item item){
        PlayerInventory inv = player.getInventory();
        int slot = inv.getSlotWithStack(new ItemStack(item));
        if(slot == -1)
            return false;
        else if(slot < 9)
            inv.selectedSlot = slot;
        else interactionManager.pickFromInventory(slot);

        inv.updateItems();
        return true;
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
            player.sendMessage(Text.of("No Redstone Torch Found"),true);
        return result;
    }

    private PistonPlacement findPlaceForPiston(BlockPos bedrockPos){
        for(Direction d:Direction.values()){
            BlockPos probe = bedrockPos.offset(d);
            if(player.world.getBlockState(probe).isAir()) {
                Direction ext = canPistonExtend(probe);
                if(ext != null)
                    return new PistonPlacement(probe,d);
            }
        }
        return null;
    }

    private Direction canPistonExtend(BlockPos pistonBody){
        for(Direction d: Direction.values()){
            if(player.world.getBlockState(pistonBody.offset(d)).getMaterial().isReplaceable()){
                return d;
            }
        }
        return null;
    }

    private BlockPos findRedstoneTorchPlace(BlockPos pistonBody, Direction faceing){
        for(Direction d:Direction.values()){
            if(d.equals(faceing)) {
                System.out.println("Skipping Direction: "+d);
                continue;
            }
            BlockPos probePos = pistonBody.offset(d);
            BlockState probeState = player.world.getBlockState(probePos);
            if(probeState.getMaterial().isReplaceable() && player.world.getBlockState(probePos.down()).hasSolidTopSurface(player.world,probePos.down(),player)){
                player.sendMessage(Text.of("That's a solid top surface"), true);
                return probePos;
            }
        }

        /*for(int yValue = pistonBody.getY()-1; yValue<pistonBody.getY()+1; yValue++){
            BlockPos probe = new BlockPos(pistonBody.getX(),yValue,pistonBody.getZ());
            for(Direction d: Direction.values()){
                if(d.equals(Direction.UP) || d.equals(Direction.DOWN))
                    continue;
                else if(player.world.getBlockState(probe.offset(d)).isAir()){

                }

            }
        }*/

        return null;
    }

    private void placePiston(BlockPos pistonPos, Direction dir){
        if(pistonPos==null || dir == null)
            return;
        player.sendMessage(Text.of("placing piston @ "+pistonPos.toString()), true);
        //System.out.println("Sending rotation packet with direction: " + dir.toString());
        netHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(dirToYaw(dir),dirToPitch(dir),player.isOnGround()));
        //player.refreshPositionAndAngles(player.getBlockPos(), dirToYaw(dir),dirToPitch(dir));

        //System.out.println("select piston to main hand");
        selectPiston();

        //System.out.println("placing piston in world");
        interactionManager.interactBlock(
                player,
                //(ClientWorld) player.world,
                player.getActiveHand(),
               new BlockHitResult(Vec3d.ofCenter(pistonPos),dir,pistonPos,true));
    }

    private void placeTorch(BlockPos tp){
        player.sendMessage(Text.of("placing torch @ " + tp.toString()), true);
        selectItem(Items.REDSTONE_TORCH);
        //player.refreshPositionAndAngles(player.getBlockPos(),dirToYaw(Direction.DOWN), dirToPitch(Direction.DOWN));
        netHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(dirToYaw(Direction.DOWN), player.getPitch(), player.isOnGround()));
        interactionManager.interactBlock(
                player,
                //(ClientWorld) player.world,
                player.getActiveHand(),
                new BlockHitResult(
                        Vec3d.ofCenter(tp),
                        Direction.DOWN,
                        tp,
                        true
                )
        );
    }

    private float dirToYaw(Direction d){
        switch(d) {
            case NORTH: return 180.0f;
            case EAST: return 270.0f;
            case SOUTH: return 0.0f;
            case WEST: return 90.0f;
            default: return player.getYaw();
        }
    }

    private float dirToPitch(Direction d){
        switch(d){
            case UP: return -90.0f;
            case DOWN: return 90.0f;
            default: return player.getPitch();
        }
    }

    private record PistonPlacement(BlockPos pos, Direction dir){}
}