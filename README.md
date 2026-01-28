# Spacemaker

A powerful 3D Android AR scanning application for creating accurate floorplans, blueprints, and architectural documentation using augmented reality technology.

## Features

- **AR-Powered Scanning**: Utilize Google ARCore for real-time 3D space scanning
- **Point Cloud Capture**: Capture millions of 3D points to accurately represent physical spaces
- **Floorplan Generation**: Convert AR scan data into structured floorplan formats
- **Blueprint Export**: Save scans as XYZ point cloud files for further processing
- **Real-time Feedback**: Live point count and scanning status updates
- **Simple Interface**: Intuitive UI for easy scanning and data management

## Requirements

- Android device with ARCore support (Android 7.0 / API Level 24 or higher)
- Camera permission
- ARCore services installed (automatically prompted if not present)

## Installation

### Prerequisites

1. Android Studio Arctic Fox or later
2. Android SDK 34 or higher
3. Gradle 8.1.0 or higher
4. JDK 17

### Build Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/Kaleaon/Spacemaker.git
   cd Spacemaker
   ```

2. Open the project in Android Studio

3. Sync Gradle files

4. Build and run on an ARCore-compatible device:
   ```bash
   ./gradlew assembleDebug
   ```

## Usage

### Starting a Scan

1. Launch the Spacemaker app
2. Grant camera permissions when prompted
3. Tap "Start AR Scan" button
4. Move your device slowly around the space you want to scan
5. The app will automatically capture 3D points as you move

### Scanning Tips

- Move slowly and steadily for best results
- Ensure good lighting conditions
- Point the camera at different angles to capture all surfaces
- Watch the point count to track scanning progress
- Scan walls, floors, and ceilings for complete floorplans

### Saving Blueprints

1. Once scanning is complete, tap "Stop Scan"
2. Review the captured point count
3. Tap "Save Blueprint" to export the data
4. Files are saved in XYZ format in the app's documents directory
5. File naming format: `blueprint_<timestamp>.xyz`

### Blueprint File Format

The exported files use the XYZ point cloud format:
```
# Spacemaker Blueprint
# Points: <count>
# Format: X Y Z
<x1> <y1> <z1>
<x2> <y2> <z2>
...
```

## Architecture

### Technologies Used

- **Kotlin**: Primary programming language
- **Google ARCore**: AR framework for spatial tracking and point cloud generation
- **AndroidX**: Modern Android development components
- **Material Components**: Google's Material Design UI components
- **Camera2 API**: Camera access and control
- **OpenGL ES 2.0**: Rendering engine for AR visualization

### Project Structure

```
app/
├── src/main/
│   ├── java/com/kaleaon/spacemaker/
│   │   ├── MainActivity.kt           # Main entry point and permissions
│   │   └── ARScanActivity.kt         # AR scanning and point cloud capture
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml     # Main screen layout
│   │   │   └── activity_ar_scan.xml  # AR scanning screen layout
│   │   ├── values/
│   │   │   ├── strings.xml           # String resources
│   │   │   ├── colors.xml            # Color definitions
│   │   │   └── themes.xml            # App themes
│   │   └── mipmap/                   # App icons
│   └── AndroidManifest.xml           # App configuration and permissions
├── build.gradle                       # Module-level build configuration
└── proguard-rules.pro                # ProGuard rules
```

### Key Components

#### MainActivity
- Handles app initialization
- Checks ARCore availability
- Manages camera permissions
- Launches AR scanning activity

#### ARScanActivity
- Manages ARCore session
- Implements OpenGL rendering
- Captures 3D point cloud data
- Handles scan controls and export

## Data Export

Scanned blueprints are saved to:
```
/Android/data/com.kaleaon.spacemaker/files/Documents/
```

These files can be:
- Imported into CAD software
- Processed with point cloud tools
- Converted to other 3D formats
- Used for architectural visualization

## Permissions

The app requires the following permissions:
- **Camera**: Required for AR scanning
- **Storage**: For saving blueprint files (Android 9 and below)

## Known Limitations

- Requires ARCore-compatible device
- Best results in well-lit environments
- Large scans may consume significant memory
- Point cloud accuracy depends on device sensors

## Future Enhancements

- 2D floorplan visualization from point cloud
- Multi-format export (PLY, OBJ, IFC)
- Room detection and labeling
- Measurement tools
- Cloud storage integration
- Collaborative scanning

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License

This project is available for educational and commercial use.

## Support

For ARCore support and compatible devices, visit:
https://developers.google.com/ar/devices

## Acknowledgments

- Google ARCore team for the AR framework
- Android development community
- Material Design guidelines