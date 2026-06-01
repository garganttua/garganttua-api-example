#!/usr/bin/env python3
"""
Benchmark des modes de réflexion de garganttua-api-example : runtime / aot / native.

Pour chaque mode : construit l'artefact, l'exécute N fois (défaut 20) en mesurant
temps mur, CPU (user+sys), RSS pic et démarrage applicatif, capture les timings du
pipeline (stages) et les stats d'exécution (opérations), puis produit un rapport PDF.

Usage :
    python3 benchmark/benchmark.py [--runs 20] [--warmup 3] [--skip-build] [--modes native,aot,runtime]

Prérequis :
    - Un JDK GraalVM 21 pour le build natif. Auto-détecté sous ~/.sdkman/candidates/java/*graal*,
      ou via $GRAALVM_HOME.
    - Maven + les artefacts garganttua dans ~/.m2 (mêmes que pour `mvn -Dreflection=native package`).
    - matplotlib (auto-installé via `pip install --user` si absent).

Sorties (à la racine du module) :
    reflection-benchmark.pdf, benchmark/results.csv, benchmark/results.json
"""
import argparse, glob, json, os, platform, re, shutil, subprocess, sys, time, datetime, statistics as st

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)                       # module root (contains pom.xml)
WORK = os.path.join(HERE, ".work")
PDF = os.path.join(PROJECT, "reflection-benchmark.pdf")
CSV = os.path.join(HERE, "results.csv")
JSON = os.path.join(HERE, "results.json")

MODES_ALL = ["runtime", "aot", "native"]
COLORS = {"runtime": "#d9534f", "aot": "#f0ad4e", "native": "#5cb85c"}
ANSI = re.compile(r"\x1b\[[0-9;]*m")
STARTUP = re.compile(r"Startup time[^0-9]*([0-9]+(?:[.,][0-9]+)?)\s*(ms|s)\b")
ROW = re.compile(r"^\s+(\S+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s*$")


# ---------------------------------------------------------------- environment
def find_graalvm():
    if os.environ.get("GRAALVM_HOME"):
        return os.environ["GRAALVM_HOME"]
    cands = sorted(glob.glob(os.path.expanduser("~/.sdkman/candidates/java/*graal*")))
    # le projet est en JDK 21 (--enable-preview) : préférer un GraalVM 21.x
    pool = [c for c in cands if "21" in os.path.basename(c)] or cands
    if pool:
        return pool[-1]
    sys.exit("error: GraalVM JDK 21 introuvable. Exporte GRAALVM_HOME=/chemin/vers/graalvm-21.")


def ensure_matplotlib():
    try:
        import matplotlib  # noqa
        return
    except ImportError:
        print("[setup] installation de matplotlib (pip --user) ...")
        subprocess.run([sys.executable, "-m", "pip", "install", "--user", "--quiet", "matplotlib"], check=True)


def first_line(cmd):
    try:
        r = subprocess.run(cmd, capture_output=True, text=True)
        return (r.stdout + r.stderr).splitlines()[0]
    except Exception:
        return "?"


def env_info(graalvm, run_java):
    cpu = mem = "?"
    try:
        for l in open("/proc/cpuinfo"):
            if l.startswith("model name"):
                cpu = l.split(":", 1)[1].strip(); break
    except Exception:
        pass
    try:
        for l in open("/proc/meminfo"):
            if l.startswith("MemTotal"):
                mem = f"{int(l.split()[1]) / 1024 / 1024:.1f} GiB"; break
    except Exception:
        pass
    return {
        "cpu": cpu, "cores": os.cpu_count(), "ram": mem,
        "os": f"{platform.system()} {platform.release()}",
        "jdk_run": first_line([run_java, "-version"]),
        "graalvm_build": first_line([os.path.join(graalvm, "bin", "native-image"), "--version"]),
        "date": datetime.datetime.now().strftime("%Y-%m-%d %H:%M"),
    }


