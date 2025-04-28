package com.defavlt.defaultcalc;

// NeoForge-Bus & Lifecycle
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

// Minecraft & Chat-Ausgabe
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

// Brigadier für den Command
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;

// mXparser für die Mathematik
import org.jetbrains.annotations.NotNull;
import org.mariuszgromada.math.mxparser.Expression;

@Mod(DefaultCalc.MODID)
public class DefaultCalc {
    public static final String MODID = "defaultcalc";

    public DefaultCalc(IEventBus bus) {
        // Listener für das Client-Setup
        bus.addListener(this::onClientSetup);
        // NeoForge-Eventbus für client-seitige Commands
        NeoForge.EVENT_BUS.register(this);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // hier kann weiterer Client-Init-Code stehen, wenn nötig
    }

    /**
     * Registriert den rein client-seitigen /dc-Command.
     * Der Server merkt nichts davon.
     */
    @SubscribeEvent
    public void onRegisterClientCommands(@NotNull RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("dc")
                        .then(Commands.argument("expr", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    // Ausdruck auslesen
                                    String expr = ctx.getArgument("expr", String.class).trim();
                                    Expression e = new Expression(expr);

                                    // Syntax & Berechnung
                                    String reply;
                                    if (!e.checkSyntax()) {
                                        reply = "[Syntax Fehler]";
                                    } else {
                                        double result = e.calculate();
                                        reply = Double.isNaN(result)
                                                ? "[Ungültig]"
                                                : "Ergebnis: " + formatResult(result);
                                    }

                                    // Ausgabe im Client-Chat
                                    LocalPlayer player = Minecraft.getInstance().player;
                                    if (player != null) {
                                        player.displayClientMessage(Component.literal(reply), false);
                                    }

                                    return 1;
                                })
                        )
        );
    }

    /** Formatiert Zahlen: Ganzzahl vs. Dezimal / wissenschaftliche Notation */
    private static String formatResult(double number) {
        if (number == (long) number) {
            return String.format("%d", (long) number);
        } else if (Math.abs(number) < 1E-4 || (Math.abs(number) > 1E10 && number != 0)) {
            return String.format("%.4G", number);
        } else {
            return String.format("%.4f", number).replaceAll("\\.?0*$", "");
        }
    }
}
