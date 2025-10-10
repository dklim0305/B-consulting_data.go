import os, io, zipfile, xml.etree.ElementTree as ET
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
import pandas as pd

KEY = "41b2f29c22b92f1f912e4c80a33fbf1f62ebb4ce"
SAVE_DIR = r"C:\Users\admin\Downloads"
ZIP_PATH = os.path.join(SAVE_DIR, "corpCode.zip")
XML_PATH = os.path.join(SAVE_DIR, "CORPCODE.xml")
XLSX_PATH = os.path.join(SAVE_DIR, "corp_codes.xlsx")
URL = f"https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key={KEY}"

def make_session():
    s = requests.Session()
    retry = Retry(
        total=7, connect=7, read=7, backoff_factor=1.2,
        status_forcelist=(429, 500, 502, 503, 504),
        allowed_methods=frozenset(["GET", "HEAD"])  # 최신 버전 호환
    )
    adapter = HTTPAdapter(max_retries=retry, pool_connections=10, pool_maxsize=10)
    s.mount("https://", adapter)
    s.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) requests/2.x",
        "Accept": "*/*",
        "Connection": "close"
    })
    return s

def fetch_zip_stream():
    os.makedirs(SAVE_DIR, exist_ok=True)
    sess = make_session()
    with sess.get(URL, timeout=(10, 60), stream=True, verify=True) as r:
        r.raise_for_status()
        ctype = (r.headers.get("Content-Type") or "").lower()
        if "zip" not in ctype:
            err_path = os.path.join(SAVE_DIR, "corpCode_error_response.txt")
            text = r.text
            with open(err_path, "w", encoding="utf-8", errors="ignore") as f:
                f.write(text)
            print("ZIP 아님. 에러 응답 저장:", err_path)
            print("Content-Type:", ctype)
            print("미리보기:", text[:500])
            return None
        with open(ZIP_PATH, "wb") as f:
            for chunk in r.iter_content(chunk_size=1024 * 64):
                if chunk:
                    f.write(chunk)
        print("ZIP 저장 완료:", ZIP_PATH)
        return ZIP_PATH

def extract_xml(zip_path):
    with zipfile.ZipFile(zip_path, "r") as zf:
        name = zf.namelist()[0]
        zf.extract(name, SAVE_DIR)
        src = os.path.join(SAVE_DIR, name)
        if src != XML_PATH:
            if os.path.exists(XML_PATH):
                os.remove(XML_PATH)
            os.replace(src, XML_PATH)
    print("XML 추출 완료:", XML_PATH)
    return XML_PATH

def parse_xml_to_rows(xml_path):
    with open(xml_path, "rb") as f:
        root = ET.fromstring(f.read())
    rows = []
    for el in root.findall(".//list"):
        rows.append({
            "corp_code":   (el.findtext("corp_code") or "").strip(),
            "corp_name":   (el.findtext("corp_name") or "").strip(),
            "corp_eng":    (el.findtext("corp_eng_name") or "").strip(),
            "stock_code":  (el.findtext("stock_code") or "").strip(),
            "modify_date": (el.findtext("modify_date") or "").strip(),
        })
    return rows

def save_excel(rows):
    if not rows:
        print("파싱 결과 없음")
        return
    df = pd.DataFrame(rows, columns=["corp_code","corp_name","corp_eng","stock_code","modify_date"])
    df = df.sort_values(["corp_name","corp_code"], kind="stable").reset_index(drop=True)

    name_to_code = (df[["corp_name","corp_code"]]
                    .query("corp_name != ''")
                    .drop_duplicates(subset=["corp_name"], keep="first")
                    .rename(columns={"corp_name":"key(corp_name)","corp_code":"value(corp_code)"}))

    code_to_name = (df[["corp_code","corp_name"]]
                    .query("corp_code != ''")
                    .drop_duplicates(subset=["corp_code"], keep="first")
                    .rename(columns={"corp_code":"key(corp_code)","corp_name":"value(corp_name)"}))

    with pd.ExcelWriter(XLSX_PATH, engine="openpyxl") as w:
        df.to_excel(w, index=False, sheet_name="corp_codes(full)")
        name_to_code.to_excel(w, index=False, sheet_name="name_to_code(KV)")
        code_to_name.to_excel(w, index=False, sheet_name="code_to_name(KV)")
    print("엑셀 저장 완료:", XLSX_PATH)

def main():
    try:
        zip_path = fetch_zip_stream()
        if not zip_path:
            return
        xml_path = extract_xml(zip_path)
        rows = parse_xml_to_rows(xml_path)
        save_excel(rows)
    except requests.exceptions.ConnectionError as e:
        print("연결 오류:", e)
    except zipfile.BadZipFile:
        print("ZIP 손상: 파일이 완전하지 않음")
    except Exception as e:
        print("기타 오류:", repr(e))

if __name__ == "__main__":
    main()