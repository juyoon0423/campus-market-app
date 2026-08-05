const FALLBACK_API_URL = "http://localhost:8080";

export function getImageUrl(imageUrl?: string | null): string | null {
  if (!imageUrl) {
    return null;
  }

  if (imageUrl.startsWith("http")) {
    return imageUrl;
  }

  const apiUrl = process.env.NEXT_PUBLIC_API_URL || FALLBACK_API_URL;
  const imagePath = imageUrl.startsWith("/") ? imageUrl : `/${imageUrl}`;
  return `${apiUrl}${imagePath}`;
}
