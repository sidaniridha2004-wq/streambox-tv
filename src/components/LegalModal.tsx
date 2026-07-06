import { useState, type ReactNode } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

export type LegalKey = "about" | "privacy" | "terms" | "dmca" | "contact" | "faq" | "report";

interface LegalModalProps {
  kind: LegalKey;
  children: ReactNode;
  className?: string;
}

const CONTACT_EMAIL = "contact@auratv.example"; // TODO: replace with real address
const DMCA_EMAIL = "dmca@auratv.example"; // TODO: replace with real address
const TELEGRAM_URL = "https://t.me/Aura_TV";

const TITLES: Record<LegalKey, { title: string; description: string }> = {
  about: {
    title: "About AuraTV",
    description: "What AuraTV is and how it works.",
  },
  privacy: {
    title: "Privacy Policy",
    description: "How we handle your data.",
  },
  terms: {
    title: "Terms of Service",
    description: "The rules of using AuraTV.",
  },
  dmca: {
    title: "DMCA / Copyright",
    description: "For rights holders and takedown requests.",
  },
  contact: {
    title: "Contact",
    description: "Get in touch with the AuraTV team.",
  },
  faq: {
    title: "Frequently Asked Questions",
    description: "Quick answers to the most common questions.",
  },
  report: {
    title: "Report a problem",
    description: "Tell us about a broken stream or a bug.",
  },
};

function Body({ kind }: { kind: LegalKey }) {
  if (kind === "about") {
    return (
      <div className="space-y-4 text-sm leading-relaxed text-muted-foreground">
        <p>
          AuraTV is a lightweight platform that brings live sports fixtures, TV channels,
          and match-day updates together in one clean, fast interface.
        </p>
        <p>
          Stream links and schedule data may come from third-party sources. Availability
          can change without notice and we cannot guarantee that every stream will be
          online at every moment.
        </p>
        <p>
          Reliability and user experience are our top priorities. If something isn't
          working — or you have an idea to make it better — we'd love to hear from you
          on the Contact page.
        </p>
      </div>
    );
  }
  if (kind === "privacy") {
    return (
      <div className="space-y-3 text-sm leading-relaxed text-muted-foreground">
        <p>
          AuraTV may use cookies and basic analytics to understand how the site is used
          and to keep it fast and reliable.
        </p>
        <p>
          We may work with third-party advertising partners. These partners can use
          cookies and device information to show ads that are more relevant to you.
        </p>
        <p>
          External links — such as Telegram, streaming sources, or partner sites — are
          outside our control. Their own privacy practices apply once you leave AuraTV.
        </p>
        <p>You can contact us anytime if you have privacy concerns or requests.</p>
      </div>
    );
  }
  if (kind === "terms") {
    return (
      <div className="space-y-3 text-sm leading-relaxed text-muted-foreground">
        <p>You are responsible for how you use AuraTV.</p>
        <p>
          Content, schedules, and stream availability can change at any time without
          notice. We do not guarantee 24/7 uptime.
        </p>
        <p>
          You must respect copyright and the laws that apply where you live regarding
          streaming and broadcast content.
        </p>
        <p>
          We may update these terms occasionally. Continued use of AuraTV means you
          accept the current version.
        </p>
      </div>
    );
  }
  if (kind === "dmca") {
    return (
      <div className="space-y-3 text-sm leading-relaxed text-muted-foreground">
        <p>
          If you are a rights holder and believe content on AuraTV needs to be reviewed
          or removed, please contact us. We take legitimate takedown requests seriously
          and act on them promptly.
        </p>
        <p>Please include in your request:</p>
        <ul className="ml-5 list-disc space-y-1">
          <li>The specific URL or channel involved.</li>
          <li>Proof of your rights to the content.</li>
          <li>Contact information for a reply.</li>
        </ul>
        <p>
          Send DMCA notices to{" "}
          <a href={`mailto:${DMCA_EMAIL}`} className="text-primary hover:underline">
            {DMCA_EMAIL}
          </a>
          .
        </p>
      </div>
    );
  }
  if (kind === "contact") {
    return (
      <div className="space-y-3 text-sm leading-relaxed text-muted-foreground">
        <p>The fastest way to reach us is Telegram:</p>
        <p>
          <a
            href={TELEGRAM_URL}
            target="_blank"
            rel="noreferrer"
            className="font-semibold text-primary hover:underline"
          >
            @Aura_TV on Telegram
          </a>
        </p>
        <p>
          Or email us at{" "}
          <a href={`mailto:${CONTACT_EMAIL}`} className="text-primary hover:underline">
            {CONTACT_EMAIL}
          </a>
          .
        </p>
        <p>We usually reply within 24–48 hours.</p>
      </div>
    );
  }
  if (kind === "report") {
    return (
      <div className="space-y-3 text-sm leading-relaxed text-muted-foreground">
        <p>
          Found a broken stream, missing channel, or a bug? Tell us so we can fix it
          quickly. Please mention the channel name or match if you can.
        </p>
        <div className="flex flex-col gap-2 pt-1">
          <a
            href={TELEGRAM_URL}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center justify-center rounded-full bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground hover:brightness-110"
          >
            Report on Telegram
          </a>
          <a
            href={`mailto:${CONTACT_EMAIL}?subject=AuraTV%20problem%20report`}
            className="inline-flex items-center justify-center rounded-full border border-white/10 bg-white/[0.04] px-4 py-2.5 text-sm font-semibold text-foreground hover:bg-white/[0.08]"
          >
            Email us
          </a>
        </div>
      </div>
    );
  }
  // FAQ
  const items = [
    {
      q: "Is AuraTV free?",
      a: "Yes. AuraTV is free to use. We may show a small number of clearly labeled ads to help cover hosting costs.",
    },
    {
      q: "Why is a stream or channel not working?",
      a: "Streams come from third-party sources and can go offline without notice. Try another quality or another channel — most issues clear up within a few minutes.",
    },
    {
      q: "How do I report a broken link?",
      a: "Use the 'Report a problem' link in the footer, or message us directly on Telegram. Please mention the exact channel or match.",
    },
    {
      q: "Do you host the streams yourselves?",
      a: "No. AuraTV only organises publicly available links. We do not store or broadcast any video content.",
    },
    {
      q: "How often is the schedule updated?",
      a: "The match schedule refreshes automatically every couple of minutes so scores, kickoff times, and statuses stay current.",
    },
  ];
  return (
    <div className="space-y-4">
      {items.map((it) => (
        <div key={it.q}>
          <h3 className="text-sm font-semibold text-foreground">{it.q}</h3>
          <p className="mt-1 text-sm leading-relaxed text-muted-foreground">{it.a}</p>
        </div>
      ))}
    </div>
  );
}

export function LegalModal({ kind, children, className }: LegalModalProps) {
  const [open, setOpen] = useState(false);
  const meta = TITLES[kind];
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <button
          type="button"
          className={`inline bg-transparent p-0 align-baseline font-inherit ${className ?? ""}`}
        >
          {children}
        </button>
      </DialogTrigger>
      <DialogContent className="max-h-[85vh] overflow-y-auto border-white/10 bg-background sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="font-display text-xl">{meta.title}</DialogTitle>
          <DialogDescription>{meta.description}</DialogDescription>
        </DialogHeader>
        <div className="mt-2">
          <Body kind={kind} />
        </div>
      </DialogContent>
    </Dialog>
  );
}
