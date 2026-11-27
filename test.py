import os
import sys
import subprocess

base_dir = os.path.dirname(os.path.abspath(__file__))
tools_dir = os.environ.get("ANDROID_TOOLS_DIR", r"C:\Users\Vezir\tools")
jdk_bin = os.environ.get("JDK_HOME_BIN", os.path.join(tools_dir, "jdk-21", "bin"))

aapt2 = os.path.join(tools_dir, "aapt2.exe" if os.name == "nt" else "aapt2")
javac = os.path.join(jdk_bin, "javac.exe" if os.name == "nt" else "javac")
java = os.path.join(jdk_bin, "java.exe" if os.name == "nt" else "java")
android_jar = os.environ.get("ANDROID_JAR", os.path.join(tools_dir, "android-33.jar"))

bin_dir = os.path.join(base_dir, "bin")
gen_dir = os.path.join(base_dir, "gen")
os.makedirs(bin_dir, exist_ok=True)


def _en_guncel_kaynak_zamani():
    res_dir = os.path.join(base_dir, "res")
    manifest = os.path.join(base_dir, "AndroidManifest.xml")
    en_yeni = os.path.getmtime(manifest) if os.path.exists(manifest) else 0
    for root, dirs, files in os.walk(res_dir):
        en_yeni = max(en_yeni, os.path.getmtime(root))
        for name in files:
            en_yeni = max(en_yeni, os.path.getmtime(os.path.join(root, name)))
    return en_yeni


def ensure_r_java():
    r_java = os.path.join(gen_dir, "com", "tamekran", "launcher", "R.java")
    if os.path.exists(r_java) and os.path.getmtime(r_java) >= _en_guncel_kaynak_zamani():
        return
    print("[*] gen/R.java yok veya guncel degil -> AAPT2 ile uretiliyor...")
    os.makedirs(gen_dir, exist_ok=True)
    tmp_zip = os.path.join(bin_dir, "test_res.zip")
    tmp_apk = os.path.join(bin_dir, "test_base.apk")
    r_compile = subprocess.run([
        aapt2, "compile", "--dir", os.path.join(base_dir, "res"), "-o", tmp_zip
    ], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r_compile.returncode != 0:
        print("AAPT2 compile hatasi:", r_compile.stderr)
        sys.exit(1)
    r_link = subprocess.run([
        aapt2, "link", tmp_zip,
        "-I", android_jar,
        "--manifest", os.path.join(base_dir, "AndroidManifest.xml"),
        "-o", tmp_apk,
        "--java", gen_dir,
        "--auto-add-overlay"
    ], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r_link.returncode != 0:
        print("AAPT2 link hatasi:", r_link.stderr)
        sys.exit(1)
    for f in (tmp_zip, tmp_apk):
        if os.path.exists(f):
            os.remove(f)


import glob
import json
import re


def run_static_guards():
    """SPEC.md ve Tasarım Felsefesi statik mimari denetimleri."""
    hatalar = []

    # 1. UI Tasarım Felsefesi Guard: res/layout/*.xml icinde sabit 36-44dp yukseklik yasagi
    layout_pattern = re.compile(r'android:layout_height="(36|38|40|42|44)dp"')
    for xml_path in glob.glob(os.path.join(base_dir, "res", "layout", "*.xml")):
        with open(xml_path, "r", encoding="utf-8", errors="replace") as f:
            for lineno, line in enumerate(f, 1):
                if layout_pattern.search(line):
                    rel = os.path.relpath(xml_path, base_dir)
                    hatalar.append(f"[UI Felsefe Ihlali] {rel}:{lineno} -> {line.strip()}")

    # 2. SPEC.md Kor catch yasagi: catch (Throwable/Exception ignored) {}
    catch_pattern = re.compile(r'catch\s*\(\s*(?:Throwable|Exception)\s+ignored\s*\)\s*\{\s*\}')
    for java_path in glob.glob(os.path.join(base_dir, "src", "**", "*.java"), recursive=True):
        with open(java_path, "r", encoding="utf-8", errors="replace") as f:
            content = f.read()
        for m in catch_pattern.finditer(content):
            lineno = content[:m.start()].count("\n") + 1
            rel = os.path.relpath(java_path, base_dir)
            hatalar.append(f"[Kor Catch Ihlali] {rel}:{lineno}")

    # 3. version.json sema kontrolu
    v_path = os.path.join(base_dir, "version.json")
    try:
        with open(v_path, "r", encoding="utf-8") as f:
            vj = json.load(f)
        for key in ("versionCode", "versionName", "apkSha256"):
            if key not in vj or not vj[key]:
                hatalar.append(f"[version.json Eksik Alan] {key}")
    except Exception as e:
        hatalar.append(f"[version.json Okuma Hatasi] {e}")

    if hatalar:
        print("[!] STATIK MIMARI GUARD HATALARI:")
        for h in hatalar:
            print("  -", h)
        sys.exit(1)
    print("[*] Statik mimari guard kontrolleri gecti (UI Felsefesi + Kor Catch + version.json).")


run_static_guards()
ensure_r_java()

test_files = glob.glob(os.path.join(base_dir, "tests", "**", "*.java"), recursive=True)
sep = ";" if os.name == "nt" else ":"
cp = sep.join([android_jar, gen_dir, os.path.join(base_dir, "src")])

print(f"[*] Birim testleri derleniyor ({len(test_files)} dosya)...")
cmd_compile = [javac, "-Xlint:all,-serial,-deprecation,-options", "-Werror", "-encoding", "UTF-8", "-cp", cp, "-d", bin_dir] + test_files
res = subprocess.run(cmd_compile, capture_output=True, text=True, encoding="utf-8", errors="replace")
if res.returncode != 0:
    print("Test derleme hatasi:", res.stderr)
    sys.exit(1)

print("[*] Birim testleri calistiriliyor...\n")
run_cp = sep.join([android_jar, bin_dir, os.path.join(base_dir, "src"), gen_dir])
cmd_run = [java, "-cp", run_cp, "tests.BirimTestler"]
res_run = subprocess.run(cmd_run)
sys.exit(res_run.returncode)
