# OSM Tags Guide for Surveillance Cameras

This document provides a comprehensive guide to tagging surveillance cameras in OpenStreetMap.

## 🏷️ Mandatory Tags

Every surveillance camera **must** have these tags:

```
man_made=surveillance
surveillance:type=camera
```

## 📋 Optional Tags

### Camera Type (`camera:type`)

Describes the physical type of camera.

| Value | Description | Example |
|-------|-------------|---------|
| `fixed` | Fixed direction camera | Standard CCTV pointing in one direction |
| `dome` | Dome camera (360° coverage) | Ceiling-mounted dome cameras |
| `ptz` | Pan-Tilt-Zoom motorized camera | Remote-controlled rotating camera |
| `panoramic` | Panoramic/fisheye camera | 180-360° field of view camera |

**Example**:
```
camera:type=dome
```

### Camera Mount (`camera:mount`)

How the camera is physically mounted.

| Value | Description |
|-------|-------------|
| `pole` | Mounted on a dedicated pole |
| `wall` | Mounted on a building wall |
| `ceiling` | Mounted on a ceiling (indoor) |
| `street_lamp` | Mounted on a street lamp pole |

**Example**:
```
camera:mount=pole
```

### Camera Direction (`camera:direction`)

Direction the camera is pointing (in degrees, 0-360).

- **0° = North**
- **90° = East**
- **180° = South**
- **270° = West**

**Example**:
```
camera:direction=45
```

**Note**: Only applicable for fixed cameras, not domes or panoramic.

### Surveillance Type (`surveillance`)

General surveillance environment.

| Value | Description |
|-------|-------------|
| `public` | Public surveillance (accessible areas) |
| `outdoor` | Outdoor surveillance |
| `indoor` | Indoor surveillance |

**Example**:
```
surveillance=public
```

### Operator (`operator`)

Who operates/owns the camera.

**Examples**:
```
operator=City of Paris
operator=RATP
operator=Police Nationale
operator=Carrefour
```

### Operator Type (`operator:type`)

Type of organization operating the camera.

| Value | Description |
|-------|-------------|
| `public` | Public entity (government, city) |
| `private` | Private company/individual |
| `commercial` | Commercial establishment |

**Example**:
```
operator:type=public
```

### Surveillance Zone (`surveillance:zone`)

What area is being monitored.

| Value | Description |
|-------|-------------|
| `town` | General town/city surveillance |
| `parking` | Parking lot surveillance |
| `traffic` | Traffic monitoring |
| `building` | Building entrance/interior |
| `public_transport` | Public transport (station, bus) |
| `shop` | Shop/store surveillance |
| `bank` | Bank/ATM surveillance |
| `entrance` | Entrance monitoring |

**Example**:
```
surveillance:zone=parking
```

### Description (`description`)

Free-text additional information.

**Examples**:
```
description=Monitors the main square
description=ATM security camera
description=Traffic light camera
```

### Level (`level`)

Floor/level where camera is located (for indoor or multi-level).

**Examples**:
```
level=0     # Ground floor
level=1     # First floor
level=-1    # Basement
```

### Height (`height`)

Mounting height above ground.

**Examples**:
```
height=5 m
height=3.5 m
height=10 m
```

## 🎯 Complete Examples

### Example 1: Simple Public Camera

```
man_made=surveillance
surveillance:type=camera
camera:type=dome
camera:mount=pole
surveillance=public
```

### Example 2: Detailed Fixed Camera

```
man_made=surveillance
surveillance:type=camera
camera:type=fixed
camera:mount=wall
camera:direction=180
surveillance=public
operator=City of Lyon
operator:type=public
surveillance:zone=town
height=4 m
description=Monitors the town square
```

### Example 3: Traffic Camera

```
man_made=surveillance
surveillance:type=camera
camera:type=ptz
camera:mount=street_lamp
surveillance=outdoor
operator:type=public
surveillance:zone=traffic
description=Speed enforcement camera
```

