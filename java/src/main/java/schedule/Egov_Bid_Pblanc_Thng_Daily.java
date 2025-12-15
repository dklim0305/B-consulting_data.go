package schedule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

public class Egov_Bid_Pblanc_Thng_Daily implements Runnable{

    @Override
    public void run() {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            System.out.println("===== 조달청 입찰공고목록 정보에 대한 물품조회 데이터 수집 시작 " + startTime + " =====");

            int pageNo = 1;
            int numOfRows = 999;

            Calendar cal = Calendar.getInstance();
            String format = "yyyyMMdd";
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            cal.add(cal.DATE, -1);
            String date = sdf.format(cal.getTime());
            String inqryBgnDt = date + "0000";
            String inqryEndDt = date + "2359";

            JsonObject jsonObj = callApi(pageNo, numOfRows, inqryBgnDt, inqryEndDt);
            int totalCount = jsonObj.getAsJsonObject("response").getAsJsonObject("body").get("totalCount").getAsInt();
            int pageCount = totalCount / numOfRows + 1;
            int dataCount = 0;

            JsonArray resultJsonArr = new JsonArray();

            for (int i = 1; i <= pageCount; i++) {
                JsonArray itemJsonArray = callApi(i, numOfRows, inqryBgnDt, inqryEndDt).getAsJsonObject("response").getAsJsonObject("body").getAsJsonArray("items");
                System.out.println("===== " + i + "/" + pageCount + "페이지 =====");

                for (JsonElement ele : itemJsonArray) {
                    JsonObject item = (JsonObject) ele;
                    resultJsonArr.add(item);

                    dataCount++;
                    System.out.println(dataCount + "/" + totalCount + "건");
                }
            }

            // 계약정보 DB INSERT
            insertCntrctInfo(resultJsonArr);

            LocalDateTime endTime = LocalDateTime.now();
            System.out.println("===== 조달청 입찰공고목록 정보에 대한 물품조회 데이터 수집 종료 " + endTime + " =====");

            Duration duration = Duration.between(startTime, endTime);
            long diffHours = duration.toHours();
            long diffMinutes = duration.toMinutes();
            long diffSeconds = duration.getSeconds();

            System.out.println("===== 총 소요시간 : " + diffHours + "시간 " + diffMinutes + "분 " + diffSeconds + "초 =====");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // 계약정보 API 호출
    public static JsonObject callApi(int pageNo, int numOfRows, String inqryBgnDt, String inqryEndDt) throws IOException {
        StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/1230000/ad/BidPublicInfoService/getBidPblancListInfoThng"); /*URL*/
        urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=odXIjL8t56rG0Fz1a69qgTTpLKxSPvIJG%2FlPU3bFsLOjdSAKcwHy8Wx0OCox8vLCtZEl6B9Jw%2BlWyoMylWEwsg%3D%3D"); /*Service Key*/
        urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode(String.valueOf(pageNo), "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode(String.valueOf(numOfRows), "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("type","UTF-8") + "=" + URLEncoder.encode("json", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("inqryDiv","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"));      // 조회구분 1 : 등록일시, 2 : 입찰공고번호, 3 : 변경일시
        urlBuilder.append("&" + URLEncoder.encode("inqryBgnDt","UTF-8") + "=" + URLEncoder.encode(inqryBgnDt, "UTF-8"));    // 조회시작일시 YYYYMMDDHHMM (조회구분 1, 3 선택 시 필수)
        urlBuilder.append("&" + URLEncoder.encode("inqryEndDt","UTF-8") + "=" + URLEncoder.encode(inqryEndDt, "UTF-8"));    // 조회종료일시 YYYYMMDDHHMM (조회구분 1, 3 선택 시 필수)
        // urlBuilder.append("&" + URLEncoder.encode("bidNtceNo","UTF-8") + "=" + URLEncoder.encode(bidNtceNo, "UTF-8"));     // 입찰공고번호 (조획구분 2 선택 시 필수)

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        System.out.println("Response code: " + conn.getResponseCode());
        BufferedReader rd;
        if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        JsonParser parser = new JsonParser();
        JsonObject resultObj = parser.parse(sb.toString()).getAsJsonObject();

        return resultObj;
    }

    // 데이터 전처리 -> csv 변환 시 셀 깨지는 현상으로 큰따옴표 제거
    public static String preprocessingData (JsonElement data, String dataName) {
        dataName = data.getAsJsonObject().get(dataName).getAsString();

        if (dataName == null) {
            return "";
        }

        dataName = dataName.replace("\"", "");

        return dataName;
    }

    // 계약정보 DB INSERT
    public static void insertCntrctInfo(JsonArray resultJsonArr) throws ClassNotFoundException {

        Class.forName("org.postgresql.Driver");

        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "postgres";

        try (Connection connection = DriverManager.getConnection(url, user, password);) {
            Statement statement = connection.createStatement();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String now = sdf.format(new Date());

            String sql = "INSERT INTO BID_PBLANC_LIST_INFO_THNG (" +
                                                                "BID_NTCE_NO," +
                                                                "BID_NTCE_ORD," +
                                                                "RE_NTCE_YN," +
                                                                "RGST_TY_NM," +
                                                                "NTCE_KIND_NM," +
                                                                "INTRBID_YN," +
                                                                "BID_NTCE_DT," +
                                                                "REF_NO," +
                                                                "BID_NTCE_NM," +
                                                                "NTCE_INSTT_CD," +
                                                                "NTCE_INSTT_NM," +
                                                                "DMINSTT_CD," +
                                                                "DMINSTT_NM," +
                                                                "BID_METHD_NM," +
                                                                "CNTRCT_CNCLS_MTHD_NM," +
                                                                "NTCE_INSTT_OFCL_NM," +
                                                                "NTCE_INSTT_OFCL_TEL_NO," +
                                                                "NTCE_INSTT_OFCL_EMAIL_ADRS," +
                                                                "EXCTV_NM," +
                                                                "BID_QLFCT_RGST_DT," +
                                                                "CMMN_SPLDMD_AGRMNT_RCPTDOC_METHD," +
                                                                "CMMN_SPLDMD_AGRMNT_CLSE_DT," +
                                                                "CMMN_DPLDMD_CORP_RGN_LMT_YN," +
                                                                "BID_BEGIN_DT," +
                                                                "BID_CLSE_DT," +
                                                                "OPENG_DT," +
                                                                "NTCE_SPEC_DOC_URL1," +
                                                                "NTCE_SPEC_DOC_URL2," +
                                                                "NTCE_SPEC_DOC_URL3," +
                                                                "NTCE_SPEC_DOC_URL4," +
                                                                "NTCE_SPEC_DOC_URL5," +
                                                                "NTCE_SPEC_DOC_URL6," +
                                                                "NTCE_SPEC_DOC_URL7," +
                                                                "NTCE_SPEC_DOC_URL8," +
                                                                "NTCE_SPEC_DOC_URL9," +
                                                                "NTCE_SPEC_DOC_URL10," +
                                                                "NTCE_DPEC_FILE_NM1," +
                                                                "NTCE_DPEC_FILE_NM2," +
                                                                "NTCE_DPEC_FILE_NM3," +
                                                                "NTCE_DPEC_FILE_NM4," +
                                                                "NTCE_DPEC_FILE_NM5," +
                                                                "NTCE_DPEC_FILE_NM6," +
                                                                "NTCE_DPEC_FILE_NM7," +
                                                                "NTCE_DPEC_FILE_NM8," +
                                                                "NTCE_DPEC_FILE_NM9," +
                                                                "NTCE_DPEC_FILE_NM10," +
                                                                "RBID_PERMSN_YN," +
                                                                "PRDCT_CLSFC_LMT_YN," +
                                                                "MNFCT_YN," +
                                                                "PREARNG_PRCE_DCSN_MTHD_NM," +
                                                                "TOT_PRDPRC_NUM," +
                                                                "DRWT_PRDPRC_NUM," +
                                                                "ASIGN_BDGT_AMT," +
                                                                "PRESMPT_PRCE," +
                                                                "OPENG_PLCE," +
                                                                "BID_NTCE_DTL_URL," +
                                                                "BID_NTCE_URL," +
                                                                "BID_PRTCPT_FEE_PAYMNT_YN," +
                                                                "BID_PRTCPT_FEE," +
                                                                "BID_GRNTYMNY_PAYMNT_YN," +
                                                                "CRDTR_NM," +
                                                                "DTIL_PRDCT_CLSFC_NO," +
                                                                "DTIL_PRDCT_CLSFC_NO_NM," +
                                                                "PRDCT_SPEC_NM," +
                                                                "PRDCT_QTY," +
                                                                "PRDCT_UNIT," +
                                                                "PRDCT_UPRC," +
                                                                "DLVR_TMLMT_DT," +
                                                                "DLVR_DAYNUM," +
                                                                "DLVRY_CNDTN_NM," +
                                                                "PURCHS_OBJ_PRDCT_LIST," +
                                                                "UNTY_NTCE_NO," +
                                                                "CMMN_SPLDMD_METHD_CD," +
                                                                "CMMN_SPLDMD_METHD_NM," +
                                                                "STD_NTCE_DOC_URL," +
                                                                "BRFFC_BIDPRC_PERMSN_YN," +
                                                                "DSGNT_CMPT_YN," +
                                                                "RSRVTN_PRCE_RE_MKNG_MTHD_NM," +
                                                                "ARSLT_APPL_DOC_RCPT_MTHD_NM," +
                                                                "ARSLT_APPL_DOC_RCPT_DT," +
                                                                "ORDER_PLAN_UNTY_NO," +
                                                                "SUCSFBID_LWLT_RATE," +
                                                                "RGST_DT," +
                                                                "BF_SPEC_RGST_NO," +
                                                                "INFO_BIZ_YN," +
                                                                "SUCSFBID_MTHD_CD," +
                                                                "SUCSFBID_MTHD_NM," +
                                                                "CHG_DT," +
                                                                "DMINSTT_OFCL_EMAIL_ADRS," +
                                                                "INDSTRYTY_LMT_YN," +
                                                                "CHG_NTCE_RSN," +
                                                                "RBID_OPENG_DT," +
                                                                "VAT," +
                                                                "INDUTY_VAT," +
                                                                "BID_WGRNTEE_RCPT_CLSE_DT," +
                                                                "RGN_LMT_BID_LOCPLC_JDGM_BSS_CD," +
                                                                "RGN_LMT_BID_LOCPLC_JDGM_BSS_NM," +
                                                                "TECH_ABLT_EVL_RT," +
                                                                "BID_PRCE_EVL_RT," +
                                                                "DEL_YN," +
                                                                "FRST_RGSR_DTL_DTTM," +
                                                                "LAST_CHNG_DTL_DTTM" +
                                                                ")" +
                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                                  "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                                  "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                                  "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                                  "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                                  "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                                  "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                                  "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                                  "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                                  "?, ?, ?, ?, ?, ?, ?, ?, ?, 'N', '" + now + "', '" + now + "')";
            PreparedStatement ps = connection.prepareStatement(sql);

            System.out.println("===== 계약정보 DB INSERT =====");

            int cnt = 0;

            for (JsonElement e : resultJsonArr) {
                String bidNtceNo = e.getAsJsonObject().get("bidNtceNo").getAsString();                               // 입찰공고번호
                String bidNtceOrd = e.getAsJsonObject().get("bidNtceOrd").getAsString();                          // 입찰공고차수
                String reNtceYn = e.getAsJsonObject().get("reNtceYn").getAsString();                               // 재공고여부
                String rgstTyNm = e.getAsJsonObject().get("rgstTyNm").getAsString();                                // 등록유형명
                String ntceKindNm = e.getAsJsonObject().get("ntceKindNm").getAsString();                             // 공고종류명
                String intrbidYn = e.getAsJsonObject().get("intrbidYn").getAsString();                     // 국제입찰여부
                String bidNtceDt = e.getAsJsonObject().get("bidNtceDt").getAsString();                      // 입찰공고일시
                String refNo = e.getAsJsonObject().get("refNo").getAsString();                            // 참조번호
                String bidNtceNm = e.getAsJsonObject().get("bidNtceNm").getAsString();                           // 입찰공고명
                String ntceInsttCd = e.getAsJsonObject().get("ntceInsttCd").getAsString();                      // 공고기관코드
                String ntceInsttNm = e.getAsJsonObject().get("ntceInsttNm").getAsString();;                           // 공고기관명
                String dminsttCd = e.getAsJsonObject().get("dminsttCd").getAsString();                              // 수요기관코드
                String dminsttNm = e.getAsJsonObject().get("dminsttNm").getAsString();                          // 수요기관명
                String bidMethdNm = e.getAsJsonObject().get("bidMethdNm").getAsString();                         // 입찰방식명
                String cntrctCnclsMthdNm = e.getAsJsonObject().get("cntrctCnclsMthdNm").getAsString();                      // 계약체결방법명
                String ntceInsttOfclNm = e.getAsJsonObject().get("ntceInsttOfclNm").getAsString();                       // 공고기관담당자명
                String ntceInsttOfclTelNo = e.getAsJsonObject().get("ntceInsttOfclTelNo").getAsString();                     // 공고기관담당자전화번호
                String ntceInsttOfclEmailAdrs = e.getAsJsonObject().get("ntceInsttOfclEmailAdrs").getAsString();                 // 공고기관담당자이메일주소
                String exctvNm = e.getAsJsonObject().get("exctvNm").getAsString();                                  // 집행관명
                String bidQlfctRgstDt = e.getAsJsonObject().get("bidQlfctRgstDt").getAsString();                        // 입찰참가자격등록마감일시
                String cmmnSpldmdAgrmntRcptdocMethd = e.getAsJsonObject().get("cmmnSpldmdAgrmntRcptdocMethd").getAsString();       // 공동수급협정서접수방식
                String cmmnSpldmdAgrmntClseDt = e.getAsJsonObject().get("cmmnSpldmdAgrmntClseDt").getAsString();             // 공동수급협정마감일시
                String cmmnSpldmdCorpRgnLmtYn = e.getAsJsonObject().get("cmmnSpldmdCorpRgnLmtYn").getAsString();                // 공동수급업체지역제한여부
                String bidBeginDt = e.getAsJsonObject().get("bidBeginDt").getAsString();                      // 입찰개시일시
                String bidClseDt = e.getAsJsonObject().get("bidClseDt").getAsString();                          // 입찰마감일시
                String opengDt = e.getAsJsonObject().get("opengDt").getAsString();                            // 개찰일시
                String ntceSpecDocUrl1 = e.getAsJsonObject().get("ntceSpecDocUrl1").getAsString();            // 공고규격서URL1
                String ntceSpecDocUrl2 = e.getAsJsonObject().get("ntceSpecDocUrl2").getAsString();                // 공고규격서URL2
                String ntceSpecDocUrl3 = e.getAsJsonObject().get("ntceSpecDocUrl3").getAsString();               // 공고규격서URL3
                String ntceSpecDocUrl4 = e.getAsJsonObject().get("ntceSpecDocUrl4").getAsString();                  // 공고규격서URL4
                String ntceSpecDocUrl5 = e.getAsJsonObject().get("ntceSpecDocUrl5").getAsString();                   // 공고규격서URL5
                String ntceSpecDocUrl6 = e.getAsJsonObject().get("ntceSpecDocUrl6").getAsString();                      // 공고규격서URL6
                String ntceSpecDocUrl7 = e.getAsJsonObject().get("ntceSpecDocUrl7").getAsString();                      // 공고규격서URL7
                String ntceSpecDocUrl8 = e.getAsJsonObject().get("ntceSpecDocUrl8").getAsString();              // 공고규격서URL8
                String ntceSpecDocUrl9 = e.getAsJsonObject().get("ntceSpecDocUrl9").getAsString();                  // 공고규격서URL9
                String ntceSpecDocUrl10 = e.getAsJsonObject().get("ntceSpecDocUrl10").getAsString();;               // 공고규격서URL10
                String ntceSpecFileNm1 = e.getAsJsonObject().get("ntceSpecFileNm1").getAsString();           // 공구규격파일명1
                String ntceSpecFileNm2 = e.getAsJsonObject().get("ntceSpecFileNm2").getAsString();                     // 공구규격파일명2
                String ntceSpecFileNm3 = e.getAsJsonObject().get("ntceSpecFileNm3").getAsString();                      // 공구규격파일명3
                String ntceSpecFileNm4 = e.getAsJsonObject().get("ntceSpecFileNm4").getAsString();                  // 공구규격파일명4
                String ntceSpecFileNm5 = e.getAsJsonObject().get("ntceSpecFileNm5").getAsString();                 // 공구규격파일명5
                String ntceSpecFileNm6 = e.getAsJsonObject().get("ntceSpecFileNm6").getAsString();                 // 공구규격파일명6
                String ntceSpecFileNm7 = e.getAsJsonObject().get("ntceSpecFileNm7").getAsString();                   // 공구규격파일명7
                String ntceSpecFileNm8 = e.getAsJsonObject().get("ntceSpecFileNm8").getAsString();             // 공구규격파일명8
                String ntceSpecFileNm9 = e.getAsJsonObject().get("ntceSpecFileNm9").getAsString();                      // 공구규격파일명9
                String ntceSpecFileNm10 = e.getAsJsonObject().get("ntceSpecFileNm10").getAsString();                     // 공구규격파일명10
                String rbidPermsnYn = e.getAsJsonObject().get("rbidPermsnYn").getAsString();                         // 재입찰허용여부
                String prdctClsfcLmtYn = e.getAsJsonObject().get("prdctClsfcLmtYn").getAsString();                      // 물품분류제한여부
                String mnfctYn = e.getAsJsonObject().get("mnfctYn").getAsString();                              // 제조여부
                String prearngPrceDcsnMthdNm = e.getAsJsonObject().get("prearngPrceDcsnMthdNm").getAsString();                // 예정가격결정방법명
                String totPrdprcNum = e.getAsJsonObject().get("totPrdprcNum").getAsString();                         // 총예가건수
                String drwtPrdprcNum = e.getAsJsonObject().get("drwtPrdprcNum").getAsString();                        // 추첨예가건수
                String asignBdgtAmt = e.getAsJsonObject().get("asignBdgtAmt").getAsString();                         // 배정예산금액
                String presmptPrce = e.getAsJsonObject().get("presmptPrce").getAsString();                          // 추정가격
                String opengPlce = e.getAsJsonObject().get("opengPlce").getAsString();                            // 개찰장소
                String bidNtceDtlUrl = e.getAsJsonObject().get("bidNtceDtlUrl").getAsString();                        // 입찰공고상세URL
                String bidNtceUrl = e.getAsJsonObject().get("bidNtceUrl").getAsString();                           // 입찰공고URL
                String bidPrtcptFeePaymntYn = e.getAsJsonObject().get("bidPrtcptFeePaymntYn").getAsString();                 // 입찰참가수수료납부여부
                String bidPrtcptFee = e.getAsJsonObject().get("bidPrtcptFee").getAsString();                         // 입찰참가수수료
                String bidGrntymnyPaymntYn = e.getAsJsonObject().get("bidGrntymnyPaymntYn").getAsString();                  // 입찰보증금납부여부
                String crdtrNm = e.getAsJsonObject().get("crdtrNm").getAsString();                              // 채권자명
                String dtilPrdctClsfcNo = e.getAsJsonObject().get("dtilPrdctClsfcNo").getAsString();                     // 세부품명번호
                String dtilPrdctClsfcNoNm = e.getAsJsonObject().get("dtilPrdctClsfcNoNm").getAsString();                   // 세부품명
                String prdctSpecNm = e.getAsJsonObject().get("prdctSpecNm").getAsString();                          // 물품규격명
                String prdctQty = e.getAsJsonObject().get("prdctQty").getAsString();                             // 물품수량
                String prdctUnit = e.getAsJsonObject().get("prdctUnit").getAsString();                            // 물품단위
                String prdctUprc = e.getAsJsonObject().get("prdctUprc").getAsString();                            // 물품단가
                String dlvrTmlmtDt = e.getAsJsonObject().get("dlvrTmlmtDt").getAsString();                          // 납품기한일시
                String dlvrDaynum = e.getAsJsonObject().get("dlvrDaynum").getAsString();                           // 납품일시
                String dlvryCndtnNm = e.getAsJsonObject().get("dlvryCndtnNm").getAsString();                         // 인도조건명
                String purchsObjPrdctList = e.getAsJsonObject().get("purchsObjPrdctList").getAsString();                   // 구매대상물품목록
                String untyNtceNo = e.getAsJsonObject().get("untyNtceNo").getAsString();                           // 통합공고번호
                String cmmnSpldmdMethdCd = e.getAsJsonObject().get("cmmnSpldmdMethdCd").getAsString();                    // 공동수급방식코드
                String cmmnSpldmdMethdNm = e.getAsJsonObject().get("cmmnSpldmdMethdNm").getAsString();                    // 공동수급방식명
                String stdNtceDocUrl = e.getAsJsonObject().get("stdNtceDocUrl").getAsString();                        // 표준공고서URL
                String brffcBidprcPermsnYn = e.getAsJsonObject().get("brffcBidprcPermsnYn").getAsString();                  // 지사투찰허용여부
                String dsgntCmptYn = e.getAsJsonObject().get("dsgntCmptYn").getAsString();                          // 지명경쟁여부
                String rsrvtnPrceReMkngMthdNm = e.getAsJsonObject().get("rsrvtnPrceReMkngMthdNm").getAsString();               // 예비가격재작성방법명
                String arsltApplDocRcptMthdNm = e.getAsJsonObject().get("arsltApplDocRcptMthdNm").getAsString();               // 실적신청서접수방법명
                String arsltApplDocRcptDt = e.getAsJsonObject().get("arsltApplDocRcptDt").getAsString();                   // 실적신청서접수일시
                String orderPlanUntyNo = e.getAsJsonObject().get("orderPlanUntyNo").getAsString();                      // 발주계획통합번호
                String sucsfbidLwltRate = e.getAsJsonObject().get("sucsfbidLwltRate").getAsString();                     // 낙찰하한율
                String rgstDt = e.getAsJsonObject().get("rgstDt").getAsString();                               // 등록일시
                String bfSpecRgstNo = e.getAsJsonObject().get("bfSpecRgstNo").getAsString();                         // 사전규격등록번호
                String infoBizYn = e.getAsJsonObject().get("infoBizYn").getAsString();                            // 정보화사업여부
                String sucsfbidMthdCd = e.getAsJsonObject().get("sucsfbidMthdCd").getAsString();                       // 낙찰방법코드
                String sucsfbidMthdNm = e.getAsJsonObject().get("sucsfbidMthdNm").getAsString();                       // 낙찰방법명
                String chgDt = e.getAsJsonObject().get("chgDt").getAsString();                                // 변경일시
                String dminsttOfclEmailAdrs = e.getAsJsonObject().get("dminsttOfclEmailAdrs").getAsString();                 // 수요기관담당자이메일주소
                String indstrytyLmtYn = e.getAsJsonObject().get("indstrytyLmtYn").getAsString();                       // 업종제한여부
                String chgNtceRsn = e.getAsJsonObject().get("chgNtceRsn").getAsString();                           // 변경공고사유
                String rbidOpengDt = e.getAsJsonObject().get("rbidOpengDt").getAsString();                          // 재입찰개찰일시
                String VAT = e.getAsJsonObject().get("VAT").getAsString();                                  // 부가가치세
                String indutyVAT = e.getAsJsonObject().get("indutyVAT").getAsString();                            // 주공종부가가치세
                String bidWgrnteeRcptClseDt = e.getAsJsonObject().get("bidWgrnteeRcptClseDt").getAsString();                 // 입찰보증서접수마감일시
                String rgnLmtBidLocplcJdgmBssCd = e.getAsJsonObject().get("rgnLmtBidLocplcJdgmBssCd").getAsString();             // 지역제한입찰소재지판단기준코드
                String rgnLmtBidLocplcJdgmBssNm = e.getAsJsonObject().get("rgnLmtBidLocplcJdgmBssNm").getAsString();             // 지역제한입찰소재지판단기준명
                String techAbltEvlRt = e.getAsJsonObject().get("techAbltEvlRt").getAsString();                        // 기술능력평가비율
                String bidPrceEvlRt = e.getAsJsonObject().get("bidPrceEvlRt").getAsString();                         // 입찰가격평가비율

                ps.setString(1, bidNtceNo);
                ps.setString(2, bidNtceOrd);
                ps.setString(3, reNtceYn);
                ps.setString(4, rgstTyNm);
                ps.setString(5, ntceKindNm);
                ps.setString(6, intrbidYn);
                ps.setString(7, bidNtceDt);
                ps.setString(8, refNo);
                ps.setString(9, bidNtceNm);
                ps.setString(10, ntceInsttCd);
                ps.setString(11, ntceInsttNm);
                ps.setString(12, dminsttCd);
                ps.setString(13, dminsttNm);
                ps.setString(14, bidMethdNm);
                ps.setString(15, cntrctCnclsMthdNm);
                ps.setString(16, ntceInsttOfclNm);
                ps.setString(17, ntceInsttOfclTelNo);
                ps.setString(18, ntceInsttOfclEmailAdrs);
                ps.setString(19, exctvNm);
                ps.setString(20, bidQlfctRgstDt);
                ps.setString(21, cmmnSpldmdAgrmntRcptdocMethd);
                ps.setString(22, cmmnSpldmdAgrmntClseDt);
                ps.setString(23, cmmnSpldmdCorpRgnLmtYn);
                ps.setString(24, bidBeginDt);
                ps.setString(25, bidClseDt);
                ps.setString(26, opengDt);
                ps.setString(27, ntceSpecDocUrl1);
                ps.setString(28, ntceSpecDocUrl2);
                ps.setString(29, ntceSpecDocUrl3);
                ps.setString(30, ntceSpecDocUrl4);
                ps.setString(31, ntceSpecDocUrl5);
                ps.setString(32, ntceSpecDocUrl6);
                ps.setString(33, ntceSpecDocUrl7);
                ps.setString(34, ntceSpecDocUrl8);
                ps.setString(35, ntceSpecDocUrl9);
                ps.setString(36, ntceSpecDocUrl10);
                ps.setString(37, ntceSpecFileNm1);
                ps.setString(38, ntceSpecFileNm2);
                ps.setString(39, ntceSpecFileNm3);
                ps.setString(40, ntceSpecFileNm4);
                ps.setString(41, ntceSpecFileNm5);
                ps.setString(42, ntceSpecFileNm6);
                ps.setString(43, ntceSpecFileNm7);
                ps.setString(44, ntceSpecFileNm8);
                ps.setString(45, ntceSpecFileNm9);
                ps.setString(46, ntceSpecFileNm10);
                ps.setString(47, rbidPermsnYn);
                ps.setString(48, prdctClsfcLmtYn);
                ps.setString(49, mnfctYn);
                ps.setString(50, prearngPrceDcsnMthdNm);
                ps.setString(51, totPrdprcNum);
                ps.setString(52, drwtPrdprcNum);
                ps.setString(53, asignBdgtAmt);
                ps.setString(54, presmptPrce);
                ps.setString(55, opengPlce);
                ps.setString(56, bidNtceDtlUrl);
                ps.setString(57, bidNtceUrl);
                ps.setString(58, bidPrtcptFeePaymntYn);
                ps.setString(59, bidPrtcptFee);
                ps.setString(60, bidGrntymnyPaymntYn);
                ps.setString(61, crdtrNm);
                ps.setString(62, dtilPrdctClsfcNo);
                ps.setString(63, dtilPrdctClsfcNoNm);
                ps.setString(64, prdctSpecNm);
                ps.setString(65, prdctQty);
                ps.setString(66, prdctUnit);
                ps.setString(67, prdctUprc);
                ps.setString(68, dlvrTmlmtDt);
                ps.setString(69, dlvrDaynum);
                ps.setString(70, dlvryCndtnNm);
                ps.setString(71, purchsObjPrdctList);
                ps.setString(72, untyNtceNo);
                ps.setString(73, cmmnSpldmdMethdCd);
                ps.setString(74, cmmnSpldmdMethdNm);
                ps.setString(75, stdNtceDocUrl);
                ps.setString(76, brffcBidprcPermsnYn);
                ps.setString(77, dsgntCmptYn);
                ps.setString(78, rsrvtnPrceReMkngMthdNm);
                ps.setString(79, arsltApplDocRcptMthdNm);
                ps.setString(80, arsltApplDocRcptDt);
                ps.setString(81, orderPlanUntyNo);
                ps.setString(82, sucsfbidLwltRate);
                ps.setString(83, rgstDt);
                ps.setString(84, bfSpecRgstNo);
                ps.setString(85, infoBizYn);
                ps.setString(86, sucsfbidMthdCd);
                ps.setString(87, sucsfbidMthdNm);
                ps.setString(88, chgDt);
                ps.setString(89, dminsttOfclEmailAdrs);
                ps.setString(90, indstrytyLmtYn);
                ps.setString(91, chgNtceRsn);
                ps.setString(92, rbidOpengDt);
                ps.setString(93, VAT);
                ps.setString(94, indutyVAT);
                ps.setString(95, bidWgrnteeRcptClseDt);
                ps.setString(96, rgnLmtBidLocplcJdgmBssCd);
                ps.setString(97, rgnLmtBidLocplcJdgmBssNm);
                ps.setString(98, techAbltEvlRt);
                ps.setString(99, bidPrceEvlRt);

                int result = ps.executeUpdate();

                cnt++;

                if (result > 0) {
                    System.out.println("[SUCCESS] " + cnt + "/" + resultJsonArr.size() + "번째 데이터 INSERT 처리");
                } else {
                    System.out.println("[NO CHANGE] INSERT 없음");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    // CSV 추출
    public static void convertJsonToCsv(JsonArray resultJsonArr) throws IOException {

        Calendar cal = Calendar.getInstance();
        String format = "yyyyMMdd";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        cal.add(cal.DATE, -1);
        String date = sdf.format(cal.getTime());

        String filePath = "C:\\Users\\admin\\Desktop";
        String fileName = "입찰공고목록 정보에 대한 물품조회_" + date + "~" + date + ".csv";
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath + "\\" + fileName), StandardCharsets.UTF_8));
        JsonObject itemObj = resultJsonArr.get(0).getAsJsonObject();

        // 키값 추출
        ArrayList<String> keyArr = new ArrayList<>();
        Set<String> keySet = itemObj.keySet();
        int idx = 0;

        for (String key : keySet) {
            keyArr.add(key);
            writer.write(key);
            if (++idx < keySet.size()) {
                writer.write(",");
            }
        }

        writer.write("\n");

        for (JsonElement jsonElement : resultJsonArr) {
            idx = 0;
            for (int i = 0; i < keyArr.size(); i++) {
                String data = jsonElement.getAsJsonObject().get(keyArr.get(i)).toString();
                writer.write(jsonElement.getAsJsonObject().get(keyArr.get(i)).toString());
                if (++idx < keySet.size()) {
                    writer.write(",");
                }
            }
            writer.write("\n");
        }

        writer.close();

        System.out.println("수집 결과 : " + filePath + "\\" + fileName);
    }


}


