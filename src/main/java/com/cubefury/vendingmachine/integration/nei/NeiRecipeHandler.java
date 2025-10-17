package com.cubefury.vendingmachine.integration.nei;

import static codechicken.lib.gui.GuiDraw.changeTexture;
import static codechicken.lib.gui.GuiDraw.drawTexturedModalRect;
import static net.minecraft.util.EnumChatFormatting.UNDERLINE;
import static net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.trade.ICondition;
import com.cubefury.vendingmachine.integration.betterquesting.BqAdapter;
import com.cubefury.vendingmachine.integration.betterquesting.BqCondition;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.Trade;
import com.cubefury.vendingmachine.util.BigItemStack;
import com.cubefury.vendingmachine.util.Translator;

import betterquesting.api.questing.IQuest;
import betterquesting.api.storage.BQ_Settings;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.themes.gui_args.GArgsNone;
import betterquesting.api2.client.gui.themes.presets.PresetGUIs;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui2.GuiHome;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.client.gui2.GuiQuestLines;
import betterquesting.client.themes.ThemeRegistry;
import betterquesting.questing.QuestDatabase;
import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.API;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeCatalyst;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.event.FMLInterModComms;

public class NeiRecipeHandler extends TemplateRecipeHandler {

    private static final int SLOT_SIZE = 18;
    private static final int GUI_WIDTH = 166;
    private static final int GRID_COUNT = 4;
    private static final int LINE_SPACE = GuiDraw.fontRenderer.FONT_HEIGHT + 1;
    private static final int CONDITIONS_START_Y = 27 + LINE_SPACE;
    private UUID currentPlayerId;
    private int textColorConditionDefault;
    private int textColorConditionSatisfied;
    private int textColorConditionUnsatisfied;

    private int lastHoveredRecipeIndex = -1;
    private Rectangle lastHoveredTextArea = null;
    private UUID lastHoveredQuestId = null;

    public NeiRecipeHandler() {}

    public void addHandler() {
        FMLInterModComms.sendRuntimeMessage(
            FMLCommonHandler.instance()
                .findContainerFor(VendingMachine.MODID),
            "NEIPlugins",
            "register-crafting-handler",
            "vendingmachine@" + this.getRecipeName() + "@" + this.getOverlayIdentifier());
        GuiCraftingRecipe.craftinghandlers.add(this);
        API.registerUsageHandler(this);
    }

    private UUID getCurrentPlayerUUID() {
        if (currentPlayerId == null) {
            currentPlayerId = NameCache.INSTANCE.getUUIDFromPlayer(Minecraft.getMinecraft().thePlayer);
        }
        return currentPlayerId;
    }

    private void setTextColors() {
        textColorConditionDefault = Translator.getColor("vendingmachine.gui.neiColor.conditionDefault");
        textColorConditionSatisfied = Translator.getColor("vendingmachine.gui.neiColor.conditionSatisfied");
        textColorConditionUnsatisfied = Translator.getColor("vendingmachine.gui.neiColor.conditionUnsatisfied");
    }

