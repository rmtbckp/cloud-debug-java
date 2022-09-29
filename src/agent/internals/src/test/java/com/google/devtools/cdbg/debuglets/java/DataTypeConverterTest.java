/*
 * Copyright 2022 Google LLC
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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class DataTypeConverterTest {

  @Test
  public void printHexBinaryWorksAsExpected() {
    assertThat(DataTypeConverter.printHexBinary(new byte[] {})).isEqualTo("");
    assertThat(DataTypeConverter.printHexBinary(new byte[] {0x00})).isEqualTo("00");
    assertThat(
            DataTypeConverter.printHexBinary(
                new byte[] {
                  0x01,
                  0x23,
                  0x45,
                  0x67,
                  (byte) 0x89,
                  (byte) 0xAB,
                  (byte) 0xCD,
                  (byte) 0xEF,
                  (byte) 0xFE,
                  (byte) 0xDC,
                  (byte) 0xBA,
                  (byte) 0x98,
                  0x76,
                  0x54,
                  0x32,
                  0x10
                }))
        .isEqualTo("0123456789ABCDEFFEDCBA9876543210");
  }
}
