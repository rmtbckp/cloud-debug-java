/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.cdbg.debuglets.java;

import static com.google.devtools.cdbg.debuglets.java.AgentLogger.infofmt;
import static com.google.devtools.cdbg.debuglets.java.AgentLogger.warnfmt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Finds, loads and parses debugger configuration file.
 */
public class YamlConfigParser {
  /**
   * A set of blacklist patterns found in the parsed config.
   */
  private String[] blacklistPatterns;

  /**
   * A set of whitelist patterns found in the parsed config.
   */
  private String[] whitelistPatterns;

  /**
   * Parses the given InputStream as YAML with an expected structure.
   *
   * Throws YamlConfigParserException if the stream can not be parsed or
   * if it has an unexpected structure.
   *
   * An example of legal structure would be:
   *
   * blacklist:
   *   - com.secure.*
   *   - com.foo.MyClass
   * whitelist:
   *   - *
   *
   * An empty config is also legal.
   */
  public YamlConfigParser(String yamlConfig) throws YamlConfigParserException {
    blacklistPatterns = new String[0];
    whitelistPatterns = new String[0];

    try (InputStream inputStream =
         new ByteArrayInputStream(yamlConfig.getBytes(StandardCharsets.UTF_8))) {
      parseYaml(inputStream);
    } catch (YamlConfigParserException e) {
      warnfmt("%s", e.toString());
      throw e;
    } catch (IOException e) {
      // IOException is not expected on a string reader, but the API contract
      // requires we catch it anyway.
      warnfmt("%s", e.toString());
      throw new YamlConfigParserException("IOException: " + e.toString());
    }

    infofmt("Config Load OK. Added %d blacklist and %d whitelist patterns",
            blacklistPatterns.length,
            whitelistPatterns.length);
  }

  /**
   * Returns blacklists found in config.
   */
  public String[] getBlacklistPatterns() {
    return blacklistPatterns;
  }

  /**
   * Returns whitelists found in config.
   */
  public String[] getWhitelistPatterns() {
    return whitelistPatterns;
  }

  /**
   * Checks a pattern for legality.
   *
   * For example, a pattern can not contain whitespace or logical
   * characters, such as "+".
   */
  private boolean isLegalPattern(String pattern) {
    for (char c : pattern.toCharArray()) {
      if (c != '*' && c != '.' && !Character.isJavaIdentifierPart(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Adds a list of patterns to the given set.
   *
   * Throws YamlConfigParserException if the pattern list contains
   * non-strings or illegal pattern characters.
   */
  private void addPatternsToSet(Set<String> set, List<Object> patterns)
      throws YamlConfigParserException {
    for (Object listElement : patterns) {
      if (!(listElement instanceof String)) {
        throw new YamlConfigParserException(
            "Unexpected non-string content: " + listElement);
      }

      String pattern = (String) listElement;
      if (!isLegalPattern(pattern)) {
        throw new YamlConfigParserException(
            "Illegal pattern specified: " + pattern);
      }

      set.add(pattern);
    }
  }

  /**
   * Parses the given InputStream as YAML with an expected structure.
   *
   * Throws YamlConfigParserException if the stream can not be parsed or
   * if it has an unexpected structure.
   */
  private void parseYaml(InputStream yamlConfig) throws YamlConfigParserException {
    Yaml yaml = new Yaml();
    Set<String> blacklistPatternSet = new HashSet<>();
    Set<String> whitelistPatternSet = new HashSet<>();

    try {
      // Note that the cast below could be unsafe, but the code that follows
      // checks types before acting on them to avoid casting errors.
      Map<String, Object> data =
          (Map<String, Object>) yaml.loadAs(yamlConfig, Map.class);

      if (data == null) {
        // Nothing was loaded
        return;
      }

      for (Map.Entry<String, Object> entry : data.entrySet()) {
        if (!(entry.getValue() instanceof List)) {
          throw new YamlConfigParserException(
              "Unexpected non-list content for: " + entry.getKey());
        }

        List<Object> value = (List<Object>) entry.getValue();

        switch (entry.getKey()) {
          case "blacklist":
            addPatternsToSet(blacklistPatternSet, value);
            break;
          case "whitelist":
            addPatternsToSet(whitelistPatternSet, value);
            break;
          default:
            throw new YamlConfigParserException(
                "Unrecognized key in config: " + entry.getKey());
        }
      }
    } catch (YAMLException e) {
      // Yaml failed to parse
      throw new YamlConfigParserException(e.toString());
    }

    blacklistPatterns = blacklistPatternSet.toArray(new String[0]);
    whitelistPatterns = whitelistPatternSet.toArray(new String[0]);
  }
}