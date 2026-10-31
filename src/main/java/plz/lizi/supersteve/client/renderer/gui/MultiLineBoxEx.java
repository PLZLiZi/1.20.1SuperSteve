package plz.lizi.supersteve.client.renderer.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.google.common.base.Objects;

public class MultiLineBoxEx extends MultiLineEditBox {
    private static final int SELECTION_COLOR = 0xFF264F78;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int J_DEFAULT = 0xFFD4D4D4;
    private static final int J_KEYWORD = 0xFF569CD6;
    private static final int J_CONTROL = 0xFFC586C0;
    private static final int J_STRING = 0xFFCE9178;
    private static final int J_COMMENT = 0xFF6A9955;
    private static final int J_NUMBER = 0xFFB5CEA8;
    private static final int J_ANNOTATION = 0xFFDCDCAA;
    private static final int J_TYPE = 0xFF4EC9B0;
    private static final int J_MEMBER = 0xFFDCDCAA;
    private static final int J_VARIABLE = 0xFF9CDCFE;
    private static final int J_CONSTANT = 0xFF4FC1FF;
    private static final int J_CURSOR = 0xFFD4D4D4;
    private static final int J_SELECT_BG = 0x50264F78;
    private static final int PLACEHOLDER_COLOR = -857677600;
    private static final Set<String> JAVA_CONTROL = new HashSet<>(List.of(
            "assert", "break", "case", "catch", "continue", "default", "do", "else", "finally", "for",
            "goto", "if", "instanceof", "new", "return", "super", "switch", "throw", "try", "while", "yield"));
    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(List.of(
            "abstract", "boolean", "byte", "char", "class", "const", "double", "enum", "exports", "extends",
            "false", "final", "float", "implements", "import", "int", "interface", "long", "module", "native",
            "non-sealed", "null", "open", "opens", "package", "permits", "private", "protected", "provides",
            "public", "record", "requires", "sealed", "short", "static", "strictfp", "this", "to", "transient",
            "transitive", "true", "uses", "var", "void", "volatile", "with"));
    private boolean canEdit = true;
    private boolean dragScrollbar = false;
    private double dragGrabOffset = 0.0;
    private boolean syntaxHighlight = false;
    private boolean autoGutter = false;
    private int bgColor = 0xFF1E1E1E;
    private int gutterWidth = 0;
    private int gutterBgColor = 0xFF1E1E1E;
    private int lineNumberColor = 0xFF717171;
    private int lineNumberCurrentColor = 0xFFD4D4D4;
    private int scrollTrackColor = 0xFF262626;
    private int scrollThumbColor = 0xFF5A5A5A;
    private String lastTokenValue = null;
    private List<int[]> tokenRuns = null;
    private Consumer<String> valueListenerChain = null;

    public MultiLineBoxEx(Font f, int x, int y, int w, int h, Component placeholder, Component message) {
        super(f, x, y, w, h, placeholder, message);
    }

    public MultiLineBoxEx(Font f, int x, int y, int w, int h, Component placeholder, Component message, int bgColor) {
        super(f, x, y, w, h, placeholder, message);
        this.bgColor = bgColor;
        this.gutterBgColor = bgColor;
    }

    public void setBgColor(int color) {
        this.bgColor = color;
        this.gutterBgColor = color;
    }

    public void setGutterWidth(int gutterWidth) {
        if (this.gutterWidth == gutterWidth)
            return;
        this.gutterWidth = gutterWidth;
        if (this.textField != null) {
            int limit = this.textField.characterLimit();
            int cursor = this.textField.cursor();
            String content = this.textField.value();
            this.textField = new MultilineTextField(this.font, this.getWrapWidth());
            this.textField.setCharacterLimit(limit);
            this.textField.setValue(content);
            this.textField.seekCursor(Whence.ABSOLUTE, cursor);
            this.textField.setCursorListener(this::scrollToCursor);
            if (this.valueListenerChain != null)
                this.textField.setValueListener(this.valueListenerChain);
        }
    }

    @Override
    public void setValueListener(Consumer<String> consumer) {
        this.valueListenerChain = consumer;
        super.setValueListener(consumer);
    }

    public int getGutterWidth() {
        return this.gutterWidth;
    }

    public void setAutoGutter(boolean autoGutter) {
        this.autoGutter = autoGutter;
    }

