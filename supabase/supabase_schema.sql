-- =============================================================================
-- ParishSync: Supabase PostgreSQL Schema & Row Level Security (RLS) Policies
-- Run this script in the Supabase SQL Editor to provision live cloud tables,
-- Realtime publication channels, and Role-Based Access Control (RBAC) policies.
-- =============================================================================

-- 0a. Districts Table (Maps to Room `districts`)
CREATE TABLE IF NOT EXISTS public.districts (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    region_code TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

-- 0b. Congregations Table (Maps to Room `congregations`)
CREATE TABLE IF NOT EXISTS public.congregations (
    id TEXT PRIMARY KEY,
    district_id TEXT NOT NULL DEFAULT '' REFERENCES public.districts(id) ON DELETE RESTRICT,
    display_name TEXT NOT NULL,
    address TEXT NOT NULL DEFAULT '',
    rector_name TEXT NOT NULL DEFAULT '',
    primary_service_times TEXT NOT NULL DEFAULT '',
    sunday_service_time TEXT NOT NULL DEFAULT '10:30 AM',
    wednesday_service_time TEXT NOT NULL DEFAULT '7:30 PM',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

-- 1. User Profiles Table (Maps to Room `user_profiles`)
CREATE TABLE IF NOT EXISTS public.user_profiles (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    full_name TEXT NOT NULL,
    congregation_id TEXT NOT NULL REFERENCES public.congregations(id),
    role TEXT NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('MEMBER', 'MINISTER', 'ADMINISTRATOR')),
    delegated_role TEXT NOT NULL DEFAULT 'NONE' CHECK (delegated_role IN ('NONE', 'VOLUNTEER_SCHEDULER')),
    phone TEXT NOT NULL DEFAULT '',
    is_choir_member BOOLEAN NOT NULL DEFAULT FALSE,
    preferred_ministries TEXT NOT NULL DEFAULT '',
    is_active_session BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

-- 1b. Multi-Role Pivot Table (Maps to Room `user_roles`)
-- Every registered user holds `member` by default; additional stackable roles:
-- `minister`, `rector`, `volunteer_scheduler`, `admin`
CREATE TABLE IF NOT EXISTS public.user_roles (
    user_id TEXT NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('member', 'minister', 'rector', 'volunteer_scheduler', 'admin')),
    assigned_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    PRIMARY KEY (user_id, role)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON public.user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_congregation ON public.user_profiles(congregation_id);

-- 2. Announcements & Live Bulletins (Maps to Room `announcements`)
CREATE TABLE IF NOT EXISTS public.announcements (
    id TEXT PRIMARY KEY,
    congregation_id TEXT NOT NULL,
    title TEXT NOT NULL,
    summary TEXT NOT NULL,
    body TEXT NOT NULL,
    category TEXT NOT NULL,
    is_urgent BOOLEAN NOT NULL DEFAULT FALSE,
    service_date TEXT NOT NULL DEFAULT '',
    liturgical_order TEXT NOT NULL DEFAULT '',
    author_name TEXT NOT NULL,
    author_role TEXT NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    is_synced BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3. Volunteer Roster Assignments (Maps to Room `volunteer_assignments`)
CREATE TABLE IF NOT EXISTS public.volunteer_assignments (
    id TEXT PRIMARY KEY,
    congregation_id TEXT NOT NULL,
    duty_type TEXT NOT NULL,
    custom_task_title TEXT NOT NULL DEFAULT '',
    assignment_tier TEXT NOT NULL DEFAULT 'MAIN',
    assignee_name TEXT NOT NULL,
    backup_assignee_name TEXT NOT NULL DEFAULT '',
    assignee_user_id TEXT,
    service_date TEXT NOT NULL,
    service_time TEXT NOT NULL,
    notes TEXT NOT NULL DEFAULT '',
    sub_tasks_json TEXT NOT NULL DEFAULT '[]',
    is_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_scheduled BOOLEAN NOT NULL DEFAULT FALSE,
    source_pdf_name TEXT,
    is_local_user_match BOOLEAN NOT NULL DEFAULT FALSE,
    is_synced BOOLEAN NOT NULL DEFAULT TRUE
);

-- 4. Choir & Hymn Selections (Maps to Room `hymn_selections`)
CREATE TABLE IF NOT EXISTS public.hymn_selections (
    id TEXT PRIMARY KEY,
    congregation_id TEXT NOT NULL,
    service_date TEXT NOT NULL,
    liturgical_sunday TEXT NOT NULL,
    liturgical_part TEXT NOT NULL,
    hymn_number TEXT NOT NULL,
    title TEXT NOT NULL,
    musical_key TEXT NOT NULL,
    composer_or_tune TEXT NOT NULL,
    choir_rehearsal_notes TEXT NOT NULL,
    four_part_satb_ready BOOLEAN NOT NULL DEFAULT TRUE
);

-- 5. Digital Prayer Requests & Pastoral Visits (Maps to Room `prayer_requests`)
CREATE TABLE IF NOT EXISTS public.prayer_requests (
    id TEXT PRIMARY KEY,
    congregation_id TEXT NOT NULL,
    requester_name TEXT NOT NULL,
    requester_contact TEXT NOT NULL,
    request_type TEXT NOT NULL,
    content TEXT NOT NULL,
    is_private_to_clergy BOOLEAN NOT NULL DEFAULT TRUE,
    status TEXT NOT NULL DEFAULT 'NEW',
    ministerial_notes TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    is_synced BOOLEAN NOT NULL DEFAULT TRUE
);

-- 6. Ride Sharing Board (Maps to Room `ride_shares`)
CREATE TABLE IF NOT EXISTS public.ride_shares (
    id TEXT PRIMARY KEY,
    congregation_id TEXT NOT NULL,
    post_type TEXT NOT NULL,
    member_name TEXT NOT NULL,
    phone_contact TEXT NOT NULL,
    neighborhood TEXT NOT NULL,
    service_date TEXT NOT NULL,
    service_time TEXT NOT NULL,
    seats_count INTEGER NOT NULL DEFAULT 1,
    matched_member_name TEXT,
    notes TEXT NOT NULL DEFAULT '',
    is_fulfilled BOOLEAN NOT NULL DEFAULT FALSE,
    is_synced BOOLEAN NOT NULL DEFAULT TRUE
);

-- 7. Discipleship Small Groups / DSGs (Maps to Room `small_groups`)
CREATE TABLE IF NOT EXISTS public.small_groups (
    id TEXT PRIMARY KEY,
    congregation_id TEXT NOT NULL,
    name TEXT NOT NULL,
    leader_name TEXT NOT NULL,
    meeting_schedule TEXT NOT NULL,
    location TEXT NOT NULL,
    current_topic TEXT NOT NULL,
    member_count INTEGER NOT NULL DEFAULT 1,
    max_capacity INTEGER NOT NULL DEFAULT 12,
    pastoral_log_notes TEXT NOT NULL DEFAULT '',
    service_liturgy_notes TEXT NOT NULL DEFAULT '',
    is_user_enrolled BOOLEAN NOT NULL DEFAULT FALSE
);

-- =============================================================================
-- Enable Row Level Security (RLS) across all ParishSync tables
-- =============================================================================
ALTER TABLE public.congregations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.volunteer_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.hymn_selections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.prayer_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ride_shares ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.small_groups ENABLE ROW LEVEL SECURITY;

-- Helper functions for multi-role RBAC & congregation-level multi-tenancy
CREATE OR REPLACE FUNCTION public.current_user_roles()
RETURNS TEXT[] AS $$
  SELECT COALESCE(
    ARRAY(SELECT role FROM public.user_roles WHERE user_id = auth.uid()::TEXT),
    ARRAY['member']::TEXT[]
  );
$$ LANGUAGE SQL SECURITY DEFINER STABLE;

CREATE OR REPLACE FUNCTION public.has_role(target_role TEXT)
RETURNS BOOLEAN AS $$
  SELECT target_role = ANY(public.current_user_roles())
    OR (target_role = 'member'); -- Base membership for every authenticated user
$$ LANGUAGE SQL SECURITY DEFINER STABLE;

CREATE OR REPLACE FUNCTION public.current_user_congregation_id()
RETURNS TEXT AS $$
  SELECT COALESCE(
    (SELECT congregation_id FROM public.user_profiles WHERE id = auth.uid()::TEXT LIMIT 1),
    ''
  );
$$ LANGUAGE SQL SECURITY DEFINER STABLE;

CREATE OR REPLACE FUNCTION public.can_access_congregation(target_congregation_id TEXT)
RETURNS BOOLEAN AS $$
  SELECT public.has_role('admin')
    OR (
      public.current_user_congregation_id() <> ''
      AND target_congregation_id = public.current_user_congregation_id()
    );
$$ LANGUAGE SQL SECURITY DEFINER STABLE;

-- Congregations RLS: Users view their own congregation; Admin views & manages all
CREATE POLICY "Users can view their assigned congregation; Admins view all"
  ON public.congregations FOR SELECT
  USING (public.can_access_congregation(id));

CREATE POLICY "Admins and Rectors can manage congregations"
  ON public.congregations FOR ALL
  USING (public.has_role('admin') OR (public.has_role('rector') AND public.can_access_congregation(id)))
  WITH CHECK (public.has_role('admin') OR (public.has_role('rector') AND public.can_access_congregation(id)));

-- Announcements RLS: Strictly congregation-scoped; only minister/rector/admin can write
CREATE POLICY "Members can read own congregation announcements"
  ON public.announcements FOR SELECT
  USING (public.can_access_congregation(congregation_id));

CREATE POLICY "Clergy and Admins can publish congregation announcements"
  ON public.announcements FOR ALL
  USING (
    public.can_access_congregation(congregation_id)
    AND (public.has_role('minister') OR public.has_role('rector') OR public.has_role('admin'))
  )
  WITH CHECK (
    public.can_access_congregation(congregation_id)
    AND (public.has_role('minister') OR public.has_role('rector') OR public.has_role('admin'))
  );

-- Volunteer Roster RLS: Strictly congregation-scoped; ONLY volunteer_scheduler or admin can create/edit/delete
-- (Ministers/Rectors without volunteer_scheduler or admin have read-only access)
CREATE POLICY "Members can view own congregation volunteer roster"
  ON public.volunteer_assignments FOR SELECT
  USING (public.can_access_congregation(congregation_id));

CREATE POLICY "Members can confirm their own assigned duty"
  ON public.volunteer_assignments FOR UPDATE
  USING (
    public.can_access_congregation(congregation_id)
    AND assignee_user_id = auth.uid()::TEXT
  )
  WITH CHECK (
    public.can_access_congregation(congregation_id)
    AND assignee_user_id = auth.uid()::TEXT
  );

CREATE POLICY "Volunteer Schedulers and Admins manage congregation schedules"
  ON public.volunteer_assignments FOR ALL
  USING (
    public.can_access_congregation(congregation_id)
    AND (public.has_role('volunteer_scheduler') OR public.has_role('admin'))
  )
  WITH CHECK (
    public.can_access_congregation(congregation_id)
    AND (public.has_role('volunteer_scheduler') OR public.has_role('admin'))
  );

-- Prayer Requests RLS: Congregation-scoped; private intentions visible ONLY to clergy/admin
CREATE POLICY "Members can submit congregation prayer requests"
  ON public.prayer_requests FOR INSERT
  WITH CHECK (public.can_access_congregation(congregation_id));

CREATE POLICY "Clergy view congregation prayer requests; members view public only"
  ON public.prayer_requests FOR SELECT
  USING (
    public.can_access_congregation(congregation_id)
    AND (
      is_private_to_clergy = FALSE
      OR public.has_role('minister')
      OR public.has_role('rector')
      OR public.has_role('admin')
    )
  );

CREATE POLICY "Clergy can triage congregation prayer requests"
  ON public.prayer_requests FOR UPDATE
  USING (
    public.can_access_congregation(congregation_id)
    AND (public.has_role('minister') OR public.has_role('rector') OR public.has_role('admin'))
  );

-- User Profiles & User Roles RLS: Congregation-scoped directory; Admins/Rectors manage roles
CREATE POLICY "Users can view own congregation directory; Admins view all"
  ON public.user_profiles FOR SELECT
  USING (public.can_access_congregation(congregation_id));

CREATE POLICY "Admins can update any user profile; Rectors within congregation"
  ON public.user_profiles FOR UPDATE
  USING (
    public.has_role('admin')
    OR (public.has_role('rector') AND public.can_access_congregation(congregation_id))
  );

CREATE POLICY "Users can view role assignments in their congregation"
  ON public.user_roles FOR SELECT
  USING (
    public.has_role('admin')
    OR EXISTS (
      SELECT 1 FROM public.user_profiles up
      WHERE up.id = user_roles.user_id
        AND public.can_access_congregation(up.congregation_id)
    )
  );

CREATE POLICY "Admins and Rectors can manage user_roles"
  ON public.user_roles FOR ALL
  USING (
    public.has_role('admin')
    OR (
      public.has_role('rector')
      AND role IN ('member', 'volunteer_scheduler', 'minister')
      AND EXISTS (
        SELECT 1 FROM public.user_profiles up
        WHERE up.id = user_roles.user_id
          AND public.can_access_congregation(up.congregation_id)
      )
    )
  );

-- Enable Supabase Realtime on live tables
ALTER PUBLICATION supabase_realtime ADD TABLE public.announcements;
ALTER PUBLICATION supabase_realtime ADD TABLE public.volunteer_assignments;
ALTER PUBLICATION supabase_realtime ADD TABLE public.prayer_requests;
ALTER PUBLICATION supabase_realtime ADD TABLE public.ride_shares;

-- 8. Device Push Subscriptions (Maps to Room `push_subscriptions`)
CREATE TABLE IF NOT EXISTS public.push_subscriptions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
    congregation_id TEXT NOT NULL,
    device_label TEXT NOT NULL DEFAULT 'Mobile Device',
    platform TEXT NOT NULL DEFAULT 'android',
    endpoint TEXT NOT NULL UNIQUE,
    p256dh TEXT NOT NULL DEFAULT '',
    auth TEXT NOT NULL DEFAULT '',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_push_subscriptions_user ON public.push_subscriptions(user_id);

ALTER TABLE public.push_subscriptions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users manage own push subscriptions"
  ON public.push_subscriptions FOR ALL
  USING (user_id = auth.uid()::TEXT)
  WITH CHECK (user_id = auth.uid()::TEXT);

CREATE POLICY "Schedulers, Rectors, and Admins can read congregation push subscriptions"
  ON public.push_subscriptions FOR SELECT
  USING (
    public.can_access_congregation(congregation_id)
    AND (public.has_role('volunteer_scheduler') OR public.has_role('rector') OR public.has_role('admin'))
  );
