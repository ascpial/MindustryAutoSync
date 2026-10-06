package mas;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import arc.Core;
import arc.backend.android.AndroidApplication;
import arc.files.Fi;
import arc.func.Cons;
import mindustry.Vars;

public class FileManager {
    private static final String ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT";
    private static final String ACTION_CREATE_DOCUMENT = "android.intent.action.CREATE_DOCUMENT";
    private static final int FLAG_GRANT_PERSISTABLE_URI_PERMISSION = 0x00000040;
    private static final int FLAG_GRANT_PREFIX_URI_PERMISSION = 0x00000080;

    /**
     * Pick a file using a native system chooser.
     * On Android, this function makes the accessed URI persistent through the Content Resolver system.
     * @param open Whether to open or save a file
     * @param title The title of the native file dialog
     * @param cons File selection listener
     * @param extensions Extensions to filter
     */
    static void showFileChooser(boolean open, String title, Cons<String> cons, String... extensions) {
        String extension = extensions[0];
        if (Vars.android) {
                Intent intent = new Intent(open ? ACTION_OPEN_DOCUMENT : ACTION_CREATE_DOCUMENT);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | FLAG_GRANT_PREFIX_URI_PERMISSION);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(extension.equals("zip") && !open && extensions.length == 1 ? "application/zip" : "*/*");
            intent.putExtra(Intent.EXTRA_TITLE, "export." + extension);

            AndroidApplication app = (AndroidApplication) Core.app;

            app.addResultListener(i -> app.startActivityForResult(intent, i), (code, in) -> {
                if(code == Activity.RESULT_OK && in != null && in.getData() != null){
                    Uri uri = in.getData();

                    if(uri.getPath().contains("(invalid)")) return;

                    final int takeFlags = intent.getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    try {
                        app.getContentResolver().getClass()
                                .getMethod("takePersistableUriPermission", Uri.class, int.class)
                                .invoke(app.getContentResolver(), uri, takeFlags);
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException("Persistent access to the selected file is unavailable.", ex);
                    }

                    Core.app.post(() -> Core.app.post(() -> cons.get(uri.toString())));
                }
            });
        } else {
            Vars.platform.showFileChooser(open, title, extension, f -> {cons.get(f.absolutePath());});
        }
    }
}
