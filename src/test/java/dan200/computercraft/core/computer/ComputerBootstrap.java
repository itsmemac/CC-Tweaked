/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.core.computer.mainthread.MainThread;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.test.core.computer.BasicEnvironment;
import dan200.computercraft.test.core.filesystem.MemoryMount;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Helper class to run a program on a computer.
 */
public class ComputerBootstrap
{
    private static final int TPS = 20;
    public static final int MAX_TIME = 10;

    public static void run( String program, Consumer<Computer> setup, int maxTimes )
    {
        MemoryMount mount = new MemoryMount()
            .addFile( "test.lua", program )
            .addFile( "startup.lua", "assertion.assert(pcall(loadfile('test.lua', nil, _ENV))) os.shutdown()" );

        run( mount, setup, maxTimes );
    }

    public static void run( String program, int maxTimes )
    {
        run( program, x -> {}, maxTimes );
    }

    public static void run( IWritableMount mount, Consumer<Computer> setup, int maxTicks )
    {
        ComputerCraft.logComputerErrors = true;
        ComputerCraft.maxMainComputerTime = ComputerCraft.maxMainGlobalTime = Integer.MAX_VALUE;

        Terminal term = new Terminal( ComputerCraft.computerTermWidth, ComputerCraft.computerTermHeight, true );
        MainThread mainThread = new MainThread();
        BasicEnvironment environment = new BasicEnvironment( mount );
        ComputerContext context = new ComputerContext( environment, 1, mainThread );
        final Computer computer = new Computer( context, environment, term, 0 );

        AssertApi api = new AssertApi();
        computer.addApi( api );

        setup.accept( computer );

        try
        {
            computer.turnOn();
            boolean everOn = false;

            for( int tick = 0; tick < TPS * maxTicks; tick++ )
            {
                long start = System.currentTimeMillis();

                computer.tick();
                mainThread.tick();

                if( api.message != null )
                {
                    ComputerCraft.log.debug( "Shutting down due to error" );
                    computer.shutdown();
                    Assertions.fail( api.message );
                    return;
                }

                long remaining = (1000 / TPS) - (System.currentTimeMillis() - start);
                if( remaining > 0 ) Thread.sleep( remaining );

                // Break if the computer was once on, and is now off.
                everOn |= computer.isOn();
                if( (everOn || tick > TPS) && !computer.isOn() ) break;
            }

            if( computer.isOn() || !api.didAssert )
            {
                StringBuilder builder = new StringBuilder().append( "Did not correctly [" );
                if( !api.didAssert ) builder.append( " assert" );
                if( computer.isOn() ) builder.append( " shutdown" );
                builder.append( " ]\n" );

                for( int line = 0; line < 19; line++ )
                {
                    builder.append( String.format( "%2d | %" + term.getWidth() + "s |\n", line + 1, term.getLine( line ) ) );
                }

                computer.shutdown();
                Assertions.fail( builder.toString() );
            }
        }
        catch( InterruptedException ignored )
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            try
            {
                context.ensureClosed( 1, TimeUnit.SECONDS );
            }
            catch( InterruptedException e )
            {
                throw new IllegalStateException( "Runtime thread was interrupted", e );
            }
        }
    }

    public static class AssertApi implements ILuaAPI
    {
        boolean didAssert;
        String message;

        @Override
        public String[] getNames()
        {
            return new String[] { "assertion" };
        }

        @LuaFunction
        public final void log( IArguments arguments )
        {
            ComputerCraft.log.info( "[Computer] {}", Arrays.toString( arguments.getAll() ) );
        }

        @LuaFunction( "assert" )
        public final Object[] doAssert( IArguments arguments ) throws LuaException
        {
            didAssert = true;

            Object arg = arguments.get( 0 );
            if( arg == null || arg == Boolean.FALSE )
            {
                message = arguments.optString( 1, "Assertion failed" );
                throw new LuaException( message );
            }

            return arguments.getAll();
        }
    }
}
