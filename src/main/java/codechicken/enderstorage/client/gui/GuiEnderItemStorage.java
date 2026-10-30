package codechicken.enderstorage.client.gui;

import codechicken.enderstorage.container.ContainerEnderItemStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class GuiEnderItemStorage extends AbstractContainerScreen<ContainerEnderItemStorage> {

    public GuiEnderItemStorage(ContainerEnderItemStorage container, Inventory playerInv, Component title) {
        super(container, playerInv, title);

        if (container.chestInv.getSize() == 2) {
            imageHeight = 222;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title.getVisualOrderText(), 8, 6, 0xFF404040, false);
        graphics.text(font, playerInventoryTitle.getVisualOrderText(), 8, imageHeight - 94, 0xFF404040, false);
        menu.chestInv.freq.ownerName().ifPresent(name -> {
            graphics.text(font, name.getVisualOrderText(), 170 - font.width(name), 6, 0xFF404040, false);
        });
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        Identifier texture = Identifier.withDefaultNamespace(menu.chestInv.getSize() == 0 ? "textures/gui/container/dispenser.png" : "textures/gui/container/generic_54.png");
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        switch (menu.chestInv.getSize()) {
            case 0:
            case 2:
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, imageWidth, imageHeight, 256, 256);
                break;
            case 1:
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, imageWidth, 71, 256, 256);
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + 71, 0, 126, imageWidth, 96, 256, 256);
                break;

        }
    }
}