# ---------------------------------------------------------------- build
def mvn_build(mode, graalvm):
    env = dict(os.environ)
    if mode == "native":
        env["JAVA_HOME"] = graalvm
        env["PATH"] = os.path.join(graalvm, "bin") + os.pathsep + env["PATH"]
    log = os.path.join(WORK, f"build-{mode}.log")
    with open(log, "w") as f:
        rc = subprocess.run(["mvn", f"-Dreflection={mode}", "clean", "package", "-DskipTests"],
                            cwd=PROJECT, env=env, stdout=f, stderr=subprocess.STDOUT).returncode
    if rc != 0:
        sys.exit(f"error: build {mode} a échoué — voir {log}")
    if mode == "native":
        src = os.path.join(PROJECT, "target", "garganttua-api-example")
        dst = os.path.join(WORK, "native")
    else:
        jars = [j for j in glob.glob(os.path.join(PROJECT, "target", "garganttua-api-example-*.jar"))
                if "original-" not in os.path.basename(j)]
        src = jars[0]; dst = os.path.join(WORK, f"{mode}.jar")
    shutil.copy2(src, dst)
    return dst


def argv_for(mode, artifact, run_java):
    if mode == "native":
        return [artifact]
    return [run_java, "--enable-preview", "-jar", artifact]


# ---------------------------------------------------------------- run one
def run_once(argv):
    out = os.path.join(WORK, "_out.txt")
    fd = os.open(out, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o644)
    t0 = time.perf_counter()
    pid = os.posix_spawn(argv[0], argv, os.environ,
                         file_actions=[(os.POSIX_SPAWN_DUP2, fd, 1), (os.POSIX_SPAWN_DUP2, fd, 2)])
    _, status, ru = os.wait4(pid, 0)
    wall = (time.perf_counter() - t0) * 1000.0
    os.close(fd)
    cpu = (ru.ru_utime + ru.ru_stime) * 1000.0
    txt = ANSI.sub("", open(out, errors="ignore").read())
    m = STARTUP.search(txt)
    startup = (float(m.group(1).replace(",", ".")) * (1000 if m.group(2) == "s" else 1)) if m else None
    return dict(wall_ms=wall, rss_mb=ru.ru_maxrss / 1024.0, cpu_ms=cpu,
                cpu_pct=(cpu / wall * 100.0 if wall else 0.0), startup_ms=startup,
                ok=os.WIFEXITED(status) and os.WEXITSTATUS(status) == 0), txt


def parse_obs(txt):
    ops, stages = {}, {}
    for ln in txt.splitlines():
        g = ROW.match(ln)
        if not g:
            continue
        src, cnt, ok, ko, avg = g.group(1), int(g.group(2)), int(g.group(3)), int(g.group(4)), int(g.group(8))
        if src.startswith("api:operation:"):
            ops[src] = dict(cnt=cnt, ok=ok, ko=ko, avg=avg)
        elif src.startswith("stage:"):
            stages[src] = dict(cnt=cnt, avg=avg)
    return dict(ops=ops, stages=stages)


# ---------------------------------------------------------------- report
def col(res, m, k):
    return [r[k] for r in res[m]["runs"] if r.get(k) is not None]


def stat(res, m, k):
    v = col(res, m, k)
    return dict(mean=st.mean(v), sd=st.pstdev(v), med=st.median(v), lo=min(v), hi=max(v)) if v else \
        dict(mean=0, sd=0, med=0, lo=0, hi=0)


def wrap(s, w=96):
    import textwrap
    return "\n".join(textwrap.fill(l, w) for l in s.split("\n"))


