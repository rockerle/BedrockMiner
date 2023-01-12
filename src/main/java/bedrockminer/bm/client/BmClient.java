package bedrockminer.bm.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Environment(EnvType.CLIENT)
public class BmClient implements ClientModInitializer {
    public static Boolean clickMine;
    public static Miner miner;
    public static List<Block> mineableBlocks = List.of(Blocks.BEDROCK);
    @Override
    public void onInitializeClient() {
        clickMine = false;
        KeyBinding mine = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "toggle miner",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "Bedrockminer"));
        ClientTickEvents.END_CLIENT_TICK.register(client ->{
            if(miner==null && client.player!=null)
                miner = new Miner(client.player);
            if(clickMine) {
                try {
                    miner.tick();
                }catch(Exception e){
                    miner.reset();
                }
            }
            if(mine.wasPressed()) {
                if(clickMine) {
                    clickMine = false;
                    miner.reset();
                }
                else{
                    clickMine = true;
                }
                client.player.sendMessage(Text.of(String.format("Toggle bedrock miner %s",clickMine?"§2on":"§4off")),true);
            }
        });
    }
}