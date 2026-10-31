# FitTrack cloud sync — one-time Supabase setup

Sync is off until the app is built with a Supabase project's URL and anon key.
These steps take about ten minutes and are done once.

## 1. Create the project
1. Go to https://supabase.com, sign in, and create a new project (free tier is fine).
2. Pick any name and region close to you. Save the database password somewhere; the app never needs it.

## 2. Create the tables
1. In the project dashboard open **SQL Editor → New query**.
2. Paste the whole contents of `schema.sql` from this folder and run it.
   It creates `exercises`, `workouts`, `workout_exercises`, `workout_sets`,
   a global change counter, and row-level security so each user only sees their own rows.
   The script is idempotent; re-running it is safe.

## 3. Turn on email sign-in
1. **Authentication → Providers → Email**: make sure it is enabled.
2. Optional for development: under **Authentication → Settings**, turn off
   "Confirm email" so test accounts work immediately. Turn it back on before release.

## 4. Give the app the keys
1. **Project Settings → API**: copy the **Project URL** and the **anon public** key.
   The anon key is safe to ship in the app; row-level security is what protects the data.
2. Add to `local.properties` in the repo root (this file is git-ignored):

```
supabase.url=https://YOUR-PROJECT-REF.supabase.co
supabase.anonKey=YOUR-ANON-KEY
```

3. Rebuild. Settings → Cloud sync will now offer sign-in.

## What syncs in phase 1
Completed workouts, their exercises and sets, and custom exercises. The
in-progress workout draft, steps, and the Pro entitlement stay on-device.
