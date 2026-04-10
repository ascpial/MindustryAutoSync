package mas;

import arc.Core;
import arc.Events;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.SettingsMenuDialog;

import java.io.IOException;

import static mindustry.Vars.ui;

public class MindustryAutomaticSync extends Mod {
    private final ExportManager em;

    // Message shown when game has finalized starting
    private String message;
    public MindustryAutomaticSync() {
        Core.settings.defaults("mas-export-location", "");
        Core.settings.defaults("mas-enable-export", false);
        Core.settings.defaults("mas-enable-import", false);

        em = new ExportManager();
        try {
            // TODO: Show warning if import failed, and most importantly the reason it failed
            boolean valid = em.importData();
            if (!valid) {
                message = "@mas-invalid-import";
            }
        } catch (IllegalArgumentException e) {
            message = Core.bundle.get("mas-invalid-import") + "\n" + e;
        }

        Events.on(EventType.SaveLoadEvent.class, e -> {
            // TODO: This is very ugly, but it's in place until I find a better way...
            // This will probably involve mixins, I need to figure this out
            Timer.schedule(() -> {
                try {
                    em.exportData();
                } catch (IOException exc) {
                    exc.printStackTrace();
                    ui.showException(exc);
                } catch (SecurityException exc) {
                    exc.printStackTrace();
                    ui.showInfo("@mas-insufficient-permissions");
                }
            }, 0.25F);
        });
        Events.on(EventType.DisposeEvent.class, e -> {
            try {
                em.exportData();
            } catch (IOException exc) {
                exc.printStackTrace();
                ui.showException(exc);
            } catch (SecurityException exc) {
                exc.printStackTrace();
                ui.showInfo("@mas-insufficient-permissions");
            }
        });
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if (message != null) {
                Vars.ui.showInfo(message);
            }
        });
    }

    @Override
    public void init() {
        Vars.ui.settings.addCategory(
                Core.bundle.get("mas-settings"),
                "mas-setting-category",
                this::buildSettings
        );
    }

    public void buildSettings(SettingsMenuDialog.SettingsTable table) {
        table.clearChildren();
        table.defaults().size(400f, 60f).left();
        SettingsMenuDialog.SettingsTable.AreaTextSetting exportLocation = new SettingsMenuDialog.SettingsTable.AreaTextSetting(
                "mas-export-location", "", (s) -> {
        }
        );
        exportLocation.add(table);
        table.button("@mas-choose-location", () -> {
                    FileManager.showFileChooser(false, Core.bundle.get("mas-choose-file"), uri -> {
                        System.out.printf("MAS: chosen export location: %s\n", uri);
                        Core.settings.put("mas-export-location", uri);
                        buildSettings(table);
                    }, "zip");
                })
                .padTop(3f).row();
        SettingsMenuDialog.SettingsTable.CheckSetting enableExport = new SettingsMenuDialog.SettingsTable.CheckSetting("mas-enable-export", false, (n) -> {
        });
        enableExport.add(table);
        SettingsMenuDialog.SettingsTable.CheckSetting loadIfChanged = new SettingsMenuDialog.SettingsTable.CheckSetting("mas-enable-import", false, (n) -> {
        });
        loadIfChanged.add(table);
        table.button(
                Core.bundle.get("settings.reset", "Reset to Defaults"),
                () -> {
                    Core.settings.remove("mas-export-location");
                    Core.settings.remove("mas-enable-export");
                    Core.settings.remove("mas-enable-import");
                    buildSettings(table);
                }
        ).growX().row();
    }
}
