package network.datahop;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.tuweni.devp2p.EthereumNodeRecord;
import org.apache.tuweni.io.Base64URLSafe;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONException;
import org.json.JSONObject;

public class DnsListParser {

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  private static JSONObject readJsonFromFile(String filename) throws IOException, JSONException {
    BufferedReader rd = null;

    try {

      rd = new BufferedReader(new FileReader(filename));
      String jsonText = readAll(rd);
      // System.out.println("JSON string "+jsonText);
      JSONObject json = new JSONObject(jsonText);
      return json;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (org.json.JSONException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
    InputStream is = new URL(url).openStream();
    try {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      String jsonText = readAll(rd);
      // System.out.println("JSON string "+jsonText);
      JSONObject json = new JSONObject(jsonText);
      return json;
    } finally {
      is.close();
    }
  }

  private static String parseIp(String address) {

    // iterate over each octet
    String[] parts = address.split(Pattern.quote("."));
    String ip = "";
    for (int i = 0; i < 3; i++) {
      // shift the previously parsed bits over by 1 byte
      ip = ip + parts[i];
      if (i < 2) ip = ip + ".";
    }
    return ip;
  }

  public static HashMap<String, List<String>> getAddressesMapFromUrl(String url) {
    HashMap<String, List<String>> ipMap = new HashMap<String, List<String>>();

    try {
      JSONObject json = readJsonFromUrl(url);

      Security.addProvider(new BouncyCastleProvider());
      int i = 0;
      for (String keyStr : json.keySet()) {
        JSONObject json2 = json.getJSONObject(keyStr);

        //System.out.println("Record key: " + keyStr);
        EthereumNodeRecord enr =
            EthereumNodeRecord.fromRLP(
                (Base64URLSafe.decode(json2.getString("record").substring(4))));

        String addr = enr.ip().toString().substring(1);
        String subAddr = parseIp(addr);
        if (ipMap.get(subAddr) == null) {
          List<String> l = new ArrayList<>();
          l.add(addr);
          ipMap.put(subAddr, l);

        } else {
          ipMap.get(subAddr).add(addr);
        }
      }

    } catch (Exception e) {
      System.err.println("Exception " + e);
      return null;
    }

    return ipMap;
  }

  public static void main(String[] args) {
    // TODO Auto-generated method stub

    HashMap<String, List<String>> ipMap =
        getAddressesMapFromUrl(
            "https://raw.githubusercontent.com/ethereum/discv4-dns-lists/master/all.json");

    for (String net : ipMap.keySet()) {

      System.out.print("Network " + net + " ");
      for (String addr : ipMap.get(net)) {
        System.out.print(addr + " ");
      }
      System.out.println();
    }
  }
}
