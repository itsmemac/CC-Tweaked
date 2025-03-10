/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.List;

public class NoTermComputerScreen<T extends ContainerComputerBase> extends Screen implements IHasContainer<T>
{
    private final T menu;
    private final Terminal terminalData;
    private WidgetTerminal terminal;

    public NoTermComputerScreen( T menu, PlayerInventory player, ITextComponent title )
    {
        super( title );
        this.menu = menu;
        terminalData = menu.getTerminal();
    }

    @Nonnull
    @Override
    public T getMenu()
    {
        return menu;
    }

    @Override
    protected void init()
    {
        passEvents = true; // Pass mouse vents through to the game's mouse handler.
        // First ensure we're still grabbing the mouse, so the user can look around. Then reset bits of state that
        // grabbing unsets.
        minecraft.mouseHandler.grabMouse();
        minecraft.screen = this;
        KeyBinding.releaseAll();

        super.init();
        minecraft.keyboardHandler.setSendRepeatsToGui( true );

        terminal = addWidget( new WidgetTerminal( terminalData, new ClientInputHandler( menu ), 0, 0 ) );
        terminal.visible = false;
        terminal.active = false;
        setFocused( terminal );
    }

    @Override
    public final void removed()
    {
        super.removed();
        minecraft.keyboardHandler.setSendRepeatsToGui( false );
    }

    @Override
    public final void tick()
    {
        super.tick();
        terminal.update();
    }

    @Override
    public boolean mouseScrolled( double pMouseX, double pMouseY, double pDelta )
    {
        minecraft.player.inventory.swapPaint( pDelta );
        return super.mouseScrolled( pMouseX, pMouseY, pDelta );
    }

    @Override
    public void onClose()
    {
        minecraft.player.closeContainer();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public final boolean keyPressed( int key, int scancode, int modifiers )
    {
        // Forward the tab key to the terminal, rather than moving between controls.
        if( key == GLFW.GLFW_KEY_TAB && getFocused() != null && getFocused() == terminal )
        {
            return getFocused().keyPressed( key, scancode, modifiers );
        }

        return super.keyPressed( key, scancode, modifiers );
    }

    @Override
    public void render( MatrixStack transform, int mouseX, int mouseY, float partialTicks )
    {
        super.render( transform, mouseX, mouseY, partialTicks );

        FontRenderer font = minecraft.font;
        List<IReorderingProcessor> lines = font.split( new TranslationTextComponent( "gui.computercraft.pocket_computer_overlay" ), (int) (width * 0.8) );
        float y = 10.0f;
        for( IReorderingProcessor line : lines )
        {
            font.drawShadow( transform, line, (float) ((width / 2) - (minecraft.font.width( line ) / 2)), y, 0xFFFFFF );
            y += 9.0f;
        }
    }
}
