DROP FUNCTION IF EXISTS public.admin_update_channel(text, text, jsonb);
DROP FUNCTION IF EXISTS public.admin_set_channels_active(text, text[], boolean);
DROP FUNCTION IF EXISTS public.admin_insert_channel(text, jsonb);
DROP FUNCTION IF EXISTS public.admin_delete_channel(text, text);
DROP FUNCTION IF EXISTS public.admin_insert_now_on_tv(text, jsonb);
DROP FUNCTION IF EXISTS public.admin_update_now_on_tv(text, uuid, jsonb);
DROP FUNCTION IF EXISTS public.admin_delete_now_on_tv(text, uuid);