def generate_report(data, modes):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.backends.backend_pdf import PdfPages

    env, N, res, OBS = data["env"], data["n_runs"], data["results"], data["obs"]
    W = {m: stat(res, m, "wall_ms") for m in modes}
    S = {m: stat(res, m, "startup_ms") for m in modes}
    R = {m: stat(res, m, "rss_mb") for m in modes}
    C = {m: stat(res, m, "cpu_ms") for m in modes}
    P = {m: stat(res, m, "cpu_pct") for m in modes}
    SZ = {m: res[m]["artifact_size"] / 1e6 for m in modes}
    cols = [COLORS[m] for m in modes]
    pp = PdfPages(PDF)

    # ---- page 1 : overview
    fig = plt.figure(figsize=(8.27, 11.69))
    fig.suptitle("garganttua-api-example — Benchmark des modes de réflexion",
                 fontsize=16, fontweight="bold", y=0.975)
    fig.text(0.5, 0.945, f"{'  vs  '.join(modes)}   —   {N} exécutions par mode",
             ha="center", fontsize=11, color="#444")
    meta = wrap(f"Date        : {env['date']}\n"
                f"CPU         : {env['cpu']} ({env['cores']} coeurs)\n"
                f"RAM / OS    : {env['ram']}  /  {env['os']}\n"
                f"JDK (jars)  : {env['jdk_run']}\n"
                f"Build natif : {env['graalvm_build']}", 92)
    fig.text(0.07, 0.905, meta, fontsize=8, va="top", family="monospace",
             bbox=dict(boxstyle="round", fc="#f7f7f7", ec="#ccc"))
    fig.text(0.07, 0.795, wrap(
        f"Methodo : {data['warmup']} runs de chauffe ecartes, puis {N} runs mesures par mode. "
        "Meme charge dans tous les modes. Temps mur = duree du process complet ; CPU = temps CPU "
        "user+sys (ru_*time) ; RSS pic = ru_maxrss ; startup = bootstrap rapporte par l'app. "
        "os.wait4, runs sequentiels, meme machine.", 96),
        fontsize=8, va="top", bbox=dict(boxstyle="round", fc="#fbfbf2", ec="#ddd"))
    rows = [[m, f"{W[m]['mean']:.0f} ± {W[m]['sd']:.0f}", f"{S[m]['mean']:.0f}",
             f"{R[m]['mean']:.0f}", f"{C[m]['mean']:.0f}", f"{P[m]['mean']:.0f} %", f"{SZ[m]:.0f}"]
            for m in modes]
    ax = fig.add_axes([0.05, 0.60, 0.90, 0.12]); ax.axis("off")
    tbl = ax.table(cellText=rows,
                   colLabels=["mode", "temps mur ms\n(moy±σ)", "startup\n(ms)", "RSS pic\n(MB)",
                              "CPU\n(ms)", "CPU\n(%)", "exécutable\n(MB)"],
                   cellLoc="center", loc="center",
                   colWidths=[0.15, 0.21, 0.12, 0.12, 0.13, 0.10, 0.15])
    tbl.auto_set_font_size(False); tbl.set_fontsize(8.5); tbl.scale(1, 2.1)
    for j in range(7):
        tbl[0, j].set_facecolor("#333"); tbl[0, j].set_text_props(color="white", fontweight="bold")
    for i, m in enumerate(modes, 1):
        tbl[i, 0].set_facecolor(COLORS[m]); tbl[i, 0].set_text_props(color="white", fontweight="bold")
    if "native" in modes and "runtime" in modes:
        fig.text(0.06, 0.555, "Synthèse\n" + wrap(
            f"- native : {W['runtime']['mean']/max(W['native']['mean'],1):.1f}x plus rapide (temps mur) que runtime.\n"
            f"- native : ~{C['runtime']['mean']/max(C['native']['mean'],1):.0f}x moins de CPU "
            f"(~{P['native']['mean']:.0f}% mono-coeur vs ~{P['runtime']['mean']:.0f}% JVM JIT+GC).\n"
            f"- native : {R['runtime']['mean']/max(R['native']['mean'],1):.1f}x moins de RAM (RSS pic).\n"
            f"- native : demarrage {S['runtime']['mean']/max(S['native']['mean'],1):.1f}x plus court.\n"
            f"- prix : executable autonome {SZ['native']:.0f} MB (vs jar + JVM) et build natif plus long.", 96),
            fontsize=9, va="top", bbox=dict(boxstyle="round", fc="#eef6ee", ec="#5cb85c"))
    fig.text(0.5, 0.05, "Plus c'est bas, mieux c'est.", ha="center", fontsize=8, color="#888")
    pp.savefig(fig); plt.close(fig)

    # ---- page 2 : metric charts
    x = list(range(len(modes)))
    fig, axes = plt.subplots(3, 2, figsize=(8.27, 11.69))
    fig.suptitle("Détails par métrique", fontsize=14, fontweight="bold", y=0.985)

    def bar(ax, vals, errs, title, unit):
        bars = ax.bar(x, vals, yerr=errs, capsize=4, color=cols, edgecolor="#333")
        ax.set_xticks(x); ax.set_xticklabels(modes); ax.set_title(title, fontsize=10.5, fontweight="bold")
        ax.set_ylabel(unit); ax.grid(axis="y", ls=":", alpha=0.5)
        for b, v in zip(bars, vals):
            ax.text(b.get_x() + b.get_width() / 2, v, f"{v:.0f}", ha="center", va="bottom",
                    fontsize=8.5, fontweight="bold")
        ax.set_ylim(0, max(vals) * 1.2 if max(vals) else 1)

    bar(axes[0, 0], [W[m]['mean'] for m in modes], [W[m]['sd'] for m in modes], "Temps mur total (moy ± σ)", "ms")
    bar(axes[0, 1], [S[m]['mean'] for m in modes], [S[m]['sd'] for m in modes], "Démarrage applicatif", "ms")
    bar(axes[1, 0], [R[m]['mean'] for m in modes], [R[m]['sd'] for m in modes], "Mémoire — RSS pic", "MB")
    bar(axes[1, 1], [C[m]['mean'] for m in modes], [C[m]['sd'] for m in modes], "CPU consommé (user+sys)", "ms")
    bar(axes[2, 0], [P[m]['mean'] for m in modes], [P[m]['sd'] for m in modes], "Occupation CPU moyenne", "%")
    bp = axes[2, 1].boxplot([col(res, m, "wall_ms") for m in modes], patch_artist=True, tick_labels=modes)
    for patch, m in zip(bp["boxes"], modes):
        patch.set_facecolor(COLORS[m]); patch.set_alpha(0.85)
    for md in bp["medians"]:
        md.set_color("#111")
    axes[2, 1].set_title("Distribution du temps mur", fontsize=10.5, fontweight="bold")
    axes[2, 1].set_ylabel("ms"); axes[2, 1].grid(axis="y", ls=":", alpha=0.5)
    fig.tight_layout(rect=[0, 0.01, 1, 0.965])
    pp.savefig(fig); plt.close(fig)

    # ---- page 3 : pipeline stage timings
    allstages = sorted({s for m in modes for s in OBS[m]["stages"]},
                       key=lambda s: -OBS.get("native", OBS[modes[0]])["stages"].get(s, {}).get("avg", 0))
    if allstages:
        top = allstages[:14]; y = list(range(len(top))); h = 0.8 / len(modes)
        fig = plt.figure(figsize=(8.27, 11.69))
        fig.suptitle("Timings du pipeline — temps moyen par stage", fontsize=14, fontweight="bold", y=0.98)
        fig.text(0.5, 0.955, "Durée moyenne (µs) de chaque stage du workflow, par mode "
                 "(1 run représentatif, agrégé sur les opérations).", ha="center", fontsize=8.5, color="#555")
        ax = fig.add_axes([0.30, 0.27, 0.64, 0.63])
        for k, m in enumerate(modes):
            ax.barh([yi + (len(modes) / 2 - 0.5 - k) * h for yi in y],
                    [OBS[m]["stages"].get(s, {}).get("avg", 0) for s in top],
                    height=h, color=COLORS[m], label=m, edgecolor="#333", lw=0.4)
        ax.set_yticks(y); ax.set_yticklabels([s.replace("stage:", "") for s in top], fontsize=8)
        ax.invert_yaxis(); ax.set_xlabel("µs (moyenne par exécution du stage)")
        ax.grid(axis="x", ls=":", alpha=0.5); ax.legend(fontsize=9, loc="lower right")
        ax.set_title(f"Top {len(top)} stages (triés par coût)", fontsize=10, fontweight="bold")
        fig.text(0.07, 0.2, wrap("Lecture : les stages cryptographiques (create-authorization : génération "
                 "de clé EC + signature) dominent. Chaque stage est plus rapide en natif ; sur un service long "
                 "les modes JVM se rapprochent au chaud mais paient toujours startup, RAM et CPU de warmup.", 100),
                 fontsize=8.5, va="top", bbox=dict(boxstyle="round", fc="#f7f7f7", ec="#ccc"))
        pp.savefig(fig); plt.close(fig)

    # ---- page 4 : execution stats
    allops = sorted({o for m in modes for o in OBS[m]["ops"]})
    if allops:
        fig = plt.figure(figsize=(8.27, 11.69))
        fig.suptitle("Statistiques d'exécution — opérations API", fontsize=14, fontweight="bold", y=0.98)
        fig.text(0.5, 0.955, "Par opération : nombre d'appels, succès/échecs, temps moyen (µs) par mode.",
                 ha="center", fontsize=8.5, color="#555")
        orows = []
        for o in allops:
            ref = next((OBS[m]["ops"][o] for m in modes if o in OBS[m]["ops"]), {})
            orows.append([o.replace("api:operation:", "")[:46], str(ref.get("cnt", "-")),
                          f"{ref.get('ok','-')}/{ref.get('ko','-')}",
                          *[str(OBS[m]["ops"].get(o, {}).get("avg", "-")) for m in modes]])
        ax = fig.add_axes([0.04, 0.55, 0.92, 0.36]); ax.axis("off")
        t = ax.table(cellText=orows, colLabels=["opération", "appels", "ok/ko"] + [f"{m} µs" for m in modes],
                     cellLoc="center", loc="upper center",
                     colWidths=[0.40, 0.09, 0.10] + [0.13] * len(modes))
        t.auto_set_font_size(False); t.set_fontsize(7.8); t.scale(1, 1.7)
        for j in range(3 + len(modes)):
            t[0, j].set_facecolor("#333"); t[0, j].set_text_props(color="white", fontweight="bold")
        srows = [[s.replace("stage:", "")[:34], str(next((OBS[m]["stages"][s]["cnt"] for m in modes
                  if s in OBS[m]["stages"]), "-")), *[str(OBS[m]["stages"].get(s, {}).get("avg", "-")) for m in modes]]
                 for s in allstages]
        ax2 = fig.add_axes([0.06, 0.05, 0.88, 0.44]); ax2.axis("off")
        ax2.set_title("Tous les stages — temps moyen (µs) par mode", fontsize=10, fontweight="bold", y=1.0)
        t2 = ax2.table(cellText=srows, colLabels=["stage", "appels"] + [f"{m} µs" for m in modes],
                       cellLoc="center", loc="upper center", colWidths=[0.40, 0.12] + [0.16] * len(modes))
        t2.auto_set_font_size(False); t2.set_fontsize(7.2); t2.scale(1, 1.32)
        for j in range(2 + len(modes)):
            t2[0, j].set_facecolor("#333"); t2[0, j].set_text_props(color="white", fontweight="bold")
        pp.savefig(fig); plt.close(fig)

    pp.close()


