#!/usr/bin/env python3
"""Interactive, read-only iCloud Photos access probe. No upload or deletion."""
import argparse
import getpass
import json
import logging
import os
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--china", action="store_true", help="Use mainland China service")
    parser.add_argument("--session-dir", type=Path, required=True, help="Private directory outside Git")
    args = parser.parse_args()
    from pyicloud import PyiCloudService
    logging.disable(logging.CRITICAL)
    os.umask(0o077)
    args.session_dir.mkdir(parents=True, exist_ok=True, mode=0o700)
    args.session_dir.chmod(0o700)
    account = input("Apple Account: ").strip()
    password = getpass.getpass("Password (not saved by this script): ")
    try:
        api = PyiCloudService(account, password, cookie_directory=str(args.session_dir),
                              china_mainland=args.china, with_family=False)
        if api.requires_2fa:
            if not api.validate_2fa_code(getpass.getpass("Verification code: ")):
                raise RuntimeError("Verification failed")
            if not api.trust_session():
                raise RuntimeError("Session trust failed")
        asset = next(iter(api.photos.all), None)
        print(json.dumps({"photos_access": True, "empty_library": asset is None,
                          "available_versions": sorted(asset.versions) if asset else [],
                          "uploaded": False, "deleted": False}, ensure_ascii=False))
    except Exception as error:
        print(json.dumps({"photos_access": False, "error_type": type(error).__name__,
                          "hint": "Check 2FA, region, web access and ADP. No library writes attempted."}))
        raise SystemExit(1)


if __name__ == "__main__":
    main()
