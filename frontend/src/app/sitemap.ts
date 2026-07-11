import type { MetadataRoute } from "next";
import { getEnv } from "@/config/env";

export default function sitemap(): MetadataRoute.Sitemap {
  const baseUrl = getEnv().NEXT_PUBLIC_APP_URL;
  const now = new Date();

  const staticRoutes = ["", "/finance", "/hr", "/inventory", "/manufacturing", "/procurement", "/sales"];

  return staticRoutes.map((route) => ({
    url: `${baseUrl}${route}`,
    lastModified: now,
    changeFrequency: "weekly",
    priority: route === "" ? 1 : 0.7,
  }));
}
