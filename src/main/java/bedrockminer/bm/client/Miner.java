package bedrockminer.bm.client;

import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.enchantment.Enchantment;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;


public class Miner{

    private Task currentTask;
    private final ClientPlayerEntity player;
    private final ClientPlayNetworkHandler netHandler;
    private final ClientPlayerInteractionManager interactionManager;
    private Direction toFace;
    private boolean alreadyRunning;
    private BlockPos bedrockBlock;
    private BlockPos supportBlock;
    private BlockPos torchPos;
    private PistonPlacement pistonPlacement;
    private Item pistonType, pickaxeType;
    private boolean[] checks = {false,false}; //checklist for pistonplacement,torchplacement,
    private int failedCounter = -1;

    private final List<Item> allowedTools = List.of(Items.NETHERITE_PICKAXE,Items.DIAMOND_PICKAXE);
    private List<Item> allowedSupportBlocks = List.of(Items.SLIME_BLOCK, Items.NETHERRACK);
    private List<Block> allowedBlocksToMine = new ArrayList<>(List.of(Blocks.BEDROCK));

    public Miner(ClientPlayerEntity player){
        this.player = player;
        this.netHandler = player.networkHandler;
        this.interactionManager = MinecraftClient.getInstance().interactionManager;
        this.alreadyRunning = false;
        this.currentTask = Task.NOTHING;
    }
    //-------------------- Miner ---------------------------------------
    public void tick(){
        switch(currentTask){
            case INIT -> {
                if(failedCounter>0){
                    player.sendMessage(Text.of("Couldn't find valid piston location"),true);
                    this.reset();
                    break;
                }
                if(pistonPlacement!=null)
                    torchPos = findRedstoneTorchPlace(pistonPlacement.pos(),pistonPlacement.dir());
                if(pistonPlacement!=null && torchPos!=null) {
                    this.toFace = pistonPlacement.dir().getOpposite();
                    this.currentTask = Task.ROTATEPLAYER;
                    break;
                }
                if(pistonPlacement ==null || torchPos ==null){
                    failedCounter++;
                }
            }
            case PLACEPISTON -> {
                placePiston(pistonPlacement.pos,pistonPlacement.dir().getOpposite());
                if(torchPos!=null && pistonPlacement!=null) {
                    this.currentTask = Task.ROTATEPLAYER;
                    toFace = Direction.DOWN;
                    checks[0]=true;
                }
            }
            case REDSTONETORCH -> {
                if(supportBlock!=null){
                    selectItem(null,allowedSupportBlocks);
                    placeBlock(supportBlock);
                }
                if(torchPos!=null && checks[0] && !checks[1]) {
                    selectItem(null, Items.REDSTONE_TORCH);
                    placeBlock(torchPos);
                    checks[1] = true;
                    BlockPos dirPos = pistonPlacement.pos.subtract(new Vec3i(bedrockBlock.getX(), bedrockBlock.getY(), bedrockBlock.getZ()));
                    toFace = Direction.fromVector(dirPos.getX(),dirPos.getY(),dirPos.getZ());
                    this.currentTask=Task.ROTATEPLAYER;
                }
            }
            case ROTATEPLAYER -> {
                if(toFace!=null) {
                    netHandler.sendPacket(
                            new PlayerMoveC2SPacket.LookAndOnGround(
                                    dirToYaw(toFace),
                                    dirToPitch(toFace),
                                    player.isOnGround()));
                    if(!checks[0] && !checks[1])
                        this.currentTask = Task.PLACEPISTON;
                    if(checks[0] && !checks[1]) {
                        this.currentTask = Task.REDSTONETORCH;
                        this.toFace = Direction.DOWN;
                    }
                    if(checks[0]&&checks[1])
                        this.currentTask = Task.SWITCHTOPICK;
                } else{
                    player.sendMessage(Text.of("Direction to rotate was null while checks[] is: "+checks.toString()));
                    this.reset();
                }
            }
            case SWITCHTOPICK ->{
                toFace=pistonPlacement.dir().getOpposite();
                if(selectItem(Enchantments.EFFICIENCY,pickaxeType)){
                    this.currentTask = Task.MINEPISTON;
                }else{
                    player.sendMessage(Text.of("§5Pickaxe has not been found!"));
                    this.reset();
                }
            }
            case MINEPISTON -> {
                if(failedCounter>0 || this.pistonPlacement==null)
                    this.reset();
                if(player.getWorld().getBlockState(this.pistonPlacement.pos()).isOf(Blocks.MOVING_PISTON)){
                    player.sendMessage(Text.of("probably too late to mine the piston in time. Try again another time"));
                    failedCounter++;
                    break;
                }

                if(player.getWorld().getBlockState(this.pistonPlacement.pos()).get(Properties.EXTENDED)) {
                    mineBedrock();
                }else{
                    failedCounter++;
                    break;
                }
                if(this.supportBlock==null)
                    this.reset();
                else
                    this.currentTask = Task.MINESUPPORT;
                checks[0]=checks[1]=false;
            }
            case MINESUPPORT ->{
                breakBlock(supportBlock);
                this.reset();
            }
            case NOTHING -> {
                checks[0]=checks[1]=false;
                this.alreadyRunning=false;
                this.failedCounter = 0;
                this.bedrockBlock = null;
                this.supportBlock = null;
            }
        }
    }
    public void start(BlockPos bp, Direction offsetDir){
        this.currentTask = Task.INIT;
        this.bedrockBlock = bp;
        this.alreadyRunning=true;
        this.pistonPlacement = new PistonPlacement(
                bp.offset(offsetDir),
                player.getWorld().getBlockState(bp.offset(offsetDir).offset(Direction.UP)).isAir()?Direction.UP:canPistonExtend(bp.offset(offsetDir)));
        if(this.pistonPlacement.pos().equals(player.getBlockPos()) || player.getWorld().isOutOfHeightLimit(this.pistonPlacement.pos()))
            this.pistonPlacement=null;
        if(!checkPickaxe()){
            player.sendMessage(Text.of("$5No Pickaxe with Efficiency V in inventory found!"));
            this.reset();
        }
        if(player.getInventory().contains(Items.PISTON.getDefaultStack())){
            int s = player.getInventory().getSlotWithStack(Items.PISTON.getDefaultStack());
            if(s>-1){
                if(player.getInventory().getStack(s).getCount()>=2){
                    this.pistonType = Items.PISTON;
                    this.currentTask = Task.INIT;
                    return;
                }
            }
        }
        if(player.getInventory().contains(Items.STICKY_PISTON.getDefaultStack())){
            int s = player.getInventory().getSlotWithStack(Items.STICKY_PISTON.getDefaultStack());
            if(s>-1){
                if(player.getInventory().getStack(s).getCount()>=2){
                    this.pistonType = Items.STICKY_PISTON;
                    this.currentTask = Task.INIT;
                }
            }
        }else{
            player.sendMessage(Text.of("§4No sticky pistons found in inventory"));
            this.reset();
        }
    }
    public boolean isRunning(){return this.alreadyRunning;}
    public void reset(){
        this.alreadyRunning=false;
        this.bedrockBlock = null;
        this.pistonPlacement = null;
        this.pistonType = null;
        this.failedCounter = -1;
        this.currentTask = Task.NOTHING;
    }

