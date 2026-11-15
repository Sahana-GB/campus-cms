#!/usr/bin/env python3
"""Turn the profiling results into one self-contained HTML page with a bar chart.

Reads profiling/before.txt and profiling/after.txt (written by scripts/profile_c.sh) and writes
profiling/profiling_report.html. Standard library only. Open the HTML file in any browser.

  python3 python/profile_html.py
  python3 python/profile_html.py --before profiling/before.txt --after profiling/after.txt --out report.html
"""
import argparse
import ast
import html
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ROW = re.compile(r"^(.+?)\s{2,}([\d.]+)\s+([\d.]+)\s+([\d.]+)\s*$")


def parse(path):
    """Return (header lines, {query: average ms}, {query: plan lines})."""
    text = Path(path).read_text(encoding="utf-8").splitlines()
    averages, plans, current, header = {}, {}, None, []
    in_plans = False
    for line in text:
        if line.startswith("QUERY PLANS"):
            in_plans = True
            continue
        if not in_plans:
            m = ROW.match(line)
            if m and not line.startswith("query"):
                averages[m.group(1).strip()] = float(m.group(2))
            elif line.strip() and not set(line.strip()) <= {"-"} and not line.startswith("query"):
                header.append(line.strip())
        else:
            if line.startswith("[") and line.endswith("]"):
                current = line[1:-1]
                plans[current] = []
            elif current and line.strip() and not set(line.strip()) <= {"-"}:
                plans[current].append(line.rstrip())
    return header, averages, plans


def bar_width(value, low=0.01, high=200.0):
    """Width in percent on a log scale, so 0.03 ms and 146 ms are both visible."""
    value = max(value, low)
    return 100.0 * (math.log10(value) - math.log10(low)) / (math.log10(high) - math.log10(low))


def speedup_text(before, after):
    ratio = before / after if after else 0
    if ratio < 1.15:
        return "no real change", "flat"
    return f"{ratio:.1f}x faster", "good"


PAGE = """<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Campus CMS profiling report</title>
<style>
:root { --bg:#f4f5f9; --card:#ffffff; --ink:#1f2937; --muted:#6b7280; --line:#e5e7eb;
        --before:#e45756; --after:#4f46e5; --good:#15803d; --flat:#6b7280; }
@media (prefers-color-scheme: dark) {
  :root { --bg:#0f172a; --card:#1e293b; --ink:#e5e7eb; --muted:#94a3b8; --line:#334155;
          --before:#f87171; --after:#818cf8; --good:#4ade80; --flat:#94a3b8; } }
* { box-sizing: border-box; }
body { font-family: "Segoe UI", Arial, sans-serif; background: var(--bg); color: var(--ink);
       margin: 0; padding: 28px 16px; line-height: 1.45; }
main { max-width: 980px; margin: 0 auto; }
h1 { font-size: 26px; margin: 0 0 4px; }
p.sub { color: var(--muted); margin: 0 0 22px; }
.cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; margin-bottom: 22px; }
.card { background: var(--card); border: 1px solid var(--line); border-radius: 10px; padding: 14px 16px; }
.card .big { font-size: 28px; font-weight: 700; color: var(--after); }
.card .small { color: var(--muted); font-size: 13px; }
section.chart { background: var(--card); border: 1px solid var(--line); border-radius: 10px; padding: 18px 18px 8px; }
h2 { font-size: 18px; margin: 0 0 4px; }
.legend { display: flex; gap: 18px; font-size: 13px; color: var(--muted); margin: 6px 0 14px; flex-wrap: wrap; }
.dot { display: inline-block; width: 12px; height: 12px; border-radius: 3px; margin-right: 6px; vertical-align: -1px; }
.row { display: grid; grid-template-columns: 220px 1fr 110px; gap: 12px; align-items: center;
       padding: 10px 0; border-top: 1px solid var(--line); }
.name { font-size: 14px; }
.bars .line { display: flex; align-items: center; gap: 8px; margin: 3px 0; }
.bar { height: 14px; border-radius: 3px; min-width: 3px; }
.bar.before { background: var(--before); } .bar.after { background: var(--after); }
.val { font-size: 12px; color: var(--muted); white-space: nowrap; }
.badge { font-size: 13px; font-weight: 600; text-align: right; }
.badge.good { color: var(--good); } .badge.flat { color: var(--flat); }
.note { color: var(--muted); font-size: 13px; margin: 10px 0 12px; }
details { background: var(--card); border: 1px solid var(--line); border-radius: 10px; margin: 12px 0; padding: 10px 16px; }
summary { cursor: pointer; font-weight: 600; }
pre { background: var(--bg); padding: 10px 12px; border-radius: 6px; overflow-x: auto; font-size: 12.5px; }
.plans { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
@media (max-width: 720px) { .row { grid-template-columns: 1fr; } .badge { text-align: left; } .plans { grid-template-columns: 1fr; } }
footer { color: var(--muted); font-size: 12.5px; margin: 18px 0; }
</style></head><body><main>
<h1>Campus CMS profiling report</h1>
<p class="sub">Enrollment and reporting queries, before and after adding indexes. __DATA__</p>
<div class="cards">__CARDS__</div>
<section class="chart">
<h2>Average query time (ms)</h2>
<div class="legend"><span><span class="dot" style="background:var(--before)"></span>Before indexes</span>
<span><span class="dot" style="background:var(--after)"></span>After indexes</span>
<span>Bar length uses a log scale, so small and large times are both visible.</span></div>
__ROWS__
</section>
<p class="note">Grade averages must read every published grade, so no index can skip that work. The "all PENDING"
list returns thousands of rows, so its time goes into building and sorting the result.</p>
<h2>Query plans (SCAN = reads the whole table, SEARCH = uses an index)</h2>
__PLANS__
<footer>Generated by python/profile_html.py from profiling/before.txt and profiling/after.txt.</footer>
</main></body></html>"""


