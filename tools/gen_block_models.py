import os, json
MOD = 'frontiermilitary'
RES = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources')
A = os.path.join(RES, 'assets', MOD)
D = os.path.join(RES, 'data')

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write('\n')

DIRS = ['north', 'east', 'south', 'west', 'up', 'down']
def face(tex, uv, cull=None):
    f = {'uv': [round(v, 3) for v in uv], 'texture': tex}
    return f

def box(frm, to, tex, uvs, faces=DIRS):
    """uvs: dict dir->(u1,v1,u2,v2); faces: какие грани выводить."""
    return {'from': list(frm), 'to': list(to),
            'faces': {d: face(tex, uvs[d]) for d in faces if d in uvs}}

def plane_x(x, y0, y1, z0, z1, tex, uv):
    """Плоскость x=const (грани east/west)."""
    return {'from': [x, y0, z0], 'to': [x, y1, z1],
            'faces': {'east': face(tex, uv), 'west': face(tex, uv)}}

def plane_z(z, y0, y1, x0, x1, tex, uv):
    return {'from': [x0, y0, z], 'to': [x1, y1, z],
            'faces': {'north': face(tex, uv), 'south': face(tex, uv)}}

# --- поворот набора элементов вокруг Y (только кубоиды без rotation) ---
FACE_ROT = {90: {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'},
            180: {'north': 'south', 'east': 'west', 'south': 'north', 'west': 'east'},
            270: {'north': 'west', 'west': 'south', 'south': 'east', 'east': 'north'}}
def rot_y(elements, ang):
    if ang == 0: return elements
    out = []
    for e in elements:
        assert 'rotation' not in e
        fx, fy, fz = e['from']; tx, ty, tz = e['to']
        def r(x, z):
            x, z = x - 8, z - 8
            if ang == 90:  x, z = -z, x
            elif ang == 180: x, z = -x, -z
            else: x, z = z, -x
            return x + 8, z + 8
        a = r(fx, fz); b = r(tx, tz)
        ne = {'from': [min(a[0], b[0]), fy, min(a[1], b[1])], 'to': [max(a[0], b[0]), ty, max(a[1], b[1])],
              'faces': {}}
        for d, v in e['faces'].items():
            nd = FACE_ROT[ang].get(d, d)
            ne['faces'][nd] = v
        out.append(ne)
    return out

STD_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}

def model(textures, elements, display=None, cutout=False, ao=True):
    m = {}
    if cutout: m['render_type'] = 'minecraft:cutout'
    if not ao: m['ambientocclusion'] = False
    m['textures'] = textures
    m['elements'] = elements
    if display is not None: m['display'] = display
    return m

def sides_multipart(prefix, key_pos='true', extra_post_when=None, post_model=None, side_model=None):
    parts = [{'apply': {'model': f'{MOD}:block/{post_model}'}}]
    for ang, d in ((0, 'north'), (90, 'east'), (180, 'south'), (270, 'west')):
        ap = {'model': f'{MOD}:block/{side_model}'}
        if ang: ap['y'] = ang
        parts.append({'when': {d: key_pos}, 'apply': ap})
    return {'multipart': parts}

# ================================================================= БЕТОННЫЙ БАРЬЕР
def barrier_levels(kind):
    """Профиль «Джерси»: широкое основание, плавное сужение к верху (мелкие ступени)."""
    top = 16 if kind in ('tall', 'post') else 14
    k = 1.0 if kind != 'post' else 1.33
    lv = [(0, 2, 3.0 * k)]
    ys = [2 + 1.5 * i for i in range(8)] + [top]
    for i in range(8):
        y0, y1 = ys[i], ys[i + 1]
        hw = (2.85 - 0.17 * i) * k
        lv.append((y0, y1, round(hw, 3)))
    return lv

def barrier_side(kind):
    els = []
    for i, (y0, y1, hw) in enumerate(barrier_levels(kind)):
        uv_side = (0, 16 - y1, 8, 16 - y0)
        uvs = {'east': uv_side, 'west': uv_side, 'up': (8 - hw, 4, 8 + hw, 12), 'down': (8 - hw, 4, 8 + hw, 12)}
        faces = ['east', 'west', 'up'] + (['down'] if i == 0 else [])
        els.append(box((8 - hw, y0, 0), (8 + hw, y1, 8), '#c', uvs, faces))
    return els

