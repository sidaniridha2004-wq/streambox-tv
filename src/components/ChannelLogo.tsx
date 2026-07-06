import { useEffect, useState } from "react";
import { categoryColorForGroup } from "@/lib/channel-category";

interface Props {
  src?: string;
  name: string;
  /** Original channel group label; used to pick the fallback color. */
  group?: string;
  className?: string;
  size?: number;
}

function firstLetter(name: string): string {
  const clean = name.replace(/\b(HD|FHD|4K|SD)\b/gi, "").trim();
  return (clean[0] || name[0] || "?").toUpperCase();
}

export function ChannelLogo({ src, name, group, className = "", size = 48 }: Props) {
  const [broken, setBroken] = useState(!src);
  // Reset the broken flag when the source URL changes so a fresh URL
  // (e.g. after an admin logo edit) gets a chance to load.
  useEffect(() => { setBroken(!src); }, [src]);
  const color = categoryColorForGroup(group ?? "");
  const style = { width: size, height: size };

  if (broken || !src) {
    return (
      <div
        className={`relative flex items-center justify-center overflow-hidden rounded-xl bg-black ${className}`}
        style={style}
        aria-label={name}
      >
        <img 
          src="/default-channel.png" 
          alt="Default Channel" 
          className="h-full w-full object-cover"
        />
        {/* Overlay a subtle tint based on the category color so channels still look slightly distinct */}
        <div 
          className="absolute inset-0 opacity-20 mix-blend-color" 
          style={{ backgroundColor: color }} 
        />
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={name}
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setBroken(true)}
      className={className}
      style={{
        ...style,
        objectFit: "contain",
        borderRadius: 8,
        background: "#fff",
        padding: 2,
      }}
    />
  );
}
