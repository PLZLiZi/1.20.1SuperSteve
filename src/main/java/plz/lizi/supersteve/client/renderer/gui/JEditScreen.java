package plz.lizi.supersteve.client.renderer.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.api.CfrBridge;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.network.SSNetworks;
import plz.lizi.supersteve.power.HotCplr;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import com.google.common.base.Objects;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

public class JEditScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 120;
    private static final int SEARCH_HEIGHT = 20;
    private static final int BOTTOM_BAR_HEIGHT = 80;
    private static final int TAB_BAR_HEIGHT = 20;
    private static final int PLUS_BUTTON_WIDTH = 20;
    private EditBox searchBox;
    private String query = null;
    private MyObjectSelectionList classList;
    private MultiLineBoxEx codeEditor;
    private final List<Tab> tabs = new ArrayList<>();
    private int activeTabindex = -1;
    public MultiLineBoxEx consoleBox;

    public JEditScreen() {
        super(Component.translatable("item.supersteve.jedit"));
        if (!tabs.isEmpty())
            activeTabindex = 0;
    }

    public MultiLineBoxEx getCodeEditor() {
        return codeEditor;
    }

    public MultiLineBoxEx getConsoleBox() {
        return consoleBox;
    }

    public MyObjectSelectionList getClassList() {
        return classList;
    }

    @Override
    public void init() {
        List<ClassEntry> savedSearchQuery = (this.classList != null) ? this.classList.children() : new ArrayList<>();
        String savedConsoleContent = (this.consoleBox != null) ? this.consoleBox.getValue() : "";
        String savedEditorContent = (this.codeEditor != null) ? this.codeEditor.getValue() : "";
        super.init();
        this.clearWidgets();
        int rightAreaX = SIDEBAR_WIDTH + 4;
        int rightAreaWidth = this.width - rightAreaX - 4;
        int editorHeight = this.height - BOTTOM_BAR_HEIGHT - TAB_BAR_HEIGHT - 8;
        this.searchBox = new EditBox(this.font, 4, 4, SIDEBAR_WIDTH, SEARCH_HEIGHT, Component.literal("Search..."));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);
        int listY = 4 + SEARCH_HEIGHT + 4;
        int listHeight = this.height - listY - 4;
        this.classList = new MyObjectSelectionList(this.minecraft, SIDEBAR_WIDTH, listHeight, listY, listY + listHeight, 18);
        this.addRenderableWidget(this.classList);
        int editorY = 4 + TAB_BAR_HEIGHT;
        this.codeEditor = new MultiLineBoxEx(this.font, rightAreaX, editorY, rightAreaWidth, editorHeight, Component.literal(""), Component.literal("Input"));
        this.codeEditor.setCharacterLimit(Integer.MAX_VALUE);
        this.codeEditor.setEdit(false);
        this.codeEditor.setValueListener(text -> {
            if (activeTabindex >= 0 && activeTabindex < tabs.size()) {
                tabs.get(activeTabindex).content = text;
            }
        });
        this.addRenderableWidget(this.codeEditor);
        int bottomY = this.height - BOTTOM_BAR_HEIGHT + 12;
        int bottomWidth = this.width - rightAreaX - 4;
        int bottomHeight = this.height - bottomY - 4;
        this.consoleBox = new MultiLineBoxEx(this.font, rightAreaX, bottomY, bottomWidth, bottomHeight, Component.literal("Console"), Component.literal(""));
        this.consoleBox.setEdit(false);
        this.consoleBox.setValue("");
        this.addRenderableWidget(this.consoleBox);
        this.codeEditor.setValue(savedEditorContent);
        this.consoleBox.setValue(savedConsoleContent);
        this.classList.children().clear();
        this.classList.children().addAll(savedSearchQuery);
        onSearchChanged("");
        this.setInitialFocus(this.codeEditor);
    }

    private void onSearchChanged(String query) {
        if (Objects.equal(query, this.query))
            return;
        this.query = query;
        classList.clearEntries();
        Set<Class<?>> classes = new HashSet<>();
        for (ClassLoader cl = Minecraft.class.getClassLoader(); cl != null; cl = cl.getParent()) {
            classes.addAll(PLZBase.loadedClasses(cl));
        }
        for (ClassLoader cl = ILaunchPluginService.class.getClassLoader(); cl != null; cl = cl.getParent()) {
            classes.addAll(PLZBase.loadedClasses(cl));
        }
        for (var clazz : classes) {
            if (clazz == null || getClassS1plName(clazz).isEmpty() || !clazz.getName().contains(query))
                continue;
            classList.addEntry(new ClassEntry(clazz));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.fill(0, 0, SIDEBAR_WIDTH + 8, this.height, 0xFF1E1E1E);
        int rightAreaX = SIDEBAR_WIDTH + 8;
        guiGraphics.fill(rightAreaX, 0, this.width, this.height, 0xFF181818);
        int bottomY = this.height - BOTTOM_BAR_HEIGHT - 4;
        guiGraphics.fill(rightAreaX, bottomY, this.width, this.height, 0xFF121212);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTabBar(guiGraphics, mouseX, mouseY);
    }

    private void renderTabBar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = SIDEBAR_WIDTH + 10;
        int tabY = 4;
        int tabHeight = TAB_BAR_HEIGHT - 2;
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            int tabWidth = this.font.width(tab.title) + 20;
            if (tab.exec != null) {
                tabWidth += 10;
            }
            boolean isHovered = mouseX >= startX && mouseX <= startX + tabWidth && mouseY >= tabY && mouseY <= tabY + tabHeight;
            int tabColor = (i == activeTabindex) ? 0xFF2D2D2D : (isHovered ? 0xFF2A2A2A : 0xFF252526);
            int textColor = (i == activeTabindex) ? 0xFFFFFFFF : 0xFF969696;
            guiGraphics.fill(startX, tabY, startX + tabWidth, tabY + tabHeight, tabColor);
            guiGraphics.drawString(this.font, tab.title, startX + 6, tabY + 4, textColor, false);
            if (tab.exec != null) {
                int execX = startX + tabWidth - 20;
                boolean isExecHovered = isHovered && mouseX >= execX && mouseX <= execX + 8;
                int execColor = isExecHovered ? 0xFF4CAF50 : 0xFF717171;
                guiGraphics.drawString(this.font, ">", execX, tabY + 4, execColor, false);
            }
            boolean isXHovered = isHovered && mouseX >= (startX + tabWidth - 14) && mouseX <= (startX + tabWidth);
            int xColor = isXHovered ? 0xFFFF4D4F : 0xFF717171;
            guiGraphics.drawString(this.font, "x", startX + tabWidth - 12, tabY + 4, xColor, false);
            startX += tabWidth + 2;
        }
        int plusButtonWidth = 20;
        boolean isPlusHovered = mouseX >= startX && mouseX <= startX + plusButtonWidth && mouseY >= tabY && mouseY <= tabY + tabHeight;
        int plusColor = isPlusHovered ? 0xFF3E3E42 : 0xFF252526;
        int plusTextColor = isPlusHovered ? 0xFFFFFFFF : 0xFF969696;
        guiGraphics.fill(startX, tabY, startX + plusButtonWidth, tabY + tabHeight, plusColor);
        guiGraphics.drawString(this.font, "+", startX + 7, tabY + 4, plusTextColor, false);
    }

    public void compileSendToExec(String code) {
        consoleBox.setValue("");
        long ms = System.currentTimeMillis();
        String err = null;
        byte[] buf = null;
        try {
            buf = SSUtil.MC_OBF_UTIL.obfB(HotCplr.compileToClassfile(code, SuperSteveMod.class.getClassLoader()));
            if (buf == null)
                err = "Classfile is null";
        } catch (Throwable e) {
            err = e.getMessage();
        }
        consoleBox.setValue((err != null ? err + "\n\n" : "") + "Client compile " + (err == null ? "SUCCESSFUL" : "FAILED") + " in " + ((float) ((System.currentTimeMillis() - ms) / 1000F)) + "s\n\n");
        if (err != null)
            return;
        consoleBox.setValue(consoleBox.getValue() + "Sending classfile to server & execute\n\n");
        SSNetworks.PACKET_HANDLER.sendToServer(new SSNetworks.JCplrMsg(buf));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = SIDEBAR_WIDTH + 10;
        int tabY = 4;
        int tabHeight = TAB_BAR_HEIGHT - 2;
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            int tabWidth = this.font.width(tab.title) + 20;
            if (tab.exec != null) {
                tabWidth += 10;
            }
            if (mouseX >= startX && mouseX <= startX + tabWidth && mouseY >= tabY && mouseY <= tabY + tabHeight) {
                if (tab.exec != null) {
                    int execX = startX + tabWidth - 20;
                    if (mouseX >= execX && mouseX <= execX + 8) {
                        tab.exec.accept(tab);
                        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.4F));
                        return true;
                    }
                }
                if (mouseX >= (startX + tabWidth - 14) && mouseX <= (startX + tabWidth)) {
                    tabs.remove(i);
                    if (tabs.isEmpty()) {
                        activeTabindex = -1;
                        this.codeEditor.setValue("");
                        this.codeEditor.setEdit(false);
                    } else {
                        if (activeTabindex == i) {
                            if (activeTabindex >= tabs.size()) {
                                activeTabindex = tabs.size() - 1;
                            }
                            Tab nextTab = tabs.get(activeTabindex);
                            this.codeEditor.setValue(nextTab.content);
                            this.codeEditor.setEdit(nextTab.editable);
                        } else if (activeTabindex > i) {
                            activeTabindex--;
                        }
                    }
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
                activeTabindex = i;
                this.codeEditor.setValue(tab.content);
                this.codeEditor.setEdit(tab.editable);
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            startX += tabWidth + 2;
        }
        if (mouseX >= startX && mouseX <= startX + PLUS_BUTTON_WIDTH && mouseY >= tabY && mouseY <= tabY + tabHeight) {
            String defaultContent = "package plz.lizi.supersteve.dynamic;\n\npublic class DynamicClass {\n   // Main entry\n    public static void main() {\n    \n    }\n}";
            Tab tab = new Tab("Executor.java", defaultContent, true, t -> {
                compileSendToExec(t.content);
            });
            if (tabs.contains(tab))
                return true;
            tabs.add(tab);
            activeTabindex = tabs.size() - 1;
            this.codeEditor.setValue(defaultContent);
            this.codeEditor.setEdit(true);
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class Tab {
        String title;
        String content;
        boolean editable;
        Consumer<Tab> exec;

        Tab(String title, String content, boolean editable, Consumer<Tab> exec) {
            this.title = title;
            this.content = content;
            this.editable = editable;
            this.exec = exec;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Tab t))
                return false;
            return title.equals(t.title);
        }
    }

    private static String getClassS1plName(Class<?> clazz) {
        if (clazz == null)
            return null;
        return clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
    }

    private class ClassEntry extends ObjectSelectionList.Entry<ClassEntry> {
        private final Class<?> clazz;

        ClassEntry(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            int color = isMouseOver ? 0xFFFFFF : 0xAAAAAA;
            guiGraphics.drawString(JEditScreen.this.font, getClassS1plName(clazz), left + 4, top + 4, color, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                Tab tab = new Tab(getClassS1plName(clazz), "Preparing...", false, null/* TODO: hot retransform */);
                if (tabs.contains(tab))
                    return true;
                tab.content = CfrBridge.decompile(SSUtil.MC_OBF_UTIL.deobfB(PLZBase.getClassBytes(PLZBase.getJarPath(clazz), clazz.getName())));
                tabs.add(tab);
                activeTabindex = tabs.size() - 1;
                JEditScreen.this.codeEditor.setValue(tab.content);
                JEditScreen.this.codeEditor.setEdit(false);
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(getClassS1plName(clazz));
        }
    }
    public static class MyObjectSelectionList extends ObjectSelectionList<ClassEntry> {
        public MyObjectSelectionList(Minecraft p_94442_, int p_94443_, int p_94444_, int p_94445_, int p_94446_, int p_94447_) {
            super(p_94442_, p_94443_, p_94444_, p_94445_, p_94446_, p_94447_);
            setRenderBackground(false);
            setRenderHeader(false, 0);
            setRenderTopAndBottom(false);
        }

        @Override
        public int addEntry(JEditScreen.ClassEntry p_93487_) {
            return super.addEntry(p_93487_);
        }

        @Override
        public boolean removeEntry(ClassEntry p_93503_) {
            return super.removeEntry(p_93503_);
        }

        @Override
        public void clearEntries() {
            super.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return SIDEBAR_WIDTH;
        }

        @Override
        public int getRowLeft() {
            return 4;
        }

        @Override
        public void updateSize(int w, int h, int top, int bottom) {
            super.updateSize(SIDEBAR_WIDTH + 4, h, top, bottom);
        }
    }
}