def barrier_post():
    els = []
    for i, (y0, y1, hw) in enumerate(barrier_levels('post')):
        us = (8 - hw, 16 - y1, 8 + hw, 16 - y0)
        uvs = {d: us for d in ('north', 'east', 'south', 'west')}
        uvs['up'] = (8 - hw, 4, 8 + hw, 12 if hw > 3 else 4 + 2 * hw)
        uvs['up'] = (8 - hw, 4, 8 + hw, 4 + 2 * hw)
        uvs['down'] = uvs['up']
        faces = ['north', 'east', 'south', 'west', 'up'] + (['down'] if i == 0 else [])
        els.append(box((8 - hw, y0, 8 - hw), (8 + hw, y1, 8 + hw), '#c', uvs, faces))
    return els

def gen_barrier():
    tex = {'c': f'{MOD}:block/concrete_barrier', 'particle': f'{MOD}:block/concrete_barrier'}
    w(f'{A}/models/block/concrete_barrier_post.json', model(tex, barrier_post()))
    w(f'{A}/models/block/concrete_barrier_side.json', model(tex, barrier_side('low')))
    w(f'{A}/models/block/concrete_barrier_side_tall.json', model(tex, barrier_side('tall')))
    inv = barrier_post() + rot_y(barrier_side('low'), 0) + rot_y(barrier_side('low'), 180)
    # предметная модель: прямой отрезок барьера (две стороны + центральный профиль без поста)
    inv = barrier_side('low') + rot_y(barrier_side('low'), 180)
    w(f'{A}/models/item/concrete_barrier.json', model(tex, inv, STD_DISPLAY))
    ms = []
    ms.append({'when': {'up': 'true'}, 'apply': {'model': f'{MOD}:block/concrete_barrier_post'}})
    for ang, d in ((0, 'north'), (90, 'east'), (180, 'south'), (270, 'west')):
        for h, mn in (('low', 'concrete_barrier_side'), ('tall', 'concrete_barrier_side_tall')):
            ap = {'model': f'{MOD}:block/{mn}'}
            if ang: ap['y'] = ang
            ms.append({'when': {d: h}, 'apply': ap})
    w(f'{A}/blockstates/concrete_barrier.json', {'multipart': ms})

# ================================================================= МЕТАЛЛИЧЕСКОЕ ОГРАЖДЕНИЕ
def fence_post():
    P = '#p'
    def u(x0, v0, x1, v1): return (x0, v0, x1, v1)
    els = []
    els.append(box((5.5, 0, 5.5), (10.5, 1, 10.5), P, {d: u(0, 0, 5, 1) for d in ('north', 'east', 'south', 'west')} | {'up': u(0, 0, 5, 5), 'down': u(0, 0, 5, 5)}))
    side = u(2, 1, 5, 16)
    els.append(box((6.5, 1, 6.5), (9.5, 15, 9.5), P, {d: side for d in ('north', 'east', 'south', 'west')} | {'up': u(2, 2, 5, 5)}, ['north', 'east', 'south', 'west']))
    els.append(box((6, 15, 6), (10, 16, 10), P, {d: u(1, 0, 5, 1) for d in ('north', 'east', 'south', 'west')} | {'up': u(1, 1, 5, 5), 'down': u(1, 1, 5, 5)}))
    return els

def fence_side():
    P, M = '#p', '#m'
    els = []
    # верхняя труба и нижний натяжной трос
    top = {'east': (0, 5, 8, 6.5), 'west': (0, 5, 8, 6.5), 'up': (0, 3, 1.5, 11), 'down': (0, 3, 1.5, 11)}
    els.append(box((7.25, 11, 0), (8.75, 12.5, 8), P, top, ['east', 'west', 'up', 'down']))
    bot = {'east': (0, 5, 8, 5.8), 'west': (0, 5, 8, 5.8), 'up': (0, 3, 1.2, 11), 'down': (0, 3, 1.2, 11)}
    els.append(box((7.4, 1, 0), (8.6, 1.8, 8), P, bot, ['east', 'west', 'up', 'down']))
    # сетка-рабица (плоскость, cutout)
    els.append(plane_x(8, 1.8, 11, 0, 8, M, (0, 0, 8, 9.2)))
    # колючая проволока поверх (плоскость, cutout) — выше верхней трубы
    els.append(plane_x(8, 12, 16, 0, 8, M, (0, 11, 8, 15)))
    return els