# ---------------------------------------------------------------- main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--runs", type=int, default=20)
    ap.add_argument("--warmup", type=int, default=3)
    ap.add_argument("--modes", default="runtime,aot,native")
    ap.add_argument("--skip-build", action="store_true", help="réutilise les artefacts de benchmark/.work")
    args = ap.parse_args()
    modes = [m.strip() for m in args.modes.split(",") if m.strip() in MODES_ALL]

    os.makedirs(WORK, exist_ok=True)
    ensure_matplotlib()
    graalvm = find_graalvm()
    run_java = os.environ.get("BENCH_JAVA", os.path.join(graalvm, "bin", "java"))
    print(f"[env] GraalVM={graalvm}\n[env] run-java={run_java}\n[env] modes={modes} runs={args.runs}")

    artifacts = {}
    for m in modes:
        path = os.path.join(WORK, "native" if m == "native" else f"{m}.jar")
        if args.skip_build and os.path.exists(path):
            print(f"[build] {m}: réutilise {path}")
        else:
            print(f"[build] {m} ...", flush=True)
            path = mvn_build(m, graalvm)
        artifacts[m] = path

    results, obs = {}, {}
    for m in modes:
        argv = argv_for(m, artifacts[m], run_java)
        print(f"[run] {m}: warmup x{args.warmup}", flush=True)
        for _ in range(args.warmup):
            run_once(argv)
        runs = []
        for i in range(args.runs):
            r, txt = run_once(argv)
            runs.append(r)
            print(f"[run] {m} {i+1}/{args.runs}: wall={r['wall_ms']:.0f}ms cpu={r['cpu_ms']:.0f}ms"
                  f"({r['cpu_pct']:.0f}%) rss={r['rss_mb']:.0f}MB start={r['startup_ms']} ok={r['ok']}", flush=True)
        obs[m] = parse_obs(txt)   # last run's observability
        results[m] = dict(runs=runs, artifact_size=os.path.getsize(artifacts[m]))

    data = dict(env=env_info(graalvm, run_java), n_runs=args.runs, warmup=args.warmup,
                results=results, obs=obs)
    json.dump(data, open(JSON, "w"), indent=2)
    with open(CSV, "w") as f:
        f.write("mode,run,wall_ms,rss_mb,cpu_ms,cpu_pct,startup_ms,ok\n")
        for m in modes:
            for i, r in enumerate(results[m]["runs"], 1):
                f.write(f"{m},{i},{r['wall_ms']:.1f},{r['rss_mb']:.1f},{r['cpu_ms']:.1f},"
                        f"{r['cpu_pct']:.1f},{r['startup_ms']},{r['ok']}\n")
    print("[report] génération du PDF ...")
    generate_report(data, modes)
    print(f"\nOK -> {PDF}\n   -> {CSV}\n   -> {JSON}")


if __name__ == "__main__":
    main()
