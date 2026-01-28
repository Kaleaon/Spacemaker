# Sweet Home 3D Plugins - Detailed Guide

This document provides comprehensive information about essential Sweet Home 3D plugins, their features, and usage instructions.

## Table of Contents

1. [Advanced Editing Plugins](#advanced-editing-plugins)
2. [Lighting Plugins](#lighting-plugins)
3. [Utility Design Plugins](#utility-design-plugins)
4. [Import/Export Plugins](#importexport-plugins)
5. [Navigation and UI Plugins](#navigation-and-ui-plugins)
6. [Troubleshooting](#troubleshooting)

---

## Advanced Editing Plugins

### Advanced Editing Plugin

**Purpose**: Enhances object manipulation capabilities for precise design control.

**Features**:
- Rotate objects with exact angle input
- Resize multiple objects proportionally
- Move objects by exact coordinates
- Batch operations for repetitive tasks
- Mirror/flip objects horizontally or vertically
- Align multiple objects to a reference point

**How to Use**:
1. Select one or more objects
2. Access via `Edit > Advanced Editing` or toolbar button
3. Choose the transformation type
4. Enter precise values or use interactive controls
5. Apply changes

**Tips**:
- Use keyboard shortcuts for faster workflow
- Hold Shift for constrained transformations
- Group related objects before batch operations

---

### AutoDimensioning Plugin

**Purpose**: Automatically generates dimension lines for walls and objects.

**Features**:
- Auto-generate wall dimensions
- Create room area labels
- Add total floor area calculations
- Customize dimension line styles
- Export dimensions as text

**How to Use**:
1. Go to `Plan > AutoDimensioning`
2. Select dimension type (walls, rooms, or custom)
3. Choose formatting options
4. Click "Generate"

**Configuration Options**:
- Measurement units (metric/imperial)
- Decimal precision
- Font size and style
- Line thickness and color

---

### Object Tags Plugin

**Purpose**: Organize and manage objects with custom tags for filtering and bulk operations.

**Features**:
- Add custom tags to any object
- Filter view by tags
- Select all objects with specific tag
- Bulk edit tagged objects
- Export tag lists

**Use Cases**:
- Tag all "bedroom furniture" for easy selection
- Tag items by purchase status ("owned", "to buy")
- Tag by installation phase

---

## Lighting Plugins

### Sweet Home Lights Plugin

**Purpose**: Professional lighting design with advanced controls.

**Features**:
- **Light Strips**: Create LED strip lighting along paths
- **Light Grids**: Distribute multiple lights in patterns
- **Color Control**: Full RGB and color temperature settings
- **Brightness**: Precise lumen control
- **Light Types**: Spot, ambient, directional options

**How to Use**:

#### Creating Light Strips
1. Select `Lights > Create Light Strip`
2. Draw the path for the strip
3. Set properties:
   - Color (RGB or temperature in Kelvin)
   - Brightness
   - Number of light points
4. Apply

#### Creating Light Grids
1. Select `Lights > Create Light Grid`
2. Define the area
3. Set grid parameters:
   - Rows and columns
   - Spacing
   - Light properties
4. Generate

**Best Practices**:
- Use warm lights (2700K-3000K) for living areas
- Use cool lights (4000K-5000K) for workspaces
- Layer lighting: ambient + task + accent

---

### Automate Lights Rendering Plugin

**Purpose**: Streamline lighting setup and rendering for consistent results.

**Features**:
- Preset lighting configurations
- Batch render multiple views
- Time-of-day simulation
- Lighting templates for room types

**Templates Included**:
- Living room (evening ambiance)
- Kitchen (bright task lighting)
- Bedroom (soft relaxation)
- Office (productive daylight)
- Bathroom (bright and functional)

---

## Utility Design Plugins

### Wirings Plugin

**Purpose**: Design electrical wiring, plumbing, and cable routes in 3D.

**Features**:
- Draw 3D polylines for wiring/piping
- Snap to floor, walls, or ceiling
- Automatic measurement labels
- Different line types and colors
- Layer management for wire types

**Wire Types**:
| Type | Suggested Color | Usage |
|------|-----------------|-------|
| Electrical | Black/Red | Power lines |
| Low Voltage | Blue | Network, phone |
| Plumbing Hot | Red | Hot water |
| Plumbing Cold | Blue | Cold water |
| HVAC | Green | Ventilation |
| Gas | Yellow | Gas lines |

**How to Use**:
1. Select `Tools > Wirings > New Wire`
2. Choose wire type and color
3. Click points to define the route
4. Use snapping for precise placement:
   - Press `F` for floor snap
   - Press `W` for wall snap
   - Press `C` for ceiling snap
5. Double-click to finish

**Pro Tips**:
- Plan routes to minimize wire length
- Group related wires in conduits
- Use layers to show/hide different systems

---

### Stair Builder Plugin

**Purpose**: Create custom staircases with various configurations.

**Features**:
- Multiple stair types:
  - Straight stairs
  - L-shaped stairs
  - U-shaped stairs
  - Spiral stairs
  - Curved stairs
- Custom dimensions:
  - Rise and run
  - Width
  - Number of steps
  - Landing size
- Railing options
- Material customization

**How to Use**:
1. Select `3D View > Insert Stairs` or use toolbar
2. Choose stair type
3. Set dimensions:
   - Total height (floor to floor)
   - Stair width
   - Step depth (run)
4. Configure railings if needed
5. Place in floor plan
6. Adjust position and rotation

**Building Code Guidelines**:
- Standard rise: 7-8 inches (178-203 mm)
- Standard run: 10-11 inches (254-279 mm)
- Minimum width: 36 inches (914 mm)
- Maximum rise between landings: 12 feet (3.7 m)

---

### Roof Generator Plugin

**Purpose**: Automatically generate roof structures for your designs.

**Features**:
- Roof styles:
  - Gable roof
  - Hip roof
  - Mansard roof
  - Flat roof
  - Shed roof
  - Gambrel roof
- Customizable:
  - Pitch angle
  - Overhang
  - Material
  - Dormer windows

**How to Use**:
1. Complete your floor plan with walls
2. Select `3D View > Generate Roof`
3. Choose roof style
4. Set parameters:
   - Pitch (degrees or ratio)
   - Overhang distance
   - Ridge direction
5. Preview and adjust
6. Generate

---

## Import/Export Plugins

### SVG Import/Export Plugin

**Purpose**: Work with Scalable Vector Graphics for CAD compatibility.

**Features**:
- Import SVG floor plans
- Export designs as SVG
- Scale and position imported graphics
- Trace SVG to create walls

**Import Workflow**:
1. `File > Import > SVG File`
2. Select SVG file
3. Set scale (e.g., 1 pixel = 1 cm)
4. Position in plan
5. Use as reference or trace

**Export Workflow**:
1. `File > Export > SVG`
2. Choose elements to export
3. Set resolution and scale
4. Save file

---

### Plan Image Export Plugin

**Purpose**: Generate high-resolution floor plan images.

**Features**:
- Custom resolution (up to 10000x10000 pixels)
- Choose included elements
- Add grid overlay
- Include dimensions
- Transparent background option

**Export Options**:
| Option | Description |
|--------|-------------|
| Resolution | DPI setting for print quality |
| Format | PNG, JPEG, PDF |
| Elements | Walls, furniture, dimensions, etc. |
| Background | White, transparent, or custom color |

---

### OBJ/DAE Export Plugins

**Purpose**: Export 3D models for use in other applications.

**Supported Formats**:
- OBJ (Wavefront)
- DAE (Collada)
- 3DS (3D Studio)

**Use Cases**:
- Import into Blender for advanced rendering
- Use in game engines
- 3D printing (export furniture)
- AR/VR applications

---

## Navigation and UI Plugins

### Middle Mouse Panning

**Purpose**: Use middle mouse button for panning the view.

**Features**:
- Pan in 2D plan view
- Pan in 3D view
- Faster navigation workflow

---

### Object Replacement Plugin

**Purpose**: Replace multiple instances of an object at once.

**Features**:
- Find all instances of a furniture item
- Replace with different item
- Maintain position and rotation
- Batch replacement

**Use Case**: Upgrade all chairs in a design to a different model.

---

## Troubleshooting

### Common Plugin Issues

**Plugin Not Appearing After Installation**
1. Verify file is in correct plugins folder
2. Ensure file extension is `.sh3p`
3. Restart Sweet Home 3D completely
4. Check for version compatibility

**Plugin Conflicts**
1. Disable recently added plugins
2. Test one by one to identify conflict
3. Check for updates to both plugins

**Performance Issues with Many Plugins**
1. Disable unused plugins
2. Restart application
3. Consider increasing Java heap size

### Getting Help

- [Sweet Home 3D Forum](https://sweethome3d.com/forum/)
- [SourceForge Bug Tracker](https://sourceforge.net/p/sweethome3d/bugs/)
- [Plugin Developer Documentation](https://sweethome3d.com/pluginDeveloperGuide.jsp)

---

## Plugin Development

For developers interested in creating custom plugins:

- Language: Java
- API: Sweet Home 3D Plugin API
- Documentation: [Plugin Developer Guide](https://sweethome3d.com/pluginDeveloperGuide.jsp)

### Basic Plugin Structure

```java
package com.example.myplugin;

import com.eteks.sweethome3d.plugin.Plugin;
import com.eteks.sweethome3d.plugin.PluginAction;

public class MyPlugin extends Plugin {
    @Override
    public PluginAction[] getActions() {
        return new PluginAction[] {
            new MyPluginAction()
        };
    }
}
```

---

*Last Updated: January 2026*