    @Override
    public String getOverlayIdentifier() {
        return "vendingmachine";
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals(getOverlayIdentifier())) {
            setTextColors();
            for (NeiRecipeCache.CacheEntry entry : NeiRecipeCache.recipeCache) {
                this.arecipes.add(new CachedTradeRecipe(entry.trade(), entry.requirements()));
            }
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        setTextColors();
        for (NeiRecipeCache.CacheEntry entry : NeiRecipeCache.recipeCache) {
            for (BigItemStack compareTo : entry.trade().toItems) {
                if (matchStack(result, compareTo)) {
                    this.arecipes.add(new CachedTradeRecipe(entry.trade(), entry.requirements()));
                    break;
                }
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        setTextColors();
        for (NeiRecipeCache.CacheEntry entry : NeiRecipeCache.recipeCache) {
            for (BigItemStack compareTo : entry.trade().fromItems) {
                if (matchStack(ingredient, compareTo)) {
                    this.arecipes.add(new CachedTradeRecipe(entry.trade(), entry.requirements()));
                    break;
                }
            }
        }
    }

    public static List<ItemStack> extractStacks(BigItemStack bigStack) {
        if (bigStack.hasOreDict()) {
            List<ItemStack> ret = Arrays.asList(
                bigStack.getOreIngredient()
                    .getMatchingStacks());
            ret.forEach(s -> s.stackSize = bigStack.stackSize);
            return ret;
        } else {
            return Collections.singletonList(translateBigStack(bigStack));
        }
    }

    public static ItemStack translateBigStack(BigItemStack bigStack) {
        ItemStack stack = bigStack.getBaseStack();
        stack.stackSize = bigStack.stackSize;
        return stack;
    }

    private static boolean matchStack(ItemStack compared, BigItemStack bigStackCompareTo) {
        for (ItemStack compareTo : extractStacks(bigStackCompareTo)) {
            if (NEIServerUtils.areStacksSameTypeCraftingWithNBT(compared, compareTo)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getRecipeName() {
        return VendingMachine.NAME;
    }

    @Override
    public String getGuiTexture() {
        return "vendingmachine:textures/gui/nei.png";
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        changeTexture(getGuiTexture());
        drawTexturedModalRect(0, 0, 0, 0, GUI_WIDTH, 105);
    }

    // Caching the last hovered valid quest here is a bit jank, but it works I guess
    public boolean isMouseOverBqCondition(int recipeIndex, int curY, UUID questId, String text) {
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiRecipe)) return false;
        GuiRecipe<?> gui = (GuiRecipe<?>) Minecraft.getMinecraft().currentScreen;

        List<String> textArray = GuiDraw.fontRenderer.listFormattedStringToWidth(text, GUI_WIDTH);
        int width = textArray.stream()
            .map(GuiDraw::getStringWidth)
            .max(Comparator.naturalOrder())
            .orElse(0);
        int height = GuiDraw.fontRenderer.FONT_HEIGHT + (textArray.size() - 1) * LINE_SPACE;

        Point offset = gui.getRecipePosition(recipeIndex);

        Point pos = GuiDraw.getMousePosition();

        // very cursed I'm sorry :doom:
        GuiRecipeCatalyst catalystWidget = gui.getRecipeCatalystWidget();

        int guiLeft = catalystWidget.x + catalystWidget.w - 6;
        int guiTop = catalystWidget.y + 9;

        Point relMousePos = new Point(pos.x - guiLeft - offset.x, pos.y - guiTop - offset.y);
        Rectangle textArea = new Rectangle(2, curY - GuiDraw.fontRenderer.FONT_HEIGHT, width + 2, height + 1);
        if (textArea.contains(relMousePos)) {
            lastHoveredRecipeIndex = recipeIndex;
            lastHoveredTextArea = textArea;
            lastHoveredQuestId = questId;
            return true;
        }
        return false;
    }

    public boolean isMouseOnLastHovered(GuiRecipe<?> gui, int recipeIndex) {
        if (lastHoveredTextArea == null || lastHoveredQuestId == null || lastHoveredRecipeIndex != recipeIndex) {
            return false;
        }
        GuiRecipeCatalyst catalystWidget = gui.getRecipeCatalystWidget();

        int guiLeft = catalystWidget.x + catalystWidget.w - 6;
        int guiTop = catalystWidget.y + 9;

        Point offset = gui.getRecipePosition(recipeIndex);
        Point pos = GuiDraw.getMousePosition();
        Point relMousePos = new Point(pos.x - guiLeft - offset.x, pos.y - guiTop - offset.y);
        return lastHoveredTextArea.contains(relMousePos);
    }

    @Override
    public boolean mouseClicked(GuiRecipe<?> gui, int button, int recipeIndex) {
        if (super.mouseClicked(gui, button, recipeIndex)) return true;
        if (VendingMachine.isBqLoaded && isMouseOnLastHovered(gui, recipeIndex)) {
            // prepare "Back" behavior
            processBqGui();
            return true;
        }
        return false;
    }

    @Optional.Method(modid = "betterquesting")
    public void processBqGui() {
        GuiScreen parentScreen;
        if (GuiHome.bookmark instanceof GuiQuest && BQ_Settings.useBookmark) {
            // back to GuiQuestLines
            parentScreen = ((GuiScreenCanvas) GuiHome.bookmark).parent;
        } else if (GuiHome.bookmark instanceof GuiScreenCanvas && BQ_Settings.useBookmark) {
            // for example, GuiQuestLines.parent is GuiHome
            // going back to home screen is not good
            parentScreen = GuiHome.bookmark;
        } else {
            // init quest screen
            parentScreen = ThemeRegistry.INSTANCE.getGui(PresetGUIs.HOME, GArgsNone.NONE);
            if (BQ_Settings.useBookmark && BQ_Settings.skipHome) {
                parentScreen = new GuiQuestLines(parentScreen);
            }
        }
        GuiQuest toDisplay = new GuiQuest(parentScreen, lastHoveredQuestId);
        toDisplay.setPreviousScreen(Minecraft.getMinecraft().currentScreen);
        Minecraft.getMinecraft()
            .displayGuiScreen(toDisplay);
        if (BQ_Settings.useBookmark) {
            GuiHome.bookmark = toDisplay;
        }
    }

    @Override
    public void drawExtras(int recipeIndex) {
        CachedTradeRecipe recipe = (CachedTradeRecipe) this.arecipes.get(recipeIndex);

        float scale = 0.5f;
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1);
        for (PositionedStack ps : recipe.ncInputs) {
            GuiDraw.fontRenderer.drawString(
                "NC",
                (int) (ps.relx / scale),
                (int) (ps.rely / scale),
                Translator.getColor("vendingmachine.gui.nc_inputs_overlay_color"),
                false);
        }
        GL11.glPopMatrix();

        GuiDraw.drawString(
            Translator.translate("vendingmachine.gui.requirementHeader"),
            2,
            27,
            textColorConditionDefault,
            false);
        int y = CONDITIONS_START_Y;
        for (ICondition condition : recipe.requirements) {
            StringBuilder requirementString = new StringBuilder();
            int color = textColorConditionDefault;
            if (VendingMachine.isBqLoaded && condition instanceof BqCondition) {
                StringBuilder unformatted = new StringBuilder(
                    Translator.translate("vendingmachine.gui.requirement.betterquesting"));
                unformatted.append(": ");
                UUID questId = ((BqCondition) condition).getQuestId();
                IQuest quest = QuestDatabase.INSTANCE.get(questId);
                if (quest == null) {
                    requirementString
                        .append(Translator.translate("vendingmachine.gui.requirement.betterquesting.missing"));
                } else {
                    String translatedQuestKey = getTextWithoutFormattingCodes(
                        QuestTranslation.translateQuestName(questId, quest));
                    unformatted.append(
                        translatedQuestKey.length() <= 18 ? translatedQuestKey
                            : translatedQuestKey.substring(0, 18) + "...");
                    requirementString.append(
                        isMouseOverBqCondition(recipeIndex, y, questId, unformatted.toString()) ? UNDERLINE : "");
                    requirementString.append(unformatted);
                }
                color = BqAdapter.INSTANCE.checkPlayerCompletedQuest(getCurrentPlayerUUID(), questId)
                    ? textColorConditionSatisfied
                    : textColorConditionUnsatisfied;
            } else {
                requirementString.append(Translator.translate("vendingmachine.gui.requirement.unknown"));
            }

            List<String> requirementsArray = GuiDraw.fontRenderer
                .listFormattedStringToWidth(requirementString.toString(), GUI_WIDTH);
            for (String line : requirementsArray) {
                GuiDraw.drawString(line, 2, y, color, false);
                y += LINE_SPACE;
            }
        }
    }

    public class CachedTradeRecipe extends CachedRecipe {

        private final List<PositionedStack> inputs = new ArrayList<>();
        private final List<PositionedStack> ncInputs = new ArrayList<>();
        private final List<PositionedStack> outputs = new ArrayList<>();
        private final List<ICondition> requirements = new ArrayList<>();

        private CachedTradeRecipe(Trade trade, List<ICondition> requirements) {
            loadInputs(trade);
            loadOutputs(trade);

            this.requirements.addAll(requirements);
        }

        private void loadInputs(Trade trade) {
            int xOffset = 3, y = 7;
            int index = 0;
            for (BigItemStack stack : trade.fromItems) {
                if (index >= GRID_COUNT) {
                    break;
                }
                int x = xOffset + index * SLOT_SIZE;
                inputs.add(new PositionedStack(extractStacks(stack), x, y));
                index++;
            }

            for (BigItemStack stack : trade.nonConsumedItems) {
                if (index >= GRID_COUNT) {
                    break;
                }
                int x = xOffset + index * SLOT_SIZE;
                ncInputs.add(new PositionedStack(extractStacks(stack), x, y));
                index++;
            }

            for (CurrencyItem ci : trade.fromCurrency) {
                if (index >= GRID_COUNT) {
                    break;
                }
                int x = xOffset + index * SLOT_SIZE;
                inputs.add(new PositionedStack(ci.getItemRepresentation(), x, y));
                index++;
            }
        }

        private void loadOutputs(Trade trade) {
            int xOffset = 93, y = 7;
            int index = 0;
            for (BigItemStack stack : trade.toItems) {
                if (index >= GRID_COUNT) {
                    break;
                }
                int x = xOffset + index * SLOT_SIZE;
                outputs.add(new PositionedStack(extractStacks(stack), x, y));
                index++;
            }
        }

        @Override
        public PositionedStack getResult() {
            return null;
        }

        @Override
        public List<PositionedStack> getIngredients() {
            List<PositionedStack> allInputs = new ArrayList<>();
            allInputs.addAll(inputs);
            allInputs.addAll(ncInputs);
            return getCycledIngredients(cycleticks / 20, allInputs);
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            return outputs;
        }

        public List<PositionedStack> getNcInputs() {
            return ncInputs;
        }
    }

    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(75, 0, 16, 24), getOverlayIdentifier()));
    }
}
