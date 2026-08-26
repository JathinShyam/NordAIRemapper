# I Wanted My OnePlus Nord 5 Side Button to Do *My* Shortcuts — Here’s What It Took

*Building Keyforge: a simple story about a hard phone button, without assuming you speak Android.*

---

> **Not affiliated with OnePlus / OPPO.** Keyforge is an independent app.

> **Medium tip:** Images below are sized for comfortable reading (`280` / `420` wide, centered). Upload from this folder when you publish.

---

## What is this about?

Your OnePlus Nord 5 has a physical **side button** (the **Plus Key**). Out of the box it can do things like flashlight or camera.

A lot of people want more: play/pause music, open WhatsApp, show a small menu — *their* shortcuts.

**Keyforge** is an app I built for that. It’s **free**.

Repo + APKs: [github.com/JathinShyam/NordAIRemapper](https://github.com/JathinShyam/NordAIRemapper) · [latest-debug release](https://github.com/JathinShyam/NordAIRemapper/releases/tag/latest-debug)

I got hooked on the idea after watching [this YouTube video about customizing the Nothing Essential Key](https://www.youtube.com/watch?v=YhY94x3HL7o&t=356s). That world already has remappers (often paid on the Play Store). I searched for the same experience on Nord 5, found nothing that fit, and decided to solve it myself.

Sounds easy. On this phone, it wasn’t.

---

## The surprise: the phone “eats” the button

On many phones, apps can “hear” a side button the same way they hear volume keys.

On Nord 5, OxygenOS (OnePlus’s software) often handles the Plus Key **inside the system**. Your remapper app never gets a normal “button pressed” message.

Volume still works that way. The Plus Key often does **not**.

So the real job of Keyforge became three simple goals:

1. **Notice** when you press the Plus Key (even if the phone hides it).
2. **Unlock** a special permission so that noticing is allowed.
3. **Stay out of the way** when you open banking / UPI apps.

<div align="center">

<img src="./home.jpg" alt="Keyforge Home screen with Remapping on and three actions" width="280" />

</div>

<div align="center">

*Figure 1. Home when things work: Remapping on, Service Active, and three actions — Single, Double, Long.*

</div>

---

## Rule #1: Prove the button works before making it pretty

It’s fun to design nice screens. We almost started there.

Better idea: first prove *“we saw the Plus Key press.”* Only then polish Home, Settings, and menus.

If the button isn’t detected, a beautiful app is just a brochure.

<div align="center">

<img src="./settings_page.jpg" alt="Keyforge Settings screen" width="280" />

</div>

<div align="center">

*Figure 2. Settings later — feedback, overlays, key setup, advanced tools. Useful only after detection works.*

</div>

---

## Part 1 — How Keyforge “hears” the button

### Plan A (the normal Android way)

Android has a feature called **Accessibility**. Apps use it (with your permission) to help with things like reading the screen — and sometimes to notice keys.

Plan A:

1. Turn on Accessibility for Keyforge.
2. Listen for key presses.
3. If it’s the Plus Key → do your action.
4. Leave volume and power alone.

That design is still in the app. It’s clean and safe.

### Plan B (what Nord 5 actually needs)

When we tested Plan A, we mostly saw **volume** keys. The Plus Key was often missing.

So we added Plan B: watch the phone’s **system log** (a running diary of what the OS is doing). When you press Plus Key, OxygenOS often writes a line there. Keyforge looks for that line and treats it as a press.

Think of it like this:

- Plan A = hearing someone speak in the room  
- Plan B = reading the note they left on the whiteboard when they won’t speak to you  

On Nord 5, Plan B is usually the one that works. Accessibility stays important anyway — for actions like screenshot, lock screen, or recent apps.

### Ways to detect (simple table)

| Mode | In plain English | Good to know |
|------|------------------|--------------|
| **Auto** | Try everything that works | Best default on Nord 5 |
| **Accessibility** | Only “hear” normal key messages | Plus Key often never arrives here |
| **Logcat** | Watch the system log | Needs a one-time Unlock (next section) |

Even in Accessibility mode, Keyforge may still keep the log watcher ready on Nord 5 — otherwise the button can go silent.

<div align="center">

<img src="./runtime_detection_pipeline.png" alt="Diagram of how a Plus Key press flows through Keyforge" width="420" />

</div>

<div align="center">

*Figure 3. Runtime detection pipeline — from the physical button, through two listening paths, into one engine that decides Single / Double / Long, then runs your action.*

</div>

### Single, double, long — without the jargon

Once Keyforge sees a press, it waits a moment before deciding:

- **Hold longer** → Long press  
- **Two taps close together** → Double  
- **One tap, then silence** → Single  

Single must wait. If it fired immediately, every double would look like two singles.

Then Keyforge checks a few common-sense rules: Is learning mode on? Are you in an excluded app? Is the phone locked and that gesture blocked? Only then does it run the action.

---

## Part 2 — The one-time “Unlock” (why setup feels special)

To watch the system log, Android needs a permission called **READ_LOGS**.

You **cannot** turn that on with a normal “Allow” popup. Google / phone makers keep it locked.

Old advice was: plug into a computer and type a command. Most people won’t do that.

So Keyforge built **Enable Plus Key detection** — Unlock on the phone itself when you can:

1. Turn on **Wireless debugging** (a Developer option).
2. Pair once inside the app (**Built-in**), or use **Shizuku**, or copy **Manual ADB** steps.
3. Keyforge applies a short list of unlocks.
4. It checks that things really worked — not a fake “success.”

### What Unlock is asking for (plain English)

| Name | Why Keyforge wants it | “Done” looks like |
|------|------------------------|-------------------|
| Accessibility | Hear some keys + run system actions | Enabled |
| Read logs | See Plus Key in the system log | Granted |
| Log visibility | Phone actually shares other apps’ log lines | Visible |
| Overlay | Floating menu / on-screen feedback | Granted |
| Notifications | Warn you if detection stops | Granted |
| Battery exemption | Keep listening in the background | Exempt |
| Secure settings write | Pause Accessibility for banking (hands-free) | Granted |
| Usage access | Know when you left the banking app | Granted |

**Important order:** banking-related unlocks first, read-logs **last**. Doing read-logs too early can restart the app mid-setup and skip the rest. We learned that the hard way.

<div align="center">

<img src="./enable_keydetection_flow.png" alt="Flowchart of Built-in, Shizuku, and Manual ADB unlock paths" width="420" />

</div>

<div align="center">

*Figure 4. Three ways to Unlock — Built-in, Shizuku, or Manual ADB — all end at the same checks, then restart the log watcher.*

</div>

<div align="center">

<img src="./plus_key_detection_page.jpg" alt="Enable Plus Key detection screen with Built-in selected" width="280" />

</div>

<div align="center">

*Figure 5. The Unlock screen: chips for Read logs, Log access, and Banking pause; pick Built-in / Shizuku / Manual ADB; pair using a notification (you type the code there, not in a form).*

</div>

### Bugs that taught us humility (short version)

- **Permission on, still blind:** After reboot, the phone may grant “read logs” but still hide other apps’ lines until you open Keyforge once. The app now warns you instead of failing quietly.
- **Wrong wireless port:** Pairing and connecting are different steps. Mixing them looks like progress and grants nothing.
- **App restarts mid-Unlock:** Granting read-logs can restart Keyforge. Put it last; finish banking unlocks first.
- **“Success” too early:** We used to celebrate after one permission. Now three chips must look healthy — including banking pause.

---

## Part 3 — Banking / UPI apps said “no Accessibility”

In India and elsewhere, many UPI / bank apps refuse to run if **any** Accessibility service is on. They’re protecting you from overlay scams — but they also block honest remappers.

“Don’t remap inside BHIM” is not enough. The bank is checking a system list, not your remap actions.

So Keyforge added **Auto-Pause Accessibility**:

1. You add BHIM (or another app) to exclusions and turn Auto-Pause on.  
2. You open that app → Keyforge turns its Accessibility **off**.  
3. You pay.  
4. You leave → Keyforge turns Accessibility **back on**.  

If Unlock included the extra banking permissions, this is **hands-free**. If not, Keyforge pauses and asks you to turn Accessibility back on manually (honest, but more work).

<div align="center">

<img src="./banking_autopause_flow.png" alt="Banking auto-pause flowchart" width="420" />

</div>

<div align="center">

*Figure 6. Banking auto-pause: you open an excluded app → Keyforge pauses → when you leave (or switch to another excluded app), it resumes or stays paused.*

</div>

<div align="center">

<img src="./exclusions_page.jpg" alt="Per-App Exclusions with Auto-Pause and BHIM" width="280" />

</div>

<div align="center">

*Figure 7. Real setup: Auto-Pause on, Hands-Free Ready, BHIM excluded.*

</div>

Two different ideas (easy to mix up):

| Mode | What happens | Does the bank still “see” Keyforge? |
|------|--------------|--------------------------------------|
| Soft exclusion | Remaps don’t fire in that app | Yes |
| Auto-pause | Accessibility turns off while you’re in that app | No (while paused) |

---

## What’s running in the background?

You don’t need to memorize this. It’s the cast behind Figures 1–7:

| Piece | What it does for you |
|-------|----------------------|
| Accessibility service | Keys (when available), watches which app is open, system actions |
| Log watcher | Watches system log for Plus Key (needs Unlock) |
| Floating menu service | Optional multi-button overlay |
| Feedback popup | Brief “action happened” toast on the edge |
| Auto-resume service | Turns Accessibility back on after banking |
| Pairing helper | Finishes Wireless Unlock without getting killed mid-way |
| Boot helper | After restart, reminds you to open the app if logs go blind |

If something breaks, Keyforge tries to **tell you** with a notification — for example detection stopped, Accessibility paused for banking, logs blind after boot, or “open Keyforge once after reboot.”

**Promises we keep:**

- No root required  
- No ads, accounts, or cloud sync of your setup  
- We learn your key identity on *your* phone (we don’t hard-code one magic number)  
- Honest limit: without root, OnePlus’s own Plus Key action may still fire too (**dual-fire**)

---

## How a new user actually gets going

1. Install Keyforge  
2. Turn on Accessibility for it  
3. Do Unlock once (Figure 5)  
4. Press the Plus Key — Home should show activity (Figure 1)  
5. Set Single / Double / Long  
6. If you use UPI, add the app + Auto-Pause (Figure 7)  
7. Accept that stock Plus Key may still do its thing sometimes  

If steps 3–4 fail quietly, nothing else matters. That’s why status chips and “Last Used” exist.

---

## What I’d tell another beginner builder

1. **The button is easy; the phone policy is hard.** Design for “the OS won’t tell me.”  
2. **If setup needs a computer command, build a guided Unlock — or accept fewer users.**  
3. **Silent failure is worse than a scary warning.**  
4. **Banking apps don’t care about your elegant remaps.** Plan for Accessibility itself.  
5. **Be honest about limits** (dual-fire, one-time Unlock, open-after-boot). Trust beats fake “one tap magic.”

---

## Why keep going

Keyforge won’t make OnePlus open an official Plus Key API. It won’t remove dual-fire without root. It won’t turn Unlock into a single Play Store checkbox.

What it *can* do is meet the button where the phone leaves a clue, ask for power once with a clear story, run *your* shortcuts, and step aside when you open BHIM.

The Plus Key was never “just another key.”  
On Nord 5, remapping it is a small systems puzzle wearing a hardware costume.

If you’re on a Nord 5 and want that side button for *your* shortcuts: get the APK from **[github.com/JathinShyam/NordAIRemapper](https://github.com/JathinShyam/NordAIRemapper)** ([latest-debug](https://github.com/JathinShyam/NordAIRemapper/releases/tag/latest-debug)). Bring Wireless debugging, a few minutes for Unlock, and one Plus Key press — then watch for the pulse on Home.

That pulse means it worked.

---

### Image checklist for Medium

| Figure | File |
|------|------|
| 1 | `home.jpg` |
| 2 | `settings_page.jpg` |
| 3 | `runtime_detection_pipeline.png` |
| 4 | `enable_keydetection_flow.png` |
| 5 | `plus_key_detection_page.jpg` |
| 6 | `banking_autopause_flow.png` |
| 7 | `exclusions_page.jpg` |

### Suggested tags

`Android` · `OnePlus` · `OxygenOS` · `Indie Apps` · `Mobile` · `UPI` · `Accessibility`