def gen_fence():
    tex = {'p': f'{MOD}:block/metal_fence', 'm': f'{MOD}:block/metal_fence_mesh', 'particle': f'{MOD}:block/metal_fence'}
    w(f'{A}/models/block/metal_fence_post.json', model(tex, fence_post(), cutout=True))
    w(f'{A}/models/block/metal_fence_side.json', model(tex, fence_side(), cutout=True))
    inv = fence_post() + fence_side() + rot_y(fence_side(), 180)
    w(f'{A}/models/item/metal_fence.json', model(tex, inv, STD_DISPLAY, cutout=True))
    w(f'{A}/blockstates/metal_fence.json', sides_multipart('f', post_model='metal_fence_post', side_model='metal_fence_side'))

# ================================================================= ЗАЩИТНАЯ РЕШЁТКА
def bars_post():
    B = '#b'
    s = (1, 0, 4, 16)
    return [box((6.5, 0, 6.5), (9.5, 16, 9.5), B, {d: s for d in ('north', 'east', 'south', 'west')} | {'up': (1, 1, 4, 4), 'down': (1, 1, 4, 4)})]

def bars_side():
    B = '#b'
    els = []
    def rail(y0, y1, th_x0, th_x1):
        h = y1 - y0
        return box((th_x0, y0, 0), (th_x1, y1, 6.5), B,
                   {'east': (0, 4, 6.5, 4 + h), 'west': (0, 4, 6.5, 4 + h),
                    'up': (1, 0, 1 + (th_x1 - th_x0), 6.5), 'down': (1, 0, 1 + (th_x1 - th_x0), 6.5)},
                   ['east', 'west', 'up', 'down'])
    els.append(rail(13.5, 15.5, 7, 9))      # верхняя рама
    els.append(rail(0.5, 2.5, 7, 9))        # нижняя рама
    els.append(rail(7, 8.5, 7.25, 8.75))    # средняя полоса
    for zc in (1.0, 3.25, 5.5):             # вертикальные прутья 1x1
        els.append(box((7.5, 2.5, zc - .5), (8.5, 13.5, zc + .5), B,
                       {'east': (4, 2, 5, 13), 'west': (4, 2, 5, 13), 'north': (4, 2, 5, 13), 'south': (4, 2, 5, 13)},
                       ['east', 'west', 'north', 'south']))
    return els

def gen_bars():
    tex = {'b': f'{MOD}:block/security_bars', 'particle': f'{MOD}:block/security_bars'}
    w(f'{A}/models/block/security_bars_post.json', model(tex, bars_post()))
    w(f'{A}/models/block/security_bars_side.json', model(tex, bars_side()))
    inv = bars_post() + bars_side() + rot_y(bars_side(), 180)
    w(f'{A}/models/item/security_bars.json', model(tex, inv, STD_DISPLAY))
    w(f'{A}/blockstates/security_bars.json', sides_multipart('b', post_model='security_bars_post', side_model='security_bars_side'))

# ================================================================= ФОНАРЬ ПОСТА
def lamp_elements(hanging):
    L = '#l'
    def m(w_, h_): return (0, 0, w_, min(h_, 4))
    els = []
    def metal(frm, to, extra_faces=DIRS):
        sx, sy, sz = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
        uvs = {'north': m(sx, sy), 'south': m(sx, sy), 'east': m(sz, sy), 'west': m(sz, sy),
               'up': m(sx, sz), 'down': m(sx, sz)}
        els.append(box(frm, to, L, uvs, extra_faces))
    # основание и верх
    off = 1 if hanging else 0
    metal((5, 0 + off, 5), (11, 1 + off, 11))
    # стойки клетки
    for (px, pz) in ((5, 5), (10, 5), (5, 10), (10, 10)):
        els.append(box((px, 1 + off, pz), (px + 1, 7 + off, pz + 1), L,
                       {d: (8, 4, 9, 11) for d in ('north', 'east', 'south', 'west')}, ['north', 'east', 'south', 'west']))
    # стекло
    gl = {'north': (0, 4.5, 4.5, 10), 'south': (0, 4.5, 4.5, 10), 'east': (0, 4.5, 4.5, 10), 'west': (0, 4.5, 4.5, 10),
          'up': (1, 5, 5.5, 9.5), 'down': (1, 5, 5.5, 9.5)}
    els.append(box((5.5, 1 + off, 5.5), (10.5, 7 + off, 10.5), L, gl, ['north', 'east', 'south', 'west']))
    # нить / горелка
    els.append(box((7, 2.5 + off, 7), (9, 5 + off, 9), L, {d: (3, 6.5, 4, 7.5) for d in DIRS}))
    # колпак
    metal((5, 7 + off, 5), (11, 8 + off, 11))
    metal((6, 8 + off, 6), (10, 8.8 + off, 10))
    # дужка
    metal((7.25, 8.8 + off, 7.25), (8.75, 9.8 + off, 8.75))
    if hanging:
        # цепь — две скрещенные плоскости (cutout)
        ch = '#l'
        els.append({'from': [8, 10.8, 6.5], 'to': [8, 16, 9.5], 'faces': {'east': face(ch, (0, 12, 3, 16)), 'west': face(ch, (0, 12, 3, 16))}})
        els.append({'from': [6.5, 10.8, 8], 'to': [9.5, 16, 8], 'faces': {'north': face(ch, (0, 12, 3, 16)), 'south': face(ch, (0, 12, 3, 16))}})
    return els

