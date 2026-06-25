package plz.lizi.supersteve.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import plz.lizi.supersteve.client.renderer.gui.JEditScreen;

public class JEdit extends Item {
    public JEdit() {
        super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.level.isClientSide) {
            Minecraft.getInstance().setScreen((Screen) (Object) new JEditScreen());
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
