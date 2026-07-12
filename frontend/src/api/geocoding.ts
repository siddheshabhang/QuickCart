export async function reverseGeocode(lat: number, lon: number): Promise<string | null> {
  try {
    const response = await fetch(
      `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json`,
      {
        headers: {
          "Accept-Language": "en",
        },
      }
    );
    if (!response.ok) {
      return null;
    }
    const data = await response.json();
    return data.display_name || null;
  } catch (error) {
    console.error("Failed to reverse geocode:", error);
    return null;
  }
}