def gen_lamp():
    tex = {'l': f'{MOD}:block/watch_post_lamp', 'particle': f'{MOD}:block/watch_post_lamp'}
    w(f'{A}/models/block/watch_post_lamp.json', model(tex, lamp_elements(False), cutout=True))
    w(f'{A}/models/block/watch_post_lamp_hanging.json', model(tex, lamp_elements(True), cutout=True))
    disp = dict(STD_DISPLAY)
    disp['gui'] = {'rotation': [30, 225, 0], 'translation': [0, -1.5, 0], 'scale': [1.1, 1.1, 1.1]}
    disp['ground'] = {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.45, 0.45, 0.45]}
    disp['fixed'] = {'rotation': [0, 0, 0], 'translation': [0, -2, 0], 'scale': [0.9, 0.9, 0.9]}
    disp['thirdperson_righthand'] = {'rotation': [0, 0, 0], 'translation': [0, 1, 0], 'scale': [0.6, 0.6, 0.6]}
    disp['firstperson_righthand'] = {'rotation': [0, 45, 0], 'translation': [0, -2, 0], 'scale': [0.7, 0.7, 0.7]}
    disp['firstperson_lefthand'] = {'rotation': [0, 225, 0], 'translation': [0, -2, 0], 'scale': [0.7, 0.7, 0.7]}
    w(f'{A}/models/item/watch_post_lamp.json', model(tex, lamp_elements(False), disp, cutout=True))
    w(f'{A}/blockstates/watch_post_lamp.json', {'variants': {
        'hanging=false': {'model': f'{MOD}:block/watch_post_lamp'},
        'hanging=true': {'model': f'{MOD}:block/watch_post_lamp_hanging'}}})

# ================================================================= data: теги и лут
BLOCKS = ['metal_fence', 'security_bars', 'concrete_barrier', 'watch_post_lamp']
def gen_data():
    ids = [f'{MOD}:{b}' for b in BLOCKS]
    w(f'{D}/minecraft/tags/blocks/mineable/pickaxe.json', {'replace': False, 'values': ids})
    w(f'{D}/minecraft/tags/blocks/needs_stone_tool.json', {'replace': False, 'values': ids})
    w(f'{D}/minecraft/tags/blocks/fences.json', {'replace': False, 'values': [f'{MOD}:metal_fence']})
    w(f'{D}/minecraft/tags/blocks/walls.json', {'replace': False, 'values': [f'{MOD}:concrete_barrier']})
    w(f'{D}/minecraft/tags/items/fences.json', {'replace': False, 'values': [f'{MOD}:metal_fence']})
    w(f'{D}/minecraft/tags/items/walls.json', {'replace': False, 'values': [f'{MOD}:concrete_barrier']})
    for b in BLOCKS:
        w(f'{D}/{MOD}/loot_tables/blocks/{b}.json', {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1.0, 'bonus_rolls': 0.0,
                       'entries': [{'type': 'minecraft:item', 'name': f'{MOD}:{b}'}],
                       'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
            'random_sequence': f'{MOD}:blocks/{b}'})

if __name__ == '__main__':
    gen_barrier(); gen_fence(); gen_bars(); gen_lamp(); gen_data()
    print('block models, blockstates, tags, loot tables done')
