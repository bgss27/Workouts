#!/usr/bin/env python3
"""Generate DIY exercise animation GIFs for key exercises."""
from PIL import Image, ImageDraw
import math
import os

WIDTH, HEIGHT = 400, 300
FRAMES = 30
BG = (245, 245, 250)
BODY = (60, 60, 80)
SKIN = (220, 185, 155)
BAR = (120, 120, 140)
PLATE = (80, 80, 100)
BENCH = (180, 140, 100)
BLUE = (66, 133, 244)
FLOOR_Y = 260

OUT_DIR = os.path.expanduser("~/Downloads/diy_animations")
os.makedirs(OUT_DIR, exist_ok=True)

def lerp(a, b, t): return a + (b - a) * t
def ease(t): return 0.5 - 0.5 * math.cos(math.pi * t)

def phase_from_frame(i):
    r = i / FRAMES
    return r * 2 if r < 0.5 else 1 - (r - 0.5) * 2

def draw_head(d, x, y, r=12):
    d.ellipse([x-r, y-r, x+r, y+r], fill=SKIN)

def draw_limb(d, x1, y1, x2, y2, w=7, c=SKIN):
    d.line([(x1,y1),(x2,y2)], fill=c, width=w)

def draw_barbell(d, cx, y, half_w=100):
    d.line([(cx-half_w, y), (cx+half_w, y)], fill=BAR, width=4)
    for side in [-1, 1]:
        px = cx + side * half_w
        d.rectangle([px-5, y-14, px+5, y+14], fill=PLATE)
        ox1 = px + side*5
        ox2 = px + side*15
        if ox1 > ox2: ox1, ox2 = ox2, ox1
        d.rectangle([ox1, y-10, ox2, y+10], fill=PLATE)

def draw_dumbbell(d, x, y):
    d.line([(x-15,y),(x+15,y)], fill=BAR, width=3)
    d.rectangle([x-18, y-8, x-12, y+8], fill=PLATE)
    d.rectangle([x+12, y-8, x+18, y+8], fill=PLATE)

def draw_floor(d):
    d.line([(0, FLOOR_Y), (WIDTH, FLOOR_Y)], fill=(200,200,210), width=2)

def save_gif(frames, name):
    path = os.path.join(OUT_DIR, f"{name}.gif")
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=80, loop=0)
    print(f"  {name}.gif ({len(frames)} frames)")

# ==================== EXERCISES ====================

def bench_press():
    frames = []
    for i in range(FRAMES):
        t = ease(phase_from_frame(i))
        img = Image.new("RGB", (WIDTH, HEIGHT), BG)
        d = ImageDraw.Draw(img)
        draw_floor(d)
        # Bench
        d.rectangle([100,210,110,FLOOR_Y], fill=BENCH)
        d.rectangle([280,210,290,FLOOR_Y], fill=BENCH)
        d.rounded_rectangle([80,195,310,215], radius=5, fill=BENCH)
        d.rectangle([85,100,95,200], fill=BAR)
        d.rectangle([295,100,305,200], fill=BAR)
        # Person
        ty = 190
        draw_head(d, 130, ty-12)
        d.rounded_rectangle([140,ty-15,260,ty+10], radius=5, fill=BLUE)
        draw_limb(d, 260,ty, 280,ty+40, c=BODY)
        draw_limb(d, 280,ty+40, 275,ty+70, c=BODY)
        # Arms + bar
        bar_y = lerp(ty-5, ty-70, t)
        es = lerp(30, 5, t)
        ey = lerp(ty-5, ty-40, t)
        draw_limb(d, 155,ty-8, 155-es,ey)
        draw_limb(d, 155-es,ey, 150,bar_y+5)
        draw_limb(d, 240,ty-8, 240+es,ey)
        draw_limb(d, 240+es,ey, 245,bar_y+5)
        draw_barbell(d, 195, bar_y, 110)
        frames.append(img)
    save_gif(frames, "bench_press")

