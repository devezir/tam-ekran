import os
import sys
import json
import re
import hashlib
import subprocess
import zipfile
import shutil
import glob
from datetime import datetime

base_dir = os.path.dirname(os.path.abspath(__file__))
tools_dir = os.environ.get("ANDROID_TOOLS_DIR", r"C:\Users\Vezir\tools")
jdk_bin = os.environ.get("JDK_HOME_BIN", os.path.join(tools_dir, "jdk-21", "bin"))

aapt2 = os.path.join(tools_dir, "aapt2.exe" if os.name == "nt" else "aapt2")
android_jar = os.environ.get("ANDROID_JAR", os.path.join(tools_dir, "android-33.jar"))
r8_jar = os.path.join(tools_dir, "r8.jar")
signer_jar = os.path.join(tools_dir, "uber-apk-signer.jar")
javac = os.path.join(jdk_bin, "javac.exe" if os.name == "nt" else "javac")
java = os.path.join(jdk_bin, "java.exe" if os.name == "nt" else "java")

version_file = os.path.join(base_dir, "version.json")
manifest_file = os.path.join(base_dir, "AndroidManifest.xml")
releases_dir = os.path.join(base_dir, "releases")
keystore_file = os.path.join(base_dir, "keystore", "tamekran-release.jks")
signing_props_file = os.path.join(base_dir, "keystore", "signing.properties")

os.makedirs(releases_dir, exist_ok=True)


def load_signing_config():
    cfg = {
        "ksAlias": os.environ.get("TAMEKRAN_KS_ALIAS", "tamekran"),
        "ksPass": os.environ.get("TAMEKRAN_KS_PASS", ""),
        "ksKeyPass": os.environ.get("TAMEKRAN_KS_KEY_PASS", "")
    }
    if not cfg["ksPass"] and os.path.exists(signing_props_file):
        with open(signing_props_file, "r", encoding="utf-8") as pf:
            for line in pf:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, v = line.split("=", 1)
                k = k.strip()
                v = v.strip()
                if k in cfg and not cfg[k]:
                    cfg[k] = v
    if not cfg["ksKeyPass"]:
        cfg["ksKeyPass"] = cfg["ksPass"]
    return cfg


def compute_sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        while True:
            chunk = f.read(8192)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest()


# 1. Surum Yonetimi (version.json)
if not os.path.exists(version_file):
    print("Hata: version.json bulunamadi!")
    sys.exit(1)

with open(version_file, "r", encoding="utf-8") as f:
    ver_data = json.load(f)

cur_vc = ver_data.get("versionCode", 1)
cur_vn = ver_data.get("versionName", "1.0.0")

if "--bump" in sys.argv:
    bump_idx = sys.argv.index("--bump")
    bump_type = sys.argv[bump_idx + 1] if len(sys.argv) > bump_idx + 1 else "patch"
    parts = [int(p) for p in cur_vn.split(".")]
    while len(parts) < 3:
        parts.append(0)

    if bump_type == "major":
        parts[0] += 1
        parts[1] = 0
        parts[2] = 0
    elif bump_type == "minor":
        parts[1] += 1
        parts[2] = 0
    else:
        parts[2] += 1

    cur_vn = f"{parts[0]}.{parts[1]}.{parts[2]}"
    cur_vc += 1
    desc = "Surum guncellemesi"
    if len(sys.argv) > bump_idx + 2 and not sys.argv[bump_idx + 2].startswith("--"):
        desc = sys.argv[bump_idx + 2]

    ver_data["versionCode"] = cur_vc
    ver_data["versionName"] = cur_vn
    ver_data["apkName"] = f"TamEkran_v{cur_vn}_vc{cur_vc}.apk"
    ver_data["history"].insert(0, {
        "versionCode": cur_vc,
        "versionName": cur_vn,
        "date": datetime.now().strftime("%Y-%m-%d"),
        "changes": desc
    })
    with open(version_file, "w", encoding="utf-8") as f:
        json.dump(ver_data, f, indent=2, ensure_ascii=False)
    print(f"[*] Surum yukseltildi: v{cur_vn} (versionCode: {cur_vc})")

# 2. AndroidManifest.xml Senkronizasyonu
if os.path.exists(manifest_file):
    with open(manifest_file, "r", encoding="utf-8") as f:
        manifest_content = f.read()

    manifest_content = re.sub(r'android:versionCode="\d+"', f'android:versionCode="{cur_vc}"', manifest_content)
    manifest_content = re.sub(r'android:versionName="[^"]+"', f'android:versionName="{cur_vn}"', manifest_content)

    with open(manifest_file, "w", encoding="utf-8") as f:
        f.write(manifest_content)
    print(f"[*] AndroidManifest.xml guncellendi: versionCode={cur_vc}, versionName={cur_vn}")

