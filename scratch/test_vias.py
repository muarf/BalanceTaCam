import math

METERS_PER_DEG_LAT = 111320.0

def get_orthogonal_vias(cam_lat, cam_lon, prev_lat, prev_lon, next_lat, next_lon, offsets=[35.0, 60.0, 90.0, 120.0]):
    # Local road direction
    d_lat = next_lat - prev_lat
    cos_lat = math.cos(math.radians(cam_lat))
    d_lon = (next_lon - prev_lon) * cos_lat
    norm = math.hypot(d_lat, d_lon)
    if norm == 0:
        return []
    
    t_lat = d_lat / norm
    t_lon = d_lon / norm
    
    # Orthogonal normal vector
    n_lat = -t_lon
    n_lon = t_lat
    
    vias = []
    for off in offsets:
        for side in [1.0, -1.0]:
            via_lat = cam_lat + (off * side * n_lat) / METERS_PER_DEG_LAT
            via_lon = cam_lon + (off * side * n_lon) / (METERS_PER_DEG_LAT * cos_lat)
            vias.append((via_lat, via_lon, off, side))
    return vias

# Test on Ferber camera (lat=48.866146, lon=2.4060144) with north-south road
vias = get_orthogonal_vias(48.866146, 2.4060144, 48.8655, 2.4060, 48.8670, 2.4060)
print(f"Generated {len(vias)} local orthogonal vias:")
for v in vias:
    d_m = math.hypot((v[0]-48.866146)*METERS_PER_DEG_LAT, (v[1]-2.4060144)*METERS_PER_DEG_LAT*math.cos(math.radians(48.866146)))
    print(f"  lat={v[0]:.6f}, lon={v[1]:.6f}, side={v[3]}, dist={d_m:.1f}m")
