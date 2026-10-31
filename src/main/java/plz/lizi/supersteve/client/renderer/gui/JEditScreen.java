package plz.lizi.supersteve.client.renderer.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import plz.lizi.supersteve.power.Agt;
import plz.lizi.supersteve.power.HotCplr;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import com.google.common.base.Objects;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

public class JEditScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 150;
    private static final int SIDEBAR_BG_WIDTH = SIDEBAR_WIDTH + 16;
    private static final int SEARCH_HEIGHT = 18;
    private static final int TAB_BAR_TOP = 4;
    private static final int TAB_BAR_HEIGHT = 20;
    private static final int EDITOR_TOP = TAB_BAR_TOP + TAB_BAR_HEIGHT;
    private static final int CONSOLE_HEADER_HEIGHT = 20;
    private static final int CONSOLE_PANEL_HEIGHT = 96;
    private static final int STATUS_BAR_HEIGHT = 8;
    private static final int PLUS_BUTTON_WIDTH = 24;
    private static final int CLEAR_BTN_WIDTH = 40;
    private static final int MAX_LIST_ENTRIES = 800;
    private static final int TAB_GAP = 3;
    private static final int TAB_PAD_L = 8;
    private static final int TAB_PAD_R = 6;
    private static final int TAB_ICON_W = 8;
    private static final int TAB_BTN_W = 16;

    private static final int C_EDITOR_BG = 0xFF1E1E1E;
    private static final int C_SIDEBAR_BG = 0xFF252526;
    private static final int C_TAB_INACTIVE_BG = 0xFF2D2D2D;
    private static final int C_TAB_HOVER_BG = 0xFF3A3A3A;
    private static final int C_TAB_ACTIVE_BG = 0xFF1E1E1E;
    private static final int C_TAB_SEPARATOR = 0xFF1F1F1F;
    private static final int C_ACCENT = 0xFF007ACC;
    private static final int C_PANEL_BG = 0xFF181818;
    private static final int C_PANEL_HEADER = 0xFF252526;
    private static final int C_BORDER = 0xFF1F1F1F;
    private static final int C_TEXT_SECONDARY = 0xFF969696;
    private static final int C_TEXT_DIM = 0xFF6E6E6E;
    private static final int C_RUN_GREEN = 0xFF4CAF50;
    private static final int C_STATUS_BG = 0xFF007ACC;
    private static final int C_STATUS_BORDER = 0xFF0061A3;
    private static final int C_ICON_EDITABLE = 0xFF4CAF50;
    private static final int C_ICON_READONLY = 0xFF9D9D9D;
    private static final int C_HIGHLIGHT_BG = 0x1AFFFFFF;

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
        int rightAreaX = SIDEBAR_BG_WIDTH + 2;
        int rightAreaWidth = this.width - rightAreaX - 4;
        int consoleTop = this.height - STATUS_BAR_HEIGHT - CONSOLE_PANEL_HEIGHT;
        int editorY = EDITOR_TOP + 2;
        int editorHeight = consoleTop - editorY;

        this.searchBox = new ThemedBox(this.font, 8, 18, SIDEBAR_WIDTH - 14, SEARCH_HEIGHT, Component.translatable("item.supersteve.jedit"), "Search classes...");
        this.searchBox.setMaxLength(Integer.MAX_VALUE);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        int listY = 18 + SEARCH_HEIGHT + 8;
        int listBottom = this.height - 4;
        this.classList = new MyObjectSelectionList(this.minecraft, SIDEBAR_WIDTH, listBottom - listY, listY, listBottom, 18);
        this.addRenderableWidget(this.classList);

        this.codeEditor = new MultiLineBoxEx(this.font, rightAreaX, editorY, rightAreaWidth, editorHeight, Component.literal(""), Component.literal("Input"), C_EDITOR_BG);
        this.codeEditor.setSyntaxHighlight(true);
        this.codeEditor.setAutoGutter(true);
        this.codeEditor.setCharacterLimit(Integer.MAX_VALUE);
        this.codeEditor.setEdit(false);
        this.codeEditor.setValueListener(text -> {
            if (activeTabindex >= 0 && activeTabindex < tabs.size()) {
                tabs.get(activeTabindex).content = text;
            }
        });
        this.addRenderableWidget(this.codeEditor);

        int consoleY = consoleTop + CONSOLE_HEADER_HEIGHT;
        int consoleHeight = (consoleTop + CONSOLE_PANEL_HEIGHT - 4) - consoleY;
        this.consoleBox = new MultiLineBoxEx(this.font, rightAreaX, consoleY, rightAreaWidth, consoleHeight, Component.literal("Console"), Component.literal(""), C_PANEL_BG);
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
            if (classList.getItemCount() >= MAX_LIST_ENTRIES)
                break;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int rightX = SIDEBAR_BG_WIDTH;
        int statusY = this.height - STATUS_BAR_HEIGHT;
        int consoleTop = statusY - CONSOLE_PANEL_HEIGHT;

        guiGraphics.fill(0, 0, rightX, this.height, C_SIDEBAR_BG);
        guiGraphics.drawString(this.font, "CLASSES", 10, 6, C_TEXT_DIM, false);
        guiGraphics.vLine(rightX, 0, this.height, 0xFF1A1A1A);

        guiGraphics.fill(rightX, 0, this.width, statusY, C_EDITOR_BG);

        guiGraphics.fill(rightX, TAB_BAR_TOP, this.width, TAB_BAR_TOP + TAB_BAR_HEIGHT, C_TAB_INACTIVE_BG);
        guiGraphics.hLine(rightX, this.width, TAB_BAR_TOP + TAB_BAR_HEIGHT, C_BORDER);

        guiGraphics.fill(rightX, consoleTop, this.width, statusY, C_PANEL_BG);
        guiGraphics.fill(rightX, consoleTop, this.width, consoleTop + CONSOLE_HEADER_HEIGHT, C_PANEL_HEADER);
        guiGraphics.hLine(rightX, this.width, consoleTop + CONSOLE_HEADER_HEIGHT, C_BORDER);
        guiGraphics.drawString(this.font, "CONSOLE", rightX + 10, consoleTop + 6, C_TEXT_SECONDARY, false);
        boolean clearHover = mouseX >= this.width - CLEAR_BTN_WIDTH - 8 && mouseX <= this.width - 8 && mouseY >= consoleTop && mouseY <= consoleTop + CONSOLE_HEADER_HEIGHT;
        guiGraphics.drawString(this.font, "Clear", this.width - CLEAR_BTN_WIDTH - 6, consoleTop + 6, clearHover ? 0xFFFFFFFF : C_TEXT_DIM, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTabBar(guiGraphics, mouseX, mouseY);
        renderStatusBar(guiGraphics);
    }

    private int tabWidth(int index) {
        Tab tab = tabs.get(index);
        return TAB_PAD_L + TAB_ICON_W + 4 + this.font.width(tab.title) + (tab.exec != null ? TAB_BTN_W : 0) + TAB_BTN_W + TAB_PAD_R;
    }

    private int tabStartX(int index) {
        int x = SIDEBAR_WIDTH + 10;
        for (int i = 0; i < index; i++) {
            x += tabWidth(i) + TAB_GAP;
        }
        return x;
    }

    private boolean tabFits(int wx) {
        return wx <= this.width - 6;
    }

    private void renderTabBar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int tabY = TAB_BAR_TOP;
        int tabHeight = TAB_BAR_HEIGHT;
        for (int i = 0; i < tabs.size(); i++) {
            int tx = tabStartX(i);
            int tw = tabWidth(i);
            if (!tabFits(tx + tw)) {
                break;
            }
            Tab tab = tabs.get(i);
            boolean active = i == activeTabindex;
            boolean hovered = mouseX >= tx && mouseX <= tx + tw && mouseY >= tabY && mouseY <= tabY + tabHeight;
            int bg = active ? C_TAB_ACTIVE_BG : (hovered ? C_TAB_HOVER_BG : C_TAB_INACTIVE_BG);
            guiGraphics.fill(tx, tabY, tx + tw, tabY + tabHeight, bg);
            if (active) {
                guiGraphics.fill(tx, tabY, tx + tw, tabY + 2, C_ACCENT);
            } else {
                guiGraphics.vLine(tx + tw, tabY, tabY + tabHeight, C_TAB_SEPARATOR);
            }
            int iconColor = tab.exec != null ? C_ICON_EDITABLE : C_ICON_READONLY;
            guiGraphics.fill(tx + TAB_PAD_L, tabY + (tabHeight - 8) / 2, tx + TAB_PAD_L + TAB_ICON_W, tabY + (tabHeight - 8) / 2 + 8, iconColor);
            int textX = tx + TAB_PAD_L + TAB_ICON_W + 4;
            int textColor = active ? 0xFFFFFFFF : C_TEXT_SECONDARY;
            String label = this.font.plainSubstrByWidth(tab.title, tx + tw - TAB_PAD_R - TAB_BTN_W * 2 - (tab.exec != null ? TAB_BTN_W : 0) - 4 - TAB_PAD_L - TAB_ICON_W - 4);
            guiGraphics.drawString(this.font, label, textX, tabY + 5, textColor, false);

            int buttonsBottom = tx + tw - TAB_BTN_W;
            boolean showClose = active || hovered;
            if (tab.exec != null) {
                int runX = buttonsBottom - TAB_BTN_W;
                boolean runVisible = active || hovered;
                if (runVisible) {
                    boolean runHover = hovered && mouseX >= runX && mouseX <= runX + TAB_BTN_W;
                    if (runHover) {
                        guiGraphics.fill(runX, tabY + 2, runX + TAB_BTN_W, tabY + 2 + TAB_BTN_W, 0x334CAF50);
                    }
                    int gx = runX + (TAB_BTN_W - this.font.width(">")) / 2;
                    guiGraphics.drawString(this.font, ">", gx, tabY + 5, runHover ? 0xFFFFFFFF : C_RUN_GREEN, false);
                }
            }
            if (showClose) {
                boolean closeHover = hovered && mouseX >= buttonsBottom && mouseX <= tx + tw;
                if (closeHover) {
                    guiGraphics.fill(buttonsBottom, tabY + 2, buttonsBottom + TAB_BTN_W, tabY + 2 + TAB_BTN_W, 0x26E81123);
                }
                int cx = buttonsBottom + (TAB_BTN_W - this.font.width("\u00d7")) / 2;
                guiGraphics.drawString(this.font, "\u00d7", cx, tabY + 5, closeHover ? 0xFFFFFFFF : C_TEXT_SECONDARY, false);
            }
        }
        int px = tabs.isEmpty() ? SIDEBAR_WIDTH + 10 : tabStartX(tabs.size());
        if (tabFits(px + PLUS_BUTTON_WIDTH)) {
            boolean plusHover = mouseX >= px && mouseX <= px + PLUS_BUTTON_WIDTH && mouseY >= tabY && mouseY <= tabY + tabHeight;
            int plusBg = plusHover ? C_TAB_HOVER_BG : C_TAB_INACTIVE_BG;
            guiGraphics.fill(px, tabY, px + PLUS_BUTTON_WIDTH, tabY + tabHeight, plusBg);
            guiGraphics.drawString(this.font, "+", px + 8, tabY + 5, plusHover ? 0xFFFFFFFF : C_TEXT_SECONDARY, false);
        }
    }

    private void renderStatusBar(GuiGraphics guiGraphics) {
        int sy = this.height - STATUS_BAR_HEIGHT;
        guiGraphics.fill(0, sy, this.width, this.height, C_STATUS_BG);
        guiGraphics.hLine(0, this.width, sy, C_STATUS_BORDER);
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
        int statusY = this.height - STATUS_BAR_HEIGHT;
        int consoleTop = statusY - CONSOLE_PANEL_HEIGHT;
        if (button == 0 && mouseX >= this.width - CLEAR_BTN_WIDTH - 8 && mouseX <= this.width - 8 && mouseY >= consoleTop && mouseY <= consoleTop + CONSOLE_HEADER_HEIGHT) {
            consoleBox.setValue("");
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        int tabY = TAB_BAR_TOP;
        int tabHeight = TAB_BAR_HEIGHT;
        if (mouseY >= tabY && mouseY <= tabY + tabHeight) {
            for (int i = 0; i < tabs.size(); i++) {
                int tx = tabStartX(i);
                int tw = tabWidth(i);
                if (!tabFits(tx + tw)) {
                    break;
                }
                if (mouseX >= tx && mouseX <= tx + tw) {
                    Tab tab = tabs.get(i);
                    int buttonsBottom = tx + tw - TAB_BTN_W;
                    if (tab.exec != null) {
                        int runX = buttonsBottom - TAB_BTN_W;
                        if (mouseX >= runX && mouseX <= runX + TAB_BTN_W) {
                            tab.exec.accept(tab);
                            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.4F));
                            return true;
                        }
                    }
                    if (mouseX >= buttonsBottom) {
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
            }
            int px = tabs.isEmpty() ? SIDEBAR_WIDTH + 10 : tabStartX(tabs.size());
            if (tabFits(px + PLUS_BUTTON_WIDTH) && mouseX >= px && mouseX <= px + PLUS_BUTTON_WIDTH) {
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
            if (isMouseOver) {
                guiGraphics.fill(left, top, left + width, top + height, C_HIGHLIGHT_BG);
            }
            guiGraphics.fill(left + 4, top + (height - 8) / 2, left + 10, top + (height - 8) / 2 + 6, 0xFF2D8CC7);
            guiGraphics.drawString(JEditScreen.this.font, getClassS1plName(clazz), left + 14, top + 4, isMouseOver ? 0xFFFFFFFF : 0xFFA6A6A6, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                Tab tab = new Tab(getClassS1plName(clazz), "Preparing...", false, null);
                if (tabs.contains(tab))
                    return true;
                tab.content = CfrBridge.decompile(SSUtil.MC_OBF_UTIL.deobfB(Agt.getClassBytes(clazz)));
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
        public int getItemCount() {
            return super.getItemCount();
        }

        @Override
        public int getRowWidth() {
            return SIDEBAR_WIDTH;
        }

        @Override
        public int getRowLeft() {
            return 8;
        }

        @Override
        public void updateSize(int w, int h, int top, int bottom) {
            super.updateSize(SIDEBAR_BG_WIDTH, h, top, bottom);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }
    }

    private static class ThemedBox extends EditBox {
        private final Font fontRenderer;
        private final int fillColor = 0xFF2A2A2A;
        private final int borderColor = 0xFF3C3C3C;
        private final int focusColor = C_ACCENT;
        private final String hintText;

        ThemedBox(Font font, int x, int y, int w, int h, Component message, String hintText) {
            super(font, x, y, w, h, message);
            this.fontRenderer = font;
            this.hintText = hintText;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (!this.isVisible())
                return;
            this.setBordered(false);
            int border = this.isFocused() ? focusColor : borderColor;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, border);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, fillColor);
            if (hintText != null && this.getValue().isEmpty() && !this.isFocused()) {
                guiGraphics.drawString(this.fontRenderer, hintText, this.getX() + 2, this.getY() + (this.height - 8) / 2, 0xFF808080, false);
            }
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}