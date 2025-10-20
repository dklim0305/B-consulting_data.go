from urllib.request import urlopen, Request
from urllib.parse import urlencode
import pandas as pd
import json
from datetime import datetime
import time
import hashlib
import os
import glob
import random

# ================== 설정 ==================
URL = 'https://apis.data.go.kr/1230000/ao/PubDataOpnStdService/getDataSetOpnStdBidPblancInfo'
SERVICE_KEY = 'GkdwpZIMCMx6y3h8SR+VRHft47yi6WPAG1ugNGWTsNVfXtdcZriNYv4C/ufr11gzctGilPi8rFm6O03M51qT0w=='   # 실제 키로 교체

START_DATE = '202503010000'
END_DATE   = '202503311159'

START_PAGE   = 1
NUM_OF_ROWS  = 100
MAX_RETRIES  = 5
TIMEOUT_SEC  = 30

# 트래픽/속도 제어
MAX_TPS        = 30
AVG_RESP_SEC   = 0.5
TARGET_RPS     = min(MAX_TPS, 1.0 / AVG_RESP_SEC)  # 2 RPS
TARGET_INTERVAL = 1.0 / TARGET_RPS                 # 0.5 sec
JITTER_RANGE_S = (0.0, 0.05)

# 중간 저장
SAVE_DIR = r"C:\Users\admin\Downloads"
PARTIAL_PREFIX = "bid_result_partial_"
PARTIAL_EVERY_PAGES = 50

# 컬럼명 변환
col_mapping = {
    "bidNtceNo": "입찰공고번호",
    "bidNtceOrd": "입찰공고차수",
    "refNtceNo": "참조공고번호",
    "refNtceOrd": "참조공고차수",
    "ppsNtceYn": "나라장터공고여부",
    "bidNtceNm": "입찰공고명",
    "bidNtceSttusNm": "입찰공고상태명",
    "bidNtceDate": "입찰공고일자",
    "bidNtceBgn": "입찰공고시각",
    "bsnsDivNm": "업무구분명",
    "intrntnlBidYn": "국제입찰여부",
    "cmmnCntrctYn": "공동계약여부",
    "cmmnReciptMethdNm": "공동수급방식명",
    "elctrnBidYn": "전자입찰여부",
    "cntrctCnclsSttusNm": "계약체결형태명",
    "cntrctCnclsMthdNm": "계약체결방법명",
    "bidwinrDcsnMthdNm": "낙찰자결정방법명",
    "ntceInsttNm": "공고기관명",
    "ntceInsttCd": "공고기관코드",
    "ntceInsttOfclDeptNm": "공고기관담당자부서명",
    "ntceInsttOfclNm": "공고기관담당자명",
    "ntceInsttOfclTel": "공고기관담당자전화번호",
    "ntceInsttOfclEmailAdrs": "공고기관담당자이메일주소",
    "dmndInsttNm": "수요기관명",
    "dmndInsttCd": "수요기관코드",
    "dmndInsttOfclDeptNm": "수요기관담당자부서명",
    "dmndInsttOfclNm": "수요기관담당자명",
    "dmndInsttOfclTel": "수요기관담당자전화번호",
    "dmndInsttOfclEmailAdrs": "수요기관담당자이메일주소",
    "presnatnOprtnYn": "설명회실시여부",
    "presnatnOprtnDate": "설명회실시일자",
    "presnatnOprtnTm": "설명회실시시각",
    "presnatnOprtnPlce": "설명회실시장소",
    "bidPrtcptQlfctRgstClseDate": "입찰참가자격등록마감일자",
    "bidPrtcptQlfctRgstClseTm": "입찰참가자격등록마감시각",
    "cmmnReciptAgrmntClseDate": "공동수급협정마감일자",
    "cmmnReciptAgrmntClseTm": "공동수급협정마감시각",
    "bidBeginDate": "입찰개시일자",
    "bidBeginTm": "입찰개시시각",
    "bidClseDate": "입찰마감일자",
    "bidClseTm": "입찰마감시각",
    "opengDate": "개찰일자",
    "opengTm": "개찰시각",
    "opengPlce": "개찰장소",
    "asignBdgtAmt": "배정예산금액",
    "presmptPrce": "추정가격",
    "rsrvtnPrceDcsnMthdNm": "예정가격결정방법명",
    "rgnLmtYn": "지역제한여부",
    "prtcptPsblRgnNm": "참가가능지역명",
    "indstrytyLmtYn": "업종제한여부",
    "bidprcPsblIndstrytyNm": "투찰가능업종명",
    "bidNtceUrl": "입찰공고URL",
    "dataBssDate": "데이터기준일자"
}
# ==========================================