def build(before_path, after_path):
    bh, before, bplans = parse(before_path)
    ah, after, aplans = parse(after_path)
    names = [n for n in before if n in after]
    rows = []
    for n in names:
        b, a = before[n], after[n]
        text, cls = speedup_text(b, a)
        rows.append(
            '<div class="row"><div class="name">%s</div><div class="bars">'
            '<div class="line"><div class="bar before" style="width:%.1f%%"></div><span class="val">%.3f ms</span></div>'
            '<div class="line"><div class="bar after" style="width:%.1f%%"></div><span class="val">%.3f ms</span></div>'
            '</div><div class="badge %s">%s</div></div>'
            % (html.escape(n), bar_width(b), b, bar_width(a), a, cls, text))
    best = max(names, key=lambda n: before[n] / after[n] if after[n] else 0)
    cards = (
        '<div class="card"><div class="big">%d</div><div class="small">queries measured</div></div>'
        '<div class="card"><div class="big">%.0fx</div><div class="small">best speed-up: %s</div></div>'
        '<div class="card"><div class="big">%.1f ms &rarr; %.1f ms</div><div class="small">Browse Courses, the first student page</div></div>'
        % (len(names), before[best] / after[best], html.escape(best),
           before.get("student: browse courses", 0), after.get("student: browse courses", 0)))
    plans = []
    for n in names:
        if n in bplans and n in aplans:
            plans.append(
                '<details><summary>%s</summary><div class="plans"><div><b>Before</b><pre>%s</pre></div>'
                '<div><b>After</b><pre>%s</pre></div></div></details>'
                % (html.escape(n), html.escape("\n".join(bplans[n])), html.escape("\n".join(aplans[n]))))
    data = ""
    if bh and bh[0].startswith("rows:"):
        try:
            r = ast.literal_eval(bh[0][len("rows:"):].strip())
            data = "Data: " + ", ".join("{:,} {}".format(v, k) for k, v in r.items()) + "."
        except (ValueError, SyntaxError):
            data = html.escape(bh[0])
    page = PAGE.replace("__DATA__", data).replace("__CARDS__", cards)
    return page.replace("__ROWS__", "\n".join(rows)).replace("__PLANS__", "\n".join(plans))


def main():
    p = argparse.ArgumentParser(description="Make an HTML page from the profiling results")
    p.add_argument("--before", default=str(ROOT / "profiling" / "before.txt"))
    p.add_argument("--after", default=str(ROOT / "profiling" / "after.txt"))
    p.add_argument("--out", default=str(ROOT / "profiling" / "profiling_report.html"))
    args = p.parse_args()
    Path(args.out).write_text(build(args.before, args.after), encoding="utf-8")
    print(f"Saved HTML: {args.out}")


if __name__ == "__main__":
    main()