# 2.5 Opsiyonel Kalite Kapisi (Artik 3/5 sonrasinda otomatik kosulur; atlamak icin --skip-tests)

os.chdir(base_dir)

# 3. Kaynaklari Derle (AAPT2)
print("\n[1/5] Kaynaklar derleniyor (AAPT2)...")
res_zip = os.path.join(base_dir, "compiled_res.zip")
if os.path.exists(res_zip):
    os.remove(res_zip)

cmd_compile = [aapt2, "compile", "--dir", "res", "-o", res_zip]
res = subprocess.run(cmd_compile, capture_output=True, text=True, encoding="utf-8", errors="replace")
if res.returncode != 0:
    print("AAPT2 derleme hatasi:", res.stderr)
    sys.exit(1)
print("AAPT2 derleme basarili.")

# 4. Kaynaklari Bagla ve R.java Uret
print("[2/5] Kaynaklar baglaniyor ve R.java uretiliyor...")
base_apk = os.path.join(base_dir, "base.apk")
if os.path.exists(base_apk):
    os.remove(base_apk)

gen_dir = os.path.join(base_dir, "gen")
shutil.rmtree(gen_dir, ignore_errors=True)
os.makedirs(gen_dir, exist_ok=True)

cmd_link = [
    aapt2, "link", res_zip,
    "-I", android_jar,
    "--manifest", "AndroidManifest.xml",
    "-o", base_apk,
    "--java", "gen",
    "--auto-add-overlay"
]
res = subprocess.run(cmd_link, capture_output=True, text=True, encoding="utf-8", errors="replace")
if res.returncode != 0:
    print("AAPT2 link hatasi:", res.stderr)
    sys.exit(1)
print("AAPT2 link basarili.")

# 5. Java Kodlarini Derle (javac)
print("[3/5] Java kodlari derleniyor (javac)...")
obj_dir = os.path.join(base_dir, "obj")
shutil.rmtree(obj_dir, ignore_errors=True)
os.makedirs(obj_dir, exist_ok=True)

java_files = glob.glob("gen/**/*.java", recursive=True) + glob.glob("src/**/*.java", recursive=True)
cmd_javac = [
    javac,
    "-Xlint:all,-serial,-deprecation,-options",
    "-Werror",
    "-encoding", "UTF-8",
    "-source", "1.8",
    "-target", "1.8",
    "-cp", android_jar,
    "-d", obj_dir
] + java_files
res = subprocess.run(cmd_javac, capture_output=True, text=True, encoding="utf-8", errors="replace")
if res.returncode != 0:
    print("javac hatasi:", res.stderr)
    print("stdout:", res.stdout)
    sys.exit(1)
print("javac derleme basarili.")

# 5.5 Zorunlu Birim Test Kapisi (Verification Before Completion)
if "--skip-tests" in sys.argv and "--debug" not in sys.argv:
    print("[HATA] Release derlemesinde '--skip-tests' kullanilamaz! Yalnizca '--debug' modunda test atlanabilir.")
    sys.exit(1)