    //-------------------- Inventory Actions -----------------------------------
    private boolean checkPickaxe(){
        PlayerInventory inv = player.getInventory();
        int pickSlot = -1;
        ItemStack tempStack;
        for(int i=0;i<36;i++){
            tempStack = inv.getStack(i);
            if(EnchantmentHelper.getLevel(Enchantments.EFFICIENCY,tempStack)==5 && allowedTools.contains(tempStack.getItem())) {
                pickSlot = i;
                break;
            }
        }
        if(pickSlot==-1) {
            player.sendMessage(Text.of("§5no pickaxe in inventory found"), true);
            return false;
        }
        pickaxeType = inv.getStack(pickSlot).getItem();
        return true;
    }
    private boolean selectItem(Enchantment e, List<Item> items){
        for (Item i : items) {
            if(selectItem(e,i))
                return true;
        }
        return false;
    }
    private boolean selectItem(Enchantment e, Item item){
        PlayerInventory inv = player.getInventory();
        ItemStack iS;
        int slot = -1;
        for(int i=0;i<36;i++){
            iS = inv.getStack(i);
            if(iS.isOf(item)) {
                if(e==null) {
                    slot = i;
                    break;
                } else {
                    if(EnchantmentHelper.getLevel(e,iS)==5){
                        slot = i;
                        break;
                    }
                }
            }
        }
        if(slot < 0)
            return false;
        else if(slot < 9)
            inv.selectedSlot = slot;
        else interactionManager.pickFromInventory(slot);

        inv.updateItems();
        return true;
    }