    public boolean isAutoGutter() {
        return this.autoGutter;
    }

    private void updateGutterWidthForText() {
        String value = this.textField.value();
        int newlines = newlineCountBefore(value, value.length());
        int maxLine = newlines + 1;
        int digits = Math.max(1, Integer.toString(maxLine).length());
        int desired = 6 + digits * this.font.width("9") + 4;
        desired = Math.max(desired, 14);
        if (desired != this.gutterWidth) {
            this.setGutterWidth(desired);
        }
    }

    public void setSyntaxHighlight(boolean enable) {
        this.syntaxHighlight = enable;
        if (!enable) {
            this.lastTokenValue = null;
            this.tokenRuns = null;
        }
    }

    @Override
    public void setValue(String pFullText) {
        super.setValue(pFullText);
        if (this.gutterWidth > 0) {
            this.setScrollAmount(0.0);
        }
        this.lastTokenValue = null;
        this.tokenRuns = null;
    }

    public boolean hasSyntaxHighlight() {
        return this.syntaxHighlight;
    }

    private int getWrapWidth() {
        return Math.max(16, this.width - this.totalInnerPadding() - this.gutterWidth);
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.bgColor);
    }

    @Override
    public void renderHighlight(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY) {
        guiGraphics.fill(RenderType.guiTextHighlight(), minX, minY, maxX, maxY, SELECTION_COLOR);
    }

    private int getScrollThumbHeight() {
        int h = this.getHeight();
        int innerH = Math.max(1, this.getInnerHeight() + 4);
        return Math.min(h, Math.max(32, h * h / innerH));
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return this.scrollbarVisible() && mouseX >= this.getX() + this.getWidth() - SCROLLBAR_WIDTH && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight();
    }

    private void setScrollAmountForMouseY(double mouseY) {
        int thumbH = this.getScrollThumbHeight();
        int trackH = this.getHeight() - thumbH;
        double f = trackH > 0 ? (mouseY - this.getY() - thumbH / 2.0) / (double) trackH : 0.0;
        f = Mth.clamp(f, 0.0, 1.0);
        this.setScrollAmount(f * this.getMaxScrollAmount());
    }

    @Override
    public void renderDecorations(GuiGraphics guiGraphics) {
        if (!this.scrollbarVisible()) {
            return;
        }
        int x = this.getX() + this.getWidth() - SCROLLBAR_WIDTH;
        int y = this.getY();
        int h = this.getHeight();
        int thumbH = this.getScrollThumbHeight();
        int maxScroll = Math.max(1, this.getMaxScrollAmount());
        int thumbY = y + (int) (this.scrollAmount() / (double) maxScroll * (double) (h - thumbH));
        guiGraphics.fill(x, y, x + SCROLLBAR_WIDTH, y + h, this.scrollTrackColor);
        guiGraphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbH, this.scrollThumbColor);
    }

    private int newlineCountBefore(String value, int end) {
        int count = 0;
        for (int i = 0; i < end && i < value.length(); i++) {
            if (value.charAt(i) == '\n')
                count++;
        }
        return count;
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.autoGutter) {
            this.updateGutterWidthForText();
        }
        if (!this.syntaxHighlight) {
            this.renderPlain(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }
        String value = this.textField.value();
        int rowH = this.font.lineHeight;
        double scroll = this.scrollAmount();
        int cursorRow = this.textField.getLineAtCursor();
        if (this.gutterWidth > 0) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0, scroll, 0.0);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.gutterWidth, this.getY() + this.getHeight(), this.gutterBgColor);
            guiGraphics.fill(this.getX() + this.gutterWidth - 1, this.getY(), this.getX() + this.gutterWidth, this.getY() + this.getHeight(), 0xFF2B2B2B);
            guiGraphics.pose().popPose();
            int row = 0;
            int cursorLogical = -1;
            for (MultilineTextField.StringView sv : this.textField.iterateLines()) {
                if (row == cursorRow) {
                    cursorLogical = newlineCountBefore(value, sv.beginIndex()) + 1;
                    break;
                }
                row++;
            }
            row = 0;
            int prevLogical = -1;
            for (MultilineTextField.StringView sv : this.textField.iterateLines()) {
                int y = this.getY() + this.innerPadding() + row * rowH;
                int logical = newlineCountBefore(value, sv.beginIndex()) + 1;
                if (logical != prevLogical) {
                    String num = Integer.toString(logical);
                    int x = this.getX() + this.gutterWidth - this.innerPadding() - this.font.width(num);
                    guiGraphics.drawString(this.font, num, x, y, logical == cursorLogical ? this.lineNumberCurrentColor : this.lineNumberColor, false);
                }
                prevLogical = logical;
                row++;
            }
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.gutterWidth, 0.0, 0.0);
        try {
            this.renderSyntaxText(guiGraphics);
        } finally {
            guiGraphics.pose().popPose();
        }
    }

    private void renderPlain(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.gutterWidth <= 0) {
            super.renderContents(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }
        String value = this.textField.value();
        int rowH = this.font.lineHeight;
        double scroll = this.scrollAmount();
        int cursorRow = this.textField.getLineAtCursor();
        int cursorLogical = -1;
        int rowc = 0;
        for (MultilineTextField.StringView sv : this.textField.iterateLines()) {
            if (rowc == cursorRow) {
                cursorLogical = newlineCountBefore(value, sv.beginIndex()) + 1;
                break;
            }
            rowc++;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0, scroll, 0.0);
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.gutterWidth, this.getY() + this.getHeight(), this.gutterBgColor);
        guiGraphics.fill(this.getX() + this.gutterWidth - 1, this.getY(), this.getX() + this.gutterWidth, this.getY() + this.getHeight(), 0xFF2B2B2B);
        guiGraphics.pose().popPose();
        int row = 0;
        int prevLogical = -1;
        for (MultilineTextField.StringView sv : this.textField.iterateLines()) {
            int y = this.getY() + this.innerPadding() + row * rowH;
            int logical = newlineCountBefore(value, sv.beginIndex()) + 1;
            if (logical != prevLogical) {
                String num = Integer.toString(logical);
                int x = this.getX() + this.gutterWidth - this.innerPadding() - this.font.width(num);
                guiGraphics.drawString(this.font, num, x, y, logical == cursorLogical ? this.lineNumberCurrentColor : this.lineNumberColor, false);
            }
            prevLogical = logical;
            row++;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.gutterWidth, 0.0, 0.0);
        try {
            super.renderContents(guiGraphics, mouseX, mouseY, partialTick);
        } finally {
            guiGraphics.pose().popPose();
        }
    }

    private void renderSyntaxText(GuiGraphics guiGraphics) {
        String value = this.textField.value();
        if (value.isEmpty() && !this.isFocused()) {
            guiGraphics.drawWordWrap(this.font, this.placeholder, this.getX() + this.innerPadding(), this.getY() + this.innerPadding(), this.width - this.totalInnerPadding(), PLACEHOLDER_COLOR);
        }
        this.ensureTokens(value);
        List<int[]> runs = this.tokenRuns;
        int rowH = this.font.lineHeight;
        int baseY = this.getY() + this.innerPadding();
        int cursor = this.textField.cursor();
        boolean showCursor = this.isFocused() && this.frame / 6 % 2 == 0;
        int selMin = -1;
        int selMax = -1;
        if (this.textField.hasSelection()) {
            MultilineTextField.StringView sel = this.textField.getSelected();
            selMin = sel.beginIndex();
            selMax = sel.endIndex();
        }
        int runIdx = 0;
        int row = 0;
        for (MultilineTextField.StringView line : this.textField.iterateLines()) {
            int lb = line.beginIndex();
            int le = line.endIndex();
            int y = baseY + row * rowH;
            if (this.withinContentAreaTopBottom(y, y + rowH)) {
                int x = this.getX() + this.innerPadding();
                while (runIdx < runs.size() && runs.get(runIdx)[1] <= lb) {
                    runIdx++;
                }
                int j = runIdx;
                if (selMin >= 0 && selMin < selMax) {
                    int r0 = Math.max(lb, selMin);
                    int r1 = Math.min(le, selMax);
                    if (r0 < r1) {
                        guiGraphics.fill(RenderType.guiTextHighlight(), x + this.font.width(value.substring(lb, r0)), y, x + this.font.width(value.substring(lb, r1)), y + rowH, J_SELECT_BG);
                    }
                }
                while (j < runs.size() && runs.get(j)[0] < le) {
                    int[] run = runs.get(j);
                    int s0 = Math.max(run[0], lb);
                    int s1 = Math.min(run[1], le);
                    if (s0 < s1) {
                        String seg = value.substring(s0, s1);
                        guiGraphics.drawString(this.font, seg, x + this.font.width(value.substring(lb, s0)), y, run[2], false);
                    }
                    j++;
                }
                if (showCursor && cursor >= lb && cursor <= le) {
                    int cx = x + this.font.width(value.substring(lb, Math.min(cursor, le)));
                    if (cursor < le) {
                        guiGraphics.fill(cx, y - 1, cx + 1, y - 1 + rowH + 1, J_CURSOR);
                    } else {
                        guiGraphics.drawString(this.font, "_", cx, y, J_CURSOR);
                    }
                }
            }
            while (runIdx < runs.size() && runs.get(runIdx)[1] <= le) {
                runIdx++;
            }
            row++;
        }
    }

    private void ensureTokens(String value) {
        if (Objects.equal(value, this.lastTokenValue)) {
            return;
        }
        this.lastTokenValue = value;
        List<int[]> runs = new ArrayList<>();
        int n = value.length();
        int i = 0;
        boolean packageMode = false;
        while (i < n) {
            char c = value.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                int s = i;
                while (i < n && (value.charAt(i) == ' ' || value.charAt(i) == '\t' || value.charAt(i) == '\r' || value.charAt(i) == '\n')) {
                    i++;
                }
                addRun(runs, s, i, J_DEFAULT);
            } else if (c == '/' && i + 1 < n && value.charAt(i + 1) == '/') {
                int s = i;
                int nl = value.indexOf('\n', i);
                i = nl < 0 ? n : nl;
                addRun(runs, s, i, J_COMMENT);
            } else if (c == '/' && i + 1 < n && value.charAt(i + 1) == '*') {
                int s = i;
                int e = value.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
                addRun(runs, s, i, J_COMMENT);
            } else if (c == '"') {
                int s = i;
                if (value.startsWith("\"\"\"", i)) {
                    int e = value.indexOf("\"\"\"", i + 3);
                    i = e < 0 ? n : e + 3;
                } else {
                    i++;
                    while (i < n) {
                        char cc = value.charAt(i);
                        if (cc == '\\') {
                            i += 2;
                            continue;
                        }
                        if (cc == '"') {
                            i++;
                            break;
                        }
                        i++;
                    }
                }
                addRun(runs, s, i, J_STRING);
            } else if (c == '\'') {
                int s = i;
                i++;
                while (i < n) {
                    char cc = value.charAt(i);
                    if (cc == '\\') {
                        i += 2;
                        continue;
                    }
                    if (cc == '\'') {
                        i++;
                        break;
                    }
                    i++;
                }
                addRun(runs, s, i, J_STRING);
            } else if (c == '@') {
                int s = i;
                i++;
                while (i < n && isIdentPart(value.charAt(i))) {
                    i++;
                }
                while (i < n && value.charAt(i) == '.') {
                    i++;
                    while (i < n && isIdentPart(value.charAt(i))) {
                        i++;
                    }
                }
                addRun(runs, s, i, J_ANNOTATION);
            } else if (isDigit(c) || (c == '.' && i + 1 < n && isDigit(value.charAt(i + 1)))) {
                int s = i;
                i++;
                while (i < n && (isIdentPart(value.charAt(i)) || value.charAt(i) == '.' || value.charAt(i) == '-')) {
                    i++;
                }
                addRun(runs, s, i, J_NUMBER);
            } else if (isIdentStart(c)) {
                int s = i;
                while (i < n && isIdentPart(value.charAt(i))) {
                    i++;
                }
                String w = value.substring(s, i);
                int color;
                if (JAVA_CONTROL.contains(w)) {
                    color = J_CONTROL;
                } else if (JAVA_KEYWORDS.contains(w)) {
                    color = J_KEYWORD;
                    if (w.equals("import") || w.equals("package")) {
                        packageMode = true;
                    }
                } else if (packageMode) {
                    color = J_TYPE;
                } else if (isConstantName(w)) {
                    color = J_CONSTANT;
                } else if (Character.isUpperCase(value.charAt(s))) {
                    color = J_TYPE;
                } else if (nextNonSpaceIs(value, i, '(')) {
                    color = J_MEMBER;
                } else {
                    color = J_VARIABLE;
                }
                addRun(runs, s, i, color);
            } else {
                if (c == ';') {
                    packageMode = false;
                }
                i++;
                addRun(runs, i - 1, i, J_DEFAULT);
            }
        }
        this.tokenRuns = runs;
    }

    private static boolean isIdentStart(char c) {
        return Character.isJavaIdentifierStart(c);
    }

    private static boolean isIdentPart(char c) {
        return Character.isJavaIdentifierPart(c);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isConstantName(String w) {
        if (w.length() < 2)
            return false;
        boolean hasUpper = false;
        for (int k = 0; k < w.length(); k++) {
            char c = w.charAt(k);
            if (c >= 'a' && c <= 'z')
                return false;
            if (c >= 'A' && c <= 'Z')
                hasUpper = true;
            if (c != '_' && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9'))
                return false;
        }
        return hasUpper;
    }

    private static boolean nextNonSpaceIs(String value, int from, char target) {
        int n = value.length();
        int k = from;
        while (k < n && (value.charAt(k) == ' ' || value.charAt(k) == '\t' || value.charAt(k) == '\r' || value.charAt(k) == '\n')) {
            k++;
        }
        return k < n && value.charAt(k) == target;
    }

    private static void addRun(List<int[]> runs, int begin, int end, int color) {
        if (end > begin) {
            runs.add(new int[] { begin, end, color });
        }
    }

    private void seekCursorPoint(double mouseX, double mouseY) {
        double x = mouseX - this.getX() - this.innerPadding() - this.gutterWidth;
        double y = mouseY - this.getY() - this.innerPadding() + this.scrollAmount();
        this.textField.seekCursorToPoint(x, y);
    }

    private void seekCursorToGutterLine(double mouseY) {
        double y = mouseY - this.getY() - this.innerPadding() + this.scrollAmount();
        this.textField.seekCursorToPoint(0.0, y);
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

    public static double SCROLL_LINES = 3.0; // 滚轮每格滚动行数（原版为 0.5 行）

    @Override
    public double scrollRate() {
        return this.font.lineHeight * SCROLL_LINES;
    }

    public void setEdit(boolean edit) {
        canEdit = edit;
    }

    public boolean canEdit() {
        return canEdit;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible)
            return false;
        if (button == 0 && this.isOverScrollbar(mouseX, mouseY)) {
            this.dragScrollbar = true;
            this.setFocused(true);
            int thumbH = this.getScrollThumbHeight();
            int maxScroll = Math.max(1, this.getMaxScrollAmount());
            int thumbY = this.getY() + (int) (this.scrollAmount() / (double) maxScroll * (double) (this.getHeight() - thumbH));
            if (mouseY < thumbY || mouseY > thumbY + thumbH) {
                this.setScrollAmountForMouseY(mouseY);
                thumbY = this.getY() + (int) (this.scrollAmount() / (double) maxScroll * (double) (this.getHeight() - thumbH));
            }
            this.dragGrabOffset = mouseY - thumbY;
            return true;
        }
        if (button == 0 && this.withinContentAreaPoint(mouseX, mouseY)) {
            if (this.gutterWidth > 0 && mouseX < this.getX() + this.gutterWidth) {
                this.seekCursorToGutterLine(mouseY);
            } else {
                this.textField.setSelecting(Screen.hasShiftDown());
                this.seekCursorPoint(mouseX, mouseY);
            }
            this.setFocused(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragScrollbar && button == 0) {
            int thumbH = this.getScrollThumbHeight();
            int maxScroll = Math.max(1, this.getMaxScrollAmount());
            double targetThumbY = mouseY - this.dragGrabOffset;
            double f = (targetThumbY - (double) this.getY()) / (double) Math.max(1, this.getHeight() - thumbH);
            this.setScrollAmount(f * maxScroll);
            return true;
        }
        if (this.withinContentAreaPoint(mouseX, mouseY) && button == 0) {
            this.textField.setSelecting(true);
            if (this.gutterWidth > 0 && mouseX < this.getX() + this.gutterWidth) {
                this.seekCursorToGutterLine(mouseY);
            } else {
                this.seekCursorPoint(mouseX, mouseY);
            }
            this.textField.setSelecting(Screen.hasShiftDown());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.dragScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
