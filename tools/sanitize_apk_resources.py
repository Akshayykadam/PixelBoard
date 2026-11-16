#!/usr/bin/env python3
"""
PixelBoard APK Resource Sanitizer
Restores pristine binary XML resources from the input APK whenever Morphe's
ResourceIdProcessor or ARSCLib corrupts attributes (e.g. expanding '?' to '?attr/'
or adding backslashes like '\\?123').
Then re-aligns with zipalign and signs with apksigner using the PixelBoard keystore.
"""

import sys
import os
import glob
import shutil
import zipfile
import subprocess

def find_sdk_tool(tool_name):
    # Check environment variables
    for env_var in ['ANDROID_HOME', 'ANDROID_SDK_ROOT']:
        val = os.environ.get(env_var)
        if val:
            candidates = sorted(glob.glob(os.path.join(val, 'build-tools', '*', tool_name)), reverse=True)
            if candidates:
                return candidates[0]

    # Check default macOS path
    default_path = os.path.expanduser('~/Library/Android/sdk/build-tools')
    candidates = sorted(glob.glob(os.path.join(default_path, '*', tool_name)), reverse=True)
    if candidates:
        return candidates[0]

    # Check which
    which_path = shutil.which(tool_name)
    if which_path:
        return which_path

    return None

PRESERVED_XML_PATTERNS = {
    'AndroidManifest.xml',
    'res/xml/settings.xml',
    'res/xml/settings_legacy.xml',
    'res/xml/spell_checker.xml',
    'res/xml/setting_expression.xml',
    'res/xml/setting_clipboard.xml',
    'res/drawable/drawable_0x7f080516.xml',
    'res/drawable/drawable_0x7f08054d.xml',
    # Known obfuscated paths for Gboard 18.6.3
    'res/B_o.xml',
    'res/IeH.xml',
    'res/oXv.xml',
    'res/41N.xml',
    'res/Zn9.xml',
    'res/70O.xml',
    'res/cWf.xml',
}

PIXELBOARD_MARKERS = [
    b'pixelboard',
    b'gboardpatches',
    b'gboard_patches',
    b'akshaykadam',
    b'GboardPatchesSettingsActivity',
    b'PixelBoardUpdateActivity',
]

def resolve_target_resource_paths(input_apk, aapt2_path):
    if not aapt2_path or not os.path.isfile(aapt2_path):
        return set()
    try:
        res = subprocess.run([aapt2_path, 'dump', 'resources', input_apk], capture_output=True, text=True, timeout=10)
        if res.returncode != 0:
            return set()
        patterns = [
            r'xml/settings\b',
            r'xml/settings_legacy\b',
            r'xml/settings_homepage\b',
            r'xml/settings_legacy_homepage\b',
            r'xml/spell_checker\b',
            r'xml/setting_expression\b',
            r'xml/setting_clipboard\b',
            r'0x7f080516',
            r'0x7f08054d',
        ]
        import re
        resolved = set()
        current_match = False
        for line in res.stdout.splitlines():
            if line.strip().startswith('resource '):
                current_match = any(re.search(p, line) for p in patterns)
            if current_match:
                m = re.search(r'\(file\)\s+(\S+\.xml)', line)
                if m:
                    resolved.add(m.group(1))
        return resolved
    except Exception as e:
        print(f"⚠️ Warning: Could not resolve dynamic resource paths: {e}")
        return set()

def sanitize_apk(target_apk, input_apk, keystore_path):
    if not os.path.isfile(target_apk):
        print(f"Target APK does not exist: {target_apk}")
        sys.exit(1)
    if not os.path.isfile(input_apk):
        print(f"Input APK does not exist: {input_apk}")
        sys.exit(1)

    aapt2 = find_sdk_tool('aapt2')
    dynamic_paths = resolve_target_resource_paths(input_apk, aapt2)
    all_preserved_patterns = PRESERVED_XML_PATTERNS | dynamic_paths

    print(f"🔍 Checking {target_apk} against {input_apk} for stock XML restoration...")
    entries_to_restore = {}
    with zipfile.ZipFile(input_apk, 'r') as zin, zipfile.ZipFile(target_apk, 'r') as zout:
        in_names = set(zin.namelist())
        for name in zout.namelist():
            if name.endswith('.xml'):
                # Always preserve newly added PixelBoard XML resources
                if name not in in_names:
                    continue

                out_data = zout.read(name)

                # Always preserve intentionally modified PixelBoard XML resources
                if name in all_preserved_patterns:
                    continue

                out_lower = out_data.lower()
                if any(m.lower() in out_lower for m in PIXELBOARD_MARKERS):
                    continue

                in_data = zin.read(name)
                # If Morphe's ARSCLib re-encoded or altered the pristine binary XML, restore it
                if out_data != in_data:
                    entries_to_restore[name] = in_data

    if not entries_to_restore:
        print("✅ All stock XML resources in APK are already pristine.")
        return

    print(f"🔧 Restoring {len(entries_to_restore)} pristine stock XML resource(s) from input APK...")
    temp_apk = target_apk + ".tmp.apk"
    aligned_apk = target_apk + ".aligned.apk"

    with zipfile.ZipFile(target_apk, 'r') as zout, zipfile.ZipFile(temp_apk, 'w') as znew:
        for item in zout.infolist():
            if item.filename in entries_to_restore:
                znew.writestr(item, entries_to_restore[item.filename])
            else:
                znew.writestr(item, zout.read(item.filename))

    zipalign = find_sdk_tool('zipalign')
    apksigner = find_sdk_tool('apksigner')

    if not zipalign or not apksigner:
        print(f"⚠️ Warning: zipalign ({zipalign}) or apksigner ({apksigner}) not found. Moving temp APK in place.")
        shutil.move(temp_apk, target_apk)
        return

    # Run zipalign
    align_cmd = [zipalign, '-f', '-p', '4', temp_apk, aligned_apk]
    res = subprocess.run(align_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"❌ zipalign failed: {res.stderr}")
        shutil.move(temp_apk, target_apk)
        return

    # Run apksigner
    sign_cmd = [
        apksigner, 'sign',
        '--ks', keystore_path,
        '--ks-pass', 'pass:pixelboard',
        '--key-pass', 'pass:pixelboard',
        '--ks-key-alias', 'pixelboard',
        aligned_apk
    ]
    res = subprocess.run(sign_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"❌ apksigner failed: {res.stderr}")
        shutil.move(temp_apk, target_apk)
        return

    # Verify signature
    res = subprocess.run([apksigner, 'verify', aligned_apk], capture_output=True, text=True)
    if res.returncode == 0:
        print("✅ Re-signed APK verified successfully!")
        shutil.move(aligned_apk, target_apk)
        if os.path.exists(temp_apk):
            os.remove(temp_apk)
    else:
        print(f"❌ Verification failed: {res.stderr}")
        shutil.move(temp_apk, target_apk)

if __name__ == '__main__':
    if len(sys.argv) < 4:
        print("Usage: python3 sanitize_apk_resources.py <target_apk> <input_apk> <keystore_path>")
        sys.exit(1)
    sanitize_apk(sys.argv[1], sys.argv[2], sys.argv[3])