    //-------------------- Placement Checks ------------------------------------
    private Direction canPistonExtend(BlockPos pistonBody){
        for(Direction d: Direction.values()){
            if(d.equals(Direction.UP))
                continue;
            //old condition from pre 1.20: player.getWorld().getBlockState(pistonBody.offset(d)).getMaterial().isReplaceable()
            if(player.getWorld().getBlockState(pistonBody.offset(d)).getPistonBehavior().equals(PistonBehavior.DESTROY)){
                return d;
            }
        }
        return null;
    }
    private BlockPos findRedstoneTorchPlace(BlockPos pistonBody, Direction facing){
        for(Direction d:Direction.values()){
            if(d.equals(facing) || d.equals(Direction.UP) || pistonBody.offset(d).equals(bedrockBlock)) {
                continue;
            }
            BlockPos probePos = pistonBody.offset(d);
            BlockState probeState = player.getWorld().getBlockState(probePos);
            if(probeState.isAir()) {
                if (player.getWorld().getBlockState(probePos.down()).hasSolidTopSurface(player.getWorld(), probePos.down(), player))
                    return probePos;
                else if (player.getWorld().getBlockState(probePos.offset(Direction.DOWN)).isAir() && !probePos.offset(Direction.DOWN).equals(player.getBlockPos().add(0,1,0)) && !probePos.offset(Direction.DOWN).equals(player.getBlockPos()) && probePos.getY() > -63 && !player.getWorld().isOutOfHeightLimit(probePos)) {
                    supportBlock = probePos.offset(Direction.DOWN);
                    return probePos;
                }
            }
        }
        return null;
    }
    //-------------------- World Interactions ----------------------------------
    private void mineBedrock(){
        breakBlock(this.torchPos);
        breakBlock(this.pistonPlacement.pos());
        replacePiston(this.pistonPlacement.pos());
        if(this.supportBlock!=null) {
            breakBlock(this.supportBlock);
            this.supportBlock=null;
        }
        breakBlock(this.pistonPlacement.pos());
        this.pistonPlacement=null;
        this.torchPos=null;
    }
    private void placePiston(BlockPos pistonPos, Direction dir){
        if(pistonPos==null || dir == null)
            return;
        selectItem(null,pistonType);
        interactionManager.interactBlock(
                player,
                player.getActiveHand(),
               new BlockHitResult(Vec3d.ofCenter(pistonPos),dir,pistonPos,true));
    }
    private void replacePiston(BlockPos pistonPos){
        if(pistonPos==null)
            return;
        int oldSlot = player.getInventory().selectedSlot;
        selectItem(null,pistonType);
        interactionManager.interactBlock(
                player,
                player.getActiveHand(),
                new BlockHitResult(Vec3d.ofCenter(pistonPos),
                        pistonPlacement.dir().getOpposite(),
                        pistonPos,
                        true));
        player.getInventory().selectedSlot = oldSlot;
    }
    private void placeBlock(BlockPos bp){interactionManager.interactBlock(
            player,
            player.getActiveHand(),
            new BlockHitResult(Vec3d.ofCenter(bp),Direction.UP,bp,true));}
    private void breakBlock(BlockPos blockPos){
        netHandler.sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,blockPos,player.getHorizontalFacing()));
        netHandler.sendPacket(
                new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,blockPos,player.getHorizontalFacing()));
    }
    //-------------------- Util ------------------------------------------------
    public List<Block> getTargetBlocks(){return allowedBlocksToMine;}
    public void addTargetBlock(Block b){allowedBlocksToMine.add(b);}
    public void removeTargetBlock(Block b){allowedBlocksToMine.remove(b);}
    private float dirToYaw(Direction d){
        return switch (d) {
            case NORTH -> 180.0f;
            case EAST -> 270.0f;
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            default -> player.getYaw();
        };
    }
    private float dirToPitch(Direction d){
        return switch (d) {
            case UP -> -90.0f;
            case DOWN -> 90.0f;
            default -> player.getPitch();
        };
    }
    private record PistonPlacement(BlockPos pos, Direction dir){}
    private enum Task{
        INIT,
        PLACEPISTON,
        REDSTONETORCH,
        ROTATEPLAYER,
        SWITCHTOPICK,
        MINEPISTON,
        MINESUPPORT,
        NOTHING
    }
}