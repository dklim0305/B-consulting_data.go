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
import java.util.*;
import java.util.Date;

public class Egov_Cntrct_Daily implements Runnable{

    @Override
    public void run() {
        try {

            LocalDateTime startTime = LocalDateTime.now();
            System.out.println("===== 나라장터 계약정보 데이터 수집 시작 " + startTime + " =====");

            int pageNo = 1;
            int numOfRows = 999;

            Calendar cal = Calendar.getInstance();
            String format = "yyyyMMdd";
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            cal.add(cal.DATE, -1);
            String date = sdf.format(cal.getTime());

            JsonObject jsonObj = callApi(pageNo, numOfRows, date, date);
            int totalCount = jsonObj.getAsJsonObject("response").getAsJsonObject("body").get("totalCount").getAsInt();
            int pageCount = totalCount / numOfRows + 1;
            int dataCount = 0;

            JsonArray resultJsonArr = new JsonArray();

            for (int i = 1; i <= pageCount; i++) {
                JsonArray itemJsonArray = callApi(i, numOfRows, date, date).getAsJsonObject("response").getAsJsonObject("body").getAsJsonArray("items");
                System.out.println("===== " + i + "/" + pageCount + "페이지 =====");

                for (JsonElement ele : itemJsonArray) {
                    JsonObject item = (JsonObject) ele;
                    String cntrctNo = preprocessingData(item, "cntrctNo");                              // 계약번호
                    String untyCntrctNo = preprocessingData(item, "untyCntrctNo");                      // 통합계약번호
                    String cntrctOrd = preprocessingData(item, "cntrctOrd");                            // 계약차수
                    String cntrctNm = preprocessingData(item, "cntrctNm");                              // 계약명
                    String bsnsDivNm = preprocessingData(item, "bsnsDivNm");                            // 업무구분명
                    String cntrctCnclsSttusNm = preprocessingData(item, "cntrctCnclsSttusNm");          // 계약체결형태명
                    String cntrctCnclsMthdNm = preprocessingData(item, "cntrctCnclsMthdNm");            // 계약체결방법명
                    String lngtrmCtnuDivNm = preprocessingData(item, "lngtrmCtnuDivNm");                // 장기계속구분명
                    String cmmnCntrctYn = preprocessingData(item, "cmmnCntrctYn");                      // 공동계약여부
                    String cntrctCnclsDate = preprocessingData(item, "cntrctCnclsDate");                // 계약체결일자
                    String cntrctPrd = preprocessingData(item, "cntrctPrd");                            // 계약기간
                    String cntrctAmt = preprocessingData(item, "cntrctAmt");                            // 계약금액
                    String ttalCntrctAmt = preprocessingData(item, "ttalCntrctAmt");                    // 총계약금액
                    String cntrctInfoUrl = preprocessingData(item, "cntrctInfoUrl");                    // 계약정보URL
                    String bidNtceNo = preprocessingData(item, "bidNtceNo");                            // 입찰공고번호
                    String bidNtceOrd = preprocessingData(item, "bidNtceOrd");                          // 입찰공고차수
                    String bidNtceNm = preprocessingData(item, "bidNtceNm");                            // 입찰공고명
                    String opengDate = preprocessingData(item, "opengDate");                            // 개찰일자
                    String opengTm = preprocessingData(item, "opengTm");                                // 개찰시각
                    String rsrvtnPrce = preprocessingData(item, "rsrvtnPrce");                          // 예정가격
                    String prvtcntrctRsn = preprocessingData(item, "prvtcntrctRsn");                    // 수의계약사유
                    String bidNtceUrl = preprocessingData(item, "bidNtceUrl");                          // 입찰공고URL
                    String cntrctInsttDivNm = preprocessingData(item, "cntrctInsttDivNm");              // 계약기관구분명
                    String cntrctInsttNm = preprocessingData(item, "cntrctInsttNm");                    // 계약기관명
                    String cntrctInsttCd = preprocessingData(item, "cntrctInsttCd");                    // 계약기관코드
                    String cntrctInsttChrgDeptNm = preprocessingData(item, "cntrctInsttChrgDeptNm");    // 계약기관담당부서명
                    String cntrctInsttOfclNm = preprocessingData(item, "cntrctInsttOfclNm");            // 계약기관담당자명
                    String cntrctInsttOfclTel = preprocessingData(item, "cntrctInsttOfclTel");          // 계약기관담당자전화번호
                    String cntrctInsttOfcl = preprocessingData(item, "cntrctInsttOfcl");                // 계약기관담당자이메일주소
                    String dmndInsttDivNm = preprocessingData(item, "dmndInsttDivNm");                  // 수요기관구분명
                    String dmndInsttNm = preprocessingData(item, "dmndInsttNm");                        // 수요기관명
                    String dmndInsttCd = preprocessingData(item, "dmndInsttCd");                        // 수요기관코드
                    String dmndInsttOfclDeptNm = preprocessingData(item, "dmndInsttOfclDeptNm");        // 수요기관담당자부서명
                    String dmndInsttOfclNm = preprocessingData(item, "dmndInsttOfclNm");                // 수요기관담당자명
                    String dmndInsttOfclTel = preprocessingData(item, "dmndInsttOfclTel");              // 수요기관담당자전화번호
                    String dmndInsttOfclEmailAdrs = preprocessingData(item, "dmndInsttOfclEmailAdrs");  // 수요기관담당자이메일주소
                    String rprsntCorpNm = preprocessingData(item, "rprsntCorpNm");                      // 대표업체명
                    String dmstcCorpYn = preprocessingData(item, "dmstcCorpYn");                        // 국내업체여부
                    String rprsntCorpCeoNm = preprocessingData(item, "rprsntCorpCeoNm");                // 대표업체대표자명
                    String rprsntCorpOfclNm = preprocessingData(item, "rprsntCorpOfclNm");              // 대표업체담당자명
                    String rprsntCorpBizrno = preprocessingData(item, "rprsntCorpBizrno");              // 대표업체사업자등록번호
                    String rprsntCorpAdrs = preprocessingData(item, "rprsntCorpAdrs");                  // 대표업체주소
                    String rprsntCorpContactTel = preprocessingData(item, "rprsntCorpContactTel");      // 대표업체연락전화번호
                    String dataBssDate = preprocessingData(item, "dataBssDate");                        // 데이터기준일자

                    item.addProperty("cntrctNo", cntrctNo);
                    item.addProperty("untyCntrctNo", untyCntrctNo);
                    item.addProperty("cntrctOrd", cntrctOrd);
                    item.addProperty("cntrctNm", cntrctNm);
                    item.addProperty("bsnsDivNm", bsnsDivNm);
                    item.addProperty("cntrctCnclsSttusNm", cntrctCnclsSttusNm);
                    item.addProperty("cntrctCnclsMthdNm", cntrctCnclsMthdNm);
                    item.addProperty("lngtrmCtnuDivNm", lngtrmCtnuDivNm);
                    item.addProperty("cmmnCntrctYn", cmmnCntrctYn);
                    item.addProperty("cntrctCnclsDate", cntrctCnclsDate);
                    item.addProperty("cntrctPrd", cntrctPrd);
                    item.addProperty("cntrctAmt", cntrctAmt);
                    item.addProperty("ttalCntrctAmt", ttalCntrctAmt);
                    item.addProperty("cntrctInfoUrl", cntrctInfoUrl);
                    item.addProperty("bidNtceNo", bidNtceNo);
                    item.addProperty("bidNtceOrd", bidNtceOrd);
                    item.addProperty("bidNtceNm", bidNtceNm);
                    item.addProperty("opengDate", opengDate);
                    item.addProperty("opengTm", opengTm);
                    item.addProperty("rsrvtnPrce", rsrvtnPrce);
                    item.addProperty("prvtcntrctRsn", prvtcntrctRsn);
                    item.addProperty("bidNtceUrl", bidNtceUrl);
                    item.addProperty("cntrctInsttDivNm", cntrctInsttDivNm);
                    item.addProperty("cntrctInsttNm", cntrctInsttNm);
                    item.addProperty("cntrctInsttCd", cntrctInsttCd);
                    item.addProperty("cntrctInsttChrgDeptNm", cntrctInsttChrgDeptNm);
                    item.addProperty("cntrctInsttOfclNm", cntrctInsttOfclNm);
                    item.addProperty("cntrctInsttOfclTel", cntrctInsttOfclTel);
                    item.addProperty("cntrctInsttOfcl", cntrctInsttOfcl);
                    item.addProperty("dmndInsttDivNm", dmndInsttDivNm);
                    item.addProperty("dmndInsttNm", dmndInsttNm);
                    item.addProperty("dmndInsttCd", dmndInsttCd);
                    item.addProperty("dmndInsttOfclDeptNm", dmndInsttOfclDeptNm);
                    item.addProperty("dmndInsttOfclNm", dmndInsttOfclNm);
                    item.addProperty("dmndInsttOfclTel", dmndInsttOfclTel);
                    item.addProperty("dmndInsttOfclEmailAdrs", dmndInsttOfclEmailAdrs);
                    item.addProperty("rprsntCorpNm", rprsntCorpNm);
                    item.addProperty("dmstcCorpYn", dmstcCorpYn);
                    item.addProperty("rprsntCorpCeoNm", rprsntCorpCeoNm);
                    item.addProperty("rprsntCorpOfclNm", rprsntCorpOfclNm);
                    item.addProperty("rprsntCorpBizrno", rprsntCorpBizrno);
                    item.addProperty("rprsntCorpAdrs", rprsntCorpAdrs);
                    item.addProperty("rprsntCorpContactTel", rprsntCorpContactTel);
                    item.addProperty("dataBssDate", dataBssDate);
                    resultJsonArr.add(item);

                    dataCount++;
                    System.out.println(dataCount + "/" + totalCount + "건");
                }
            }

            System.out.println();

            // 계약정보 DB INSERT
            insertCntrctInfo(resultJsonArr);

            LocalDateTime endTime = LocalDateTime.now();
            System.out.println("===== 나라장터 계약정보 데이터 수집 종료 " + endTime + " =====");

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
    public static JsonObject callApi(int pageNo, int numOfRows, String cntrctCnclsBgnDate, String cntrctCnclsEndDate) throws IOException {
        StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/1230000/ao/PubDataOpnStdService/getDataSetOpnStdCntrctInfo"); /*URL*/
        urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=odXIjL8t56rG0Fz1a69qgTTpLKxSPvIJG%2FlPU3bFsLOjdSAKcwHy8Wx0OCox8vLCtZEl6B9Jw%2BlWyoMylWEwsg%3D%3D"); /*Service Key*/
        urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode(String.valueOf(pageNo), "UTF-8")); /* 시군구코드 (5자리) */
        urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode(String.valueOf(numOfRows), "UTF-8")); /* 시군구코드 (5자리) */
        urlBuilder.append("&" + URLEncoder.encode("type","UTF-8") + "=" + URLEncoder.encode("json", "UTF-8")); /* 시군구코드 (5자리) */
        urlBuilder.append("&" + URLEncoder.encode("cntrctCnclsBgnDate","UTF-8") + "=" + URLEncoder.encode(cntrctCnclsBgnDate, "UTF-8")); /* 시군구코드 (5자리) */
        urlBuilder.append("&" + URLEncoder.encode("cntrctCnclsEndDate","UTF-8") + "=" + URLEncoder.encode(cntrctCnclsEndDate, "UTF-8")); /* 시군구코드 (5자리) */

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

            String sql = "INSERT INTO OPN_STD_CNTRCT_INFO (CNTRCT_NO," +
                                                        "UNTY_CNTRCT_NO," +
                                                        "CNTRCT_ORD," +
                                                        "CNTRCT_NM," +
                                                        "BSNS_DIV_NM," +
                                                        "CNTRCT_CNCLS_STTUS_NM," +
                                                        "CNTRCT_CNCLS_MTHD_NM," +
                                                        "LNGTRM_CTNU_DIV_NM," +
                                                        "CMMN_CNTRCT_YN," +
                                                        "CNTRCT_CNCLS_DATE," +
                                                        "CNTRCT_PRD," +
                                                        "CNTRCT_AMT," +
                                                        "TTAL_CNTRCT_AMT," +
                                                        "CNTRCT_INFO_URL," +
                                                        "BID_NTCE_NO," +
                                                        "BID_NTCE_ORD," +
                                                        "BID_NTCE_NM," +
                                                        "OPENG_DATE," +
                                                        "OPENG_TM," +
                                                        "RSRVTN_PRCE," +
                                                        "PRVTCNTRCT_RSN," +
                                                        "BID_NTCE_URL," +
                                                        "CNTRCT_INSTT_DIV_NM," +
                                                        "CNTRCT_INSTT_NM," +
                                                        "CNTRCT_INSTT_CD," +
                                                        "CNTRCT_INSTT_CHRG_DEPT_NM," +
                                                        "CNTRCT_INSTT_OFCL_NM," +
                                                        "CNTRCT_INSTT_OFCL_TEL," +
                                                        "CNTRCT_INSTT_OFCL," +
                                                        "DMND_INSTT_DIV_NM," +
                                                        "DMND_INSTT_NM," +
                                                        "DMND_INSTT_CD," +
                                                        "DMND_INSTT_OFCL_DEPT_NM," +
                                                        "DMND_INSTT_OFCL_NM," +
                                                        "DMND_INSTT_OFCL_TEL," +
                                                        "DMND_INSTT_OFCL_EMAIL_ADRS," +
                                                        "RPRSNT_CORP_NM," +
                                                        "DMSTC_CORP_YN," +
                                                        "RPRSNT_CORP_CEO_NM," +
                                                        "RPRSNT_CORP_OFCL_NM," +
                                                        "RPRSNT_CORP_BIZRNO," +
                                                        "RPRSNT_CORP_ADRS," +
                                                        "RPRSNT_CORP_CONTACT_TEL," +
                                                        "DATA_BSS_DATE," +
                                                        "DEL_YN," +
                                                        "FRST_RGSR_DTL_DTTM," +
                                                        "LAST_CHNG_DTL_DTTM" +
                                                        ")" +
                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'N', '" + now + "', '" + now + "')";
            PreparedStatement ps = connection.prepareStatement(sql);

