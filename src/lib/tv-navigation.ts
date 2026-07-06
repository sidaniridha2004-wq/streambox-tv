/**
 * TV Navigation — D-pad focus management for Android TV.
 *
 * Provides arrow-key (D-pad) navigation, Enter-to-click, and Back-to-navigate
 * for Android TV remotes. Also detects TV environment and applies focus
 * management to all interactive elements.
 */

// ---------------------------------------------------------------------------
// Platform detection
// ---------------------------------------------------------------------------

/** Detect Android TV via user agent or lack of touch + large screen. */
export function isAndroidTV(): boolean {
  if (typeof window === "undefined") return false;
  const ua = navigator.userAgent.toLowerCase();
  // Android TV user agents contain "TV" or "AFT" (Amazon Fire TV)
  if (ua.includes("android") && (ua.includes(" tv") || ua.includes("aft"))) return true;
  // Fallback: no fine pointer + large viewport (likely TV or set-top box)
  if (window.matchMedia("(pointer: none)").matches && window.innerWidth >= 960) return true;
  return false;
}

/** Detect if running inside Capacitor WebView. */
export function isCapacitor(): boolean {
  if (typeof window === "undefined") return false;
  return !!(window as any).Capacitor;
}

// ---------------------------------------------------------------------------
// Focusable element helpers
// ---------------------------------------------------------------------------

const FOCUSABLE_SELECTOR = [
  "a[href]",
  "button:not([disabled])",
  "[tabindex]:not([tabindex='-1'])",
  "input:not([disabled])",
  "select:not([disabled])",
  "textarea:not([disabled])",
].join(", ");

function getFocusableElements(): HTMLElement[] {
  return Array.from(document.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
    (el) => el.offsetParent !== null && getComputedStyle(el).visibility !== "hidden",
  );
}

function getRect(el: HTMLElement): DOMRect {
  return el.getBoundingClientRect();
}

// ---------------------------------------------------------------------------
// Spatial navigation — find the closest focusable element in a direction
// ---------------------------------------------------------------------------

type Direction = "up" | "down" | "left" | "right";

function findNextFocusable(current: HTMLElement, direction: Direction): HTMLElement | null {
  const allFocusable = getFocusableElements();
  const currentRect = getRect(current);
  const cx = currentRect.left + currentRect.width / 2;
  const cy = currentRect.top + currentRect.height / 2;

  let bestCandidate: HTMLElement | null = null;
  let bestDistance = Infinity;

  for (const el of allFocusable) {
    if (el === current) continue;
    const rect = getRect(el);
    const ex = rect.left + rect.width / 2;
    const ey = rect.top + rect.height / 2;

    // Check if the element is in the correct direction
    let inDirection = false;
    switch (direction) {
      case "up":
        inDirection = ey < cy - 5;
        break;
      case "down":
        inDirection = ey > cy + 5;
        break;
      case "left":
        inDirection = ex < cx - 5;
        break;
      case "right":
        inDirection = ex > cx + 5;
        break;
    }

    if (!inDirection) continue;

    // Calculate weighted distance (prefer elements more aligned in the axis of movement)
    let dx = ex - cx;
    let dy = ey - cy;

    let distance: number;
    if (direction === "up" || direction === "down") {
      // Vertical movement — weight horizontal offset more heavily
      distance = Math.abs(dy) + Math.abs(dx) * 2.5;
    } else {
      // Horizontal movement — weight vertical offset more heavily
      distance = Math.abs(dx) + Math.abs(dy) * 2.5;
    }

    if (distance < bestDistance) {
      bestDistance = distance;
      bestCandidate = el;
    }
  }

  return bestCandidate;
}

// ---------------------------------------------------------------------------
// Focus management
// ---------------------------------------------------------------------------

function scrollIntoViewSmooth(el: HTMLElement) {
  el.scrollIntoView({ behavior: "smooth", block: "nearest", inline: "nearest" });
}

