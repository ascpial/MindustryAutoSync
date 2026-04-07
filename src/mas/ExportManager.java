package mas;

import arc.Core;
import arc.Files;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.io.Streams;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static arc.Core.settings;
import static mindustry.Vars.*;

public class ExportManager {
    public static final String lastSyncedFilename = "mas_last_synced.txt";

    /**
     * Returns the date of the last recorded save sync.
     * Returns 0 if we never recorded a save sync.
     * @return Last save sync, or 0 if never recorded
      */
    public long getLastSynced() {
        Fi dataDir = Core.settings.getDataDirectory();
        Fi lastModified = dataDir.child(lastSyncedFilename);
        if (lastModified.exists()) {
            try {
                return Long.parseLong(lastModified.readString());
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * Sets the date of the last recorded save sync, in a file in the save folder.
     * @param newValue The new last save sync
     */
    public void setLastSynced(long newValue) {
        Fi dataDir = Core.settings.getDataDirectory();
        Fi lastModified = dataDir.child(lastSyncedFilename);
        lastModified.writeString(Long.toString(newValue));
    }

    /**
     * Checks if MAS export is currently enabled.
     */
    public boolean isExportEnabled() {
        return Core.settings.getBool("mas-enable-export") && !getSyncLocation().isEmpty();
    }

    /**
     * Checks if MAS import is currently enabled.
     */
    public boolean isImportEnabled() {
        return Core.settings.getBool("mas-enable-import") && !getSyncLocation().isEmpty();
    }

    /**
     * Gets the current path / URI to the sync file.
     */
    public String getSyncLocation() {
        return Core.settings.getString("mas-export-location");
    }

    /**
     * Gets an Arc Fi for the currently set up file.
     * Returns null if permissions are not sufficient to access the file.
     */
    @Nullable
    public Fi getSyncFile() {
        // TODO: more reliable issue catching
        if (android) {
            return new AndroidContentFi(getSyncLocation());
        } else {
            return Core.files.get(
                    getSyncLocation(),
                    Files.FileType.absolute
            );
        }
    }

    public ExportManager() {}

    /**
     * Stores an Arc setting, for transient backup.
     */
    public static class Setting {
        private final String key;
        private final Object value;
        public Setting(String key) {
            this.key = key;
            this.value = Core.settings.get(key, Core.settings.getDefault(key));
        }

        public void apply() {
            Core.settings.put(this.key, this.value);
        }
    }

    /**
     * Imports a MAS export.
     * The main difference with Mindustry's implementation of this function is setting backup (avoid changing device
     * settings such as scaling) and last sync check.
     * Also checks that MAS is currently enabled.
     * @param file The file to import from.
     * @return Last synced timestamp contained in the provided ZIP, or 0 if no file has been imported
     * @throws IllegalArgumentException When the provided file is not valid.
     */
    public boolean importData(Fi file) throws IllegalArgumentException {
        if (!isImportEnabled()) {
            return false;
        }
        //filter and save the current settings (graphical, keybinds...) and keep the non-gameplay related ones
        Seq<Setting> settings_backup = new Seq<>();
        for (String setting : Core.settings.keys()) {
            if (!setting.startsWith("req-") && !setting.contains("serpulo") && !setting.contains("erekir")
                    && !setting.contains("unlocked") && !setting.contains("campaign") && !setting.contains("last")) {
                settings_backup.add(new Setting(setting));
            }
        }

        Fi dest = Core.files.local("zipdata.zip");
        if (dest.exists()) dest.delete();
        try {
            file.copyTo(dest);
        } catch (ArcRuntimeException e) { // We probably cannot access the original file
            return false;
        }
        Fi zipped = new ZipFi(dest);

        Fi base = Core.settings.getDataDirectory();
        if(!zipped.child("settings.bin").exists()){
            throw new IllegalArgumentException("Not valid save data.");
        }
        Fi lastSyncedFi = zipped.child(lastSyncedFilename);
        long lastSynced;
        if (!lastSyncedFi.exists()) {
            throw new IllegalArgumentException("Not a valid MAS save data (" + lastSyncedFilename + " missing).");
        }
        try {
            String rawLastModified = lastSyncedFi.readString().trim();
            lastSynced = Long.parseLong(rawLastModified);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a valid MAS save data (cannot read last modified timestamp).");
        }
        if (lastSynced <= getLastSynced()) {
            Log.info("MAS export is up to date, not importing.");
            dest.delete();
            return false;
        }

        //delete old saves so they don't interfere
        saveDirectory.deleteDirectory();

        //purge existing tmp data, keep everything else
        tmpDirectory.deleteDirectory();

        try {
            zipped.walk(f -> f.copyTo(base.child(f.path())));
        } catch (Exception e) {
            // TODO: What if files fail, permissions are messed up?...
            e.printStackTrace();
        }
        dest.delete();

        //clear old data
        settings.clear();
        //load data so it's saved on exit
        settings.load();
        //load previous settings
        for (Setting setting : settings_backup) {
            setting.apply();
        }

        setLastSynced(lastSynced);
        Log.info("MAS export was older, save imported.");

        return true;
    }

    public boolean importData() throws IllegalArgumentException {
        return importData(getSyncFile());
    }

    /**
     * Exports Mindustry data to an MAS sync file.
     * The main difference with Mindustry implementation of this function is the inclusion of the last synced file.
     * Doesn't check that MAS is currently enabled.
     * @param file The file to export to.
     * @throws IOException When the file cannot be written to.
     */
    public void exportData(Fi file) throws IOException {
        if (!isExportEnabled()) return;
        setLastSynced(System.currentTimeMillis());
        Seq<Fi> files = new Seq<>();
        files.add(Core.settings.getSettingsFile());
        files.addAll(customMapDirectory.list());
        files.addAll(saveDirectory.list());
        // TODO: Implement smarter mod check (for example, blacklist MAS?)
        files.addAll(modDirectory.list());
        files.addAll(schematicDirectory.list());
        files.add(Core.settings.getDataDirectory().child(lastSyncedFilename));

        String base = Core.settings.getDataDirectory().path();

        //add directories
        for(Fi other : files.copy()){
            Fi parent = other.parent();
            while(!files.contains(parent) && !parent.equals(settings.getDataDirectory())){
                files.add(parent);
            }
        }

        try(OutputStream fos = file.write(false, 2048); ZipOutputStream zos = new ZipOutputStream(fos)){
            for(Fi add : files){
                String path = add.path().substring(base.length());
                if(add.isDirectory()) path += "/";
                //fix trailing / in path
                path = path.startsWith("/") ? path.substring(1) : path;
                zos.putNextEntry(new ZipEntry(path));
                if(!add.isDirectory()){
                    try(var stream = add.read()){
                        Streams.copy(stream, zos);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    public void exportData() throws IOException {
        exportData(getSyncFile());
    }
}
