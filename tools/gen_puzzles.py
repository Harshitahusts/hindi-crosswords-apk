"""Generates app/src/main/assets/puzzles.json from words.txt (word=clue per line).
Each crossword box holds one akshara (syllable), like Hindi newspaper puzzles.
Run: python3 tools/gen_puzzles.py   (deterministic)"""
import json, random, os, unicodedata

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'puzzles.json')
N, LEVELS, MIN_W, MAX_W, MAX_USE = 8, 100, 8, 12, 3

def is_cons(ch): return 'क' <= ch <= 'ह'
def is_vowel(ch): return 'ऄ' <= ch <= 'औ' or ch in 'ॠॡ'
HALANT = '्'

def aksharas(word):
    word = unicodedata.normalize('NFD', word)  # ड़ -> ड + ़ (same as the app keyboard)
    out = []
    for ch in word:
        if (is_cons(ch) or is_vowel(ch)) and not (out and out[-1].endswith(HALANT) and is_cons(ch)):
            out.append(ch)
        elif out:
            out[-1] += ch
        else:
            raise ValueError(word)
    return out

def load():
    seen, words = set(), []
    for line in open(os.path.join(HERE, 'words.txt'), encoding='utf-8'):
        line = line.strip()
        if not line: continue
        w, clue = line.split('=', 1)
        w = unicodedata.normalize('NFD', w)
        a = aksharas(w)
        if w in seen or not 2 <= len(a) <= 6: continue
        seen.add(w); words.append((w, a, clue.strip()))
    return words

def fits(g, a, r, c, dr, dc, first):
    L = len(a)
    er, ec = r + dr * (L - 1), c + dc * (L - 1)
    if not (0 <= r < N and 0 <= c < N and 0 <= er < N and 0 <= ec < N): return -1
    br, bc, ar, ac = r - dr, c - dc, er + dr, ec + dc
    if 0 <= br < N and 0 <= bc < N and g[br][bc]: return -1
    if 0 <= ar < N and 0 <= ac < N and g[ar][ac]: return -1
    cross = 0
    for i in range(L):
        y, x = r + dr * i, c + dc * i
        if g[y][x]:
            if g[y][x] != a[i]: return -1
            cross += 1
        else:
            for py, px in ((y + dc, x + dr), (y - dc, x - dr)):
                if 0 <= py < N and 0 <= px < N and g[py][px]: return -1
    if cross == L or (not first and cross == 0): return -1
    return cross

def build(rng, pool, use):
    g = [[None] * N for _ in range(N)]
    placed = []
    cands = sorted(pool, key=lambda w: (use[w[0]], rng.random()))
    first = next(w for w in cands if len(w[1]) >= 4)
    r, c = rng.randrange(N), rng.randrange(N - len(first[1]) + 1)
    dr, dc = (0, 1)
    where = {}
    for i, s in enumerate(first[1]):
        g[r][c + i] = s
        where.setdefault(s, set()).add((r, c + i))
    placed.append((first, r, c, 'A'))
    used = {first[0]}
    progress = True
    while progress and len(placed) < MAX_W:
        progress = False
        for w in cands:
            if w[0] in used: continue
            best = None
            for i, s in enumerate(w[1]):
                for (y, x) in where.get(s, ()):
                    for d, (dr, dc) in (('A', (0, 1)), ('D', (1, 0))):
                        rr, cc = y - dr * i, x - dc * i
                        k = fits(g, w[1], rr, cc, dr, dc, False)
                        if k > 0 and (best is None or k > best[0] or (k == best[0] and rng.random() < .3)):
                            best = (k, rr, cc, d)
            if best:
                _, rr, cc, d = best
                dr, dc = (0, 1) if d == 'A' else (1, 0)
                for i, s in enumerate(w[1]):
                    g[rr + dr * i][cc + dc * i] = s
                    where.setdefault(s, set()).add((rr + dr * i, cc + dc * i))
                placed.append((w, rr, cc, d)); used.add(w[0]); progress = True
                break
    return g, placed

def runs(g):
    res = set()
    for d, (dr, dc) in (('A', (0, 1)), ('D', (1, 0))):
        for r in range(N):
            for c in range(N):
                if not g[r][c]: continue
                pr, pc = r - dr, c - dc
                if 0 <= pr < N and 0 <= pc < N and g[pr][pc]: continue
                L = 0
                while 0 <= r + dr * L < N and 0 <= c + dc * L < N and g[r + dr * L][c + dc * L]: L += 1
                if L >= 2: res.add((r, c, d, L))
    return res

def validate(p):
    g = [[None if s == '' else s for s in row] for row in p['grid']]
    assert len(g) == N and all(len(row) == N for row in g)
    exp = set()
    for cl in p['clues']:
        a = aksharas(cl['ans'])
        dr, dc = (0, 1) if cl['d'] == 'A' else (1, 0)
        for i, s in enumerate(a): assert g[cl['r'] + dr * i][cl['c'] + dc * i] == s, (p['id'], cl)
        assert cl['clue'] and cl['ans'] not in cl['clue'], (p['id'], cl)
        exp.add((cl['r'], cl['c'], cl['d'], len(a)))
    assert exp == runs(g), p['id']
    assert MIN_W <= len(p['clues']) <= MAX_W
    assert len({c['ans'] for c in p['clues']}) == len(p['clues'])

def main():
    rng = random.Random(2026)
    pool = load()
    use = {w[0]: 0 for w in pool}
    puzzles, fails = [], 0
    while len(puzzles) < LEVELS:
        avail = [w for w in pool if use[w[0]] < MAX_USE + fails // 5]
        fails += 1
        best = None
        for _ in range(60):
            g, placed = build(rng, avail, use)
            if len(placed) >= MIN_W and (best is None or len(placed) > len(best[1])):
                best = (g, placed)
        if not best: continue
        g, placed = best
        if {(r, c, d, len(w[1])) for w, r, c, d in placed} != runs(g): continue
        for w, *_ in placed: use[w[0]] += 1
        placed.sort(key=lambda p: (p[1], p[2]))
        p = {'id': len(puzzles) + 1, 'title': 'वर्ग पहेली %d' % (len(puzzles) + 1), 'difficulty': 'सामान्य',
             'size': N, 'grid': [[s or '' for s in row] for row in g],
             'clues': [{'r': r, 'c': c, 'd': d, 'ans': w[0], 'clue': w[2]} for w, r, c, d in placed]}
        validate(p)
        puzzles.append(p)
        fails = 0
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump(puzzles, open(OUT, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
    cnt = [len(p['clues']) for p in puzzles]
    print('levels', len(puzzles), 'words min/max/avg', min(cnt), max(cnt), sum(cnt) / len(cnt),
          'distinct words', sum(1 for v in use.values() if v), 'of', len(pool))

if __name__ == '__main__':
    import sys
    if sys.argv[1:] == ['--check']:
        for p in json.load(open(OUT, encoding='utf-8')): validate(p)
        print('all puzzles valid')
    else:
        main()
