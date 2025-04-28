package com.defavlt.defaultcalc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@Mod(DefaultCalc.MODID)
public class DefaultCalc {
    public static final String MODID = "defaultcalc";

    public DefaultCalc(IEventBus modEventBus) {
        modEventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        System.out.println("[" + MODID + "] client setup done");
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("c")
                        .then(Commands.argument("expr", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String expr = StringArgumentType.getString(ctx, "expr");

                                    try {
                                        ScriptEngineManager manager = new ScriptEngineManager();
                                        ScriptEngine engine = manager.getEngineByName("JavaScript");

                                        if (engine == null) {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("No JavaScript engine found!")
                                            );
                                            return 0;
                                        }

                                        Object result = engine.eval(expr);

                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("defaultCalc: " + result),
                                                false
                                        );
                                        return 1;

                                    } catch (ScriptException e) {
                                        ctx.getSource().sendFailure(
                                                Component.literal("defaultCalc (error): " + e.getMessage())
                                        );
                                        return 0;
                                    }
                                })
                        )
        );
    }
}