            System.out.println("===== 계약정보 DB INSERT =====");

            int cnt = 1;

            for (JsonElement e : resultJsonArr) {
                String cntrctNo = e.getAsJsonObject().get("cntrctNo").getAsString();                                // 계약번호
                String untyCntrctNo = e.getAsJsonObject().get("untyCntrctNo").getAsString();                        // 통합계약번호
                String cntrctOrd = e.getAsJsonObject().get("cntrctOrd").getAsString();                              // 계약차수
                String cntrctNm = e.getAsJsonObject().get("cntrctNm").getAsString();                                // 계약명
                String bsnsDivNm = e.getAsJsonObject().get("bsnsDivNm").getAsString();                              // 업무구분명
                String cntrctCnclsSttusNm = e.getAsJsonObject().get("cntrctCnclsSttusNm").getAsString();            // 계약체결형태명
                String cntrctCnclsMthdNm = e.getAsJsonObject().get("cntrctCnclsMthdNm").getAsString();              // 계약체결방법명
                String lngtrmCtnuDivNm = e.getAsJsonObject().get("lngtrmCtnuDivNm").getAsString();                  // 장기계속구분명
                String cmmnCntrctYn = e.getAsJsonObject().get("cmmnCntrctYn").getAsString();                        // 공동계약여부
                String cntrctCnclsDate = e.getAsJsonObject().get("cntrctCnclsDate").getAsString();                  // 계약체결일자
                String cntrctPrd = e.getAsJsonObject().get("cntrctPrd").getAsString();;                             // 계약기간
                String cntrctAmt = e.getAsJsonObject().get("cntrctAmt").getAsString();                              // 계약금액
                String ttalCntrctAmt = e.getAsJsonObject().get("ttalCntrctAmt").getAsString();                      // 총계약금액
                String cntrctInfoUrl = e.getAsJsonObject().get("cntrctInfoUrl").getAsString();                      // 계약정보URL
                String bidNtceNo = e.getAsJsonObject().get("bidNtceNo").getAsString();                              // 입찰공고번호
                String bidNtceOrd = e.getAsJsonObject().get("bidNtceOrd").getAsString();                            // 입찰공고차수
                String bidNtceNm = e.getAsJsonObject().get("bidNtceNm").getAsString();                              // 입찰공고명
                String opengDate = e.getAsJsonObject().get("opengDate").getAsString();                              // 개찰일자
                String opengTm = e.getAsJsonObject().get("opengTm").getAsString();                                  // 개찰시각
                String rsrvtnPrce = e.getAsJsonObject().get("rsrvtnPrce").getAsString();                            // 예정가격
                String prvtcntrctRsn = e.getAsJsonObject().get("prvtcntrctRsn").getAsString();                      // 수의계약사유
                String bidNtceUrl = e.getAsJsonObject().get("bidNtceUrl").getAsString();                            // 입찰공고URL
                String cntrctInsttDivNm = e.getAsJsonObject().get("cntrctInsttDivNm").getAsString();                // 계약기관구분명
                String cntrctInsttNm = e.getAsJsonObject().get("cntrctInsttNm").getAsString();                      // 계약기관명
                String cntrctInsttCd = e.getAsJsonObject().get("cntrctInsttCd").getAsString();                      // 계약기관코드
                String cntrctInsttChrgDeptNm = e.getAsJsonObject().get("cntrctInsttChrgDeptNm").getAsString();      // 계약기관담당부서명
                String cntrctInsttOfclNm = e.getAsJsonObject().get("cntrctInsttOfclNm").getAsString();              // 계약기관담당자명
                String cntrctInsttOfclTel = e.getAsJsonObject().get("cntrctInsttOfclTel").getAsString();            // 계약기관담당자전화번호
                String cntrctInsttOfcl = e.getAsJsonObject().get("cntrctInsttOfcl").getAsString();                  // 계약기관담당자이메일주소
                String dmndInsttDivNm = e.getAsJsonObject().get("dmndInsttDivNm").getAsString();                    // 수요기관구분명
                String dmndInsttNm = e.getAsJsonObject().get("dmndInsttNm").getAsString();                          // 수요기관명
                String dmndInsttCd = e.getAsJsonObject().get("dmndInsttCd").getAsString();                          // 수요기관코드
                String dmndInsttOfclDeptNm = e.getAsJsonObject().get("dmndInsttOfclDeptNm").getAsString();          // 수요기관담당자부서명
                String dmndInsttOfclNm = e.getAsJsonObject().get("dmndInsttOfclNm").getAsString();                  // 수요기관담당자명
                String dmndInsttOfclTel = e.getAsJsonObject().get("dmndInsttOfclTel").getAsString();;               // 수요기관담당자전화번호
                String dmndInsttOfclEmailAdrs = e.getAsJsonObject().get("dmndInsttOfclEmailAdrs").getAsString();    // 수요기관담당자이메일주소
                String rprsntCorpNm = e.getAsJsonObject().get("rprsntCorpNm").getAsString();                        // 대표업체명
                String dmstcCorpYn = e.getAsJsonObject().get("dmstcCorpYn").getAsString();                          // 국내업체여부
                String rprsntCorpCeoNm = e.getAsJsonObject().get("rprsntCorpCeoNm").getAsString();                  // 대표업체대표자명
                String rprsntCorpOfclNm = e.getAsJsonObject().get("rprsntCorpOfclNm").getAsString();                // 대표업체담당자명
                String rprsntCorpBizrno = e.getAsJsonObject().get("rprsntCorpBizrno").getAsString();                // 대표업체사업자등록번호
                String rprsntCorpAdrs = e.getAsJsonObject().get("rprsntCorpAdrs").getAsString();                    // 대표업체주소
                String rprsntCorpContactTel = e.getAsJsonObject().get("rprsntCorpContactTel").getAsString();        // 대표업체연락전화번호
                String dataBssDate = e.getAsJsonObject().get("dataBssDate").getAsString();                          // 데이터기준일자

                ps.setString(1, cntrctNo);
                ps.setString(2, untyCntrctNo);
                ps.setString(3, cntrctOrd);
                ps.setString(4, cntrctNm);
                ps.setString(5, bsnsDivNm);
                ps.setString(6, cntrctCnclsSttusNm);
                ps.setString(7, cntrctCnclsMthdNm);
                ps.setString(8, lngtrmCtnuDivNm);
                ps.setString(9, cmmnCntrctYn);
                ps.setString(10, cntrctCnclsDate);
                ps.setString(11, cntrctPrd);
                ps.setString(12, cntrctAmt);
                ps.setString(13, ttalCntrctAmt);
                ps.setString(14, cntrctInfoUrl);
                ps.setString(15, bidNtceNo);
                ps.setString(16, bidNtceOrd);
                ps.setString(17, bidNtceNm);
                ps.setString(18, opengDate);
                ps.setString(19, opengTm);
                ps.setString(20, rsrvtnPrce);
                ps.setString(21, prvtcntrctRsn);
                ps.setString(22, bidNtceUrl);
                ps.setString(23, cntrctInsttDivNm);
                ps.setString(24, cntrctInsttNm);
                ps.setString(25, cntrctInsttCd);
                ps.setString(26, cntrctInsttChrgDeptNm);
                ps.setString(27, cntrctInsttOfclNm);
                ps.setString(28, cntrctInsttOfclTel);
                ps.setString(29, cntrctInsttOfcl);
                ps.setString(30, dmndInsttDivNm);
                ps.setString(31, dmndInsttNm);
                ps.setString(32, dmndInsttCd);
                ps.setString(33, dmndInsttOfclDeptNm);
                ps.setString(34, dmndInsttOfclNm);
                ps.setString(35, dmndInsttOfclTel);
                ps.setString(36, dmndInsttOfclEmailAdrs);
                ps.setString(37, rprsntCorpNm);
                ps.setString(38, dmstcCorpYn);
                ps.setString(39, rprsntCorpCeoNm);
                ps.setString(40, rprsntCorpOfclNm);
                ps.setString(41, rprsntCorpBizrno);
                ps.setString(42, rprsntCorpAdrs);
                ps.setString(43, rprsntCorpContactTel);
                ps.setString(44, dataBssDate);

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
        String fileName = "계약정보_" + date + "~" + date + ".csv";
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