def squat():
    frames = []
    for i in range(FRAMES):
        t = ease(phase_from_frame(i))
        img = Image.new("RGB", (WIDTH, HEIGHT), BG)
        d = ImageDraw.Draw(img)
        draw_floor(d)
        cx = 200
        drop = t * 60
        # Torso (leans forward slightly when squatting)
        lean = t * 15
        hip_y = 160 + drop
        shoulder_y = hip_y - 70 + t*10
        draw_head(d, cx+lean-5, shoulder_y-20)
        d.line([(cx+lean, shoulder_y),(cx, hip_y)], fill=BLUE, width=14)
        # Legs
        knee_x_l = cx - 20 - t*10
        knee_x_r = cx + 20 + t*10
        knee_y = hip_y + 30 - t*5
        foot_y = FLOOR_Y - 5
        draw_limb(d, cx-5,hip_y, knee_x_l,knee_y, 9, BODY)
        draw_limb(d, knee_x_l,knee_y, knee_x_l+5,foot_y, 8, BODY)
        draw_limb(d, cx+5,hip_y, knee_x_r,knee_y, 9, BODY)
        draw_limb(d, knee_x_r,knee_y, knee_x_r-5,foot_y, 8, BODY)
        # Arms holding bar
        draw_limb(d, cx+lean-15,shoulder_y+5, cx+lean-25,shoulder_y+20, 6)
        draw_limb(d, cx+lean+15,shoulder_y+5, cx+lean+25,shoulder_y+20, 6)
        # Barbell on back
        draw_barbell(d, cx+lean, shoulder_y, 90)
        frames.append(img)
    save_gif(frames, "squat")

def deadlift():
    frames = []
    for i in range(FRAMES):
        t = ease(phase_from_frame(i))
        img = Image.new("RGB", (WIDTH, HEIGHT), BG)
        d = ImageDraw.Draw(img)
        draw_floor(d)
        cx = 200
        # Standing vs bent over
        hip_y = lerp(220, 170, t)
        lean = lerp(40, 0, t)
        shoulder_x = cx + lean
        shoulder_y = lerp(160, 95, t)
        knee_bend = lerp(20, 0, t)
        draw_head(d, shoulder_x, shoulder_y-18)
        d.line([(shoulder_x, shoulder_y),(cx, hip_y)], fill=BLUE, width=14)
        # Legs
        knee_y = hip_y + 25 + knee_bend
        draw_limb(d, cx-8,hip_y, cx-18,knee_y, 9, BODY)
        draw_limb(d, cx-18,knee_y, cx-15,FLOOR_Y-5, 8, BODY)
        draw_limb(d, cx+8,hip_y, cx+18,knee_y, 9, BODY)
        draw_limb(d, cx+18,knee_y, cx+15,FLOOR_Y-5, 8, BODY)
        # Arms straight down holding bar
        bar_y = lerp(FLOOR_Y-10, hip_y+10, t)
        draw_limb(d, shoulder_x-10,shoulder_y+8, cx-10,bar_y, 6)
        draw_limb(d, shoulder_x+10,shoulder_y+8, cx+10,bar_y, 6)
        draw_barbell(d, cx, bar_y, 100)
        frames.append(img)
    save_gif(frames, "deadlift")

def overhead_press():
    frames = []
    for i in range(FRAMES):
        t = ease(phase_from_frame(i))
        img = Image.new("RGB", (WIDTH, HEIGHT), BG)
        d = ImageDraw.Draw(img)
        draw_floor(d)
        cx = 200
        shoulder_y = 130
        draw_head(d, cx, 105)
        d.line([(cx,120),(cx,200)], fill=BLUE, width=14)
        # Legs
        draw_limb(d, cx-5,200, cx-15,235, 9, BODY)
        draw_limb(d, cx-15,235, cx-12,FLOOR_Y-5, 8, BODY)
        draw_limb(d, cx+5,200, cx+15,235, 9, BODY)
        draw_limb(d, cx+15,235, cx+12,FLOOR_Y-5, 8, BODY)
        # Arms pressing up
        bar_y = lerp(shoulder_y, 60, t)
        elbow_out = lerp(35, 5, t)
        elbow_y = lerp(shoulder_y+15, shoulder_y-30, t)
        draw_limb(d, cx-12,shoulder_y, cx-12-elbow_out,elbow_y, 7)
        draw_limb(d, cx-12-elbow_out,elbow_y, cx-15,bar_y+5, 6)
        draw_limb(d, cx+12,shoulder_y, cx+12+elbow_out,elbow_y, 7)
        draw_limb(d, cx+12+elbow_out,elbow_y, cx+15,bar_y+5, 6)
        draw_barbell(d, cx, bar_y, 80)
        frames.append(img)
    save_gif(frames, "overhead_press")

