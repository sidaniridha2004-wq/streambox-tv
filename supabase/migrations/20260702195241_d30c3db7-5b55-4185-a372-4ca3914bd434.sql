CREATE OR REPLACE FUNCTION public.admin_password_matches(_password text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT _password = 'AuraTV@2026!';
$$;

REVOKE ALL ON FUNCTION public.admin_password_matches(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_password_matches(text) TO anon, authenticated;

CREATE OR REPLACE FUNCTION public.admin_update_channel(
  _password text,
  _slug text,
  _patch jsonb
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.admin_password_matches(_password) THEN
    RAISE EXCEPTION 'Unauthorized';
  END IF;

  UPDATE public.channels
  SET
    name = COALESCE(NULLIF(_patch->>'name', ''), name),
    category = COALESCE(NULLIF(_patch->>'category', ''), category),
    logo_url = COALESCE(_patch->>'logo_url', logo_url),
    stream_url = COALESCE(NULLIF(_patch->>'stream_url', ''), stream_url),
    is_active = COALESCE((_patch->>'is_active')::boolean, is_active)
  WHERE slug = _slug;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_update_channel(text, text, jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_update_channel(text, text, jsonb) TO anon, authenticated;

CREATE OR REPLACE FUNCTION public.admin_set_channels_active(
  _password text,
  _slugs text[],
  _is_active boolean
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.admin_password_matches(_password) THEN
    RAISE EXCEPTION 'Unauthorized';
  END IF;

  UPDATE public.channels
  SET is_active = _is_active
  WHERE slug = ANY(_slugs);
END;
$$;

REVOKE ALL ON FUNCTION public.admin_set_channels_active(text, text[], boolean) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_set_channels_active(text, text[], boolean) TO anon, authenticated;

CREATE OR REPLACE FUNCTION public.admin_insert_channel(
  _password text,
  _channel jsonb
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.admin_password_matches(_password) THEN
    RAISE EXCEPTION 'Unauthorized';
  END IF;

  INSERT INTO public.channels (slug, name, category, logo_url, stream_url, is_custom, is_active)
  VALUES (
    _channel->>'slug',
    _channel->>'name',
    _channel->>'category',
    COALESCE(_channel->>'logo_url', ''),
    _channel->>'stream_url',
    true,
    true
  );
END;
$$;

REVOKE ALL ON FUNCTION public.admin_insert_channel(text, jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_insert_channel(text, jsonb) TO anon, authenticated;

CREATE OR REPLACE FUNCTION public.admin_delete_channel(
  _password text,
  _slug text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.admin_password_matches(_password) THEN
    RAISE EXCEPTION 'Unauthorized';
  END IF;

  DELETE FROM public.channels
  WHERE slug = _slug
    AND is_custom = true;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_delete_channel(text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_delete_channel(text, text) TO anon, authenticated;

CREATE OR REPLACE FUNCTION public.admin_insert_now_on_tv(
  _password text,
  _item jsonb
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.admin_password_matches(_password) THEN
    RAISE EXCEPTION 'Unauthorized';
  END IF;

  INSERT INTO public.now_on_tv (channel_slug, title, subtitle, sort_order, is_active)
  VALUES (
    _item->>'channel_slug',
    _item->>'title',
    COALESCE(_item->>'subtitle', ''),
    COALESCE((_item->>'sort_order')::integer, 0),
    COALESCE((_item->>'is_active')::boolean, true)
  );
END;
$$;

REVOKE ALL ON FUNCTION public.admin_insert_now_on_tv(text, jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_insert_now_on_tv(text, jsonb) TO anon, authenticated;

CREATE OR REPLACE FUNCTION public.admin_update_now_on_tv(
  _password text,
  _id uuid,
  _patch jsonb
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.admin_password_matches(_password) THEN
    RAISE EXCEPTION 'Unauthorized';
  END IF;

  UPDATE public.now_on_tv
  SET
    channel_slug = COALESCE(NULLIF(_patch->>'channel_slug', ''), channel_slug),
    title = COALESCE(NULLIF(_patch->>'title', ''), title),
    subtitle = COALESCE(_patch->>'subtitle', subtitle),
    sort_order = COALESCE((_patch->>'sort_order')::integer, sort_order),
    is_active = COALESCE((_patch->>'is_active')::boolean, is_active)
  WHERE id = _id;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_update_now_on_tv(text, uuid, jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_update_now_on_tv(text, uuid, jsonb) TO anon, authenticated;

CREATE OR REPLACE FUNCTION public.admin_delete_now_on_tv(
  _password text,
  _id uuid
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.admin_password_matches(_password) THEN
    RAISE EXCEPTION 'Unauthorized';
  END IF;

  DELETE FROM public.now_on_tv
  WHERE id = _id;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_delete_now_on_tv(text, uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_delete_now_on_tv(text, uuid) TO anon, authenticated;