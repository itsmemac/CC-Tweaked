/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import com.google.common.annotations.VisibleForTesting;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.TickScheduler;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMonitor
{
    private final TileMonitor origin;

    private final boolean colour;
    private int textScale = 2;
    private @Nullable Terminal terminal;
    private final AtomicBoolean resized = new AtomicBoolean( false );
    private final AtomicBoolean changed = new AtomicBoolean( false );

    ServerMonitor( boolean colour, TileMonitor origin )
    {
        this.colour = colour;
        this.origin = origin;
    }

    synchronized void rebuild()
    {
        Terminal oldTerm = getTerminal();
        int oldWidth = oldTerm == null ? -1 : oldTerm.getWidth();
        int oldHeight = oldTerm == null ? -1 : oldTerm.getHeight();

        double textScale = this.textScale * 0.5;
        int termWidth = (int) Math.max(
            Math.round( (origin.getWidth() - 2.0 * (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN)) / (textScale * 6.0 * TileMonitor.RENDER_PIXEL_SCALE) ),
            1.0
        );
        int termHeight = (int) Math.max(
            Math.round( (origin.getHeight() - 2.0 * (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN)) / (textScale * 9.0 * TileMonitor.RENDER_PIXEL_SCALE) ),
            1.0
        );

        if( terminal == null )
        {
            terminal = new Terminal( termWidth, termHeight, colour, this::markChanged );
            markChanged();
        }
        else
        {
            terminal.resize( termWidth, termHeight );
        }

        if( oldWidth != termWidth || oldHeight != termHeight )
        {
            terminal.clear();
            resized.set( true );
            markChanged();
        }
    }

    private void markChanged()
    {
        if( !changed.getAndSet( true ) ) TickScheduler.schedule( origin.tickToken );
    }

    int getTextScale()
    {
        return textScale;
    }

    synchronized void setTextScale( int textScale )
    {
        if( this.textScale == textScale ) return;
        this.textScale = textScale;
        rebuild();
    }

    boolean pollResized()
    {
        return resized.getAndSet( false );
    }

    boolean pollTerminalChanged()
    {
        return changed.getAndSet( false );
    }

    @Nullable
    @VisibleForTesting
    public Terminal getTerminal()
    {
        return terminal;
    }
}
