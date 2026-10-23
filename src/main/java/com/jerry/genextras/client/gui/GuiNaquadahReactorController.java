package com.jerry.genextras.client.gui;

import java.util.List;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.GuiHeatTab;
import mekanism.client.gui.element.tab.GuiWarningTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.IWarningTracker;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import mekanism.common.util.text.EnergyDisplay;
import com.jerry.genextras.client.gui.element.GuiNaquadahReactorTab;
import com.jerry.genextras.client.gui.element.GuiNaquadahReactorTab.NaquadahReactorTab;
import mekanism.generators.common.GeneratorsLang;
import com.jerry.genextras.common.content.naquadah.NaquadahReactorMultiblockData;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorController;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiNaquadahReactorController extends GuiMekanismTile<TileEntityNaquadahReactorController, MekanismTileContainer<TileEntityNaquadahReactorController>> {

    public GuiNaquadahReactorController(MekanismTileContainer<TileEntityNaquadahReactorController> container, Inventory inv, Component title) {
        super(container, inv, title, DEFAULT_IMAGE_WIDTH + 10, DEFAULT_IMAGE_HEIGHT);
        dynamicSlots = true;
        inventoryLabelX += 5;
        titleLabelY = 5;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        if (tile.getMultiblock().isFormed()) {
            addRenderableWidget(new GuiEnergyTab(this, () -> {
                NaquadahReactorMultiblockData multiblock = tile.getMultiblock();
                return List.of(MekanismLang.STORING.translate(EnergyDisplay.of(multiblock.energyContainer())),
                      GeneratorsLang.PRODUCING_AMOUNT.translate(EnergyDisplay.of(multiblock.getPassiveGeneration(false, true))));
            }));
            addRenderableWidget(new GuiHeatTab(this, () -> {
                NaquadahReactorMultiblockData multiblock = tile.getMultiblock();
                Component transfer = MekanismUtils.getTemperatureDisplay(multiblock.lastTransferLoss, TemperatureUnit.KELVIN, false);
                Component environment = MekanismUtils.getTemperatureDisplay(multiblock.lastEnvironmentLoss, TemperatureUnit.KELVIN, false);
                return List.of(MekanismLang.TRANSFERRED_RATE.translate(transfer), MekanismLang.DISSIPATED_RATE.translate(environment));
            }));

            addRenderableWidget(new GuiNaquadahReactorTab(this, tile, NaquadahReactorTab.HEAT));
            addRenderableWidget(new GuiNaquadahReactorTab(this, tile, NaquadahReactorTab.FUEL));
            addRenderableWidget(new GuiNaquadahReactorTab(this, tile, NaquadahReactorTab.STAT));
        }
    }

    @Override
    protected void drawForegroundText(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        drawScrollingString(guiGraphics, MekanismLang.MULTIBLOCK_FORMED.translate(), 0, 16, TextAlignment.LEFT, titleTextColor(), 13, false);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void addWarningTab(IWarningTracker warningTracker) {
        //Move the tab to the right side of the gui so it doesn't intersect the heat tab
        addRenderableWidget(new GuiWarningTab(this, warningTracker, false));
    }
}