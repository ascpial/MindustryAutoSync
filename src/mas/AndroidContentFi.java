package mas;

import android.net.Uri;
import arc.Core;
import arc.backend.android.AndroidApplication;
import arc.files.Fi;
import arc.util.ArcRuntimeException;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implements an Arc Fi on top of the Android Content Resolver system.
 * The main difference between this and Arc's implementation is persistent resource access support.
 */
public class AndroidContentFi extends Fi {
    private final Uri uri;

    // Builds an Arc's Fi from an Android Content Resolver URI.
    // Make sure the app can access the file (through persistent permissions for example).
    public AndroidContentFi(String rawUri) {
        this(Uri.parse(rawUri));
    }

    // Builds an Arc's Fi from an Android Content Resolver URI.
    // Make sure the app can access the file (through persistent permissions for example).
    public AndroidContentFi(Uri uri) {
        this.uri = uri;
    }

    @Override
    public InputStream read() {
        AndroidApplication app = (AndroidApplication) Core.app;
        try {
            return app.getContentResolver().openInputStream(this.uri);
        } catch (FileNotFoundException ex) {
            throw new ArcRuntimeException("Error reading file: " + this.uri, ex);
        }
    }

    @Override
    public OutputStream write(boolean append){
        AndroidApplication app = (AndroidApplication) Core.app;
        try{
            return app.getContentResolver().openOutputStream(this.uri, append ? "wa" : "rwt");
        }catch(Exception ex){
            throw new ArcRuntimeException("Error writing file: " + uri, ex);
        }
    }

    @Override
    public long lastModified(){
        return 0;
    }

    public boolean isDirectory() { return false; }
}
