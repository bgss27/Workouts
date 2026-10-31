-- FitTrack cloud sync schema (phase 1: workouts, workout exercises, sets, custom exercises).
-- Run once in the Supabase SQL editor. Safe to re-run: every statement is idempotent.
--
-- Design:
--   * Every table is keyed by a client-generated text UUID ("id") and scoped to
--     auth.uid() through row-level security, so clients can only see their own rows.
--   * "seq" is a global monotonic change counter stamped by a trigger on every
--     insert/update. Clients pull "where seq > last_seen_seq" — a cursor that
--     doesn't depend on device clocks.
--   * "updated_at" (epoch millis, set by the client) decides conflicts: newest wins.
--   * "deleted" rows are tombstones kept so offline devices learn about deletions.

create sequence if not exists sync_seq;

create or replace function stamp_sync_row() returns trigger
language plpgsql as $$
begin
  new.user_id := auth.uid();
  new.seq := nextval('sync_seq');
  return new;
end $$;

-- ---------------------------------------------------------------- exercises (custom only)
create table if not exists exercises (
  id text primary key,
  user_id uuid not null default auth.uid(),
  seq bigint not null default 0,
  updated_at bigint not null,
  deleted boolean not null default false,
  name text not null,
  muscle_group text not null,
  secondary_muscle_group text,
  equipment text not null
);

-- ---------------------------------------------------------------- workouts
create table if not exists workouts (
  id text primary key,
  user_id uuid not null default auth.uid(),
  seq bigint not null default 0,
  updated_at bigint not null,
  deleted boolean not null default false,
  start_time bigint not null,
  end_time bigint,
  notes text
);

-- ---------------------------------------------------------------- workout_exercises
create table if not exists workout_exercises (
  id text primary key,
  user_id uuid not null default auth.uid(),
  seq bigint not null default 0,
  updated_at bigint not null,
  deleted boolean not null default false,
  workout_id text not null,
  -- "catalog:<normalized name>" for seeded exercises, a UUID for custom ones.
  exercise_ref text not null,
  order_index int not null,
  superset_group text
);

-- ---------------------------------------------------------------- workout_sets
create table if not exists workout_sets (
  id text primary key,
  user_id uuid not null default auth.uid(),
  seq bigint not null default 0,
  updated_at bigint not null,
  deleted boolean not null default false,
  workout_exercise_id text not null,
  set_number int not null,
  reps int not null,
  weight_kg double precision not null,
  is_warmup boolean not null default false,
  rpe int
);

-- Triggers, indexes and RLS for every synced table.
do $$
declare t text;
begin
  foreach t in array array['exercises','workouts','workout_exercises','workout_sets'] loop
    execute format('drop trigger if exists %I_stamp on %I', t, t);
    execute format('create trigger %I_stamp before insert or update on %I for each row execute function stamp_sync_row()', t, t);
    execute format('create index if not exists %I_user_seq on %I (user_id, seq)', t, t);
    execute format('alter table %I enable row level security', t);
    execute format('drop policy if exists %I_owner on %I', t, t);
    execute format('create policy %I_owner on %I for all using (user_id = auth.uid()) with check (user_id = auth.uid())', t, t);
  end loop;
end $$;
