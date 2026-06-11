package plz.lizi.supersteve.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import plz.lizi.supersteve.init.SSModItems;

public class SSPSign extends Item {
	public SSPSign(int size) {
		super(new Properties().stacksTo(size).fireResistant().rarity(Rarity.EPIC));
	}

	public SSPSign() {
		super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
	}
	
	@Override
	public void appendHoverText(ItemStack p_41421_, Level p_41422_, List<Component> p_41423_, TooltipFlag p_41424_) {
		if (p_41421_.getItem().equals(SSModItems.SSP_SIGN.get())) {
			p_41423_.add(Component.translatable("item.supersteve.ssp_sign_splinter.desc"));
		}
		super.appendHoverText(p_41421_, p_41422_, p_41423_, p_41424_);
	}
}
