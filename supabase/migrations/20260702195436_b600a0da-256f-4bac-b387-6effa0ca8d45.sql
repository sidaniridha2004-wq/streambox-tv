CREATE OR REPLACE FUNCTION public.admin_password_matches(_password text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = public
AS $$
  SELECT _password = 'AuraTV@2026!';
$$;

REVOKE ALL ON FUNCTION public.admin_password_matches(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_password_matches(text) TO anon, authenticated;

REVOKE ALL ON FUNCTION public.admin_update_channel(text, text, jsonb) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.admin_set_channels_active(text, text[], boolean) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.admin_insert_channel(text, jsonb) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.admin_delete_channel(text, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.admin_insert_now_on_tv(text, jsonb) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.admin_update_now_on_tv(text, uuid, jsonb) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.admin_delete_now_on_tv(text, uuid) FROM PUBLIC;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.channels TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.now_on_tv TO anon, authenticated;

DROP POLICY IF EXISTS "Admin password can insert channels" ON public.channels;
DROP POLICY IF EXISTS "Admin password can update channels" ON public.channels;
DROP POLICY IF EXISTS "Admin password can delete custom channels" ON public.channels;
DROP POLICY IF EXISTS "Admin password can insert now_on_tv" ON public.now_on_tv;
DROP POLICY IF EXISTS "Admin password can update now_on_tv" ON public.now_on_tv;
DROP POLICY IF EXISTS "Admin password can delete now_on_tv" ON public.now_on_tv;

CREATE POLICY "Admin password can insert channels"
  ON public.channels FOR INSERT
  TO anon, authenticated
  WITH CHECK (
    public.admin_password_matches(
      COALESCE((current_setting('request.headers', true)::jsonb ->> 'x-auratv-admin-password'), '')
    )
    AND is_custom = true
  );

CREATE POLICY "Admin password can update channels"
  ON public.channels FOR UPDATE
  TO anon, authenticated
  USING (
    public.admin_password_matches(
      COALESCE((current_setting('request.headers', true)::jsonb ->> 'x-auratv-admin-password'), '')
    )
  )
  WITH CHECK (
    public.admin_password_matches(
      COALESCE((current_setting('request.headers', true)::jsonb ->> 'x-auratv-admin-password'), '')
    )
  );

CREATE POLICY "Admin password can delete custom channels"
  ON public.channels FOR DELETE
  TO anon, authenticated
  USING (
    is_custom = true
    AND public.admin_password_matches(
      COALESCE((current_setting('request.headers', true)::jsonb ->> 'x-auratv-admin-password'), '')
    )
  );

CREATE POLICY "Admin password can insert now_on_tv"
  ON public.now_on_tv FOR INSERT
  TO anon, authenticated
  WITH CHECK (
    public.admin_password_matches(
      COALESCE((current_setting('request.headers', true)::jsonb ->> 'x-auratv-admin-password'), '')
    )
  );

CREATE POLICY "Admin password can update now_on_tv"
  ON public.now_on_tv FOR UPDATE
  TO anon, authenticated
  USING (
    public.admin_password_matches(
      COALESCE((current_setting('request.headers', true)::jsonb ->> 'x-auratv-admin-password'), '')
    )
  )
  WITH CHECK (
    public.admin_password_matches(
      COALESCE((current_setting('request.headers', true)::jsonb ->> 'x-auratv-admin-password'), '')
    )
  );

CREATE POLICY "Admin password can delete now_on_tv"
  ON public.now_on_tv FOR DELETE
  TO anon, authenticated
  USING (
    public.admin_password_matches(
      COALESCE((current_setting('request.headers', true)::jsonb ->> 'x-auratv-admin-password'), '')
    )
  );