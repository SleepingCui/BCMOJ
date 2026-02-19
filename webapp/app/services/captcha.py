import random
import string
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import io
import base64
from flask import session
from datetime import datetime, timedelta
import math
from app.core.config import get_config

def generate_captcha():
    config = get_config()
    captcha_config = config['captcha_config']
    
    width = captcha_config['width']
    height = captcha_config['height']
    font_size = captcha_config['font_size']
    text_min_len, text_max_len = tuple(captcha_config['text_length'])
    interference_lines = captcha_config['interference_lines']
    interference_points = captcha_config['interference_points']
    wave_amp_min, wave_amp_max = tuple(captcha_config['wave_amplitude'])
    wave_freq_min, wave_freq_max = tuple(captcha_config['wave_frequency'])
    noise_min, noise_max = tuple(captcha_config['noise_range'])
    bg_color = tuple(captcha_config['bg_color'])
    text_color_min, text_color_max = tuple(captcha_config['text_color_range'])
    inter_color_min, inter_color_max = tuple(captcha_config['interference_color_range'])
    expire_time = captcha_config['expire_time']
    
    captcha_text = ''.join(random.choices(string.ascii_uppercase + string.digits, k=random.randint(text_min_len, text_max_len)))
    image = Image.new('RGB', (width, height), bg_color)
    draw = ImageDraw.Draw(image)
    try:
        font = ImageFont.truetype("arial.ttf", font_size)
    except:
        font = ImageFont.load_default()
    
    try:
        bbox = draw.textbbox((0, 0), captcha_text, font=font)
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]
    except:
        text_width = draw.textlength(captcha_text, font=font) if hasattr(draw, 'textlength') else len(captcha_text) * (font_size // 2)
        text_height = font_size
    
    x = (width - text_width) // 2
    y = (height - text_height) // 2
    
    text_color = (random.randint(text_color_min, text_color_max), random.randint(text_color_min, text_color_max), random.randint(text_color_min, text_color_max))
    draw.text((x, y), captcha_text, fill=text_color, font=font)

    for _ in range(interference_lines):
        start = (random.randint(0, width), random.randint(0, height))
        end = (random.randint(0, width), random.randint(0, height))
        line_color = (random.randint(inter_color_min, inter_color_max), random.randint(inter_color_min, inter_color_max), random.randint(inter_color_min, inter_color_max))
        draw.line([start, end], fill=line_color, width=1)
    
    for _ in range(interference_points):
        x, y = random.randint(0, width), random.randint(0, height)
        point_color = (random.randint(inter_color_min, inter_color_max), random.randint(inter_color_min, inter_color_max), random.randint(inter_color_min, inter_color_max))
        draw.point((x, y), fill=point_color)
    
    pixels = image.load()
    new_image = Image.new('RGB', (width, height), bg_color)
    new_pixels = new_image.load()
    
    amplitude = random.uniform(wave_amp_min, wave_amp_max)
    frequency = random.uniform(wave_freq_min, wave_freq_max)
    
    for x in range(width):
        for y in range(height):
            offset = amplitude * math.sin(frequency * x)
            new_y = y + int(offset)
            if 0 <= new_y < height:
                new_pixels[x, y] = pixels[x, new_y]
            else:
                new_pixels[x, y] = bg_color
    
    for x in range(width):
        for y in range(height):
            r, g, b = new_pixels[x, y]
            noise = random.randint(noise_min, noise_max)
            r = max(0, min(255, r + noise))
            g = max(0, min(255, g + noise))
            b = max(0, min(255, b + noise))
            new_pixels[x, y] = (r, g, b)
    
    buffer = io.BytesIO()
    new_image.save(buffer, format='PNG')
    img_str = base64.b64encode(buffer.getvalue()).decode()
    
    session['captcha_text'] = captcha_text.upper()
    session['captcha_expire'] = (datetime.now() + timedelta(seconds=expire_time)).timestamp()
    
    return img_str

def verify_captcha(user_input):
    if 'captcha_text' not in session or 'captcha_expire' not in session:
        return False
    
    expire_time = session['captcha_expire']
    if datetime.now().timestamp() > expire_time:
        session.pop('captcha_text', None)
        session.pop('captcha_expire', None)
        return False
    is_valid = session['captcha_text'].upper() == user_input.upper()
    
    session.pop('captcha_text', None)
    session.pop('captcha_expire', None)
    
    return is_valid

def update_captcha_config(**kwargs):
    config = get_config()
    captcha_config = config['captcha_config']
    for key, value in kwargs.items():
        if key in captcha_config:
            captcha_config[key] = value

def get_captcha_config():
    config = get_config()
    captcha_config = config['captcha_config']
    return {k: v for k, v in captcha_config.items()}