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