### Example 4: Shop Security Camera

```
man_made=surveillance
surveillance:type=camera
camera:type=dome
camera:mount=ceiling
surveillance=indoor
operator=Carrefour
operator:type=commercial
surveillance:zone=shop
level=0
```

## 🔍 Finding Existing Cameras

### Overpass Turbo Query

Use [Overpass Turbo](https://overpass-turbo.eu/) to find cameras:

```overpassql
[out:json][timeout:25];
(
  node["man_made"="surveillance"]["surveillance:type"="camera"]({{bbox}});
  way["man_made"="surveillance"]["surveillance:type"="camera"]({{bbox}});
  relation["man_made"="surveillance"]["surveillance:type"="camera"]({{bbox}});
);
out body;
>;
out skel qt;
```

### JOSM Filter

In JOSM editor, use this filter:
```
man_made=surveillance surveillance:type=camera
```

## ✅ Good Practices

### DO

- ✅ Only map cameras you can verify in person
- ✅ Use the most specific tags available
- ✅ Add descriptions for unusual cameras
- ✅ Update outdated information
- ✅ Check for existing cameras before adding
- ✅ Use appropriate changeset comments

### DON'T

- ❌ Map cameras from satellite imagery alone
- ❌ Add fictional/planned cameras
- ❌ Include personal opinions in tags
- ❌ Add cameras on private property without verification
- ❌ Duplicate existing cameras
- ❌ Map military/sensitive security cameras

## 🚫 Privacy & Legal Considerations

### Mapping Guidelines

1. **Public Cameras Only**: Focus on cameras in public spaces
2. **Verify In Person**: Don't map from photos/videos
3. **Respect Privacy**: Avoid mapping sensitive locations
4. **Follow Local Laws**: Some countries restrict camera mapping

### Sensitive Locations

**Avoid mapping cameras at**:
- Military installations
- Government security facilities
- Embassies
- Nuclear facilities
- High-security prisons

### Legal Notice

This app and tagging scheme are for **informational and mapping purposes only**. Always respect:
- Local laws and regulations
- Privacy rights
- Private property
- Security concerns

## 📚 References

- [OSM Wiki: Tag:man_made=surveillance](https://wiki.openstreetmap.org/wiki/Tag:man_made%3Dsurveillance)
- [OSM Wiki: Surveillance](https://wiki.openstreetmap.org/wiki/Surveillance)
- [OSM Tagging Guidelines](https://wiki.openstreetmap.org/wiki/Tagging)
- [OSM Good Practice](https://wiki.openstreetmap.org/wiki/Good_practice)

## 🌍 Regional Variations

### France
Common operators: Police Municipale, RATP, SNCF, City names

### United Kingdom
Common operators: Metropolitan Police, Council names, Transport for London

### Germany
Common operators: Polizei, Stadt [city name], Deutsche Bahn

### United States
Common operators: Police Department, City names, DOT

## 🔄 Updating Existing Cameras

When you find outdated camera information:

1. Verify the camera still exists
2. Check all tags are current
3. Update changed tags (direction, type, etc.)
4. Add missing optional tags
5. Use changeset comment: "Updated surveillance camera information"

## 📊 Statistics & Analysis

### TagInfo

View tag usage statistics:
[taginfo.openstreetmap.org](https://taginfo.openstreetmap.org/tags/man_made=surveillance)

### Analytics

- Global cameras: 2M+
- Most common type: `dome`
- Most common mount: `pole`
- Most tagged country: France

## 🆘 Need Help?

- [OSM Help](https://help.openstreetmap.org/)
- [OSM Forum](https://community.openstreetmap.org/)
- [OSM France Forum](https://forum.openstreetmap.fr/)
- [Telegram OSM Groups](https://wiki.openstreetmap.org/wiki/Telegram)

---

**Remember**: Always map responsibly and respect local regulations! 🙏