function handleDpadNavigation(e: KeyboardEvent) {
  const directionMap: Record<string, Direction> = {
    ArrowUp: "up",
    ArrowDown: "down",
    ArrowLeft: "left",
    ArrowRight: "right",
  };

  const direction = directionMap[e.key];

  // Handle Enter key — simulate click on focused element
  if (e.key === "Enter" || e.key === " ") {
    const focused = document.activeElement as HTMLElement;
    if (focused && focused !== document.body) {
      e.preventDefault();
      focused.click();
    }
    return;
  }

  // Handle Back button (mapped to Escape or Backspace on some devices)
  if (e.key === "Escape" || (e.key === "Backspace" && document.activeElement === document.body)) {
    // Let the Capacitor back button handler deal with this
    return;
  }

  if (!direction) return;

  const focused = document.activeElement as HTMLElement;
  if (!focused || focused === document.body) {
    // No element focused — focus the first focusable element
    const first = getFocusableElements()[0];
    if (first) {
      e.preventDefault();
      first.focus();
      scrollIntoViewSmooth(first);
    }
    return;
  }

  const next = findNextFocusable(focused, direction);
  if (next) {
    e.preventDefault();
    next.focus();
    scrollIntoViewSmooth(next);
  }
}

// ---------------------------------------------------------------------------
// Initialization
// ---------------------------------------------------------------------------

let initialized = false;

/** Initialize TV navigation. Safe to call multiple times — only activates once. */
export function initTvNavigation() {
  if (initialized) return;
  if (typeof window === "undefined") return;

  // Only activate on TV or when no fine pointer is available
  const isTV = isAndroidTV();
  const noTouch = window.matchMedia("(pointer: none)").matches || window.matchMedia("(pointer: coarse)").matches;

  if (!isTV && !isCapacitor()) return;

  initialized = true;

  // Add TV mode class to body for CSS targeting
  if (isTV) {
    document.documentElement.classList.add("tv-mode");
  }

  // Add Capacitor class
  if (isCapacitor()) {
    document.documentElement.classList.add("capacitor");
  }

  // Listen for D-pad / keyboard navigation
  document.addEventListener("keydown", handleDpadNavigation, { passive: false });

  // Ensure all interactive card elements have tabIndex for D-pad focus
  const observer = new MutationObserver(() => {
    if (!isTV) return;
    // Auto-add tabIndex to cards and interactive elements that may be dynamically added
    document.querySelectorAll<HTMLElement>('[class*="card-hover"], [role="button"]').forEach((el) => {
      if (!el.hasAttribute("tabindex")) {
        el.setAttribute("tabindex", "0");
      }
    });
  });

  observer.observe(document.body, { childList: true, subtree: true });
}

/** Clean up TV navigation listeners. */
export function destroyTvNavigation() {
  if (!initialized) return;
  document.removeEventListener("keydown", handleDpadNavigation);
  initialized = false;
}

// ---------------------------------------------------------------------------
// Immersive Mode (True Fullscreen + Landscape)
// ---------------------------------------------------------------------------

/**
 * Requests true HTML5 fullscreen and locks orientation to landscape.
 * MUST be called directly inside a user interaction event handler (like onClick).
 */
export async function requestImmersiveMode() {
  if (typeof window === "undefined" || typeof document === "undefined") return;
  
  // Only apply to mobile devices and TVs
  if (window.innerWidth >= 1024 && !isAndroidTV()) return;

  try {
    if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
      await document.documentElement.requestFullscreen();
    }
    
    try {
      if (screen.orientation && (screen.orientation as any).lock) {
        await (screen.orientation as any).lock("landscape");
      }
    } catch (e) {}
  } catch (e) {
    console.warn("Immersive mode request failed (likely missing user gesture):", e);
  }
}

/**
 * Exits fullscreen and unlocks orientation.
 * Can be called on component unmount.
 */
export async function exitImmersiveMode() {
  if (typeof window === "undefined" || typeof document === "undefined") return;

  try {
    if (document.fullscreenElement && document.exitFullscreen) {
      await document.exitFullscreen();
    }

    try {
      if (screen.orientation && (screen.orientation as any).unlock) {
        (screen.orientation as any).unlock();
      }
    } catch (e) {}
  } catch (e) {
    console.warn("Exit immersive mode failed:", e);
  }
}
