# Ochre — Feature Brainstorm & Design Notes

## Core philosophy
Personal utility app. Not commercial. Useful friction-free logging during puppy stages,
transitions to passive monitoring and records as she ages. Every feature should answer
"would I actually use this in 6 months" — if not, it's slop.

---

## Training module
- **Command library** — each command as a record: name, verbal + hand cue, what correct looks like,
  reward type, status (learning / reliable / proofed), notes on struggles/breakthroughs
- **Session planning & logging** — date, duration, which commands worked, per-command reps +
  success rate, notes
- **Derived insight** — "recall hasn't been practised in 3 weeks", "down degrades without
  reinforcement every ~10 days"
- **Programme structure** — ordered plan for which commands to introduce when (puppy foundation,
  adolescence maintenance etc.) — app acts as coach not just diary
- Consistent procedures each time, remember cues exactly, track whether learned and kept up

---

## Sitter / handover export
- One-tap plain-text export via Android share sheet (WhatsApp, email, Notes, anything)
- Pulls from existing data: meal schedule, walk schedule, known commands
- Dedicated sitter profile section: emergency contacts (owner + vet + backup), house rules
  ("not allowed on sofa", "crate at night"), quirks ("barks at postman — ignore"),
  where things are (food, lead, bags), what-to-do-if (sick / escapes / won't eat)
- "Last updated" timestamp at top so sitter knows they have current version
- No app required on recipient end — pure text

---

## Pee/poo tracking — the actual insight
- Not a log for its own sake — derive: "average gap between pees is 2h 20m",
  "needs out within ~18 mins of eating"
- Self-calibrating from history, not fixed numbers
- **Post-meal timer** — when feed logged, silent countdown starts; at ~15 min nudge:
  "Bella probably needs out soon." Auto-fires from feed log, no manual action
- **Pee gap alert** — if no pee logged longer than average + buffer, quiet background notification
- Tracking useful for 4-6 weeks until owner has internalised the pattern; then done

---

## Health & body
- Weight trend with growth curve — is she on track for her breed
- Body condition score (1-9 vet scale) logged periodically
- Heat cycle tracker (if not spayed)
- Coat/skin notes — shedding, hotspots, anything to mention at vet
- **Growth rate** — automatic weekly gain/loss rate from weight logs, flags if trajectory
  is abnormal (too fast / too slow)

---

## Vet & medical
- Vaccination schedule with reminders — know when boosters are due
- Flea/tick/worming treatment log with configurable repeat reminders
  (these are genuinely easy to forget)
- Medication log with dosing schedule
- Vet visit records — discussed, prescribed, weight at visit
- Insurance claim reference — photo attach receipt, note claim number
- **"Things to ask the vet" notepad** — open at appointment, always forget otherwise

---

## Food
- Treat budget alongside meals — easy to overtreat without realising, especially in training
- Food brand/batch tracking — if there's a recall you know exactly what she ate
- Reaction log — ate X, was itchy / loose stool afterward
- Puppy feeding is more frequent than 2x/day early on — app should handle 3-4 meal schedules

---

## Behaviour
- Incident log — bite/snap/resource guard with context; useful if pattern emerges or
  for behaviourist discussion
- Fear/anxiety notes — trigger, severity, recovery
- Socialisation checklist — established list for puppy critical period
  (men with hats, children, bikes, other dogs, cats, traffic, etc.)

---

## Developmental stage awareness (high value, underrated)
- App knows her DOB → knows her exact age in weeks/months
- Hardcoded research-backed timeline of developmental stages:
  - 8-12 weeks: fear imprint period, socialisation window
  - 12-16 weeks: secondary socialisation
  - 6-8 months: adolescence onset, expect recall regression — this is normal
  - 12-18 months: adolescence peaks, second fear period possible
  - etc.
- **Timeline tab** — scrollable full timeline of all stages, what to expect, what to do
- **Home screen** — surfaces upcoming/current stage: "entering adolescence in 3 weeks —
  recall regression is normal and temporary"
- Contextual guidance timed to her actual age, not generic tips
- Most differentiated feature in the app — nothing else does this well

---

## Persistent status notification (always-on, silent)
Current: last walk, last feed, next meal, next walk due, active walk/alone timers

**Weather integrations:**
- High temp warning — "32° today, keep walks to early morning/evening, check pavement temp"
  (5-second pavement test reminder)
- Upcoming rain — "rain in 2h — walk now or wait it out"
- Low temp — relevant for short-coated breeds
- Air quality (urban areas)

**Other additions:**
- "No pee logged in 3h" — quiet nudge
- During flea/worming treatment window — "treatment due this week"
- "Last vet visit 11 months ago" — surfaces once annually
- Developmental milestone upcoming — "fear period starts in ~1 week, avoid negative experiences"

---

## Push notifications (actionable, repeating)
- Walk due / overdue — repeats if not dismissed within X mins
- Feed window open — repeats if not dismissed
- Alone mode — push at configurable intervals if active ("she's been alone 3h")
- **Dismiss button on notification logs the event** — records walk/feed/pee and resets the timer
  without opening the app. This is the key UX — one tap on the notification = done
- Post-meal "needs out soon" — auto-fires ~15 min after feed logged

---

## BLE proximity (alone mode automation)
- Cheap BLE tag on collar (£2-5, coin cell, 25-35mm disc, lasts 6-18 months)
- Android background BLE scan (SCAN_MODE_LOW_POWER) — negligible battery drain
- Tag disappears from range → 2-min grace period → auto-start alone mode
- Tag reappears → auto-end alone mode
- ~5-15m range depending on walls — effectively "is she in the building"
- Distance estimation via RSSI unreliable indoors — use as boolean near/far only
- NFC on collar tag for intentional logging (start walk, log feed) — tap = action

---

## Sleep / activity inference
- No extra logging needed — infer from existing data (alone sessions, walk times, active periods)
- Puppies need 16-18 hours sleep; flag if significantly under
- Owners often unknowingly overstimulate — useful passive check

---

## Camera / remote monitoring

### Requirement
- Low CAPEX, zero OPEX
- Willing to set up something technical
- Want to view from phone anywhere (not just local network)
- Face camera at crate initially, then room

### Recommended approach: old Android phone + local streaming

**Option A — Iriun / DroidCam (easiest)**
- Install Iriun or DroidCam on old Android phone, acts as IP camera
- View on local network easily
- Remote access requires a tunnel (Tailscale — see below)
- Zero cost if you have a spare phone

**Option B — IP Webcam app (Android)**
- Free app, streams MJPEG/RTSP over local network
- Combine with Tailscale for remote access
- More control than Iriun, exposes a web interface

**Option C — Old phone + Frigate + Home Assistant (most powerful)**
- Frigate is an open-source NVR with object detection
- Runs on a Raspberry Pi or spare PC
- Would give you motion detection, clips, alerts
- Overkill for crate watching but future-proof if you add more cameras

### Remote access: Tailscale (the right answer)
- Free for personal use, no monthly cost ever
- Installs on the camera device and your phone
- Creates a private VPN mesh — your phone can reach the camera device
  as if they're on the same network, from anywhere in the world
- No port forwarding, no router config, no dynamic DNS headaches
- Works through NAT, CGNAT, mobile data, anything
- 5 minute setup

### Recommended path
1. Grab old Android phone (or cheap £20 secondhand one)
2. Install **IP Webcam** app — plug into charger, point at crate
3. Install **Tailscale** on both that phone and your main phone
4. Access the stream URL from anywhere via Tailscale IP
5. Bookmark it in a browser or use a simple RTSP viewer app

### What you get
- Live view from anywhere, zero monthly cost
- No cloud, footage never leaves your network
- Motion alerts possible (IP Webcam has basic motion detection built in)
- Expandable — add more cameras the same way

### What you don't get (vs Ring etc.)
- No slick app — it's a browser or RTSP viewer
- No cloud recording (unless you add storage to the Pi/PC)
- Requires the camera phone to stay charged and on WiFi

---

## Non-slop ideas still to evaluate
- Route saving for walks (not GPS tracking — just "20-min route" vs "long route" as labels)
- Nearby emergency vet saved with one-tap call
- Dangerous food quick-lookup ("can she eat X")
- Partner read-only share — both people see if she's been fed/walked without texting
