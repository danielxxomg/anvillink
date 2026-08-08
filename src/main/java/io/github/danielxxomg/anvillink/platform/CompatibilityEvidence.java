// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evidence-gated compatibility matrix. Mandatory rows gate certification; probe rows (Paper 26.x)
 * are informational. Every row is {distribution, version, build, serverSha256, jdkMajor, testSuite,
 * result}. See compatibility/evidence.json and platform-compatibility spec.
 */
public final class CompatibilityEvidence {

  private CompatibilityEvidence() {}

  public record Row(
      String distribution,
      String version,
      String build,
      String serverSha256,
      int jdkMajor,
      String testSuite,
      String result) {}

  public static List<Row> read(Path evidenceFile) throws IOException {
    if (evidenceFile == null || !Files.isRegularFile(evidenceFile)) {
      throw new IllegalStateException("evidence missing: " + evidenceFile);
    }
    String text = Files.readString(evidenceFile);
    String trimmed = text.trim();
    if (trimmed.isEmpty() || trimmed.equals("[]")) {
      return List.of();
    }
    // Extract each {...} object.
    List<Row> rows = new ArrayList<>();
    Pattern obj = Pattern.compile("\\{[^}]*\\}", Pattern.DOTALL);
    Matcher m = obj.matcher(text);
    while (m.find()) {
      String o = m.group();
      String distribution = extractString(o, "distribution");
      String version = extractString(o, "version");
      String build = extractString(o, "build");
      String sha = extractString(o, "serverSha256");
      int jdk = extractInt(o, "jdkMajor");
      String suite = extractString(o, "testSuite");
      String result = extractString(o, "result");
      if (distribution == null
          || version == null
          || build == null
          || sha == null
          || suite == null
          || result == null) {
        throw new IllegalStateException("evidence row missing required field: " + o);
      }
      rows.add(new Row(distribution, version, build, sha, jdk, suite, result));
    }
    if (rows.isEmpty()) {
      throw new IllegalStateException("evidence contains no rows: " + evidenceFile);
    }
    return List.copyOf(rows);
  }

  public static boolean allMandatoryPass(List<Row> rows) {
    return paperCertified(rows) && spigotVerified(rows) && purpurVerified(rows);
  }

  public static boolean paperCertified(List<Row> rows) {
    return hasPass(rows, "Paper", "1.18.2", 17)
        && hasPass(rows, "Paper", "1.20.6", 21)
        && hasPass(rows, "Paper", "1.21.11", 21);
  }

  public static boolean spigotVerified(List<Row> rows) {
    return hasPass(rows, "Spigot", "1.20.6", 21);
  }

  public static boolean purpurVerified(List<Row> rows) {
    return hasPass(rows, "Purpur", "1.20.6", 21);
  }

  public static boolean paper26Certified(List<Row> rows) {
    return hasPass(rows, "Paper", "26.2", 25);
  }

  public static String paperTier(List<Row> rows) {
    return paperCertified(rows) ? "certified" : "uncertified";
  }

  public static String spigotTier(List<Row> rows) {
    return spigotVerified(rows) ? "verified" : "uncertified";
  }

  public static String purpurTier(List<Row> rows) {
    return purpurVerified(rows) ? "verified" : "uncertified";
  }

  public static String paper26Tier(List<Row> rows) {
    return paper26Certified(rows) ? "certified" : "uncertified";
  }

  public static String foliaTier() {
    return "experimental";
  }

  public static String foliaTier(List<Row> rows) {
    return "experimental";
  }

  private static boolean hasPass(List<Row> rows, String dist, String ver, int jdk) {
    for (Row r : rows) {
      if (r.distribution().equals(dist)
          && r.version().equals(ver)
          && r.jdkMajor() == jdk
          && "pass".equals(r.result())) {
        return true;
      }
    }
    return false;
  }

  private static String extractString(String obj, String field) {
    Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"");
    Matcher m = p.matcher(obj);
    return m.find() ? m.group(1) : null;
  }

  private static int extractInt(String obj, String field) {
    Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(\\d+)");
    Matcher m = p.matcher(obj);
    if (!m.find()) {
      throw new IllegalStateException("field " + field + " missing or not an int in " + obj);
    }
    return Integer.parseInt(m.group(1));
  }
}