if "--skip-tests" not in sys.argv:
    print("[3.5/5] Birim testleri calistiriliyor (BirimTestler)...")
    test_obj_dir = os.path.join(base_dir, "obj_test_gate")
    shutil.rmtree(test_obj_dir, ignore_errors=True)
    os.makedirs(test_obj_dir, exist_ok=True)
    test_files = glob.glob("tests/**/*.java", recursive=True)
    cmd_test_javac = [
        javac,
        "-Xlint:all,-serial,-deprecation,-options",
        "-Werror",
        "-encoding", "UTF-8",
        "-source", "1.8",
        "-target", "1.8",
        "-cp", f"{obj_dir}{os.pathsep}{android_jar}",
        "-d", test_obj_dir
    ] + test_files
    t_compile = subprocess.run(cmd_test_javac, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if t_compile.returncode != 0:
        print("Test derleme hatasi:", t_compile.stderr)
        shutil.rmtree(test_obj_dir, ignore_errors=True)
        sys.exit(1)
    t_run = subprocess.run(
        [java, "-cp", f"{test_obj_dir}{os.pathsep}{obj_dir}{os.pathsep}{android_jar}", "tests.BirimTestler"],
        capture_output=True, text=True, encoding="utf-8", errors="replace"
    )
    shutil.rmtree(test_obj_dir, ignore_errors=True)
    if t_run.returncode != 0:
        print(t_run.stdout)
        print(t_run.stderr)
        print("[HATA] Birim testleri basarisiz oldu, APK uretimi durduruldu!")
        sys.exit(1)
    for line in t_run.stdout.splitlines():
        if "SONU" in line or "BASARI" in line or "KUSURSUZ" in line:
            print(" ", line)

# 6. DEX Donusumu (D8 - min-api 8 uyumlu)
print("[4/5] Bayt kodlari DEX formatina donusturuluyor (D8 - Android 2.2 uyumlu)...")
bin_dir = os.path.join(base_dir, "bin")
shutil.rmtree(bin_dir, ignore_errors=True)
os.makedirs(bin_dir, exist_ok=True)

class_files = glob.glob("obj/**/*.class", recursive=True)
cmd_d8 = [
    java, "-cp", r8_jar,
    "com.android.tools.r8.D8",
    "--release",
    "--lib", android_jar,
    "--min-api", "8",
    "--output", bin_dir
] + class_files
res = subprocess.run(cmd_d8, capture_output=True, text=True, encoding="utf-8", errors="replace")
if res.returncode != 0:
    print("D8 hatasi:", res.stderr)
    sys.exit(1)
print("D8 donusumu basarili.")

dex_file = os.path.join(bin_dir, "classes.dex")

# 7. Paketleme ve Fail-Closed Resmi Imzalama
print("[5/5] Paketleme ve Imzalama yapiliyor...")
unsigned_apk = os.path.join(base_dir, "TamEkran_unsigned.apk")
shutil.copy2(base_apk, unsigned_apk)

with zipfile.ZipFile(unsigned_apk, "a") as z:
    z.write(dex_file, "classes.dex")

out_dir = os.path.join(base_dir, "out_sign")
shutil.rmtree(out_dir, ignore_errors=True)
os.makedirs(out_dir, exist_ok=True)

debug_mode = "--debug" in sys.argv
signing_cfg = load_signing_config()

if not debug_mode:
    if not os.path.exists(keystore_file):
        print(f"[HATA] Keystore bulunamadi ({keystore_file}). Debug derlemesi icin '--debug' kullanin.")
        sys.exit(1)
    if not signing_cfg["ksPass"]:
        print("[HATA] TAMEKRAN_KS_PASS veya keystore/signing.properties bulunamadi! Fail-closed durduruldu.")
        sys.exit(1)

cmd_sign = [
    java, "-jar", signer_jar,
    "-a", unsigned_apk,
    "-o", out_dir,
    "--allowResign"
]

if not debug_mode:
    cmd_sign += [
        "--ks", keystore_file,
        "--ksAlias", signing_cfg["ksAlias"],
        "--ksPass", signing_cfg["ksPass"],
        "--ksKeyPass", signing_cfg["ksKeyPass"]
    ]
    print("[*] Resmi Tam Ekran Keystore ile imzalaniyor...")
else:
    print("[*] UYARI: Debug sertifikasi ile imzalaniyor (--debug)...")

res = subprocess.run(cmd_sign, capture_output=True, text=True, encoding="utf-8", errors="replace")
if res.returncode != 0:
    print("Imzalama hatasi:", res.stderr)
    sys.exit(1)

signed_apks = glob.glob(os.path.join(out_dir, "*.apk"))
if not signed_apks:
    print("Hata: Imzali APK bulunamadi!")
    sys.exit(1)

final_apk_name = f"TamEkran_v{cur_vn}_vc{cur_vc}.apk"
release_apk_path = os.path.join(releases_dir, final_apk_name)
latest_apk_path = os.path.join(base_dir, "TamEkran.apk")

shutil.copy2(signed_apks[0], release_apk_path)
shutil.copy2(signed_apks[0], latest_apk_path)

apk_sha256 = compute_sha256(latest_apk_path)
ver_data["apkSha256"] = apk_sha256
with open(version_file, "w", encoding="utf-8") as f:
    json.dump(ver_data, f, indent=2, ensure_ascii=False)

# Temizlik
if os.path.exists(unsigned_apk):
    os.remove(unsigned_apk)
if os.path.exists(base_apk):
    os.remove(base_apk)
shutil.rmtree(out_dir, ignore_errors=True)

apk_size_kb = os.path.getsize(latest_apk_path) / 1024.0
imza_etiketi = "Debug Key (--debug)" if debug_mode else "Resmi Release Key (V1, V2, V3 onayli)"
print("\n" + "="*50)
print("DERLEME VE IMZALAMA BASARIYLA TAMAMLANDI!")
print(f"Son Cikti:        {latest_apk_path}")
print(f"Surum Arsivi:     {release_apk_path}")
print(f"APK Boyutu:       {apk_size_kb:.1f} KB (Tuy gibi hafif!)")
print(f"APK SHA-256:      {apk_sha256}")
print(f"Uyumluluk:        Android 2.2 (API 8) -> Android 17 (API 36+)")
print(f"Imza Durumu:      {imza_etiketi}")
print("="*50)
