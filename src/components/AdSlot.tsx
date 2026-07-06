// Reserved-space, clearly-labeled ad container. Renders a fixed-height
// placeholder box that visually blends with the dark theme so no layout
// shift occurs when a real ad script is dropped in later.
//
// Never place directly above/below primary CTAs — the parent layout is
// responsible for keeping ≥24px spacing from interactive elements.

interface AdSlotProps {
  id: "top-banner-ad" | "mid-page-ad" | "mobile-sticky-ad";
  /** Reserved height (px) on desktop. Defaults to 90. */
  desktopHeight?: number;
  /** Reserved height (px) on mobile. Defaults to 60. */
  mobileHeight?: number;
  className?: string;
}

export function AdSlot({ id, desktopHeight = 90, mobileHeight = 60, className = "" }: AdSlotProps) {
  return (
    <div className={`mx-auto w-full max-w-3xl px-4 sm:px-6 ${className}`}>
      <div className="mb-1.5 text-center text-[10px] font-semibold uppercase tracking-[0.24em] text-muted-foreground/70">
        Advertisement
      </div>
      <div
        id={id}
        role="complementary"
        aria-label="Advertisement"
        style={
          {
            "--ad-h-desktop": `${desktopHeight}px`,
            "--ad-h-mobile": `${mobileHeight}px`,
            minHeight: `var(--ad-h-mobile)`,
          } as React.CSSProperties
        }
        className="ad-slot flex w-full items-center justify-center overflow-hidden rounded-2xl border border-white/10 bg-white/[0.02] text-[11px] text-muted-foreground/50 sm:min-h-[var(--ad-h-desktop)]"
      >
        {/* Ad script will be injected here. Empty state is intentionally silent. */}
      </div>
    </div>
  );
}
