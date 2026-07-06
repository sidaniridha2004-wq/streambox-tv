// Public read + admin-password-gated writes for the "Now on TV" strip.
// Writes call database-side admin RPCs so Vercel/self-hosted deploys do not
// need the service-role runtime secret.
import { createServerFn } from "@tanstack/react-start";
import { createClient } from "@supabase/supabase-js";
import { z } from "zod";
import type { Database } from "@/integrations/supabase/types";

export interface NowOnTvRow {
  id: string;
  channel_slug: string;
  title: string;
  subtitle: string;
  sort_order: number;
  is_active: boolean;
}

const itemSchema = z.object({
  channel_slug: z.string().min(1),
  title: z.string().min(1),
  subtitle: z.string().default(""),
  sort_order: z.number().int().default(0),
  is_active: z.boolean().default(true),
});
const patchSchema = itemSchema.partial();

async function createPublicClient(adminPassword?: string) {
  return createClient<Database>(
    process.env.SUPABASE_URL!,
    process.env.SUPABASE_PUBLISHABLE_KEY!,
    {
      auth: { storage: undefined, persistSession: false, autoRefreshToken: false },
      global: adminPassword ? { headers: { "x-auratv-admin-password": adminPassword } } : undefined,
    },
  );
}

async function requirePassword(pw: string) {
  const expected = process.env.ADMIN_PASSWORD;
  if (expected && expected.length > 0) {
    const a = new TextEncoder().encode(pw);
    const b = new TextEncoder().encode(expected);
    if (a.length !== b.length) throw new Error("Unauthorized");
    let diff = 0;
    for (let i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
    if (diff !== 0) throw new Error("Unauthorized");
    return;
  }

  const supabase = await createPublicClient();
  const { data, error } = await (supabase as any).rpc("admin_password_matches", {
    _password: pw,
  });
  if (error || data !== true) throw new Error("Unauthorized");
}

/** Public list — homepage strip + admin editor share this query. */
export const listNowOnTv = createServerFn({ method: "GET" }).handler(async () => {
  const supabase = await createPublicClient();
  const { data, error } = await supabase
    .from("now_on_tv")
    .select("id,channel_slug,title,subtitle,sort_order,is_active")
    .order("sort_order", { ascending: true })
    .order("created_at", { ascending: true });
  if (error) throw new Error(error.message);
  return (data ?? []) as NowOnTvRow[];
});

export const adminInsertNowOnTv = createServerFn({ method: "POST" })
  .inputValidator((input: unknown) =>
    z.object({ password: z.string(), item: itemSchema }).parse(input),
  )
  .handler(async ({ data }) => {
    await requirePassword(data.password);
    const supabase = await createPublicClient(data.password);
    const { error } = await supabase.from("now_on_tv").insert(data.item);
    if (error) throw new Error(error.message);
    return { ok: true };
  });

export const adminUpdateNowOnTv = createServerFn({ method: "POST" })
  .inputValidator((input: unknown) =>
    z.object({ password: z.string(), id: z.string(), patch: patchSchema }).parse(input),
  )
  .handler(async ({ data }) => {
    await requirePassword(data.password);
    const supabase = await createPublicClient(data.password);
    const { error } = await supabase
      .from("now_on_tv")
      .update(data.patch)
      .eq("id", data.id);
    if (error) throw new Error(error.message);
    return { ok: true };
  });

export const adminDeleteNowOnTv = createServerFn({ method: "POST" })
  .inputValidator((input: unknown) =>
    z.object({ password: z.string(), id: z.string() }).parse(input),
  )
  .handler(async ({ data }) => {
    await requirePassword(data.password);
    const supabase = await createPublicClient(data.password);
    const { error } = await supabase.from("now_on_tv").delete().eq("id", data.id);
    if (error) throw new Error(error.message);
    return { ok: true };
  });
