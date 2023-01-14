package bedrockminer.bm.client;

import net.minecraft.block.*;
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
import net.minecraft.util.Hand;
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
    private Item pistonType;
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
                    reset();
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
                    this.currentTask = Task.NOTHING;
                }
                break;
            }
            case PLACEPISTON -> {
                placePiston(pistonPlacement.pos,pistonPlacement.dir().getOpposite());
                if(torchPos!=null && pistonPlacement!=null) {
                    this.currentTask = Task.ROTATEPLAYER;
                    toFace = Direction.DOWN;
                    checks[0]=true;
                }
                break;
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
                    toFace = Direction.fromVector(pistonPlacement.pos.subtract(new Vec3i(bedrockBlock.getX(), bedrockBlock.getY(), bedrockBlock.getZ())));
                    this.currentTask=Task.ROTATEPLAYER;
                }
                break;
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
                    this.currentTask = Task.NOTHING;
                }
                break;
            }
            case SWITCHTOPICK ->{
                selectPickaxe();
                toFace=pistonPlacement.dir().getOpposite();
                currentTask = Task.MINEPISTON;
                break;
            }
            case MINEPISTON -> {
                if(failedCounter>0 || this.pistonPlacement==null)
                    reset();
                if(player.world.getBlockState(this.pistonPlacement.pos()).isOf(Blocks.MOVING_PISTON)){
                    failedCounter++;
                    break;
                }

                if(player.world.getBlockState(this.pistonPlacement.pos()).get(Properties.EXTENDED))
                    mineBedrock();
                else{
                    failedCounter++;
                    break;
                }
                if(this.supportBlock==null)
                    this.currentTask = Task.NOTHING;
                else
                    this.currentTask = Task.MINESUPPORT;
                checks[0]=checks[1]=false;
                break;
            }
            case MINESUPPORT ->{
                breakBlock(supportBlock);
                this.supportBlock=null;
                this.currentTask = Task.NOTHING;
                break;
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
                player.world.getBlockState(bp.offset(offsetDir).offset(Direction.UP)).isAir()?Direction.UP:canPistonExtend(bp.offset(offsetDir)));
        if(this.pistonPlacement.pos().equals(player.getBlockPos()) || player.world.isOutOfHeightLimit(this.pistonPlacement.pos()))
            this.pistonPlacement=null;

        int slot = player.getInventory().getSlotWithStack(new ItemStack(Items.PISTON));
        if(slot>-1 && player.getInventory().getStack(slot).getCount()>=2) {
            selectItem(null, Items.PISTON);
            this.pistonType = Items.PISTON;
        }else if((slot=player.getInventory().getSlotWithStack(new ItemStack(Items.STICKY_PISTON)))>-1 && player.getInventory().getStack(slot).getCount()>=2) {
            selectItem(null, Items.STICKY_PISTON);
            this.pistonType = Items.STICKY_PISTON;
        }else {
            player.sendMessage(Text.of("Not enough (sticky)Piston in inventory found"), true);
            this.reset();
        }
    }
    public boolean isRunning(){return this.alreadyRunning;}
    public void reset(){
        this.alreadyRunning=false;
        this.bedrockBlock = null;
        this.pistonPlacement = null;
        this.pistonType = null;
        this.currentTask = Task.NOTHING;
    }

    //-------------------- Inventory Actions -----------------------------------
    public void selectPickaxe(){
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
            player.sendMessage(Text.of("no pick in inventory found"), true);

        }
        if(pickSlot<9) {
            inv.selectedSlot = pickSlot;
        }
        else {
            interactionManager.pickFromInventory(pickSlot);
        }
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
        ItemStack iS = new ItemStack(item);
        if(e!=null)
            iS.addEnchantment(e,5);

        int slot = inv.getSlotWithStack(iS);
        if(slot == -1)
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
            if(player.world.getBlockState(pistonBody.offset(d)).getMaterial().isReplaceable()){
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
            BlockState probeState = player.world.getBlockState(probePos);
            if(probeState.isAir()) {
                if (player.world.getBlockState(probePos.down()).hasSolidTopSurface(player.world, probePos.down(), player))
                    return probePos;
                else if (player.world.getBlockState(probePos.offset(Direction.DOWN)).isAir() && probePos.getY() > -63 && !player.world.isOutOfHeightLimit(probePos)) {
                    supportBlock = probePos.offset(Direction.DOWN);
                    return probePos;
                }
            }
        }

        return null;
    }
    //-------------------- World Interactions ----------------------------------
    public void mineBedrock(){
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
//        int slot = player.getInventory().getSlotWithStack(new ItemStack(Items.PISTON));
//        if(slot>-1 && player.getInventory().getStack(slot).getCount()>=2) {
//            selectItem(null, Items.PISTON);
//            this.pistonType = Items.PISTON;
//        }else if((slot=player.getInventory().getSlotWithStack(new ItemStack(Items.STICKY_PISTON)))>-1 && player.getInventory().getStack(slot).getCount()>=2) {
//            selectItem(null, Items.STICKY_PISTON);
//            this.pistonType = Items.STICKY_PISTON;
//        }else {
//            player.sendMessage(Text.of("Not enough (sticky)Piston in inventory found"), true);
//            this.reset();
//        }
        interactionManager.interactBlock(
                player,
                player.getActiveHand(),
               new BlockHitResult(Vec3d.ofCenter(pistonPos),dir,pistonPos,true));
    }
    public void replacePiston(BlockPos pistonPos){
        if(pistonPos==null)
            return;
        int oldSlot = player.getInventory().selectedSlot;
        selectItem(null,pistonType);
        interactionManager.interactBlock(
                player,
                Hand.MAIN_HAND,
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
    public enum Task{
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