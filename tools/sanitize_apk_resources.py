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

def sanitize_apk(target_apk, input_apk, keystore_path):
    if not os.path.isfile(target_apk):
        print(f"Target APK does not exist: {target_apk}")
        sys.exit(1)
    if not os.path.isfile(input_apk):
        print(f"Input APK does not exist: {input_apk}")
        sys.exit(1)

    print(f"🔍 Checking {target_apk} for corrupted resource entries...")
    corrupted_entries = {}
    with zipfile.ZipFile(input_apk, 'r') as zin, zipfile.ZipFile(target_apk, 'r') as zout:
        in_names = set(zin.namelist())
        for name in zout.namelist():
            if name.endswith('.xml'):
                data = zout.read(name)
                # Check for corrupted ?attr/ or escaped \?123
                if b'?attr/' in data or rb'\?123' in data:
                    if name in in_names:
                        in_data = zin.read(name)
                        if b'?attr/' not in in_data and rb'\?123' not in in_data:
                            corrupted_entries[name] = in_data
                            print(f"  ⚡ Corrupted resource detected: {name} (will restore from input APK)")

    if not corrupted_entries:
        print("✅ No corrupted XML resources found in APK.")
        return

    print(f"🔧 Restoring {len(corrupted_entries)} clean XML resource(s) from input APK...")
    temp_apk = target_apk + ".tmp.apk"
    aligned_apk = target_apk + ".aligned.apk"

    with zipfile.ZipFile(target_apk, 'r') as zout, zipfile.ZipFile(temp_apk, 'w') as znew:
        for item in zout.infolist():
            if item.filename in corrupted_entries:
                znew.writestr(item, corrupted_entries[item.filename])
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
