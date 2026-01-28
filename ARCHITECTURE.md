# Architecture Overview

## Application Structure

The Spacemaker app follows Android best practices and uses a simple, maintainable architecture.

### Layer Architecture

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│  (Activities, UI Components)        │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Business Logic Layer        │
│  (ARCore Session, Point Processing) │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│            Data Layer               │
│  (File System, Point Cloud Data)    │
└─────────────────────────────────────┘
```

## Core Components

### MainActivity
**Responsibility**: App entry point and permission management

**Key Functions**:
- Check ARCore availability on device
- Request and manage camera permissions
- Navigate to AR scanning activity

**Dependencies**:
- ARCore SDK for device compatibility check
- Android permission system

### ARScanActivity
**Responsibility**: AR scanning and point cloud capture

**Key Functions**:
- Initialize and manage ARCore session
- Render AR camera feed using OpenGL ES
- Capture 3D point cloud data from AR frames
- Export scanned data to files

**Dependencies**:
- ARCore for spatial tracking
- OpenGL ES for rendering
- Android file system for data storage

**Lifecycle**:
```
onCreate() → Initialize AR Session
    ↓
onResume() → Start AR Session
    ↓
onDrawFrame() → Capture Points (Loop)
    ↓
onPause() → Pause AR Session
    ↓
onDestroy() → Clean up AR Session
```

### BlueprintUtils
**Responsibility**: Point cloud data processing utilities

**Key Functions**:
- Calculate bounding boxes
- Compute spatial dimensions
- Filter and downsample point clouds
- Distance calculations

## Data Flow

```
AR Camera Feed
    ↓
ARCore Processing
    ↓
Point Cloud Generation
    ↓
Confidence Filtering
    ↓
Storage in Memory
    ↓
Export to File System
```

## File Format

### XYZ Point Cloud Format
The app exports data in the standard XYZ ASCII format:

```
# Header comments (optional)
X1 Y1 Z1
X2 Y2 Z2
...
Xn Yn Zn
```

**Advantages**:
- Simple and widely supported
- Human-readable
- Compatible with most 3D software
- Easy to parse and process

## Threading Model

- **Main Thread**: UI updates, user interactions
- **GL Thread**: OpenGL rendering, ARCore frame processing
- **IO Thread**: File writing operations (handled by Kotlin coroutines implicitly)

## Memory Management

- Point clouds stored in ArrayList for flexibility
- Points filtered by confidence to reduce memory usage
- Session cleanup in onDestroy to prevent leaks
- Point cloud cleared when activity is destroyed

## Security Considerations

- Camera permission required before AR access
- Files saved to app-specific directory (scoped storage)
- No network permissions requested
- No user data collection

## Scalability

Current limitations and future improvements:

**Current**:
- Single-session scanning
- In-memory point cloud storage
- Simple XYZ export

**Future Enhancements**:
- Multi-session scanning and stitching
- Database storage for large scans
- Additional export formats (PLY, OBJ, IFC)
- Cloud sync capabilities
- Real-time mesh generation

## Testing Strategy

### Unit Tests
- BlueprintUtils functions (bounding box, dimensions)
- Data filtering and downsampling
- File format generation

### Integration Tests
- Permission flow
- ARCore session lifecycle
- File I/O operations

### Manual Testing
- AR scanning on various devices
- Different lighting conditions
- Various room sizes and layouts
- Point cloud quality verification

## Dependencies

### Core
- Kotlin 1.9.0
- AndroidX Core KTX 1.12.0

### AR
- Google ARCore 1.41.0

### UI
- Material Components 1.11.0
- ConstraintLayout 2.1.4

### Camera
- CameraX 1.3.1

### Architecture Components
- Lifecycle 2.7.0
- ViewModel 2.7.0
- LiveData 2.7.0
