import { Link } from "@tanstack/react-router";
import logoAsset from "@/assets/auratv-logo.png.asset.json";
import { useI18n, type Lang } from "@/lib/i18n";
import { LegalModal } from "@/components/LegalModal";

export function Footer() {
  const { t, lang, setLang } = useI18n();
  const langs: Lang[] = ["ar", "fr", "en"];
  const linkCls =
    "text-left text-muted-foreground transition hover:text-foreground";
  return (
    <footer className="mt-20 border-t border-white/10 bg-black/40 backdrop-blur">
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:grid-cols-2 sm:px-6 lg:grid-cols-5">
        <div className="lg:col-span-2">
          <div className="flex items-center gap-3">
            <img src={logoAsset.url} alt="AuraTV" className="h-10 w-10 rounded-xl object-cover" />
            <div>
              <div className="font-display text-lg font-bold text-aurora">AuraTV</div>
              <div className="text-xs text-muted-foreground">{t("footer.tagline")}</div>
            </div>
          </div>
          <p className="mt-4 max-w-sm text-xs leading-relaxed text-muted-foreground">
            Fixtures and channel data may come from external sources. Stream availability can vary.
            If you are a rights holder and need content reviewed or removed, please contact us.
          </p>
        </div>

        <div>
          <div className="mb-3 text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
            Explore
          </div>
          <ul className="space-y-2 text-sm">
            <li><Link to="/" hash="matches" className={linkCls}>{t("nav.matches")}</Link></li>
            <li><Link to="/" hash="channels" className={linkCls}>{t("nav.channels")}</Link></li>
            <li><Link to="/" hash="favorites" className={linkCls}>{t("nav.favorites")}</Link></li>
            <li><Link to="/status" className={linkCls}>{t("nav.status")}</Link></li>
          </ul>
        </div>

        <div>
          <div className="mb-3 text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
            Company
          </div>
          <ul className="space-y-2 text-sm">
            <li><LegalModal kind="about" className={linkCls}>About</LegalModal></li>
            <li><LegalModal kind="faq" className={linkCls}>FAQ</LegalModal></li>
            <li><LegalModal kind="contact" className={linkCls}>Contact</LegalModal></li>
            <li><LegalModal kind="report" className={linkCls}>Report a problem</LegalModal></li>
          </ul>
        </div>

        <div>
          <div className="mb-3 text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
            Legal
          </div>
          <ul className="space-y-2 text-sm">
            <li><LegalModal kind="privacy" className={linkCls}>Privacy Policy</LegalModal></li>
            <li><LegalModal kind="terms" className={linkCls}>Terms of Service</LegalModal></li>
            <li><LegalModal kind="dmca" className={linkCls}>DMCA / Copyright</LegalModal></li>
          </ul>
          <div className="mt-5">
            <div className="mb-2 text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
              Language
            </div>
            <div className="inline-flex overflow-hidden rounded-full border border-white/10 bg-white/5">
              {langs.map((l) => (
                <button
                  key={l}
                  onClick={() => setLang(l)}
                  className={`px-3 py-1.5 text-xs font-semibold uppercase transition ${
                    lang === l ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
                  }`}
                >
                  {l}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
      <div className="border-t border-white/10 px-4 py-4 text-center text-xs text-muted-foreground sm:px-6">
        © 2026 AuraTV · Made for Algeria 🇩🇿
      </div>
    </footer>
  );
}
