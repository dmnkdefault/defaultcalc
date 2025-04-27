package com.defavlt.defaultcalc;

// Minecraft / Mojang Imports
import com.mojang.blaze3d.vertex.PoseStack; // GuiGraphics braucht es intern
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics; // Wichtig für das Zeichnen in neueren Versionen
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.components.EditBox; // Für Reflection benötigt

// NeoForge Imports
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
// import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext; // Ist jetzt weg/anders
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

// mXparser Imports (Version 5.0.7 oder 6.x.x in build.gradle!)
import org.mariuszgromada.math.mxparser.*;

// Reflection Import
import java.lang.reflect.Field;


@Mod(DefaultCalc.MODID)
public class DefaultCalc {

    public static final String MODID = "defaultcalc";

    public DefaultCalc(IEventBus modEventBus) {
        modEventBus.addListener(this::clientSetup);

        // --- Diese Zeile muss raus --- //
        // License.iConfirmNonCommercialUse("defavlt");

        NeoForge.EVENT_BUS.register(this);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        System.out.println("[" + MODID + "] Client Setup fertig!");
    }

    @SubscribeEvent
    public void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ChatScreen chatScreen) {

            EditBox inputField = null;
            String currentText = "";
            // --- Reflection für Chat Input ---
            try {
                // Versuche, das Feld namens "input" zu finden
                Field inputFieldReflection = ChatScreen.class.getDeclaredField("input");
                inputFieldReflection.setAccessible(true); // Zugriff erzwingen
                inputField = (EditBox) inputFieldReflection.get(chatScreen); // Feld holen und casten
                if (inputField != null) {
                    currentText = inputField.getValue().trim(); // Wert holen
                } else {
                    // Wenn das Feld aus irgendeinem Grund null ist, abbrechen
                    System.err.println("[" + MODID + "] Input field via reflection was null!");
                    return;
                }
            } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
                // Fehler abfangen, wenn Feld nicht existiert, Zugriff fehlschlägt oder Typ falsch ist
                System.err.println("[" + MODID + "] Failed to access ChatScreen.input field via reflection!");
                e.printStackTrace(); // Fehler im Log ausgeben
                return; // Bei Fehler abbrechen
            }
            // --- Ende Reflection ---

            // Nur fortfahren, wenn Text vorhanden ist und wie eine Formel aussieht
            if (!currentText.isEmpty() && currentText.matches(".*[+\\-*/^√].*") && currentText.matches("^[0-9.+\\-*/()\\s^√eE]+$")) { // 'e'/'E' für wissenschaftl. Notation erlaubt

                Expression expression = new Expression(currentText);
                String textToDraw;
                int color;

                // *** NEUE LOGIK: Erst Syntax prüfen, dann rechnen ***
                boolean syntaxOk = expression.checkSyntax(); // Prüft Syntax

                if (syntaxOk) {
                    // Syntax ist OK -> Rechnen
                    double result = expression.calculate(); // 'result' wird HIER deklariert
                    if (!Double.isNaN(result)) {
                        // Gültiges Ergebnis
                        textToDraw = "Ergebnis: " + formatResult(result);
                        color = 0xFFFFFF; // Weiß
                    } else {
                        // Berechnung ergab NaN (Fehler ODER gültiges NaN)
                        textToDraw = "[Ungültig]";
                        // Optional: Prüfe auf spezifische Fehlermeldungen von calculate(), falls vorhanden
                        // String errorMsg = expression.getErrorMessage(); // Falls calculate() auch Fehler setzt
                        // if (errorMsg != null && !errorMsg.isEmpty()) textToDraw = "[Fehler] " + errorMsg;
                        color = 0xFF5555; // Rot
                    }
                } else {
                    // Syntax ist NICHT OK
                    textToDraw = "[Syntax Fehler]";
                    // Optional: Detailliertere Meldung holen
                    // textToDraw = "[Syntax] " + expression.getErrorMessage();
                    color = 0xFF5555; // Rot
                }
                // *** Ende Neue Logik ***

                // --- Rendern ---
                Font font = mc.font;
                GuiGraphics guiGraphics = event.getGuiGraphics();
                int screenHeight = mc.getWindow().getGuiScaledHeight();
                int chatInputBottomY = screenHeight - 14;
                int yPos = chatInputBottomY - font.lineHeight - 4;
                int xPos = 5;
                // Stelle sicher, dass textToDraw nicht null ist (sollte durch Logik oben abgedeckt sein)
                if (textToDraw != null) {
                    guiGraphics.drawString(font, textToDraw, xPos, yPos, color, true);
                }
                // --- Ende Rendern ---
            }
        }
    }

    // formatResult Methode bleibt gleich
    private String formatResult(double number) {
        if (number == (long) number) { return String.format("%d", (long) number); }
        else {
            if (Math.abs(number) < 1E-4 || Math.abs(number) > 1E10 && number != 0) { return String.format("%.4G", number); }
            else { return String.format("%.4f", number).replaceAll("\\.?0*$", ""); }
        }
    }
}