def barbell_curl():
    frames = []
    for i in range(FRAMES):
        t = ease(phase_from_frame(i))
        img = Image.new("RGB", (WIDTH, HEIGHT), BG)
        d = ImageDraw.Draw(img)
        draw_floor(d)
        cx = 200
        draw_head(d, cx, 95)
        d.line([(cx,110),(cx,195)], fill=BLUE, width=14)
        # Legs
        draw_limb(d, cx-5,195, cx-12,230, 9, BODY)
        draw_limb(d, cx-12,230, cx-10,FLOOR_Y-5, 8, BODY)
        draw_limb(d, cx+5,195, cx+12,230, 9, BODY)
        draw_limb(d, cx+12,230, cx+10,FLOOR_Y-5, 8, BODY)
        # Arms curling
        elbow_y = 165
        forearm_angle = lerp(0, -80, t)
        r = 55
        hand_x_l = cx - 20 + r * math.sin(math.radians(forearm_angle))
        hand_y_l = elbow_y + r * math.cos(math.radians(forearm_angle))
        hand_x_r = cx + 20 - r * math.sin(math.radians(forearm_angle))
        draw_limb(d, cx-12,135, cx-20,elbow_y, 7)
        draw_limb(d, cx-20,elbow_y, hand_x_l,hand_y_l, 6)
        draw_limb(d, cx+12,135, cx+20,elbow_y, 7)
        draw_limb(d, cx+20,elbow_y, hand_x_r,hand_y_l, 6)
        bar_y = hand_y_l
        d.line([(hand_x_l-10, bar_y),(hand_x_r+10, bar_y)], fill=BAR, width=4)
        d.rectangle([hand_x_l-18, bar_y-8, hand_x_l-8, bar_y+8], fill=PLATE)
        d.rectangle([hand_x_r+8, bar_y-8, hand_x_r+18, bar_y+8], fill=PLATE)
        frames.append(img)
    save_gif(frames, "barbell_curl")

def lateral_raise():
    frames = []
    for i in range(FRAMES):
        t = ease(phase_from_frame(i))
        img = Image.new("RGB", (WIDTH, HEIGHT), BG)
        d = ImageDraw.Draw(img)
        draw_floor(d)
        cx = 200
        draw_head(d, cx, 90)
        d.line([(cx,105),(cx,195)], fill=BLUE, width=14)
        draw_limb(d, cx-5,195, cx-12,230, 9, BODY)
        draw_limb(d, cx-12,230, cx-10,FLOOR_Y-5, 8, BODY)
        draw_limb(d, cx+5,195, cx+12,230, 9, BODY)
        draw_limb(d, cx+12,230, cx+10,FLOOR_Y-5, 8, BODY)
        # Arms raising to sides
        angle = lerp(10, 80, t)
        arm_len = 70
        hand_lx = cx - arm_len * math.sin(math.radians(angle))
        hand_ly = 130 + arm_len * math.cos(math.radians(angle))
        hand_rx = cx + arm_len * math.sin(math.radians(angle))
        draw_limb(d, cx-10,125, hand_lx,hand_ly, 7)
        draw_limb(d, cx+10,125, hand_rx,hand_ly, 7)
        draw_dumbbell(d, hand_lx, hand_ly)
        draw_dumbbell(d, hand_rx, hand_ly)
        frames.append(img)
    save_gif(frames, "lateral_raise")

def pull_up():
    frames = []
    for i in range(FRAMES):
        t = ease(phase_from_frame(i))
        img = Image.new("RGB", (WIDTH, HEIGHT), BG)
        d = ImageDraw.Draw(img)
        # Bar at top
        d.line([(80,40),(320,40)], fill=BAR, width=6)
        d.rectangle([75,30,85,60], fill=BAR)
        d.rectangle([315,30,325,60], fill=BAR)
        cx = 200
        body_rise = t * 70
        body_top = 120 - body_rise
        draw_head(d, cx, body_top-5)
        d.line([(cx,body_top+10),(cx,body_top+80)], fill=BLUE, width=14)
        # Legs
        draw_limb(d, cx-5,body_top+80, cx-10,body_top+115, 8, BODY)
        draw_limb(d, cx+5,body_top+80, cx+10,body_top+115, 8, BODY)
        # Arms
        grip_spread = 50
        elbow_out = lerp(10, 35, t)
        elbow_y = lerp(60, 70, t)
        draw_limb(d, cx-grip_spread,42, cx-elbow_out,elbow_y, 7)
        draw_limb(d, cx-elbow_out,elbow_y, cx-8,body_top+15, 6)
        draw_limb(d, cx+grip_spread,42, cx+elbow_out,elbow_y, 7)
        draw_limb(d, cx+elbow_out,elbow_y, cx+8,body_top+15, 6)
        frames.append(img)
    save_gif(frames, "pull_up")