def build_query(page: int) -> str:
    return '?' + urlencode({
        'serviceKey': SERVICE_KEY,
        'pageNo': str(page),
        'numOfRows': str(NUM_OF_ROWS),
        'type': 'json',
        'bidNtceBgnDt': START_DATE,
        'bidNtceEndDt': END_DATE
    })

def unique_key_for_item(item: dict) -> str:
    if 'bidNtceNo' in item and 'bidNtceOrd' in item:
        return f"{item['bidNtceNo']}|{item['bidNtceOrd']}"
    elif 'bidNtceNo' in item:
        return str(item['bidNtceNo'])
    return hashlib.sha1(json.dumps(item, sort_keys=True, ensure_ascii=False).encode('utf-8')).hexdigest()

def fetch_page(page: int) -> dict:
    """API 호출 + JSON 변환, JSON이 아니면 원문 출력"""
    qp = build_query(page)
    req = Request(URL + qp, headers={'User-Agent': 'Mozilla/5.0'})
    with urlopen(req, timeout=TIMEOUT_SEC) as resp:
        raw = resp.read().decode('utf-8')

    if not raw.strip().startswith("{"):
        print(f"\npage {page} 응답 원문(앞 500자):\n{raw[:500]}\n")
        raise ValueError(f"page {page}: JSON 응답이 아님")

    return json.loads(raw)

def throttle(start_ts: float):
    elapsed = time.perf_counter() - start_ts
    need = TARGET_INTERVAL - elapsed
    if need > 0:
        time.sleep(need + random.uniform(*JITTER_RANGE_S))

def save_partial(rows, part_no):
    now = datetime.now().strftime("%Y%m%d_%H%M%S")
    path = os.path.join(SAVE_DIR, f"{PARTIAL_PREFIX}{part_no}_{now}.csv")
    pd.json_normalize(rows).to_csv(path, index=False, encoding='utf-8-sig')
    print(f"중간 저장: {path}")
    
# csv로 저장해놓다가     
# 합칠때 csv로 안한 이유가 무엇인지 궁금 
def merge_partials_to_excel():
    files = sorted(glob.glob(os.path.join(SAVE_DIR, f"{PARTIAL_PREFIX}*.csv")))
    if not files:
        print("중간 저장된 파일이 없습니다.")
        return
    dfs = [pd.read_csv(f, dtype=str, encoding='utf-8-sig') for f in files]
    df_all = pd.concat(dfs, ignore_index=True)
    df_all = df_all.rename(columns=col_mapping)
    df_all = df_all.astype(str).drop_duplicates()

    now = datetime.now().strftime("%Y%m%d_%H%M%S")
    final_path = os.path.join(SAVE_DIR, f"bid_result_{now}.xlsx")
    df_all.to_excel(final_path, index=False)
    print(f"최종 저장 완료: {final_path}")

def main():
    os.makedirs(SAVE_DIR, exist_ok=True)

    all_rows = []
    seen_keys = set()
    page = START_PAGE
    total_count = None
    part_no = 1

    try:
        while True:
            attempt = 0
            while True:
                start_ts = time.perf_counter()
                try:
                    data = fetch_page(page)
                    throttle(start_ts)
                    break
                except Exception as e:
                    throttle(start_ts)
                    attempt += 1
                    if attempt >= MAX_RETRIES:
                        print(f"page {page}: 재시도 {MAX_RETRIES}회 초과. 중단.")
                        raise
                    sleep_sec = 1 * (2 ** (attempt - 1))
                    print(f"page {page}: 오류 발생. {sleep_sec}초 후 재시도. 오류: {e}")
                    time.sleep(sleep_sec)

            if 'response' not in data or 'body' not in data['response']:
                print(f"page {page}: body 없음. 종료.")
                break

            body = data['response']['body']
            items = body.get('items')

            if total_count is None and 'totalCount' in body:
                try:
                    total_count = int(body['totalCount'])
                except Exception:
                    total_count = None

            if not items:
                print(f"page {page}: 데이터 없음. 종료.")
                break

            added = 0
            for it in items:
                uk = unique_key_for_item(it)
                if uk not in seen_keys:
                    seen_keys.add(uk)
                    all_rows.append(it)
                    added += 1

            if total_count is not None:
                print(f"page {page} 완료: {added}건 추가 (누적 {len(all_rows)}/{total_count})")
            else:
                print(f"page {page} 완료: {added}건 추가 (누적 {len(all_rows)})")

            if page % PARTIAL_EVERY_PAGES == 0:
                save_partial(all_rows, part_no)
                all_rows = []
                part_no += 1

            if total_count is not None and page * NUM_OF_ROWS >= total_count:
                print("모든 페이지 수집 완료 추정")
                break

            page += 1

    except Exception as e:
        print(f"전체 처리 중단: {e}")

    if all_rows:
        save_partial(all_rows, part_no)

    merge_partials_to_excel()

if __name__ == "__main__":
    main()




