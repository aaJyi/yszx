"""
List Roboflow workspaces - find correct workspace ID for download

Run: python scripts/list_workspaces.py
"""

import os
import requests

ROBOFLOW_API_KEY = os.environ.get("ROBOFLOW_API_KEY", os.environ.get("ROBoflow_API_KEY", ""))
API_URL = "https://api.roboflow.com"


def main():
    if not ROBOFLOW_API_KEY or ROBOFLOW_API_KEY == "YOUR_ROBOFLOW_API_KEY":
        print("Set ROBOFLOW_API_KEY first.")
        return

    # Get workspace info from API key validation
    r = requests.post(f"{API_URL}/?api_key={ROBOFLOW_API_KEY}")
    if r.status_code != 200:
        print(f"API error: {r.text}")
        return

    data = r.json()
    w = data.get("workspace", {})
    url = w.get("url") or w.get("slug") or w.get("id", "")
    name = w.get("name", "")

    print("Default workspace (use this for ROBOFLOW_WORKSPACE):")
    print(f"  url/slug: {url}")
    print(f"  name: {name}")
    print()
    print("Projects in workspace:")
    for p in w.get("projects", []):
        print(f"  - {p.get('name', p)}")
    print()
    print("Run: set ROBOFLOW_WORKSPACE=" + str(url))
    print("Or omit ROBOFLOW_WORKSPACE to use default.")


if __name__ == "__main__":
    main()
