package com.oreexcavationauto;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OreExcavationAutoScreen extends Screen {
    private static final int TAB_WHITELIST = 0;
    private static final int TAB_BLACKLIST = 1;
    private static final int TAB_ALL_BLOCKS = 2;
    private static final int ROW_HEIGHT = 20;

    private int currentTab = TAB_ALL_BLOCKS;
    private int scrollOffset = 0;
    private int panelX;
    private int panelY;
    private int panelWidth = 420;
    private int panelHeight = 300;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int visibleRows;

    private EditBox searchBox;
    private List<String> allBlockIds = new ArrayList<>();
    private List<String> filteredBlockIds = new ArrayList<>();

    public OreExcavationAutoScreen() {
        super(Component.translatable("screen.oreexcavationauto.title"));
    }

    @Override
    protected void init() {
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        listX = panelX + 10;
        listY = panelY + 108;
        listWidth = panelWidth - 20;
        listHeight = 152;
        visibleRows = listHeight / ROW_HEIGHT;

        allBlockIds.clear();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null && !"minecraft:air".equals(id.toString())) {
                allBlockIds.add(id.toString());
            }
        }
        allBlockIds.sort(Comparator.naturalOrder());

        int topY = panelY + 28;
        this.addRenderableWidget(Button.builder(
            Component.translatable(OreExcavationAutoConfig.isEnabled()
                ? "button.oreexcavationauto.disable"
                : "button.oreexcavationauto.enable"),
            button -> {
                boolean enabled = !OreExcavationAutoConfig.isEnabled();
                OreExcavationAutoConfig.setEnabled(enabled);
                button.setMessage(Component.translatable(enabled
                    ? "button.oreexcavationauto.disable"
                    : "button.oreexcavationauto.enable"));
            }
        ).bounds(panelX + 10, topY, 105, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Ore: " + (OreExcavationAutoConfig.isAutoDetect() ? "ON" : "OFF")),
            button -> {
                boolean enabled = !OreExcavationAutoConfig.isAutoDetect();
                OreExcavationAutoConfig.setAutoDetect(enabled);
                button.setMessage(Component.literal("Ore: " + (enabled ? "ON" : "OFF")));
            }
        ).bounds(panelX + 120, topY, 85, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("tab.oreexcavationauto.all"),
            button -> {
                currentTab = TAB_ALL_BLOCKS;
                scrollOffset = 0;
                refreshList();
            }
        ).bounds(panelX + 10, panelY + 54, 95, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("tab.oreexcavationauto.whitelist"),
            button -> {
                currentTab = TAB_WHITELIST;
                scrollOffset = 0;
                refreshList();
            }
        ).bounds(panelX + 110, panelY + 54, 95, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("tab.oreexcavationauto.blacklist"),
            button -> {
                currentTab = TAB_BLACKLIST;
                scrollOffset = 0;
                refreshList();
            }
        ).bounds(panelX + 210, panelY + 54, 100, 20).build());

        searchBox = new EditBox(font, panelX + 10, panelY + 82, panelWidth - 20, 20,
            Component.translatable("search.oreexcavationauto.placeholder"));
        searchBox.setHint(Component.translatable("search.oreexcavationauto.placeholder"));
        searchBox.setMaxLength(128);
        searchBox.setResponder(text -> {
            scrollOffset = 0;
            refreshList();
        });
        this.addRenderableWidget(searchBox);

        this.addRenderableWidget(Button.builder(
            Component.translatable("button.oreexcavationauto.add_current"),
            button -> addCurrentlyLookedAtBlock()
        ).bounds(panelX + 10, panelY + 270, 155, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            button -> onClose()
        ).bounds(panelX + panelWidth - 75, panelY + 270, 65, 20).build());

        refreshList();
    }

    private void refreshList() {
        String search = searchBox == null ? "" : searchBox.getValue().toLowerCase();
        filteredBlockIds = new ArrayList<>();
        List<String> source = switch (currentTab) {
            case TAB_WHITELIST -> OreExcavationAutoConfig.getWhitelist();
            case TAB_BLACKLIST -> OreExcavationAutoConfig.getBlacklist();
            default -> allBlockIds;
        };
        for (String blockId : source) {
            if (search.isEmpty() || blockId.toLowerCase().contains(search)
                || blockId.substring(blockId.indexOf(':') + 1).contains(search)) {
                filteredBlockIds.add(blockId);
            }
        }
        scrollOffset = Math.min(scrollOffset,
            Math.max(0, filteredBlockIds.size() - visibleRows));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF17191F);
        graphics.fill(panelX - 2, panelY - 2,
            panelX + panelWidth + 2, panelY + panelHeight + 2, 0xFF050608);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF242731);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 24, 0xFF343847);
        graphics.fill(panelX, panelY + panelHeight - 1,
            panelX + panelWidth, panelY + panelHeight, 0xFF50556A);
        graphics.drawCenteredString(font, title, width / 2, panelY + 8, 0xFFFFFFFF);

        int activeTabX = currentTab == TAB_ALL_BLOCKS ? panelX + 10
            : currentTab == TAB_WHITELIST ? panelX + 110 : panelX + 210;
        graphics.fill(activeTabX, panelY + 54, activeTabX + 95,
            panelY + 74, 0xFF4A5068);

        graphics.fill(listX - 1, listY - 1,
            listX + listWidth + 1, listY + listHeight + 1, 0xFF08090C);
        graphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0xFF181A20);

        int end = Math.min(scrollOffset + visibleRows, filteredBlockIds.size());
        for (int index = scrollOffset; index < end; index++) {
            int row = index - scrollOffset;
            int rowY = listY + row * ROW_HEIGHT;
            String blockId = filteredBlockIds.get(index);
            boolean selected = currentTab == TAB_WHITELIST
                ? OreExcavationAutoConfig.getWhitelist().contains(blockId)
                : OreExcavationAutoConfig.getBlacklist().contains(blockId);
            boolean hovered = mouseX >= listX && mouseX < listX + listWidth
                && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            int background = hovered ? 0xFF454545 : (row % 2 == 0 ? 0xFF202020 : 0xFF1A1A1A);
            graphics.fill(listX, rowY, listX + listWidth, rowY + ROW_HEIGHT, background);

            if (currentTab == TAB_ALL_BLOCKS) {
                boolean inWhitelist = OreExcavationAutoConfig.getWhitelist().contains(blockId);
                boolean inBlacklist = OreExcavationAutoConfig.getBlacklist().contains(blockId);
                graphics.drawString(font, blockId, listX + 6, rowY + 6, 0xFFFFFFFF);

                int whitelistX = listX + listWidth - 145;
                int blacklistX = listX + listWidth - 72;
                graphics.fill(whitelistX, rowY + 2, whitelistX + 68, rowY + 18,
                    inWhitelist ? 0xFF2E7D32 : 0xFF3A3D48);
                graphics.fill(blacklistX, rowY + 2, blacklistX + 68, rowY + 18,
                    inBlacklist ? 0xFFA33A3A : 0xFF3A3D48);
                graphics.drawCenteredString(font,
                    Component.literal(inWhitelist ? "- Белый" : "+ Белый"),
                    whitelistX + 34, rowY + 6,
                    inWhitelist ? 0xFFB5FFB5 : 0xFFFFFFFF);
                graphics.drawCenteredString(font,
                    Component.literal(inBlacklist ? "- Чёрный" : "+ Чёрный"),
                    blacklistX + 34, rowY + 6,
                    inBlacklist ? 0xFFFFBBBB : 0xFFFFFFFF);
            } else {
                int markerColor = currentTab == TAB_WHITELIST ? 0xFF55FF55 : 0xFFFF5555;
                graphics.drawString(font, "[x]", listX + 6, rowY + 6, markerColor);
                graphics.drawString(font, blockId, listX + 36, rowY + 6, 0xFFFFFFFF);
                int removeX = listX + listWidth - 76;
                graphics.fill(removeX, rowY + 2, listX + listWidth - 4, rowY + 18, 0xFF7A3232);
                graphics.drawCenteredString(font, Component.literal("Удалить"),
                    removeX + (listX + listWidth - 4 - removeX) / 2, rowY + 6, 0xFFFFFFFF);
            }
        }

        if (filteredBlockIds.isEmpty()) {
            graphics.drawCenteredString(font, Component.literal("Нет подходящих блоков"),
                listX + listWidth / 2, listY + 12, 0xFFCCCCCC);
        }

        int maxScroll = Math.max(0, filteredBlockIds.size() - visibleRows);
        if (maxScroll > 0) {
            String scrollText = (scrollOffset + 1) + "-" + end + " / " + filteredBlockIds.size();
            graphics.drawString(font, scrollText, panelX + 10, panelY + 264, 0xFFCCCCCC);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty: Screen.render() calls this method, but the vanilla
        // implementation applies a world blur that we do not want in this menu.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listX && mouseX < listX + listWidth
            && mouseY >= listY && mouseY < listY + listHeight) {
            int row = (int) ((mouseY - listY) / ROW_HEIGHT);
            int index = scrollOffset + row;
            if (index >= 0 && index < filteredBlockIds.size()) {
                String blockId = filteredBlockIds.get(index);
                if (currentTab == TAB_ALL_BLOCKS) {
                    int rowY = listY + row * ROW_HEIGHT;
                    int whitelistX = listX + listWidth - 145;
                    int blacklistX = listX + listWidth - 72;
                    if (mouseX >= whitelistX && mouseX < whitelistX + 68
                        && mouseY >= rowY + 2 && mouseY < rowY + 18) {
                        if (OreExcavationAutoConfig.getWhitelist().contains(blockId)) {
                            OreExcavationAutoConfig.removeFromWhitelist(blockId);
                        } else {
                            OreExcavationAutoConfig.addToWhitelist(blockId);
                        }
                        refreshList();
                        return true;
                    }
                    if (mouseX >= blacklistX && mouseX < blacklistX + 68
                        && mouseY >= rowY + 2 && mouseY < rowY + 18) {
                        if (OreExcavationAutoConfig.getBlacklist().contains(blockId)) {
                            OreExcavationAutoConfig.removeFromBlacklist(blockId);
                        } else {
                            OreExcavationAutoConfig.addToBlacklist(blockId);
                        }
                        refreshList();
                        return true;
                    }
                } else if (mouseX >= listX + listWidth - 76
                    && mouseX < listX + listWidth - 4) {
                    if (currentTab == TAB_WHITELIST) {
                        OreExcavationAutoConfig.removeFromWhitelist(blockId);
                    } else {
                        OreExcavationAutoConfig.removeFromBlacklist(blockId);
                    }
                    refreshList();
                    scrollOffset = Math.min(scrollOffset,
                        Math.max(0, filteredBlockIds.size() - visibleRows));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, filteredBlockIds.size() - visibleRows);
        if (scrollY < 0) {
            scrollOffset = Math.min(scrollOffset + 3, maxScroll);
        } else if (scrollY > 0) {
            scrollOffset = Math.max(scrollOffset - 3, 0);
        }
        return true;
    }

    private void addCurrentlyLookedAtBlock() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult blockHit)
            || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(
            minecraft.level.getBlockState(blockHit.getBlockPos()).getBlock());
        if (blockId == null) {
            return;
        }

        if (currentTab == TAB_WHITELIST) {
            OreExcavationAutoConfig.addToWhitelist(blockId.toString());
        } else {
            OreExcavationAutoConfig.addToBlacklist(blockId.toString());
        }
        refreshList();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
