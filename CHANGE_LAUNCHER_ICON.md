# How to Change Launcher Icon to Your SVG Logo

## Method 1: Using Android Studio (Recommended)

1. **Open Android Studio**
2. **Right-click on `app` folder** in Project view
3. Select **New → Image Asset**
4. In the **Asset Studio** window:
   - **Icon Type**: Select "Launcher Icons (Adaptive and Legacy)"
   - **Foreground Layer**:
     - Click the **Path** button
     - Navigate to: `F:\Programme\assets\logo.svg`
     - Select your SVG file
   - **Background Layer**: 
     - Choose a color (or use "Image" if you have a background)
     - Default is white, which should work
   - **Legacy Icon**: Check "Generate" to create PNG versions for older Android versions
5. Click **Next** → **Finish**
6. Android Studio will automatically:
   - Convert SVG to VectorDrawable
   - Generate all required PNG sizes
   - Update the icon files in all mipmap folders

## Method 2: Manual Conversion (If Method 1 doesn't work)

If Android Studio's Image Asset Studio doesn't work with your SVG:

1. **Convert SVG to PNG** using an online tool or Inkscape:
   - Export at these sizes:
     - 48x48 (mdpi)
     - 72x72 (hdpi)
     - 96x96 (xhdpi)
     - 144x144 (xxhdpi)
     - 192x192 (xxxhdpi)
   
2. **Replace the PNG files**:
   - `app/src/main/res/mipmap-mdpi/ic_launcher.png` → 48x48 PNG
   - `app/src/main/res/mipmap-hdpi/ic_launcher.png` → 72x72 PNG
   - `app/src/main/res/mipmap-xhdpi/ic_launcher.png` → 96x96 PNG
   - `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` → 144x144 PNG
   - `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` → 192x192 PNG

3. **For Adaptive Icons** (Android 8.0+):
   - Convert SVG to VectorDrawable format
   - Update `app/src/main/res/drawable/ic_launcher_foreground.xml`

## Method 3: Using Online Tools

1. Go to https://icon.kitchen/ or https://romannurik.github.io/AndroidAssetStudio/
2. Upload your SVG file
3. Download the generated icon pack
4. Extract and copy files to the appropriate mipmap folders

## After Changing the Icon

1. **Clean and Rebuild** the project:
   - Build → Clean Project
   - Build → Rebuild Project

2. **Uninstall and Reinstall** the app on your device/emulator to see the new icon

3. **Clear app data** if the icon doesn't update:
   - Settings → Apps → Traccar Client → Clear Data

## Notes

- The SVG file you provided is quite complex with many paths
- Android Studio's Image Asset Studio should handle it automatically
- If conversion fails, you may need to simplify the SVG or use a PNG version
- Make sure the icon looks good at small sizes (48x48px)
