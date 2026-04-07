# Mindustry Auto Sync

Mindustry Auto Sync (MAS for short) is a Java mod that allows you to export your save to a file on your system.

When the game starts, this mod also (optionally) checks the file, and, if it is newer, loads it seamlessly.

Using a file synchronization app like Google Drive or Syncthing, this means you can **synchronize your Mindustry save accross devices**.

The mod has been tested on **Linux** and **Android**.
The mod should also work on Windows and MacOS, though I never tested it.

iOS devices cannot be supported. It's not possible to load Java mods on iOS.
Even if it was possible, it requires iOS specific APIs and I don't have any Apple devices to test the mod on.

## Using the mod

First, I recommend installing a file synchronization app and making sure it works on both devices.
You need to synchronize a folder on desktop, and have it appear in the Files app on Android.

Once you got that working, I recommend following these steps, **in order** :

- **Make a backup of your save - things will go wrong.**
- Install the mod on device 1
- Setup the mod (in the settings) to export to your synchronized Drive folder (enable import and export)
- Quit the game and make sure the file has been created
- On device 2, in Mindustry, go to Settings > Game Data > Import Data, and import the save **from the synchronized folder** (this makes sure you have the right mod version and the right files)
- On device 2, go into Settings > Auto Sync and configure the mod to export to the right location (it will, at the start, use the location of your previous device

At this point, everything should be working!

Regularly make offline backup of your save. Issue will happen, and you don't want to lose your progress.

## Limitations

The most important limitation is that you cannot use two devices connected to the same save at the same moment.
If you open two games at the same time, the game that closes last will overwrite the exported file.

The file will only get imported when the game starts. If the save gets changed while the game is opened, it will likely get overwritten when the game closes.

iOS devices cannot, and will not, be supported.

## Neat things

Since you can choose to enable only export, you can use this mod to automate a backup of your file.

Since you can choose to enable only import, you can use the mod to create "read-only" devices, which won't export files and avoid collisions.

## Reporting issues

If you have an issue with the mod, don't hesitate to open an issue!

This will be especially useful for platforms I don't test on, such as Windows.

Feature requests are accepted, but as I feel like the mod is feature complete, they will probably get closed quickly.
