# Quick Start Guide

Get started with Spacemaker in 5 minutes!

## Prerequisites Check

Before you begin, ensure:
- ✅ Your Android device runs Android 7.0 (API 24) or higher
- ✅ Your device supports ARCore ([Check compatibility](https://developers.google.com/ar/devices))
- ✅ You have Android Studio installed (for building from source)

## Installation

### Option 1: Build from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/Kaleaon/Spacemaker.git
   cd Spacemaker
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the Spacemaker directory
   - Click "OK"

3. **Sync Gradle**
   - Wait for Android Studio to sync Gradle files
   - This may take a few minutes on first run

4. **Connect your device**
   - Enable Developer Options on your Android device
   - Enable USB Debugging
   - Connect via USB
   - Allow USB debugging when prompted

5. **Run the app**
   - Click the "Run" button (green triangle) in Android Studio
   - Select your device from the list
   - Wait for the app to install and launch

### Option 2: Install APK (Coming Soon)
Pre-built APK files will be available in the Releases section.

## First Scan

### 1. Launch the App
- Open Spacemaker from your app drawer
- You'll see the welcome screen

### 2. Grant Permissions
- Tap "Start AR Scan"
- Grant camera permission when prompted
- ARCore will initialize (may take a few seconds)

### 3. Start Scanning
- Point your camera at the space you want to scan
- Move slowly around the room
- The point count will increase as you scan
- Watch the status message for guidance

**Pro Tips:**
- Move steadily - don't rush
- Scan in good lighting
- Point at walls, floors, and ceilings
- Overlap your scans slightly

### 4. Stop and Save
- Tap "Stop Scan" when complete
- Review the point count
- Tap "Save Blueprint"
- Your scan is saved!

## Finding Your Scans

Scans are saved to:
```
/Android/data/com.kaleaon.spacemaker/files/Documents/
```

To access:
1. Connect your device to a computer via USB
2. Enable File Transfer mode
3. Navigate to the path above
4. Copy the `.xyz` files to your computer

## Using Your Scans

### Import into Software

**CloudCompare** (Free)
1. Download from [cloudcompare.org](https://www.cloudcompare.org)
2. Open CloudCompare
3. File → Open → Select your `.xyz` file
4. View and process your point cloud

**MeshLab** (Free)
1. Download from [meshlab.net](https://www.meshlab.net)
2. File → Import Mesh → Select your `.xyz` file
3. Process and convert to mesh

**Autodesk ReCap** (Commercial)
1. Import `.xyz` file
2. Process into 3D model
3. Export to CAD formats

### Manual Processing

The `.xyz` format is simple text:
```
# Comment
X1 Y1 Z1
X2 Y2 Z2
...
```

You can process with:
- Python (NumPy, Open3D)
- MATLAB
- R
- Custom scripts

## Troubleshooting

### "ARCore not supported" message
- Check if your device is on the [compatibility list](https://developers.google.com/ar/devices)
- Update your Android version if possible
- Ensure ARCore is installed from Google Play

### Camera permission denied
- Go to Settings → Apps → Spacemaker → Permissions
- Enable Camera permission
- Restart the app

### App crashes on start
- Ensure Android version is 7.0+
- Check ARCore is installed and up to date
- Clear app cache and data
- Reinstall the app

### Low point count
- Move more slowly
- Improve lighting
- Get closer to surfaces
- Ensure surfaces have texture (not plain white walls)

### Can't find saved files
- Use a file manager app on your device
- Look in: `/Android/data/com.kaleaon.spacemaker/files/Documents/`
- Or connect to PC and browse via USB

## Tips for Best Results

### Lighting
- ✅ Bright, even lighting
- ✅ Natural daylight works well
- ❌ Avoid direct sunlight
- ❌ Avoid very dim rooms

### Movement
- ✅ Slow, steady movement
- ✅ Smooth panning
- ❌ Quick jerky movements
- ❌ Sudden rotations

### Scanning Strategy
1. Start in one corner
2. Scan walls from floor to ceiling
3. Move around the perimeter
4. Scan obstacles and furniture
5. Return to starting point for closure

### Room Types

**Easy to Scan:**
- Regular shaped rooms
- Textured walls
- Good lighting
- Few reflective surfaces

**Challenging:**
- Large open spaces
- Glass walls
- Very dark rooms
- Highly reflective surfaces

## Next Steps

- Read the full [README.md](README.md) for detailed information
- Check out [ARCHITECTURE.md](ARCHITECTURE.md) to understand the code
- See [CONTRIBUTING.md](CONTRIBUTING.md) to contribute

## Support

- GitHub Issues: [Report bugs](https://github.com/Kaleaon/Spacemaker/issues)
- Discussions: [Ask questions](https://github.com/Kaleaon/Spacemaker/discussions)

Happy scanning! 🏗️📐
