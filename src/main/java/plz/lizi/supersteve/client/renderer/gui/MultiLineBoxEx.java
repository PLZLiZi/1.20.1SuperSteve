package plz.lizi.supersteve.client.renderer.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MultiLineBoxEx extends MultiLineEditBox {
    private boolean canEdit = true;

    public MultiLineBoxEx(Font p_239008_, int p_239009_, int p_239010_, int p_239011_, int p_239012_, Component p_239013_, Component p_239014_) {
        super(p_239008_, p_239009_, p_239010_, p_239011_, p_239012_, p_239013_, p_239014_);
    }
    
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (canEdit)
            return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == 256) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (Screen.hasControlDown()) {
            if (keyCode == 67 || keyCode == 65) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        if (keyCode == 262 || keyCode == 263 || keyCode == 264 || keyCode == 265 ||
                keyCode == 266 || keyCode == 267 || keyCode == 268 || keyCode == 269) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (canEdit)
            return super.charTyped(codePoint, modifiers);
        return false;
    }

    public void setEdit(boolean edit) {
        canEdit = edit;
    }
    
    public boolean canEdit() {
        return canEdit;
    }
}