def hip_thrust():
    frames = []
    for i in range(FRAMES):
        t = ease(phase_from_frame(i))
        img = Image.new("RGB", (WIDTH, HEIGHT), BG)
        d = ImageDraw.Draw(img)
        draw_floor(d)
        # Bench
        d.rounded_rectangle([50,180,140,200], radius=5, fill=BENCH)
        d.rectangle([55,200,65,FLOOR_Y], fill=BENCH)
        d.rectangle([125,200,135,FLOOR_Y], fill=BENCH)
        # Person - back on bench, hips thrusting up
        hip_y = lerp(230, 185, t)
        shoulder_x, shoulder_y = 100, 175
        # Torso
        d.line([(shoulder_x,shoulder_y),(200,hip_y)], fill=BLUE, width=14)
        draw_head(d, shoulder_x-15, shoulder_y-10)
        # Legs
        foot_x = 260
        knee_y = lerp(220, 200, t)
        draw_limb(d, 200,hip_y, 235,knee_y, 9, BODY)
        draw_limb(d, 235,knee_y, foot_x,FLOOR_Y-5, 8, BODY)
        # Bar on hips
        d.line([(160,hip_y),(280,hip_y)], fill=BAR, width=4)
        d.rectangle([155,hip_y-10,165,hip_y+10], fill=PLATE)
        d.rectangle([275,hip_y-10,285,hip_y+10], fill=PLATE)
        frames.append(img)
    save_gif(frames, "hip_thrust")

def plank():
    frames = []
    for i in range(FRAMES):
        t = ease(phase_from_frame(i))
        img = Image.new("RGB", (WIDTH, HEIGHT), BG)
        d = ImageDraw.Draw(img)
        draw_floor(d)
        # Plank position with slight breathing movement
        body_y = 210 + math.sin(t * math.pi) * 3
        # Arms (forearms on ground)
        d.line([(120,body_y),(120,FLOOR_Y-5)], fill=SKIN, width=7)
        d.line([(130,body_y),(130,FLOOR_Y-5)], fill=SKIN, width=7)
        # Body
        d.line([(115,body_y),(300,body_y-5)], fill=BLUE, width=14)
        # Head
        draw_head(d, 108, body_y-12, 10)
        # Legs + toes
        d.line([(300,body_y-5),(320,FLOOR_Y-5)], fill=BODY, width=8)
        # Pulse effect for core engagement
        pulse = 0.3 + t * 0.4
        d.ellipse([180,body_y-20,220,body_y+5], outline=BLUE, width=2)
        frames.append(img)
    save_gif(frames, "plank")

def tricep_pushdown():
    frames = []
    for i in range(FRAMES):
        t = ease(phase_from_frame(i))
        img = Image.new("RGB", (WIDTH, HEIGHT), BG)
        d = ImageDraw.Draw(img)
        draw_floor(d)
        # Cable machine
        d.rectangle([170,20,230,30], fill=BAR)
        d.rectangle([195,30,205,60], fill=BAR)
        cx = 200
        draw_head(d, cx, 100)
        d.line([(cx,115),(cx,200)], fill=BLUE, width=14)
        draw_limb(d, cx-5,200, cx-12,235, 9, BODY)
        draw_limb(d, cx-12,235, cx-10,FLOOR_Y-5, 8, BODY)
        draw_limb(d, cx+5,200, cx+12,235, 9, BODY)
        draw_limb(d, cx+12,235, cx+10,FLOOR_Y-5, 8, BODY)
        # Arms - elbows pinned, forearms extending down
        elbow_y = 155
        hand_y = lerp(140, 200, t)
        draw_limb(d, cx-10,125, cx-10,elbow_y, 7)
        draw_limb(d, cx-10,elbow_y, cx-8,hand_y, 6)
        draw_limb(d, cx+10,125, cx+10,elbow_y, 7)
        draw_limb(d, cx+10,elbow_y, cx+8,hand_y, 6)
        # Cable
        d.line([(200,60),(cx,hand_y)], fill=BAR, width=2)
        # Handle
        d.line([(cx-15,hand_y),(cx+15,hand_y)], fill=BAR, width=4)
        frames.append(img)
    save_gif(frames, "tricep_pushdown")

# Generate all
print("Generating DIY exercise animations...")
bench_press()
squat()
deadlift()
overhead_press()
barbell_curl()
lateral_raise()
pull_up()
hip_thrust()
plank()
tricep_pushdown()
print(f"\nDone! {len(os.listdir(OUT_DIR))} animations in {OUT_DIR}")
